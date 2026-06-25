/* Prepend the GPL-3.0 license header to every source file (skips files that already have it). */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const EXTS = new Set(['.ts', '.tsx', '.js', '.mjs', '.cjs']);
const SKIP_DIRS = new Set(['node_modules', 'dist', 'android', 'ios', '.git', '.expo', '.vercel', 'build']);

const HEADER = `/*
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
`;

let changed = 0;
function walk(dir) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.isDirectory()) {
      if (!SKIP_DIRS.has(e.name)) walk(path.join(dir, e.name));
      continue;
    }
    if (!EXTS.has(path.extname(e.name))) continue;
    const file = path.join(dir, e.name);
    const src = fs.readFileSync(file, 'utf8');
    if (src.includes('GNU General Public License')) continue; // already has it
    fs.writeFileSync(file, HEADER + '\n' + src);
    changed++;
  }
}
walk(ROOT);
console.log(`Added GPL header to ${changed} files.`);
