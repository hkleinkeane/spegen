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

// OpenSymbols token proxy (web build). Browsers are CORS-blocked from opensymbols.org, so this
// same-origin Vercel function requests a session token server-side. The OpenSymbols secret is read
// from the OPENSYMBOLS_ACCESS_TOKEN env var (falling back to the public shared secret from the
// original open-source app if it's unset), so the key is never exposed to the browser.
export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const secret = process.env.OPENSYMBOLS_ACCESS_TOKEN || 'd65234627cc790cba662f6b3';

  try {
    const upstream = await fetch('https://www.opensymbols.org/api/v2/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: 'secret=' + encodeURIComponent(secret),
    });
    const text = await upstream.text();
    res.setHeader('Content-Type', 'application/json');
    return res.status(upstream.status).send(text);
  } catch (error) {
    return res.status(502).json({ error: 'OpenSymbols token request failed', details: error.message });
  }
}
