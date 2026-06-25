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

// OpenSymbols symbol-search proxy (web build) — forwards the query server-side so the browser isn't
// CORS-blocked. The access_token the client passes was obtained via /api/opensymbols/token (which
// uses the server-side OPENSYMBOLS_ACCESS_TOKEN secret), so no key is exposed to the browser.
export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  try {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(req.query || {})) {
      params.set(key, Array.isArray(value) ? value[0] : String(value));
    }
    const upstream = await fetch('https://www.opensymbols.org/api/v2/symbols?' + params.toString());
    const text = await upstream.text();
    res.setHeader('Content-Type', 'application/json');
    return res.status(upstream.status).send(text);
  } catch (error) {
    return res.status(502).json({ error: 'OpenSymbols search failed', details: error.message });
  }
}
