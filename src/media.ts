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

// Custom media storage helpers.
import { Directory, File, Paths } from 'expo-file-system';

const IMAGE_DIR = 'custom_images';
const AUDIO_DIR = 'custom_audio';

function ensureDir(name: string): Directory {
  const dir = new Directory(Paths.document, name);
  if (!dir.exists) dir.create({ intermediates: true });
  return dir;
}

function asFile(src: File | string): File {
  return typeof src === 'string' ? new File(src) : src;
}

// File extension from URI.
function extFromUri(uri: string, fallback: string): string {
  const q = uri.split('?')[0];
  const dot = q.lastIndexOf('.');
  const slash = q.lastIndexOf('/');
  if (dot > slash && dot >= 0 && dot < q.length - 1) {
    const ext = q.slice(dot + 1).toLowerCase();
    if (/^[a-z0-9]{1,5}$/.test(ext)) return ext;
  }
  return fallback;
}

async function copyInto(dirName: string, src: File | string, key: string, fallbackExt: string): Promise<string> {
  const source = asFile(src);
  const dir = ensureDir(dirName);
  const ext = extFromUri(source.uri, fallbackExt);
  const dest = new File(dir, `${key}.${ext}`);
  try {
    if (dest.exists) dest.delete();
  } catch {
  }
  try {
    await source.copy(dest);
  } catch (copyErr) {
    // Fallback: read bytes and rewrite for SAF URIs.
    try {
      if (dest.exists) dest.delete();
    } catch {
      // ignore: nothing to delete
    }
    const b64 = await source.base64();
    dest.create({ overwrite: true, intermediates: true });
    dest.write(b64, { encoding: 'base64' });
  }
  return dest.uri;
}

export async function copyImageToStorage(src: File | string, key: string): Promise<string> {
  try {
    return await copyInto(IMAGE_DIR, src, key, 'png');
  } catch (e) {
    console.warn('copyImageToStorage failed', e);
    return '';
  }
}

export async function copyAudioToStorage(src: File | string, key: string): Promise<string> {
  try {
    return await copyInto(AUDIO_DIR, src, key, 'm4a');
  } catch (e) {
    console.warn('copyAudioToStorage failed', e);
    return '';
  }
}

// True for local file paths.
export function isLocalMediaPath(path: string): boolean {
  return path.startsWith('file:') || path.startsWith('/');
}
