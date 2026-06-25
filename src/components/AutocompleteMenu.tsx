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

// Autocomplete: next-word predictions over board.
import React, { useEffect, useMemo, useState } from 'react';
import { Pressable, ScrollView, View } from 'react-native';
import { Image } from 'expo-image';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Text } from './themed';
import { predict } from '../ngram';
import { firstSymbolUrl, resolveImageUrl } from '../opensymbols';
import { HC_IMAGE_FILTER } from '../hc';
import { useAutoTranslate } from '../useAutoTranslate';
import { useStrings } from '../i18n';
import { fmt } from '../strings';

function capitalize(s: string): string {
  return s.length > 0 ? s[0].toUpperCase() + s.slice(1) : s;
}

// One suggestion tile: resolves and shows the word's symbol image (best-effort), with the label.
function AutoItem({
  word,
  label,
  width,
  height,
  onPress,
}: {
  word: string;
  label: string;
  width: number;
  height: number;
  onPress: () => void;
}) {
  const t = useTheme();
  const appLocale = useStore((s) => s.app_locale);
  const highContrast = useStore((s) => s.highcontrastmode);
  const skinTone = useStore((s) => s.skin_tone);
  const [url, setUrl] = useState('');

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const u = await firstSymbolUrl(word.toLowerCase(), appLocale, highContrast);
      if (!cancelled && u) setUrl(u);
    })();
    return () => {
      cancelled = true;
    };
  }, [word, appLocale, highContrast]);

  const resolved = url ? resolveImageUrl(url, skinTone) : '';
  return (
    <Pressable
      onPress={onPress}
      style={{
        width,
        height,
        borderWidth: 2,
        borderColor: t.border,
        borderRadius: 16,
        overflow: 'hidden',
        backgroundColor: highContrast ? '#000000' : t.surface,
        padding: 4,
        paddingBottom: 18,
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {resolved ? (
        <Image
          source={resolved}
          style={[{ flex: 1, width: '100%' }, highContrast && HC_IMAGE_FILTER]}
          contentFit="contain"
          cachePolicy="memory-disk"
        />
      ) : (
        <View style={{ flex: 1 }} />
      )}
      <Text
        numberOfLines={1}
        style={{
          position: 'absolute',
          left: 4,
          right: 4,
          bottom: 4,
          textAlign: 'center',
          fontSize: 13,
          color: highContrast ? '#FFFFFF' : t.text,
        }}
      >
        {label}
      </Text>
    </Pressable>
  );
}

export function AutocompleteMenu() {
  const inputItems = useStore((s) => s.inputItems);
  const ngramModel = useStore((s) => s.ngram_model);
  const menuWidth = useStore((s) => s.menuWidth);
  const menuHeight = useStore((s) => s.menuHeight);
  const inputBoxHeight = useStore((s) => s.inputBoxHeight);
  const boxWidth = useStore((s) => s.box_width_size_dp);
  const boxHeight = useStore((s) => s.box_height_size_dp);
  const boxPadding = useStore((s) => s.box_padding_dp);
  const addInputText = useStore((s) => s.addInputText);
  const t = useTheme();
  const S = useStrings();

  const lastWord = inputItems.length > 0 ? inputItems[inputItems.length - 1].text : '';
  const predictions = useMemo(() => predict(ngramModel, lastWord, 24), [ngramModel, lastWord, inputItems.length]);
  const tr = useAutoTranslate(predictions);

  if (menuWidth <= 0 || menuHeight <= 0) return null;

  const tileWidth = boxWidth + boxPadding;

  return (
    <View
      style={{
        position: 'absolute',
        left: 0,
        top: inputBoxHeight,
        width: menuWidth,
        height: menuHeight,
        backgroundColor: t.surface,
      }}
    >
      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 8 }}>
        <Text style={{ fontSize: 16, fontWeight: '500' }}>
          {lastWord ? fmt(S.autocomplete.after, { word: capitalize(lastWord) }) : S.autocomplete.suggestions}
        </Text>
        <Pressable
          onPress={() => useStore.setState({ showAutocomplete: false })}
          style={{ backgroundColor: t.primary, borderRadius: 6, paddingHorizontal: 12, paddingVertical: 6 }}
        >
          <Text style={{ color: t.onPrimary }}>{S.common.close}</Text>
        </Pressable>
      </View>

      {predictions.length === 0 ? (
        <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
          <Text style={{ color: t.subtext }}>{S.autocomplete.none}</Text>
        </View>
      ) : (
        <ScrollView contentContainerStyle={{ flexDirection: 'row', flexWrap: 'wrap', padding: 8, gap: 8 }}>
          {predictions.map((raw, i) => (
            <AutoItem
              key={`${i}-${raw}`}
              word={raw}
              label={capitalize(tr(raw))}
              width={tileWidth}
              height={boxHeight}
              onPress={() => addInputText(capitalize(raw))}
            />
          ))}
        </ScrollView>
      )}
    </View>
  );
}
