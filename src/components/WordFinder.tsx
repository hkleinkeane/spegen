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

// Word finder: search menus and guide navigation.
import React, { useEffect, useMemo, useState } from 'react';
import { Modal, Pressable, ScrollView, View } from 'react-native';
import { Image } from 'expo-image';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Text, TextInput } from './themed';
import { allPathsToSymbol, findMenu, type SymbolPath } from '../menus';
import { firstSymbolUrl, resolveImageUrl } from '../opensymbols';
import { HC_IMAGE_FILTER } from '../hc';
import type { MenuTemplate } from '../types';
import { fmt } from '../strings';
import { fetchTranslation, translationKey } from '../translate';
import { useStrings } from '../i18n';

function capitalize(s: string): string {
  return s.length > 0 ? s[0].toUpperCase() + s.slice(1) : s;
}

interface PathMatch {
  name: string;
  displayLabel: string;
  isSymbol: boolean;
  path: SymbolPath;
  breadcrumb: string;
}

function findPathMatches(
  menuList: MenuTemplate[],
  queries: string[],
  lang: string,
  translations: Record<string, string>
): PathMatch[] {
  const qs = queries.map((x) => x.trim().toLowerCase().replace(/\s+/g, '')).filter(Boolean);
  if (qs.length === 0) return [];
  const nonEng = !!lang && !lang.toLowerCase().startsWith('en');
  const out: PathMatch[] = [];
  for (let mi = 0; mi < menuList.length; mi++) {
    const menu = menuList[mi];
    for (let i = 0; i < menu.item_list.length; i++) {
      const name = menu.item_list[i] ?? '';
      const manual = (menu.item_translations[i] && Object.values(menu.item_translations[i]).join(' ')) || '';
      // The board-language label as shown on the board (manual label, else cached auto-translation).
      const translated = nonEng
        ? menu.item_translations[i]?.[lang]?.trim() || translations[translationKey(name, lang)] || ''
        : '';
      const displayLabel = translated || name;
      // Match against the English name, any manual labels, AND the board-language label, so a search
      // in the user's language (e.g. "je") finds the symbol shown as "je" — and shows it as "je".
      const hay = `${name} ${manual} ${translated}`.toLowerCase().replace(/\s+/g, '');
      if (!qs.some((q) => hay.includes(q))) continue;
      for (const path of allPathsToSymbol(menuList, mi, i)) {
        out.push({
          name,
          displayLabel,
          isSymbol: menu.item_type[i],
          path,
          breadcrumb: path.menuNames.join(' › '),
        });
        if (out.length >= 50) return out;
      }
    }
  }
  return out;
}

// One search result: the symbol's image, its name, the path breadcrumb, and a Find button that
// starts the guided walk. The image prefers the cached board URL and otherwise resolves it from
// OpenSymbols (best-effort).
function WordFinderCard({ match, onFind }: { match: PathMatch; onFind: () => void }) {
  const t = useTheme();
  const S = useStrings();
  const menuList = useStore((s) => s.menu_list);
  const skinTone = useStore((s) => s.skin_tone);
  const highContrast = useStore((s) => s.highcontrastmode);
  const appLocale = useStore((s) => s.app_locale);
  const [url, setUrl] = useState('');

  useEffect(() => {
    let cancelled = false;
    const menu = findMenu(menuList, match.path.containingMenuId);
    const cached = menu.image_urls[match.path.itemIndex];
    if (cached && cached.trim()) {
      setUrl(cached);
      return;
    }
    (async () => {
      const u = await firstSymbolUrl(match.name, appLocale, highContrast);
      if (!cancelled && u) setUrl(u);
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [match.name, match.path.containingMenuId, match.path.itemIndex]);

  const resolved = url ? resolveImageUrl(url, skinTone) : '';
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 6, borderBottomWidth: 1, borderColor: t.divider }}>
      <View
        style={{
          width: 56,
          height: 56,
          backgroundColor: highContrast ? '#000000' : t.surfaceAlt,
          borderRadius: 8,
          marginRight: 10,
          overflow: 'hidden',
        }}
      >
        {resolved ? (
          <Image
            source={resolved}
            style={[{ flex: 1 }, highContrast && HC_IMAGE_FILTER]}
            contentFit="contain"
            cachePolicy="memory-disk"
          />
        ) : null}
      </View>
      <View style={{ flex: 1 }}>
        <Text style={{ fontSize: 16 }}>{capitalize(match.displayLabel)}</Text>
        <Text style={{ fontSize: 12, color: t.subtext }}>{match.breadcrumb}</Text>
      </View>
      <Pressable onPress={onFind} style={{ backgroundColor: t.primary, borderRadius: 6, paddingHorizontal: 14, paddingVertical: 8 }}>
        <Text style={{ color: t.onPrimary }}>{S.wordFinder.find}</Text>
      </Pressable>
    </View>
  );
}

