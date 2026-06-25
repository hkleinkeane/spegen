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

// Auto-translation of item labels via Vercel proxy.
import { Platform } from 'react-native';

// Web via proxy, native calls Google directly.
const WEB_PROXY = process.env.EXPO_PUBLIC_TRANSLATE_PROXY || '/api/translate';
const GTX = 'https://translate.googleapis.com/translate_a/single';
const TIMEOUT_MS = 6000;

// Translation cache key.
const cache = new Map<string, string>();
const inflight = new Set<string>();

export function translationKey(text: string, targetLang: string): string {
  return `${targetLang}|${text.trim().toLowerCase()}`;
}

export function cachedTranslation(text: string, targetLang: string): string | undefined {
  return cache.get(translationKey(text, targetLang));
}

// Parse gtx response.
function parseGtx(json: unknown): string {
  if (!Array.isArray(json)) return '';
  const segments = json[0];
  if (!Array.isArray(segments)) return '';
  let out = '';
  for (const seg of segments) {
    if (Array.isArray(seg) && typeof seg[0] === 'string') out += seg[0];
  }
  return out.trim();
}

export async function fetchTranslation(text: string, targetLang: string): Promise<string | null> {
  const source = text.trim();
  if (!source || !targetLang) return null;
  const key = translationKey(source, targetLang);
  const hit = cache.get(key);
  if (hit !== undefined) return hit;
  if (inflight.has(key)) return null;
  inflight.add(key);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res =
      Platform.OS === 'web'
        ? await fetch(WEB_PROXY, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ q: source, sl: 'auto', tl: targetLang }),
            signal: controller.signal,
          })
        : await fetch(
            `${GTX}?client=gtx&sl=auto&tl=${encodeURIComponent(targetLang)}&dt=t&q=${encodeURIComponent(source)}`,
            { signal: controller.signal }
          );
    if (!res.ok) return null;
    const json = (await res.json()) as unknown;
    const translated = parseGtx(json);
    if (translated) {
      cache.set(key, translated);
      return translated;
    }
    return null;
  } catch {
    return null; // network error / abort / parse failure: keep the original label
  } finally {
    clearTimeout(timer);
    inflight.delete(key);
  }
}
