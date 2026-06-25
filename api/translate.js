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
 * Serverless proxy for the public, keyless Google Translate "gtx" endpoint.
 *
 * Browsers cannot call translate.googleapis.com directly — it returns no Access-Control-Allow-Origin
 * header, so the request is CORS-blocked. The web (and native) builds therefore route runtime label
 * translation through here; this function calls gtx server-side (where CORS doesn't apply) and
 * returns the raw gtx JSON with permissive CORS headers. No API key or secret is involved.
 */
export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  if (req.method === 'OPTIONS') {
    res.status(204).end();
    return;
  }
  if (req.method !== 'POST') {
    res.status(405).json({ error: 'POST only' });
    return;
  }

  const { q, tl, sl = 'auto' } = req.body || {};
  if (!q || !tl) {
    res.status(400).json({ error: 'missing q/tl' });
    return;
  }

  res.setHeader('Content-Type', 'application/json');

  // Preferred: official Cloud Translation API when GOOGLE_TRANSLATE_API_KEY is set in Vercel. Its
  // result is reshaped to the gtx response format so the client parser stays unchanged.
  const key = process.env.GOOGLE_TRANSLATE_API_KEY;
  if (key) {
    try {
      const up = await fetch(`https://translation.googleapis.com/language/translate/v2?key=${key}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ q, source: 'en', target: tl, format: 'text' }),
      });
      if (up.ok) {
        const j = await up.json();
        const text = j?.data?.translations?.[0]?.translatedText ?? q;
        res.status(200).send(JSON.stringify([[[text, q]]])); // gtx-shaped
        return;
      }
      // fall through to the free endpoint on a non-200
    } catch {
      // fall through
    }
  }

  const body = new URLSearchParams({ client: 'gtx', sl, tl, dt: 't', q }).toString();
  try {
    const upstream = await fetch('https://translate.googleapis.com/translate_a/single', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
    });
    const text = await upstream.text();
    res.status(upstream.status).send(text);
  } catch (e) {
    res.status(502).json({ error: String(e) });
  }
}
