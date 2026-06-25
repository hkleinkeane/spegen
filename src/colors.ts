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

// Color helpers.
import type { FitzgeraldCategory } from './types';

export const WHITE = '#FFFFFF';

// Validate hex, fall back to white.
export function normalizeHex(value: string): string {
  if (!value) return WHITE;
  if (/^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/.test(value)) return value;
  return WHITE;
}

// Override wins over category base.
export function effectiveCategoryColor(
  category: FitzgeraldCategory,
  overrides: Record<string, string>
): string {
  const override = overrides[category.name];
  return override && override.trim() ? normalizeHex(override) : normalizeHex(category.colorHex);
}

// Resolve stored color to hex.
export function resolveItemColor(
  stored: string,
  fitzgeraldKey: FitzgeraldCategory[],
  overrides: Record<string, string>
): string {
  if (!stored || !stored.trim()) return WHITE;
  if (stored.startsWith('#')) return normalizeHex(stored);
  const cat = fitzgeraldKey.find((c) => c.name === stored);
  return cat ? effectiveCategoryColor(cat, overrides) : WHITE;
}

// HSV to hex for color picker. hue 0..360, sat/val 0..1.
export function hexToHsv(hex: string): [number, number, number] {
  const h = normalizeHex(hex).slice(1);
  const r = parseInt(h.slice(0, 2), 16) / 255;
  const g = parseInt(h.slice(2, 4), 16) / 255;
  const b = parseInt(h.slice(4, 6), 16) / 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const d = max - min;
  let hue = 0;
  if (d !== 0) {
    if (max === r) hue = ((g - b) / d) % 6;
    else if (max === g) hue = (b - r) / d + 2;
    else hue = (r - g) / d + 4;
    hue *= 60;
    if (hue < 0) hue += 360;
  }
  const sat = max === 0 ? 0 : d / max;
  return [hue, sat, max];
}

// Hex to uppercase #RRGGBB.
export function hsvToHex(hue: number, sat: number, val: number): string {
  const c = val * sat;
  const x = c * (1 - Math.abs(((hue / 60) % 2) - 1));
  const m = val - c;
  let r = 0;
  let g = 0;
  let b = 0;
  if (hue < 60) [r, g, b] = [c, x, 0];
  else if (hue < 120) [r, g, b] = [x, c, 0];
  else if (hue < 180) [r, g, b] = [0, c, x];
  else if (hue < 240) [r, g, b] = [0, x, c];
  else if (hue < 300) [r, g, b] = [x, 0, c];
  else [r, g, b] = [c, 0, x];
  const to = (n: number) =>
    Math.round((n + m) * 255)
      .toString(16)
      .padStart(2, '0')
      .toUpperCase();
  return `#${to(r)}${to(g)}${to(b)}`;
}

// Readable text color for background.
export function contrastTextColor(bgHex: string): string {
  const hex = normalizeHex(bgHex).slice(1);
  const r = parseInt(hex.slice(0, 2), 16);
  const g = parseInt(hex.slice(2, 4), 16);
  const b = parseInt(hex.slice(4, 6), 16);
  // Relative luminance.
  const lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
  return lum > 0.6 ? '#000000' : '#FFFFFF';
}
