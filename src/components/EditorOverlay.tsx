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

// Editor dim overlay (scrims everything except board).
import React from 'react';
import { Pressable, View, type ViewStyle } from 'react-native';
import { useStore } from '../store';

const DIM = 'rgba(128,128,128,0.5)';
const ORANGE = '#FF8F00';

export function EditorOverlay() {
  const screenWidth = useStore((s) => s.screenWidth);
  const inputBoxHeight = useStore((s) => s.inputBoxHeight);
  const menuHeight = useStore((s) => s.menuHeight);
  const menuStaticRowHeight = useStore((s) => s.menuStaticRowHeight);
  const staticRowHeight = useStore((s) => s.staticRowHeight);
  const buttonBoxesWidth = useStore((s) => s.buttonBoxesWidth);

  if (screenWidth <= 0 || menuHeight <= 0) return null;

  const boardWidth = screenWidth - buttonBoxesWidth * 2;
  const rowsTop = inputBoxHeight + menuHeight;
  const rowsHeight = menuStaticRowHeight + staticRowHeight;

  // A scrim is a Pressable (with an empty handler) so it reliably becomes the touch responder and
  // swallows taps to the dimmed region beneath it, rather than letting them through.
  const scrim = (key: string, style: ViewStyle) => (
    <Pressable
      key={key}
      onPress={() => undefined}
      style={[{ position: 'absolute', backgroundColor: DIM, zIndex: 700 }, style]}
    />
  );

  return (
    <>
      {/* input box (top quarter) */}
      {scrim('inputbox', { left: 0, top: 0, width: screenWidth, height: inputBoxHeight })}
      {/* menu row + static row (bottom rows) */}
      {rowsHeight > 0 && scrim('rows', { left: 0, top: rowsTop, width: screenWidth, height: rowsHeight })}
      {/* button boxes (right column, beside the board) */}
      {scrim('buttons', { left: boardWidth, top: inputBoxHeight, width: buttonBoxesWidth * 2, height: menuHeight })}
      {/* orange frame around the editable board (non-interactive so board taps pass through) */}
      <View
        pointerEvents="none"
        style={{
          position: 'absolute',
          left: 0,
          top: inputBoxHeight,
          width: boardWidth,
          height: menuHeight,
          borderWidth: 3,
          borderColor: ORANGE,
          zIndex: 750,
        }}
      />
    </>
  );
}
