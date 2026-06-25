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

// Input box: shows chosen items, taps to speak.
import React, { useState } from 'react';
import { Modal, Pressable, ScrollView, View } from 'react-native';
import { Image } from 'expo-image';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Text } from './themed';
import { resolveImageUrl } from '../opensymbols';
import { translationKey } from '../translate';
import { APP_LANGUAGES } from '../types';

function capitalize(s: string): string {
  return s.length > 0 ? s[0].toUpperCase() + s.slice(1) : s;
}

// The input-box language picker switches the spoken (board) language among the app locale
// plus any languages the user added in Settings ("Spoken languages"). app_locale is the implicit
// base (stored as current_board_language === ''); the extra languages come from language_list.
function LanguagePicker() {
  const appLocale = useStore((s) => s.app_locale);
  const languageList = useStore((s) => s.language_list);
  const boardLanguage = useStore((s) => s.current_board_language);
  const setSetting = useStore((s) => s.setSetting);
  const t = useTheme();
  const langs = [appLocale, ...languageList];
  const current = boardLanguage || appLocale;
  const name = APP_LANGUAGES.find((l) => l.code === current)?.name ?? current;

  const [expanded, setExpanded] = useState(false);
  // Anchor the popover under the pill. The InputBox sits at screen (0,0), so the pill's
  // onLayout y/height are already screen coordinates the Modal can use directly.
  const [anchor, setAnchor] = useState({ top: 38, left: 6 });

  if (languageList.length === 0) return null;

  // The pill toggles a popover listing every board language (app_locale + all translation
  // keys), with a ✓ on the active one.
  // Rendered in a transparent Modal so it floats above the board and dismisses on outside tap.
  return (
    <>
      <Pressable
        onPress={() => setExpanded((e) => !e)}
        onLayout={(ev) => {
          const { y, height } = ev.nativeEvent.layout;
          setAnchor({ top: y + height + 2, left: 6 });
        }}
        style={{
          position: 'absolute',
          top: 6,
          left: 6,
          backgroundColor: t.surfaceAlt,
          borderRadius: 6,
          paddingHorizontal: 10,
          paddingVertical: 6,
        }}
      >
        <Text style={{ fontSize: 14 }}>
          {name}
          <Text style={{ fontSize: 11, color: t.subtext }}>{expanded ? ' ▲' : ' ▼'}</Text>
        </Text>
      </Pressable>

      <Modal visible={expanded} transparent animationType="fade" onRequestClose={() => setExpanded(false)}>
        <Pressable style={{ flex: 1 }} onPress={() => setExpanded(false)}>
          <View
            style={{
              position: 'absolute',
              top: anchor.top,
              left: anchor.left,
              minWidth: 160,
              backgroundColor: t.surface,
              borderRadius: 8,
              paddingVertical: 4,
              elevation: 8,
              shadowColor: '#000000',
              shadowOpacity: 0.25,
              shadowRadius: 8,
              shadowOffset: { width: 0, height: 2 },
            }}
          >
            {langs.map((code) => {
              const ln = APP_LANGUAGES.find((l) => l.code === code)?.name ?? code;
              const active = code === current;
              return (
                <Pressable
                  key={code}
                  onPress={() => {
                    // app_locale is the implicit base; store '' for it so a later app-language
                    // change keeps following the base instead of pinning the old code.
                    setSetting('current_board_language', code === appLocale ? '' : code);
                    setExpanded(false);
                  }}
                  style={{
                    paddingHorizontal: 16,
                    paddingVertical: 10,
                    backgroundColor: active ? t.surfaceAlt : t.surface,
                  }}
                >
                  <Text style={{ fontSize: 15 }}>
                    {ln}
                    {active ? '  ✓' : ''}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </Pressable>
      </Modal>
    </>
  );
}

export function InputBox() {
  const inputItems = useStore((s) => s.inputItems);
  const menuWidth = useStore((s) => s.menuWidth);
  const inputBoxHeight = useStore((s) => s.inputBoxHeight);
  const boxPadding = useStore((s) => s.box_padding_dp);
  const textBottom = useStore((s) => s.text_location_bottom);
  const skinTone = useStore((s) => s.skin_tone);
  const speakInputSentence = useStore((s) => s.speakInputSentence);
  const appLocale = useStore((s) => s.app_locale);
  const boardLanguage = useStore((s) => s.current_board_language);
  const multilingual = useStore((s) => s.multilingual_labels);
  const autoLabels = useStore((s) => s.auto_translate_labels);
  const translations = useStore((s) => s.translations);
  const t = useTheme();

  // Mirror the board's label display: translate items into the active language when label
  // translation is on and multilingual labels is off (else show the original / manual label).
  const lang = boardLanguage || appLocale;
  const showT = !multilingual && autoLabels && !!lang && !lang.toLowerCase().startsWith('en');

  if (menuWidth <= 0 || inputBoxHeight <= 0) return null;

  const border = 4;
  const itemSide = Math.max(inputBoxHeight - border * 2, 0);

  return (
    <View
      style={{
        position: 'absolute',
        left: 0,
        top: 0,
        width: menuWidth,
        height: inputBoxHeight,
        backgroundColor: t.surface,
        borderWidth: border,
        borderColor: t.border,
      }}
    >
      {/* Tap anywhere to speak; the row still scrolls horizontally when items overflow. The
          Pressable sits INSIDE the ScrollView — a Pressable wrapping a ScrollView swallows the pan
          and blocks scrolling. No scroll indicator (the box is short and self-evidently scrolls). */}
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        style={{ flex: 1 }}
        contentContainerStyle={{ minWidth: '100%' }}
      >
        <Pressable onPress={speakInputSentence} style={{ flexDirection: 'row', minWidth: '100%', height: '100%' }}>
          {inputItems.map((it, index) => {
            const label = capitalize(
              showT ? it.translations[lang]?.trim() || translations[translationKey(it.text, lang)] || it.text : it.text
            );
            const resolved = it.hasSymbol ? resolveImageUrl(it.imageUrl, skinTone) : '';
            return (
              <View key={`${index}-${it.text}`} style={{ width: itemSide, height: itemSide }}>
                {it.hasSymbol && resolved ? (
                  <Image
                    source={resolved}
                    style={{ flex: 1, width: '100%', padding: boxPadding }}
                    contentFit="contain"
                    cachePolicy="memory-disk"
                  />
                ) : (
                  <View style={{ flex: 1, justifyContent: 'center', padding: boxPadding }}>
                    {!it.hasSymbol && (
                      <Text numberOfLines={2} style={{ fontSize: 16, color: '#000000' }}>
                        {label}
                      </Text>
                    )}
                  </View>
                )}
                {it.hasSymbol && (
                  <Text
                    numberOfLines={2}
                    style={{
                      position: 'absolute',
                      left: 2,
                      right: 2,
                      [textBottom ? 'bottom' : 'top']: 2,
                      textAlign: 'center',
                      fontSize: 12,
                      color: t.text,
                    }}
                  >
                    {label}
                  </Text>
                )}
              </View>
            );
          })}
        </Pressable>
      </ScrollView>
      <LanguagePicker />
    </View>
  );
}
