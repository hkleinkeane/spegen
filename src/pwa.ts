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

// PWA setup. Web only.
import { Platform } from 'react-native';

export function registerPWA(): void {
  if (Platform.OS !== 'web') return;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const g = globalThis as any;
  const doc = g.document;
  const nav = g.navigator;
  if (!doc || !doc.head) return;

  const addHead = (tag: string, attrs: Record<string, string>, key: string) => {
    if (doc.querySelector(`[data-pwa="${key}"]`)) return;
    const el = doc.createElement(tag);
    el.setAttribute('data-pwa', key);
    for (const k in attrs) el.setAttribute(k, attrs[k]);
    doc.head.appendChild(el);
  };

  addHead('link', { rel: 'manifest', href: '/manifest.webmanifest' }, 'manifest');
  addHead('meta', { name: 'theme-color', content: '#1976D2' }, 'theme');
  addHead('link', { rel: 'apple-touch-icon', href: '/pwa-icon-192.png' }, 'apple-icon');
  addHead('meta', { name: 'apple-mobile-web-app-capable', content: 'yes' }, 'apple-cap');
  addHead('meta', { name: 'apple-mobile-web-app-title', content: 'SpeGen' }, 'apple-title');

  if (nav && 'serviceWorker' in nav) {
    const register = () => nav.serviceWorker.register('/sw.js').catch(() => undefined);
    if (doc.readyState === 'complete') register();
    else if (g.addEventListener) g.addEventListener('load', register);
  }
}
