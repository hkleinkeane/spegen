/*
 * SpeGen — a free, open-source AAC (Augmentative and Alternative Communication) app.
 * Copyright (C) 2026 Harper Klein Keane
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <https://www.gnu.org/licenses/>.
 */

/*
 * Full backup archive.
 *
 * Produces a real, compressed ZIP with the extension `.spegen`: `state.json` (the persisted state,
 * with custom media paths rewritten to archive-relative "custom_images/<name>" /
 * "custom_audio/<name>") plus the actual image and audio files stored under those folders.
 * Everything travels in one package.
 *
 * Expo's managed runtime has no native ZIP, so we zip in pure JS with jszip (works in Expo Go).
 * The bytes are written/read through expo-file-system as base64.
 */
import { Directory, File, Paths } from 'expo-file-system';
// SAF (Android "Save to a chosen folder") lives only in the legacy module in SDK 56.
import * as LegacyFS from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';
import { Platform } from 'react-native';
import JSZip from 'jszip';
import { defaultPersistedState, padMenu, type MenuTemplate, type PersistedState } from './types';
import { loadPersistedState, savePersistedState } from './persistence';
import { isLocalMediaPath } from './media';

const IMAGE_DIR = 'custom_images';
const AUDIO_DIR = 'custom_audio';

function basename(uri: string): string {
  try {
    return new File(uri).name;
  } catch {
    const q = uri.split('?')[0];
    return q.slice(q.lastIndexOf('/') + 1);
  }
}

function ensureDir(name: string): Directory {
  const dir = new Directory(Paths.document, name);
  if (!dir.exists) dir.create({ intermediates: true });
  return dir;
}

// Rewrite a menu's referenced local custom paths to archive-relative keys (by file name) before
// serialising state.json.
function toArchivePaths(menu: MenuTemplate): MenuTemplate {
  return {
    ...menu,
    custom_image_paths: menu.custom_image_paths.map((p) =>
      p && isLocalMediaPath(p) ? `${IMAGE_DIR}/${basename(p)}` : p
    ),
    custom_audio_paths: menu.custom_audio_paths.map((p) =>
      p && isLocalMediaPath(p) ? `${AUDIO_DIR}/${basename(p)}` : p
    ),
  };
}

// Builds the .spegen archive (a JSZip) from the persisted state + media directories.
async function buildArchiveZip(): Promise<JSZip> {
  const base = (await loadPersistedState()) ?? defaultPersistedState();
  const state: PersistedState = { ...base, menu_list: base.menu_list.map(toArchivePaths) };

  const zip = new JSZip();
  zip.file('state.json', JSON.stringify(state));

  // Add every file in the two media dirs. Skipped on
  // web, which has no on-disk media dirs and where expo-file-system's FS APIs aren't available.
  if (Platform.OS !== 'web') {
    for (const dirName of [IMAGE_DIR, AUDIO_DIR]) {
      const dir = new Directory(Paths.document, dirName);
      if (!dir.exists) continue;
      for (const entry of dir.list()) {
        if (entry instanceof File) {
          try {
            zip.file(`${dirName}/${entry.name}`, await entry.base64(), { base64: true });
          } catch {
            // skip an unreadable file rather than aborting the whole export
          }
        }
      }
    }
  }

  return zip;
}

async function buildArchiveBase64(): Promise<string> {
  return (await buildArchiveZip()).generateAsync({ type: 'base64', compression: 'DEFLATE' });
}

// Web "Save backup": build the archive as a Blob and trigger a browser download (acts like clicking
// a download link on a website). NEW — the native equivalent is saveArchiveToDevice() (Android SAF).
export async function downloadArchiveWeb(): Promise<void> {
  const zip = await buildArchiveZip();
  // Cast away the DOM 'Blob' return type so this compiles without the DOM lib in tsconfig.
  const blob = await (
    zip as unknown as { generateAsync: (o: { type: string; compression: string }) => Promise<unknown> }
  ).generateAsync({ type: 'blob', compression: 'DEFLATE' });
  // Web-only globals (document/URL), typed loosely for the same reason.
  const g = globalThis as any; // eslint-disable-line @typescript-eslint/no-explicit-any
  const url: string = g.URL.createObjectURL(blob);
  const stamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  const a = g.document.createElement('a');
  a.href = url;
  a.download = `spegen-backup-${stamp}.spegen`;
  g.document.body.appendChild(a);
  a.click();
  a.remove();
  g.URL.revokeObjectURL(url);
}

// Writes the archive to a transient cache file and returns it (the caller shares the uri).
export async function exportArchiveFile(): Promise<File> {
  const b64 = await buildArchiveBase64();
  const stamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  const out = new File(Paths.cache, `spegen-backup-${stamp}.spegen`);
  try {
    if (out.exists) out.delete();
  } catch {
    // ignore: nothing to delete
  }
  out.create({ overwrite: true, intermediates: true });
  out.write(b64, { encoding: 'base64' });
  return out;
}

