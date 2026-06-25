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

// High-contrast image treatment.
import { Platform } from 'react-native';
import type { ImageStyle } from 'react-native';

export const HC_IMAGE_FILTER = (
  Platform.OS === 'web'
    ? { filter: 'saturate(1.4) contrast(1.35)' }
    : { filter: [{ saturate: 1.4 }, { contrast: 1.35 }] }
) as unknown as ImageStyle;

// Check if URI targets an SVG.
export function isSvgUrl(url: string | null | undefined): boolean {
  if (!url) return false;
  return url.split('?')[0].toLowerCase().endsWith('.svg');
}