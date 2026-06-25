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

// Theme-aware Text for dark mode.
import React from 'react';
import { Text as RNText, TextInput as RNTextInput, type TextProps, type TextInputProps } from 'react-native';
import { useTheme } from '../theme';

export function Text({ style, ...rest }: TextProps) {
  const t = useTheme();
  return <RNText style={[{ color: t.text }, style]} {...rest} />;
}

// Theme-aware TextInput: defaults the typed-text color and placeholder color to the palette so
// input is visible in dark mode. An explicit `color`/`placeholderTextColor` still wins.
export function TextInput({ style, placeholderTextColor, ...rest }: TextInputProps) {
  const t = useTheme();
  return (
    <RNTextInput style={[{ color: t.text }, style]} placeholderTextColor={placeholderTextColor ?? t.subtext} {...rest} />
  );
}
