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

// App theming — light/dark/system palettes.
import { useColorScheme } from 'react-native';
import { useStore } from './store';

export interface Palette {
  isDark: boolean;
  bg: string; // screen background
  surface: string; // cards, dialogs, bars
  surfaceAlt: string; // subtle/secondary buttons, chips, slider track, input fills
  neutral: string; // solid grey buttons (white text sits on these)
  text: string; // primary text
  subtext: string; // hints / secondary text
  border: string; // strong borders (black in light)
  inputBorder: string; // text-input outlines
  panelBorder: string; // faint panel/dropdown outlines
  divider: string; // separators
  primary: string; // accent / active
  onPrimary: string; // text/icon on primary, neutral and colored solid buttons
  danger: string; // destructive actions
  scrim: string; // modal backdrop
}

export const LIGHT: Palette = {
  isDark: false,
  bg: '#FFFFFF',
  surface: '#FFFFFF',
  surfaceAlt: '#E8E8E8',
  neutral: '#757575',
  text: '#000000',
  subtext: '#888888',
  border: '#000000',
  inputBorder: '#999999',
  panelBorder: '#CCCCCC',
  divider: '#EEEEEE',
  primary: '#1976D2',
  onPrimary: '#FFFFFF',
  danger: '#D32F2F',
  scrim: '#00000088',
};

export const DARK: Palette = {
  isDark: true,
  bg: '#121212',
  surface: '#1E1E1E',
  surfaceAlt: '#2C2C2C',
  neutral: '#4A4A4A',
  text: '#ECECEC',
  subtext: '#9E9E9E',
  border: '#5A5A5A',
  inputBorder: '#5A5A5A',
  panelBorder: '#444444',
  divider: '#333333',
  primary: '#4F9BE8',
  onPrimary: '#FFFFFF',
  danger: '#EF5350',
  scrim: '#000000AA',
};

// Resolve active palette from theme_mode.
export function useTheme(): Palette {
  const mode = useStore((s) => s.theme_mode);
  const system = useColorScheme();
  const dark = mode === 'dark' || (mode === 'system' && system === 'dark');
  return dark ? DARK : LIGHT;
}
