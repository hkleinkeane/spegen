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

// Update available banner (web/PWA).
import React, { useEffect, useState } from 'react';
import { Platform, Pressable, View } from 'react-native';
import { Text } from './themed';
import { useStrings } from '../i18n';

const RUNNING_BUILD = process.env.EXPO_PUBLIC_BUILD_ID || '';

export function UpdateBanner() {
  const [show, setShow] = useState(false);
  const S = useStrings();

  useEffect(() => {
    if (Platform.OS !== 'web' || !RUNNING_BUILD) return;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const g = globalThis as any;
    let cancelled = false;

    const check = async () => {
      try {
        const res = await g.fetch('/version.json', { cache: 'no-store' });
        if (!res.ok) return;
        const data = await res.json();
        if (!cancelled && data && data.build && data.build !== RUNNING_BUILD) setShow(true);
      } catch {
        // ignore — offline, or version.json not deployed yet
      }
    };

    check();
    const onFocus = () => check();
    g.addEventListener?.('focus', onFocus);
    const interval = g.setInterval?.(check, 5 * 60 * 1000); // re-check every 5 min while open
    return () => {
      cancelled = true;
      g.removeEventListener?.('focus', onFocus);
      if (interval) g.clearInterval(interval);
    };
  }, []);

  if (Platform.OS !== 'web' || !show) return null;

  const reload = async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const g = globalThis as any;
    try {
      if (g.caches?.keys) {
        const keys = await g.caches.keys();
        await Promise.all(keys.map((k: string) => g.caches.delete(k)));
      }
    } catch {
      // ignore
    }
    g.location?.reload();
  };

  return (
    <View
      pointerEvents="box-none"
      style={{ position: 'absolute', bottom: 16, left: 12, right: 12, alignItems: 'center', zIndex: 2000 }}
    >
      <Pressable
        onPress={reload}
        style={{
          backgroundColor: '#1976D2',
          borderRadius: 24,
          paddingHorizontal: 18,
          paddingVertical: 12,
          shadowColor: '#000000',
          shadowOpacity: 0.25,
          shadowRadius: 8,
          shadowOffset: { width: 0, height: 2 },
          elevation: 6,
        }}
      >
        <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.banners.updateAvailable}</Text>
      </Pressable>
    </View>
  );
}
