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

// OpenSymbols API v2 client.
import { Platform } from 'react-native';
import { SKIN_TONES } from './types';

// OpenSymbols secret lives server-side only.
const API_BASE =
  process.env.EXPO_PUBLIC_OPENSYMBOLS_PROXY ||
  (Platform.OS === 'web' ? '/api/opensymbols' : 'https://spegen.vercel.app/api/opensymbols');
const TOKEN_URL = `${API_BASE}/token`;
const SYMBOLS_URL = `${API_BASE}/symbols`;

export interface ApiSymbolResponse {
  id: number;
  symbol_key: string;
  name: string;
  locale: string;
  license: string;
  license_url: string;
  author: string;
  author_url: string;
  source_url?: string | null;
  skins?: boolean | null;
  repo_key: string;
  hc?: boolean | null;
  extension: string;
  image_url: string;
  search_string?: string | null;
  unsafe_result: boolean;
  _href: string;
  details_url: string;
}

let accessToken = '';

export function getCachedToken(): string {
  return accessToken;
}

export async function getAccessToken(force = false): Promise<string> {
  if (accessToken && !force) return accessToken;
  try {
    // Proxy injects the secret server-side.
    const res = await fetch(TOKEN_URL, { method: 'POST' });
    if (!res.ok) {
      console.warn(`Failed to get access token: ${res.status}`);
      return accessToken;
    }
    const json = (await res.json()) as { access_token: string; expires_in: number };
    accessToken = json.access_token ?? '';
    return accessToken;
  } catch (e) {
    console.warn(`Failed to get access token: ${String(e)}`);
    return accessToken;
  }
}

async function fetchSymbols(query: string, locale: string, highContrast: boolean): Promise<ApiSymbolResponse[]> {
  const token = await getAccessToken();
  const q = highContrast ? `${query} hc:1` : query;
  const params = new URLSearchParams({
    q,
    locale: locale ?? '',
    safe: '0',
    access_token: token,
  });
  const doFetch = async () => fetch(`${SYMBOLS_URL}?${params.toString()}`);
  try {
    let res = await doFetch();
    if (res.status === 401 || res.status === 403) {
      // Token likely expired; refresh.
      await getAccessToken(true);
      params.set('access_token', accessToken);
      res = await fetch(`${SYMBOLS_URL}?${params.toString()}`);
    }
    if (!res.ok) {
      console.warn(`API call failed: ${res.status}`);
      return [];
    }
    const json = await res.json();
    return Array.isArray(json) ? (json as ApiSymbolResponse[]) : [];
  } catch (e) {
    console.warn(`API call failed: ${String(e)}`);
    return [];
  }
}

// Slice of results starting at index.
export async function searchSymbols(
  query: string,
  locale = '',
  highContrast = false,
  count = 20,
  index = 0
): Promise<ApiSymbolResponse[]> {
  const all = await fetchSymbols(query, locale, highContrast);
  return all.slice(index, index + count);
}

// First-result URL cache by query|locale|hc.
const firstUrlCache = new Map<string, string>();

// First matching symbol's image URL.
export async function firstSymbolUrl(query: string, locale = '', highContrast = false): Promise<string> {
  const key = `${query.toLowerCase().trim()}|${locale}|${highContrast ? 1 : 0}`;
  const cached = firstUrlCache.get(key);
  if (cached !== undefined) return cached;
  const all = await fetchSymbols(query, locale, highContrast);
  const url = all.length > 0 ? all[0].image_url : '';
  if (url) firstUrlCache.set(key, url);
  return url;
}

// Substitute selected skin tone into URL.
export function resolveImageUrl(url: string, skinToneHexCode: string): string {
  if (!url) return url;
  const tone = SKIN_TONES.find((t) => t.hexCode === skinToneHexCode);
  if (!tone || !tone.hexCode) return url;
  return url
    .replace('varianted-skin', `variant-${tone.skinKey}`)
    .replace(/-var[a-zA-Z0-9]+UNI/, `-${tone.hexCode}`);
}
