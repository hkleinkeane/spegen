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

// Shared renderer for board items.
import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { Image } from 'expo-image';
import { resolveImageUrl } from '../opensymbols';
import { HC_IMAGE_FILTER } from '../hc';

export interface ItemButtonProps {
  label: string;
  imageUrl: string;
  bgColor: string;
  cellWidth: number;
  cellHeight: number;
  boxWidth: number;
  boxHeight: number;
  boxPadding: number;
  borderRadius: number;
  borderWidth: number;
  highContrast: boolean;
  textBottom: boolean;
  skinTone: string;
  isFolder: boolean;
  highlighted?: boolean; // word-finder target ring
  onPress: () => void;
}

export function ItemButton({
  label,
  imageUrl,
  bgColor,
  cellWidth,
  cellHeight,
  boxWidth,
  boxHeight,
  boxPadding,
  borderRadius,
  borderWidth,
  highContrast,
  textBottom,
  skinTone,
  isFolder,
  highlighted = false,
  onPress,
}: ItemButtonProps) {
  const resolved = resolveImageUrl(imageUrl, skinTone);
  const background = highContrast ? '#000000' : bgColor;
  const stroke = highContrast ? '#FFFFFF' : '#000000';
  const labelColor = highContrast ? '#FFFFFF' : '#000000';
  const foldColor = highContrast ? '#FFFFFF' : '#000000';
  const foldSize = Math.min(Math.max(boxWidth * 0.22, 8), 48);
  const foldInset = Math.max(boxWidth * 0.1, 2);

  // Detect vector graphics assets
  const isSvg = typeof resolved === 'string' && (resolved.toLowerCase().includes('.svg') || resolved.toLowerCase().includes('data:image/svg'));

  return (
    <View style={{ width: cellWidth, height: cellHeight, alignItems: 'center', justifyContent: 'center' }}>
      <Pressable
        onPress={onPress}
        style={{
          width: boxWidth,
          height: cellHeight,
          borderRadius,
          backgroundColor: background,
          borderWidth,
          borderColor: stroke,
          padding: boxPadding,
          overflow: 'hidden',
          justifyContent: 'center',
        }}
      >
        {resolved ? (
          <Image
            source={resolved}
            // Keep the background transparent, and completely bypass layout filters for SVGs
            style={[{ flex: 1, width: '100%', height: '100%' }, highContrast && !isSvg && HC_IMAGE_FILTER]}
            // Natively tint the SVG paths to white when high-contrast mode is enabled
            tintColor={highContrast && isSvg ? '#FFFFFF' : undefined}
            contentFit="contain"
            cachePolicy="memory-disk"
            transition={150}
          />
        ) : (
          <View style={{ flex: 1 }} />
        )}

        {!!label && (
          <Text
            numberOfLines={4}
            style={{
              position: 'absolute',
              left: 4,
              right: 4,
              [textBottom ? 'bottom' : 'top']: 6,
              textAlign: 'center',
              fontSize: 14,
              color: labelColor,
            }}
          >
            {label}
          </Text>
        )}

        {isFolder && (
          <View
            style={{
              position: 'absolute',
              top: foldInset,
              right: foldInset,
              width: 0,
              height: 0,
              borderTopWidth: foldSize,
              borderLeftWidth: foldSize,
              borderTopColor: foldColor,
              borderLeftColor: 'transparent',
            }}
          />
        )}
      </Pressable>
    </View>
  );
}4