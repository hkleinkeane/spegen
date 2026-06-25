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
 * Web build for Vercel. Stamps a unique build id into the bundle (EXPO_PUBLIC_BUILD_ID, inlined by
 * Metro) AND writes the same id to dist/version.json. The running app fetches /version.json and, if
 * it differs from its own inlined id, shows an "update available" reload prompt — so users (iOS
 * home-screen included) reliably pick up new deploys. See src/components/UpdateBanner.tsx.
 */
const { execSync } = require('child_process');
const fs = require('fs');

// Prefer the git commit SHA (stable per commit) when Vercel provides it; otherwise a timestamp.
const buildId =
  process.env.VERCEL_GIT_COMMIT_SHA || Date.now().toString(36) + Math.random().toString(36).slice(2, 8);

process.env.EXPO_PUBLIC_BUILD_ID = buildId;
execSync('npx expo export --platform web', { stdio: 'inherit', env: process.env });
fs.writeFileSync('dist/version.json', JSON.stringify({ build: buildId }) + '\n');
console.log('Built web bundle, version:', buildId);
