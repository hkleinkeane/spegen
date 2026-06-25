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

// Board: paginated grid of symbols/folders.
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { NativeScrollEvent, NativeSyntheticEvent, ScrollView, View } from 'react-native';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { findMenu } from '../menus';
import { firstSymbolUrl } from '../opensymbols';
import { fetchTranslation, translationKey } from '../translate';
import { resolveItemColor } from '../colors';
import { Symbol } from './Symbol';
import { Folder } from './Folder';
import { BUTTON_SHAPES } from '../types';
import { BOARD_DOTS_RESERVE as DOTS_RESERVE, computeBoardGrid } from '../boardLayout';

function capitalize(s: string): string {
  return s.length > 0 ? s[0].toUpperCase() + s.slice(1) : s;
}

export function Board() {
  const linkedMenu = useStore((s) => s.linkedMenu);
  const menuList = useStore((s) => s.menu_list);
  const refreshNonce = useStore((s) => s.refreshNonce);
  const menuWidth = useStore((s) => s.menuWidth);
  const menuHeight = useStore((s) => s.menuHeight);
  const inputBoxHeight = useStore((s) => s.inputBoxHeight);
  const boxWidth = useStore((s) => s.box_width_size_dp);
  const boxHeight = useStore((s) => s.box_height_size_dp);
  const boxPadding = useStore((s) => s.box_padding_dp);
  const borderWidth = useStore((s) => s.item_border_width_dp);
  const buttonShapeName = useStore((s) => s.button_shape_name);
  const highContrast = useStore((s) => s.highcontrastmode);
  const textBottom = useStore((s) => s.text_location_bottom);
  const skinTone = useStore((s) => s.skin_tone);
  const appLocale = useStore((s) => s.app_locale);
  const boardLanguage = useStore((s) => s.current_board_language);
  const languageImageOverride = useStore((s) => s.language_image_override);
  const multilingualLabels = useStore((s) => s.multilingual_labels);
  const autoTranslateLabels = useStore((s) => s.auto_translate_labels);
  const translations = useStore((s) => s.translations);
  const fitzgeraldKey = useStore((s) => s.fitzgeraldKey);
  const fitzgeraldOverrides = useStore((s) => s.fitzgerald_overrides);
  const editorMode = useStore((s) => s.editorMode);
  const wordFinderHighlight = useStore((s) => s.wordFinderHighlight);
  const wordFinderActive = useStore((s) => s.wordFinderPathIds.length > 0);
  const tutorialScrollToIndex = useStore((s) => s.tutorialScrollToIndex);

  const navigateTo = useStore((s) => s.navigateTo);
  const selectSymbol = useStore((s) => s.selectSymbol);
  const setItemImageUrl = useStore((s) => s.setItemImageUrl);
  const setTranslation = useStore((s) => s.setTranslation);
  const setWordFinderHighlight = useStore((s) => s.setWordFinderHighlight);
  const handleWordFinderTap = useStore((s) => s.handleWordFinderTap);
  const openDialog = useStore((s) => s.openDialog);
  const t = useTheme();

  const menu = findMenu(menuList, linkedMenu);
  const [page, setPage] = useState(0);
  const resolvingRef = useRef<string>('');
  const scrollRef = useRef<ScrollView>(null);

  const borderRadius = BUTTON_SHAPES.find((b) => b.name === buttonShapeName)?.radius ?? 40;
  const boardLang = boardLanguage || appLocale;

  // Resolve missing image URLs for the current menu and cache them back into state.
  const itemsKey = `${menu.id}|${menu.item_list.join('')}|${refreshNonce}`;
  useEffect(() => {
    let cancelled = false;
    if (resolvingRef.current === itemsKey) return;
    resolvingRef.current = itemsKey;
    (async () => {
      for (let i = 0; i < menu.item_list.length; i++) {
        if (cancelled) return;
        const existing = menu.image_urls[i];
        if (existing && existing.trim()) continue;
        const loc = languageImageOverride ? menu.item_locales[i] || '' : appLocale;
        const url = await firstSymbolUrl(menu.item_list[i], loc, highContrast);
        if (cancelled) return;
        if (url) setItemImageUrl(menu.id, i, url);
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [itemsKey]);

  // Auto-translate: pre-fetch a translation of each item's label into the current board language
  // and cache it in the store, so the board shows (and speakItem can speak) the translated text
  // without a network round-trip on tap. Disabled when multilingual labels is on (manual per-item
  // labels take over). Skipped for English targets and for items with a manual per-language label.
  useEffect(() => {
    const target = boardLang;
    if (multilingualLabels || !autoTranslateLabels) return;
    if (!target || target.toLowerCase().startsWith('en')) return;
    let cancelled = false;
    (async () => {
      for (let i = 0; i < menu.item_list.length; i++) {
        if (cancelled) return;
        const name = menu.item_list[i];
        if (!name || !name.trim()) continue;
        const manual = menu.item_translations[i]?.[target];
        if (manual && manual.trim()) continue;
        const t = await fetchTranslation(name, target);
        if (cancelled) return;
        if (t) setTranslation(translationKey(name, target), t);
      }
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [itemsKey, boardLang, multilingualLabels, autoTranslateLabels]);

  const grid = useMemo(
    () => computeBoardGrid(boxWidth, boxHeight, boxPadding, menuWidth, menuHeight, menu.item_list.length),
    [boxWidth, boxHeight, boxPadding, menuWidth, menuHeight, menu.item_list.length]
  );

  // Tutorial page-jump: when the tutorial spotlights a
  // home-menu folder/symbol that lives on a later page, scroll the pager to that page so the
  // spotlight lands on a visible cell. Only the home menu (id 0) is spotlighted.
  useEffect(() => {
    if (tutorialScrollToIndex < 0 || linkedMenu !== 0 || menuWidth <= 0) return;
    const targetPage = Math.floor(tutorialScrollToIndex / grid.itemsPerPage);
    scrollRef.current?.scrollTo({ x: targetPage * menuWidth, y: 0, animated: true });
    setPage(targetPage);
  }, [tutorialScrollToIndex, grid.itemsPerPage, menuWidth, linkedMenu]);

  // Word-finder page-jump: when
  // the next item to tap lives on a later page, scroll the pager to it so the spotlight is visible.
  useEffect(() => {
    if (!wordFinderHighlight || wordFinderHighlight.menuId !== menu.id || menuWidth <= 0) return;
    const targetPage = Math.floor(wordFinderHighlight.index / grid.itemsPerPage);
    scrollRef.current?.scrollTo({ x: targetPage * menuWidth, y: 0, animated: true });
    setPage(targetPage);
  }, [wordFinderHighlight, grid.itemsPerPage, menuWidth, menu.id]);

  const onScroll = (e: NativeSyntheticEvent<NativeScrollEvent>) => {
    if (menuWidth <= 0) return;
    const p = Math.round(e.nativeEvent.contentOffset.x / menuWidth);
    if (p !== page) setPage(p);
  };

  if (menuWidth <= 0 || menuHeight <= 0) return null;

  const pages = Array.from({ length: grid.pageCount }, (_, p) => p);

  return (
    <View style={{ position: 'absolute', left: 0, top: inputBoxHeight, width: menuWidth, height: menuHeight }}>
      <ScrollView
        ref={scrollRef}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        // Update the active page from the live scroll position. onMomentumScrollEnd alone doesn't
        // fire on web (react-native-web uses CSS scroll-snap, not native momentum), which left the
        // pager dots stuck on page 1; onScroll fires on both web and native.
        onScroll={onScroll}
        onMomentumScrollEnd={onScroll}
        scrollEventThrottle={16}
        style={{ width: menuWidth, height: menuHeight - DOTS_RESERVE }}
      >
        {pages.map((p) => {
          const start = p * grid.itemsPerPage;
          const end = Math.min(start + grid.itemsPerPage, menu.item_list.length);
          const indices = [];
          for (let i = start; i < end; i++) indices.push(i);
          return (
            <View
              key={p}
              style={{
                width: menuWidth,
                height: menuHeight - DOTS_RESERVE,
                flexDirection: 'row',
                flexWrap: 'wrap',
                alignContent: 'flex-start',
              }}
            >
              {indices.map((i) => {
                const isSymbol = menu.item_type[i];
                // When multilingual labels is off and a non-English language is active, show the
                // manual per-language label if any, else the cached auto-translation, else the
                // original (until the async translation arrives). Multilingual on / English: original.
                const raw = menu.item_list[i];
                const translated =
                  !multilingualLabels && autoTranslateLabels && boardLang && !boardLang.toLowerCase().startsWith('en')
                    ? menu.item_translations[i]?.[boardLang]?.trim() ||
                      translations[translationKey(raw, boardLang)] ||
                      raw
                    : raw;
                const label = capitalize(translated);
                const bgColor = resolveItemColor(menu.colors[i] ?? '', fitzgeraldKey, fitzgeraldOverrides);
                const highlighted =
                  !!wordFinderHighlight &&
                  wordFinderHighlight.menuId === menu.id &&
                  wordFinderHighlight.index === i;
                const common = {
                  label,
                  imageUrl: menu.image_urls[i] ?? '',
                  bgColor,
                  cellWidth: grid.cellWidth,
                  cellHeight: grid.cellHeight,
                  boxWidth,
                  boxHeight,
                  boxPadding,
                  borderRadius,
                  borderWidth,
                  highContrast,
                  textBottom,
                  skinTone,
                  highlighted,
                };
                const onPress = () => {
                  // While a guided word-finder path is active, route taps through it (the
                  // store advances the path / completes on the ringed item) unless editing.
                  if (wordFinderActive && !editorMode) {
                    handleWordFinderTap(menu.id, i);
                    return;
                  }
                  if (wordFinderHighlight) setWordFinderHighlight(null);
                  if (editorMode) {
                    openDialog('editItem', menu.id, i);
                    return;
                  }
                  if (isSymbol) selectSymbol(menu.id, i);
                  else {
                    const target = menu.pointers[i];
                    if (target != null) navigateTo(target);
                  }
                };
                return isSymbol ? (
                  <Symbol key={menu.item_uuids[i] ?? `${menu.id}-${i}`} {...common} onPress={onPress} />
                ) : (
                  <Folder key={menu.item_uuids[i] ?? `${menu.id}-${i}`} {...common} onPress={onPress} />
                );
              })}
            </View>
          );
        })}
      </ScrollView>

      {grid.pageCount > 1 && (
        <View style={{ height: DOTS_RESERVE, flexDirection: 'row', justifyContent: 'center', alignItems: 'center' }}>
          {pages.map((p) => (
            <View
              key={p}
              style={{
                width: 8,
                height: 8,
                borderRadius: 4,
                marginHorizontal: 4,
                backgroundColor: p === page ? t.text : t.panelBorder,
              }}
            />
          ))}
        </View>
      )}
    </View>
  );
}
