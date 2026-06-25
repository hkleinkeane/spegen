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

// Auto-translate hook for UI labels.
import { useEffect } from 'react';
import { useStore } from './store';
import { cachedTranslation, fetchTranslation, translationKey } from './translate';

export function useAutoTranslate(texts: string[]): (text: string) => string {
  const appLocale = useStore((s) => s.app_locale);
  const boardLanguage = useStore((s) => s.current_board_language);
  const translations = useStore((s) => s.translations);
  const setTranslation = useStore((s) => s.setTranslation);
  const autoLabels = useStore((s) => s.auto_translate_labels);

  const lang = boardLanguage || appLocale;
  const active = autoLabels && !!lang && !lang.toLowerCase().startsWith('en');
  const key = texts.join(''); // stable dependency for the fetch effect

  useEffect(() => {
    if (!active) return;
    let cancelled = false;
    (async () => {
      for (const text of texts) {
        if (cancelled) return;
        if (!text || !text.trim()) continue;
        if (cachedTranslation(text, lang)) continue;
        const tr = await fetchTranslation(text, lang);
        if (cancelled) return;
        if (tr) setTranslation(translationKey(text, lang), tr);
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key, lang, active]);

  return (text: string) => (active ? translations[translationKey(text, lang)] || text : text);
}
