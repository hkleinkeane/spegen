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

/*
 * SpeGen service worker — offline support via runtime caching (no build-time precache list, so it
 * survives the hashed bundle filenames Expo emits). Strategy:
 *   - navigations: network-first, fall back to the cached app shell (so the app opens offline);
 *   - the OpenSymbols proxy (/api/*): never cached (needs fresh data; fails gracefully offline);
 *   - everything else (JS bundle, assets, remote symbol images): cache-first, filled at runtime,
 *     so anything you've loaded once keeps working without a connection.
 * Bump CACHE to force every client to drop old cached assets.
 */
const CACHE = 'spegen-v1';
const SHELL = ['/', '/index.html', '/manifest.webmanifest', '/favicon.ico', '/pwa-icon-192.png', '/pwa-icon-512.png'];

self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL).catch(() => undefined)));
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);
  // Never cache the OpenSymbols proxy (needs live data) or version.json (drives the update check).
  if (
    url.origin === self.location.origin &&
    (url.pathname.startsWith('/api/') || url.pathname === '/version.json')
  ) {
    return;
  }

  // App navigations: network-first so updates land immediately, with the cached shell as fallback.
  if (req.mode === 'navigate') {
    event.respondWith(
      fetch(req)
        .then((res) => {
          const copy = res.clone();
          caches.open(CACHE).then((c) => c.put('/index.html', copy)).catch(() => undefined);
          return res;
        })
        .catch(() => caches.match('/index.html').then((r) => r || caches.match('/')))
    );
    return;
  }

  // Bundle / assets / remote symbol images: cache-first, populate the cache as they're fetched.
  event.respondWith(
    caches.match(req).then((cached) => {
      if (cached) return cached;
      return fetch(req)
        .then((res) => {
          if (res && (res.status === 200 || res.type === 'opaque')) {
            const copy = res.clone();
            caches.open(CACHE).then((c) => c.put(req, copy)).catch(() => undefined);
          }
          return res;
        })
        .catch(() => cached);
    })
  );
});
