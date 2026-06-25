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

// Learn more https://docs.expo.io/guides/customizing-metro
const { getDefaultConfig } = require('expo/metro-config');

/** @type {import('expo/metro-config').MetroConfig} */
const config = getDefaultConfig(__dirname);

// Dev-only OpenSymbols proxy so the WEB build works on localhost (`expo start --web`), where there
// is no serverless backend. It mirrors the production Vercel functions (api/opensymbols/*) at the
// SAME relative paths, so the app code is identical in dev and prod and the browser is never
// CORS-blocked. This runs only on the Metro dev server — it has no effect on native, on `expo
// export`, or on production hosting.
const OPENSYMBOLS = 'https://www.opensymbols.org/api/v2';
// OpenSymbols secret: the OPENSYMBOLS_ACCESS_TOKEN env var (set it in your shell for local web dev),
// falling back to the public shared secret from the open-source app.
const CLIENT_SECRET = process.env.OPENSYMBOLS_ACCESS_TOKEN || 'd65234627cc790cba662f6b3';

const prevEnhance = config.server.enhanceMiddleware;
config.server.enhanceMiddleware = (metroMiddleware, server) => {
  const base = prevEnhance ? prevEnhance(metroMiddleware, server) : metroMiddleware;
  return async (req, res, next) => {
    const url = req.url || '';
    try {
      if (req.method === 'POST' && url.startsWith('/api/opensymbols/token')) {
        const upstream = await fetch(`${OPENSYMBOLS}/token`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: 'secret=' + encodeURIComponent(CLIENT_SECRET),
        });
        const text = await upstream.text();
        res.statusCode = upstream.status;
        res.setHeader('Content-Type', 'application/json');
        res.end(text);
        return;
      }
      if (url.startsWith('/api/opensymbols/symbols')) {
        const qs = url.includes('?') ? url.slice(url.indexOf('?')) : '';
        const upstream = await fetch(`${OPENSYMBOLS}/symbols${qs}`);
        const text = await upstream.text();
        res.statusCode = upstream.status;
        res.setHeader('Content-Type', 'application/json');
        res.end(text);
        return;
      }
      // Dev mirror of api/translate.js (the Google Translate gtx proxy) for local web dev.
      if (req.method === 'POST' && url.startsWith('/api/translate')) {
        const chunks = [];
        for await (const c of req) chunks.push(c);
        let payload = {};
        try {
          payload = JSON.parse(Buffer.concat(chunks).toString('utf8') || '{}');
        } catch {
          payload = {};
        }
        const { q, tl, sl = 'auto' } = payload;
        const upstream = await fetch('https://translate.googleapis.com/translate_a/single', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({ client: 'gtx', sl, tl: tl || '', dt: 't', q: q || '' }).toString(),
        });
        const text = await upstream.text();
        res.statusCode = upstream.status;
        res.setHeader('Content-Type', 'application/json');
        res.end(text);
        return;
      }
    } catch (e) {
      res.statusCode = 502;
      res.setHeader('Content-Type', 'application/json');
      res.end(JSON.stringify({ error: String(e) }));
      return;
    }
    return base(req, res, next);
  };
};

module.exports = config;
