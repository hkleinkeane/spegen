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

// Word-finder spotlight: greys out except the target item.
import React from 'react';
import { Pressable, View, type ViewStyle } from 'react-native';
import { useStore } from '../store';
import { findMenu } from '../menus';
import { computeBoardGrid } from '../boardLayout';

const DIM = 'rgba(128,128,128,0.5)';

export function WordFinderSpotlight() {
  const highlight = useStore((s) => s.wordFinderHighlight);
  const linkedMenu = useStore((s) => s.linkedMenu);
  const menuList = useStore((s) => s.menu_list);
  const screenWidth = useStore((s) => s.screenWidth);
  const screenHeight = useStore((s) => s.screenHeight);
  const menuWidth = useStore((s) => s.menuWidth);
  const menuHeight = useStore((s) => s.menuHeight);
  const inputBoxHeight = useStore((s) => s.inputBoxHeight);
  const boxWidth = useStore((s) => s.box_width_size_dp);
  const boxHeight = useStore((s) => s.box_height_size_dp);
  const boxPadding = useStore((s) => s.box_padding_dp);
  const cancel = useStore((s) => s.cancelWordFinderPath);

  // Only spotlight when the highlighted item is on the currently displayed menu.
  if (!highlight || highlight.menuId !== linkedMenu) return null;
  if (screenWidth <= 0 || screenHeight <= 0 || menuWidth <= 0 || menuHeight <= 0) return null;

  const menu = findMenu(menuList, linkedMenu);
  const grid = computeBoardGrid(boxWidth, boxHeight, boxPadding, menuWidth, menuHeight, menu.item_list.length);
  const localIdx = highlight.index % grid.itemsPerPage;
  const row = Math.floor(localIdx / grid.itemsPerRow);
  const col = localIdx % grid.itemsPerRow;
  const hx = col * grid.cellWidth;
  const hy = inputBoxHeight + row * grid.cellHeight;
  const hw = grid.cellWidth;
  const hh = grid.cellHeight;

  // A scrim strip is a Pressable so it both dims and (on tap) cancels the search. The gap left at
  // the hole has no strip, so taps there fall through to the real board button beneath.
  const strip = (key: string, style: ViewStyle) => (
    <Pressable key={key} onPress={cancel} style={[{ position: 'absolute', backgroundColor: DIM }, style]} />
  );

  return (
    <View
      pointerEvents="box-none"
      style={{ position: 'absolute', left: 0, top: 0, width: screenWidth, height: screenHeight, zIndex: 600 }}
    >
      {strip('top', { left: 0, top: 0, width: screenWidth, height: Math.max(hy, 0) })}
      {strip('bottom', {
        left: 0,
        top: hy + hh,
        width: screenWidth,
        height: Math.max(screenHeight - (hy + hh), 0),
      })}
      {strip('left', { left: 0, top: hy, width: Math.max(hx, 0), height: hh })}
      {strip('right', {
        left: hx + hw,
        top: hy,
        width: Math.max(screenWidth - (hx + hw), 0),
        height: hh,
      })}
    </View>
  );
}
