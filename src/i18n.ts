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

// Runtime language selection.
import { STRINGS, type Strings } from './strings';
import { useStore } from './store';
import { TRANSLATIONS } from './translations';

// Merge translation over English; gaps fall back to English.
// eslint-disable-next-line @typescript-eslint/no-explicit-any
function deepMerge(base: any, over: any): any {
  if (Array.isArray(base)) {
    return base.map((v, i) => (over && over[i] !== undefined ? deepMerge(v, over[i]) : v));
  }
  if (base && typeof base === 'object') {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const out: any = {};
    for (const k of Object.keys(base)) out[k] = over && k in over ? deepMerge(base[k], over[k]) : base[k];
    return out;
  }
  return typeof over === 'string' ? over : base;
}

const cache: Record<string, Strings> = { en: STRINGS };

// Non-reactive accessor.
export function getStrings(locale?: string): Strings {
  const code = (locale || 'en').split('-')[0];
  if (code === 'en') return STRINGS;
  const t = TRANSLATIONS[code];
  if (!t) return STRINGS;
  if (!cache[code]) cache[code] = deepMerge(STRINGS, t) as Strings;
  return cache[code];
}

// Reactive hook, re-renders on language change.
export function useStrings(): Strings {
  const locale = useStore((s) => s.app_locale);
  const on = useStore((s) => s.auto_translate_ui);
  return on ? getStrings(locale) : STRINGS;
}
