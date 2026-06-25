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

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Accept');

  // Handle preflight OPTIONS request
  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  // Reject anything that isn't a POST request
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method Not Allowed' });
  }

  try {
    const { title, body } = req.body;

    if (!title || !body) {
      return res.status(400).json({ error: 'Missing title or body parameters.' });
    }

    const GITHUB_TOKEN = process.env.GITHUB_ACCESS_TOKEN;
    
    const OWNER = 'hkleinkeane'; 
    const REPO = 'spegen';

    if (!GITHUB_TOKEN) {
      return res.status(500).json({ error: 'Server configuration error: GITHUB_ACCESS_TOKEN is missing.' });
    }

    const githubResponse = await fetch(`https://api.github.com/repos/${OWNER}/${REPO}/issues`, {
      method: 'POST',
      headers: {
        'Authorization': `token ${GITHUB_TOKEN}`,
        'Accept': 'application/vnd.github.v3+json',
        'Content-Type': 'application/json',
        'User-Agent': 'SpeGen-App-Backend',
      },
      body: JSON.stringify({ title, body }),
    });

    const data = await githubResponse.json().catch(() => ({}));

    if (githubResponse.ok) {
      return res.status(201).json({ success: true, issue: data });
    } else {
      return res.status(githubResponse.status).json({ 
        error: 'GitHub API responded with an error', 
        details: data.message || githubResponse.statusText 
      });
    }

  } catch (error) {
    return res.status(500).json({ 
      error: 'Internal Server Error', 
      details: error.message 
    });
  }
}