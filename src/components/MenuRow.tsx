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

// Menu row: shortcuts above static row.
import React from 'react';
import { Pressable, View } from 'react-native';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Text } from './themed';
import { findMenu } from '../menus';
import { useAutoTranslate } from '../useAutoTranslate';

export function MenuRow() {
  const ids = useStore((s) => s.menu_row_ids);
  const menuList = useStore((s) => s.menu_list);
  const screenWidth = useStore((s) => s.screenWidth);
  const screenHeight = useStore((s) => s.screenHeight);
  const menuStaticRowHeight = useStore((s) => s.menuStaticRowHeight);
  const staticRowHeight = useStore((s) => s.staticRowHeight);
  const textSize = useStore((s) => s.menu_row_text_size);
  const textPadding = useStore((s) => s.menu_row_text_padding);
  const highContrast = useStore((s) => s.highcontrastmode);
  const navigateTo = useStore((s) => s.navigateTo);
  const t = useTheme();
  const tr = useAutoTranslate(ids.map((id) => findMenu(menuList, id).title));

  if (screenWidth <= 0 || menuStaticRowHeight <= 0 || ids.length === 0) return null;

  // Fold marker stays the strong foreground colour so it reads in dark mode too (white in HC).
  const foldColor = highContrast ? '#FFFFFF' : t.text;
  const foldSize = Math.min(Math.max(menuStaticRowHeight * 0.25, 8), 48);

  return (
    <View
      style={{
        position: 'absolute',
        left: 0,
        top: screenHeight - menuStaticRowHeight - staticRowHeight,
        width: screenWidth,
        height: menuStaticRowHeight,
        flexDirection: 'row',
      }}
    >
      {ids.map((id, i) => (
        <Pressable
          key={`${i}-${id}`}
          onPress={() => navigateTo(id)}
          style={{
            flex: 1,
            backgroundColor: t.surface,
            borderWidth: 2,
            borderColor: t.border,
            alignItems: 'center',
            justifyContent: 'center',
            overflow: 'hidden',
          }}
        >
          <Text style={{ color: t.text, fontSize: textSize, padding: textPadding }}>
            {tr(findMenu(menuList, id).title)}
          </Text>
          <View
            style={{
              position: 'absolute',
              top: 0,
              right: 0,
              width: 0,
              height: 0,
              borderTopWidth: foldSize,
              borderLeftWidth: foldSize,
              borderTopColor: foldColor,
              borderLeftColor: 'transparent',
            }}
          />
        </Pressable>
      ))}
    </View>
  );
}