export function WordFinder() {
  const show = useStore((s) => s.showWordFinder);
  const menuList = useStore((s) => s.menu_list);
  const startWordFinderPath = useStore((s) => s.startWordFinderPath);
  const appLocale = useStore((s) => s.app_locale);
  const boardLanguage = useStore((s) => s.current_board_language);
  const translations = useStore((s) => s.translations);
  const t = useTheme();
  const S = useStrings();

  const lang = boardLanguage || appLocale;
  const [query, setQuery] = useState('');
  const [englishQuery, setEnglishQuery] = useState('');
  // When the board language isn't English, also translate the query to English so a search like
  // "je" (French) finds the English-authored item "I". Debounced, best-effort.
  useEffect(() => {
    const q = query.trim();
    if (!q || !lang || lang.toLowerCase().startsWith('en')) {
      setEnglishQuery('');
      return;
    }
    let cancelled = false;
    const timer = setTimeout(() => {
      void fetchTranslation(q, 'en').then((r) => {
        if (!cancelled && r) setEnglishQuery(r);
      });
    }, 300);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [query, lang]);
  const matches = useMemo(
    () => findPathMatches(menuList, [query, englishQuery], lang, translations),
    [menuList, query, englishQuery, lang, translations]
  );

  const close = () => {
    setQuery('');
    useStore.setState({ showWordFinder: false });
  };

  const find = (m: PathMatch) => {
    setQuery('');
    startWordFinderPath(m.path, m.name, m.isSymbol);
  };

  return (
    <Modal visible={show} transparent animationType="fade" onRequestClose={close}>
      <View style={{ flex: 1, backgroundColor: t.scrim, justifyContent: 'center', padding: 24 }}>
        <View style={{ backgroundColor: t.surface, borderRadius: 12, padding: 20, maxHeight: '85%' }}>
          <Text style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 4 }}>{S.wordFinder.title}</Text>
          <Text style={{ fontSize: 12, color: t.subtext, marginBottom: 8 }}>{S.wordFinder.subtitle}</Text>
          <TextInput
            value={query}
            onChangeText={setQuery}
            autoFocus
            placeholder={S.wordFinder.placeholder}
            style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 8, marginBottom: 8 }}
          />
          <ScrollView style={{ maxHeight: 320 }} keyboardShouldPersistTaps="handled">
            {query.trim() && matches.length === 0 && (
              <Text style={{ color: t.subtext, padding: 8 }}>{S.wordFinder.noMatches}</Text>
            )}
            {matches.map((m, idx) => (
              <WordFinderCard
                key={`${m.path.containingMenuId}-${m.path.itemIndex}-${idx}`}
                match={m}
                onFind={() => find(m)}
              />
            ))}
          </ScrollView>
          <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginTop: 12 }}>
            <Pressable onPress={close} style={{ padding: 10 }}>
              <Text style={{ fontSize: 16, fontWeight: 'bold' }}>{S.wordFinder.close}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}

/*
 * Active-guide banner. Shown only while a
 * path is in progress; names the word being found and offers Cancel. Positioned at the top so
 * it does not cover the static / menu rows at the bottom.
 */
export function WordFinderGuide() {
  const pathNames = useStore((s) => s.wordFinderPathNames);
  const active = useStore((s) => s.wordFinderPathIds.length > 0);
  const cancel = useStore((s) => s.cancelWordFinderPath);
  const S = useStrings();
  if (!active) return null;
  const target = pathNames.length > 0 ? capitalize(pathNames[pathNames.length - 1]) : '';
  return (
    <View
      pointerEvents="box-none"
      style={{ position: 'absolute', top: 8, left: 12, right: 12, alignItems: 'center', zIndex: 1500 }}
    >
      <View
        style={{
          flexDirection: 'row',
          alignItems: 'center',
          backgroundColor: '#323232',
          borderRadius: 24,
          paddingLeft: 16,
          paddingRight: 6,
          paddingVertical: 6,
          maxWidth: '100%',
        }}
      >
        <Text style={{ color: '#FFFFFF', flexShrink: 1 }}>{fmt(S.wordFinder.finding, { word: target })}</Text>
        <Pressable
          onPress={cancel}
          style={{ marginLeft: 10, backgroundColor: '#555555', borderRadius: 18, paddingHorizontal: 12, paddingVertical: 6 }}
        >
          <Text style={{ color: '#FFFFFF' }}>{S.wordFinder.cancel}</Text>
        </Pressable>
      </View>
    </View>
  );
}
