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

// Custom color picker (sat/value pad + hue slider).
import React, { useState } from 'react';
import {
  GestureResponderEvent,
  LayoutChangeEvent,
  Modal,
  Pressable,
  View,
} from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useTheme } from '../../theme';
import { Text } from '../themed';
import { fmt } from '../../strings';
import { useStrings } from '../../i18n';
import Slider from '@react-native-community/slider';
import { hexToHsv, hsvToHex } from '../../colors';

const clamp01 = (n: number) => Math.min(1, Math.max(0, n));

export function ColorPickerDialog({
  initialColor,
  onDismiss,
  onConfirm,
}: {
  initialColor: string;
  onDismiss: () => void;
  onConfirm: (hex: string) => void;
}) {
  const t = useTheme();
  const S = useStrings();
  const [{ hue, sat, val }, setHsv] = useState(() => {
    const [h, s, v] = hexToHsv(initialColor);
    return { hue: h, sat: s, val: v };
  });
  const [pad, setPad] = useState({ w: 1, h: 1 });

  const current = hsvToHex(hue, sat, val);
  const pure = hsvToHex(hue, 1, 1);

  const onPadLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    setPad({ w: Math.max(width, 1), h: Math.max(height, 1) });
  };
  const onPadTouch = (e: GestureResponderEvent) => {
    const { locationX, locationY } = e.nativeEvent;
    setHsv((s) => ({ ...s, sat: clamp01(locationX / pad.w), val: 1 - clamp01(locationY / pad.h) }));
  };

  const thumbR = 10;
  const cx = sat * pad.w - thumbR;
  const cy = (1 - val) * pad.h - thumbR;

  return (
    <Modal transparent visible animationType="fade" onRequestClose={onDismiss}>
      <View style={{ flex: 1, backgroundColor: t.scrim, justifyContent: 'center', padding: 24 }}>
        <View style={{ backgroundColor: t.surface, borderRadius: 16, padding: 20 }}>
          <Text style={{ fontSize: 18, fontWeight: 'bold' }}>{S.dialogs.colorPicker.title}</Text>

          <View
            style={{ height: 40, marginTop: 12, backgroundColor: current, borderWidth: 2, borderColor: t.border }}
          />

          <View
            onLayout={onPadLayout}
            onStartShouldSetResponder={() => true}
            onMoveShouldSetResponder={() => true}
            onResponderGrant={onPadTouch}
            onResponderMove={onPadTouch}
            style={{ height: 220, marginTop: 12, borderWidth: 1, borderColor: t.border, overflow: 'hidden' }}
          >
            <LinearGradient
              pointerEvents="none"
              colors={['#FFFFFF', pure]}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 0 }}
              style={{ position: 'absolute', left: 0, top: 0, right: 0, bottom: 0 }}
            />
            <LinearGradient
              pointerEvents="none"
              colors={['rgba(0,0,0,0)', '#000000']}
              start={{ x: 0, y: 0 }}
              end={{ x: 0, y: 1 }}
              style={{ position: 'absolute', left: 0, top: 0, right: 0, bottom: 0 }}
            />
            <View
              pointerEvents="none"
              style={{
                position: 'absolute',
                left: cx,
                top: cy,
                width: thumbR * 2,
                height: thumbR * 2,
                borderRadius: thumbR,
                borderWidth: 2,
                borderColor: '#FFFFFF',
              }}
            />
          </View>

          <Text style={{ fontSize: 12, marginTop: 12 }}>{fmt(S.dialogs.colorPicker.hue, { deg: Math.round(hue) })}</Text>
          <Slider
            style={{ width: '100%', height: 40 }}
            minimumValue={0}
            maximumValue={360}
            value={hue}
            onValueChange={(h) => setHsv((s) => ({ ...s, hue: h }))}
            minimumTrackTintColor={t.primary}
            maximumTrackTintColor={t.surfaceAlt}
            thumbTintColor={t.primary}
          />

          <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginTop: 12 }}>
            <Pressable onPress={onDismiss} style={{ padding: 10, marginRight: 8 }}>
              <Text style={{ fontSize: 16 }}>{S.common.cancel}</Text>
            </Pressable>
            <Pressable onPress={() => onConfirm(current)} style={{ padding: 10 }}>
              <Text style={{ fontSize: 16, fontWeight: 'bold', color: t.primary }}>{S.dialogs.colorPicker.ok}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