// iOS / generic native "Export backup": write the .spegen archive to a cache file, then open the OS
// share sheet so the user can save it (e.g. to Files) or send it. Returns false if sharing isn't
// available. (Android writes straight into a chosen folder via saveArchiveToDevice; web downloads.)
export async function shareArchiveFile(): Promise<boolean> {
  if (!(await Sharing.isAvailableAsync())) return false;
  const file = await exportArchiveFile();
  await Sharing.shareAsync(file.uri, {
    mimeType: 'application/octet-stream',
    dialogTitle: 'Export SpeGen backup',
    UTI: 'public.data',
  });
  return true;
}

// Android-only "Save to device": opens the Storage Access Framework folder picker and writes the
// .spegen archive directly into the chosen folder (vs only offering the share sheet). Returns the
// created document uri, or null if unsupported / the user cancelled the permission prompt.
// Note: SAF derives the file's extension from the mime type, so the saved name may not keep
// the exact ".spegen" suffix; the archive is still re-importable (Import accepts any file type).
export async function saveArchiveToDevice(): Promise<string | null> {
  if (Platform.OS !== 'android') return null;
  const SAF = LegacyFS.StorageAccessFramework;
  // Guard: the legacy SAF API lives in the older native FileSystem module; bail clearly if it
  // isn't present rather than throwing a cryptic "undefined is not a function".
  if (!SAF || typeof SAF.requestDirectoryPermissionsAsync !== 'function') {
    throw new Error('Saving to a folder is not available on this device/build.');
  }

  const perm = await SAF.requestDirectoryPermissionsAsync();
  if (!perm.granted || !perm.directoryUri) return null; // user cancelled / denied

  const b64 = await buildArchiveBase64();
  const stamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);

  // Wrap each SAF step so a native failure surfaces as a readable message (the cause of the
  // reported "expo throws an error when I click use this folder") instead of an uncaught crash.
  let uri: string;
  try {
    uri = await SAF.createFileAsync(
      perm.directoryUri,
      `spegen-backup-${stamp}.spegen`,
      'application/octet-stream'
    );
  } catch (e) {
    throw new Error(`Couldn't create the backup file in that folder (${String(e)}).`);
  }
  try {
    await SAF.writeAsStringAsync(uri, b64, { encoding: LegacyFS.EncodingType.Base64 });
  } catch (e) {
    // Best-effort cleanup of the empty/partial file created before the write failed.
    try {
      await LegacyFS.deleteAsync(uri, { idempotent: true });
    } catch {
      // ignore: nothing to clean up
    }
    throw new Error(`Couldn't write the backup to that folder (${String(e)}).`);
  }
  return uri;
}

// Restores boards, settings and custom media from a .spegen archive. Persists the result;
// the caller should re-run the store's init() to reload the board.
export async function importArchiveFile(file: File): Promise<void> {
  const zip = await JSZip.loadAsync(await file.base64(), { base64: true });

  const stateEntry = zip.file('state.json');
  if (!stateEntry) {
    throw new Error('That file is not a valid SpeGen backup (no state.json).');
  }
  const rawState = JSON.parse(await stateEntry.async('string')) as Partial<PersistedState>;
  if (!rawState || typeof rawState !== 'object' || !Array.isArray(rawState.menu_list)) {
    throw new Error('That file is not a valid SpeGen backup.');
  }

  // Collect the media entries first (jszip's forEach is synchronous), then extract them back
  // into the document dirs, remembering each new absolute uri.
  const mediaPaths: string[] = [];
  zip.forEach((path, entry) => {
    if (!entry.dir && (path.startsWith(`${IMAGE_DIR}/`) || path.startsWith(`${AUDIO_DIR}/`))) {
      mediaPaths.push(path);
    }
  });

  const written: Record<string, string> = {};
  const imageDir = ensureDir(IMAGE_DIR);
  const audioDir = ensureDir(AUDIO_DIR);
  for (const path of mediaPaths) {
    const entry = zip.file(path);
    if (!entry) continue;
    const slash = path.indexOf('/');
    const dirName = path.slice(0, slash);
    const fileName = path.slice(slash + 1);
    if (!fileName) continue;
    const dir = dirName === IMAGE_DIR ? imageDir : audioDir;
    const dest = new File(dir, fileName);
    try {
      if (dest.exists) dest.delete();
    } catch {
      // ignore: nothing to delete
    }
    try {
      dest.create({ overwrite: true, intermediates: true });
      dest.write(await entry.async('base64'), { encoding: 'base64' });
      written[path] = dest.uri;
    } catch {
      // skip a file we cannot write rather than failing the whole import
    }
  }

  // Rebuild a full persisted state (defaults <- archived state), resolving archive-relative
  // custom paths back to the freshly-written absolute uris.
  const merged: PersistedState = { ...defaultPersistedState(), ...rawState };
  merged.menu_list = (rawState.menu_list ?? []).map((m) => {
    const menu = padMenu(m);
    return {
      ...menu,
      custom_image_paths: menu.custom_image_paths.map((p) =>
        typeof p === 'string' && p.startsWith(`${IMAGE_DIR}/`) ? written[p] ?? '' : p
      ),
      custom_audio_paths: menu.custom_audio_paths.map((p) =>
        typeof p === 'string' && p.startsWith(`${AUDIO_DIR}/`) ? written[p] ?? '' : p
      ),
    };
  });

  await savePersistedState(merged);
}
