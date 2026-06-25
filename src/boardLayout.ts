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

// Board grid layout.
export const BOARD_DOTS_RESERVE = 22;

export interface BoardGrid {
  itemsPerRow: number;
  rowsPerPage: number;
  itemsPerPage: number;
  pageCount: number;
  cellWidth: number;
  cellHeight: number;
}

export function computeBoardGrid(
  boxWidth: number,
  boxHeight: number,
  boxPadding: number,
  menuWidth: number,
  menuHeight: number,
  itemCount: number
): BoardGrid {
  const itemWidth = boxWidth + boxPadding * 2;
  const availableHeight = Math.max(menuHeight - BOARD_DOTS_RESERVE, boxHeight + boxPadding * 3);
  const itemHeightNatural = boxHeight + boxPadding * 3;
  const itemsPerRow = Math.max(Math.floor(menuWidth / itemWidth), 1);
  const rowsPerPage = Math.max(Math.floor(availableHeight / itemHeightNatural), 1);
  const cellHeight = Math.floor(availableHeight / rowsPerPage);
  const itemsPerPage = Math.max(itemsPerRow * rowsPerPage, 1);
  const pageCount = Math.max(Math.ceil(itemCount / itemsPerPage), 1);
  const cellWidth = Math.floor(menuWidth / itemsPerRow);
  return { itemsPerRow, rowsPerPage, itemsPerPage, pageCount, cellWidth, cellHeight };
}
