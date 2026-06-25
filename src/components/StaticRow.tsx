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

// Static row: always-available words at the bottom.
import React from 'react';
import { Pressable, View } from 'react-native';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Text } from './themed';
import { useAutoTranslate } from '../useAutoTranslate';

export function StaticRow() {
  const terms = useStore((s) => s.static_terms);
  const screenWidth = useStore((s) => s.screenWidth);
  const screenHeight = useStore((s) => s.screenHeight);
  const staticRowHeight = useStore((s) => s.staticRowHeight);
  const textSize = useStore((s) => s.static_row_text_size);
  const textPadding = useStore((s) => s.static_row_text_padding);
  const speakStaticTerm = useStore((s) => s.speakStaticTerm);
  const t = useTheme();
  const tr = useAutoTranslate(terms);

  if (screenWidth <= 0 || staticRowHeight <= 0 || terms.length === 0) return null;

  return (
    <View
      style={{
        position: 'absolute',
        left: 0,
        top: screenHeight - staticRowHeight,
        width: screenWidth,
        height: staticRowHeight,
        flexDirection: 'row',
      }}
    >
      {terms.map((term, i) => (
        <Pressable
          key={`${i}-${term}`}
          onPress={() => speakStaticTerm(term)}
          style={{
            flex: 1,
            backgroundColor: t.surface,
            borderWidth: 2,
            borderColor: t.border,
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Text style={{ color: t.text, fontSize: textSize, padding: textPadding }}>{tr(term)}</Text>
        </Pressable>
      ))}
    </View>
  );
}
