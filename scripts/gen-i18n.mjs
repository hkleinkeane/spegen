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
 * Generates per-language translations of the UI strings.
 *
 * Reads src/strings.ts (the English source of truth), translates every user-facing leaf string
 * into each language in APP_LANGUAGES via the free Google Translate "gtx" endpoint, and writes:
 *   - src/translations/<code>.json   (full translated string table per language)
 *   - src/translations/index.ts      (a TRANSLATIONS map the runtime imports)
 *
 * Placeholders like {name} are protected so the translator can't mangle them. URLs, the app name,
 * and link labels are left untranslated. Re-run with:  node scripts/gen-i18n.mjs
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const ts = require('typescript');
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const OUT_DIR = path.join(ROOT, 'src', 'translations');

// --- load the English STRINGS object by transpiling strings.ts ---------------------------------
function loadStrings() {
  const src = fs.readFileSync(path.join(ROOT, 'src', 'strings.ts'), 'utf8');
  const js = ts.transpileModule(src, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  const mod = { exports: {} };
  // eslint-disable-next-line no-new-func
  new Function('exports', 'require', 'module', js)(mod.exports, require, mod);
  return mod.exports.STRINGS;
}

// --- language list from types.ts ---------------------------------------------------------------
function loadLanguages() {
  const src = fs.readFileSync(path.join(ROOT, 'src', 'types.ts'), 'utf8');
  const langs = [];
  const re = /\{\s*name:\s*'([^']+)',\s*code:\s*'([^']+)'\s*\}/g;
  let m;
  while ((m = re.exec(src))) langs.push({ name: m[1], code: m[2] });
  return langs;
}

// Google Translate code overrides where they differ from our app code.
const GTX_CODE = { fil: 'tl', no: 'no', zh: 'zh-CN' };

// Set this to use the reliable official API (recommended). Get a key from Google Cloud Console →
// enable "Cloud Translation API" → create an API key. Run: GOOGLE_TRANSLATE_API_KEY=xxx node scripts/gen-i18n.mjs
// (PowerShell: $env:GOOGLE_TRANSLATE_API_KEY="xxx"; node scripts/gen-i18n.mjs). Without it, the free endpoint is used.
const API_KEY = process.env.GOOGLE_TRANSLATE_API_KEY || '';
// `--sync`: instead of skipping existing language files, fill in only the keys they're missing
// (i.e. strings added to strings.ts since they were generated). Much faster than a full rebuild.
const SYNC = process.argv.includes('--sync');

// Leaf values we never translate: URLs, the brand name, link labels.
const SKIP_PATHS = new Set([
  'settings.about.appName',
  'settings.about.githubLabel',
  'settings.about.githubUrl',
  'settings.about.websiteLabel',
  'settings.about.websiteUrl',
]);
const isUrl = (s) => /^https?:\/\//.test(s);
const hasLetters = (s) => /\p{L}/u.test(s);

// --- flatten / rebuild -------------------------------------------------------------------------
function walk(v, p, out) {
  if (typeof v === 'string') out.push({ path: p, value: v });
  else if (Array.isArray(v)) v.forEach((el, i) => walk(el, `${p}.${i}`, out));
  else if (v && typeof v === 'object') flatten(v, p, out);
}
function flatten(obj, prefix, out) {
  for (const k of Object.keys(obj)) {
    walk(obj[k], prefix ? `${prefix}.${k}` : k, out);
  }
}
function setPath(obj, p, value) {
  const parts = p.split('.');
  let cur = obj;
  for (let i = 0; i < parts.length - 1; i++) cur = cur[parts[i]];
  cur[parts[parts.length - 1]] = value;
}

// --- placeholder protection --------------------------------------------------------------------
// Replace {token} with §§N§§ markers (which survive MT), remember the order, restore afterwards.
function protect(text) {
  const tokens = [];
  const masked = text.replace(/\{(\w+)\}/g, (_, name) => {
    tokens.push(name);
    return `§§${tokens.length - 1}§§`;
  });
  return { masked, tokens };
}
function restore(text, tokens) {
  return text.replace(/§§\s*(\d+)\s*§§/g, (_, n) => `{${tokens[Number(n)] ?? n}}`);
}

// --- gtx translation ---------------------------------------------------------------------------
async function gtx(text, tl) {
  const body = new URLSearchParams({ client: 'gtx', sl: 'en', tl, dt: 't', q: text }).toString();
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), 15000); // abort a stalled request rather than hang
  try {
    const res = await fetch('https://translate.googleapis.com/translate_a/single', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
      signal: ctrl.signal,
    });
    if (!res.ok) throw new Error(`gtx ${res.status}`);
    const json = await res.json();
    return (json[0] || []).map((seg) => (Array.isArray(seg) ? seg[0] : '')).join('');
  } finally {
    clearTimeout(timer);
  }
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Official Google Cloud Translation API (v2). Reliable array batching — results come back in order,
// no line-count guessing. Used when GOOGLE_TRANSLATE_API_KEY is set; otherwise the free gtx endpoint.
async function officialBatch(masked, tl) {
  const res = await fetch(`https://translation.googleapis.com/language/translate/v2?key=${API_KEY}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ q: masked.map((m) => m.masked), source: 'en', target: tl, format: 'text' }),
  });
  if (!res.ok) throw new Error(`${res.status}: ${(await res.text()).slice(0, 140)}`);
  const json = await res.json();
  const tr = json.data.translations.map((t) => t.translatedText);
  if (tr.length !== masked.length) throw new Error('returned wrong count');
  return tr.map((line, i) => restore(line, masked[i].tokens));
}

// Translate an array of strings. Prefers the official API (reliable); otherwise a gtx newline batch
// with a per-string fallback so no string is left in English just because the lines didn't line up.
async function translateBatch(values, tl) {
  const masked = values.map(protect);
  if (API_KEY) {
    try {
      return await officialBatch(masked, tl);
    } catch (e) {
      console.log(`   (official API failed: ${e.message} — using free endpoint)`);
    }
  }
  const joined = masked.map((m) => m.masked).join('\n');
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      const lines = (await gtx(joined, tl)).split('\n');
      if (lines.length === values.length) return lines.map((line, i) => restore(line, masked[i].tokens));
    } catch {
      /* retry / per-string fallback below */
    }
    await sleep(200);
  }
  // Per-string fallback: translate each individually so the mismatch only costs requests, not coverage.
  const out = [];
  for (let i = 0; i < masked.length; i++) {
    let r = masked[i].masked;
    try {
      const t = await gtx(masked[i].masked, tl);
      if (t && t.trim()) r = t;
    } catch {
      /* keep English for this one */
    }
    out.push(restore(r, masked[i].tokens));
    await sleep(100);
  }
  return out;
}

function chunk(arr, n) {
  const out = [];
  for (let i = 0; i < arr.length; i += n) out.push(arr.slice(i, i + n));
  return out;
}

function getPath(obj, p) {
  return p.split('.').reduce((o, k) => (o == null ? undefined : o[k]), obj);
}

async function main() {
  const STRINGS = loadStrings();
  const langs = loadLanguages().filter((l) => l.code !== 'en');
  const leaves = [];
  flatten(STRINGS, '', leaves);
  const translatable = leaves.filter(
    (l) => !SKIP_PATHS.has(l.path) && !isUrl(l.value) && hasLetters(l.value)
  );
  console.log(`Strings: ${leaves.length} leaves, ${translatable.length} translatable, ${langs.length} languages.`);

  fs.mkdirSync(OUT_DIR, { recursive: true });
  for (const lang of langs) {
    const file = path.join(OUT_DIR, `${lang.code}.json`);
    const exists = fs.existsSync(file);
    if (exists && !SYNC) {
      console.log(`  • ${lang.code} (${lang.name}) — already present, skipping`);
      continue;
    }
    const tl = GTX_CODE[lang.code] || lang.code;
    const result = exists ? JSON.parse(fs.readFileSync(file, 'utf8')) : JSON.parse(JSON.stringify(STRINGS));
    // Full file when new; in --sync only the keys this file is missing (newly added to strings.ts).
    const todo = exists && SYNC ? translatable.filter((leaf) => getPath(result, leaf.path) === undefined) : translatable;
    if (todo.length === 0) {
      console.log(`  • ${lang.code} (${lang.name}) — up to date`);
      continue;
    }
    try {
      const translated = [];
      for (const g of chunk(todo, 40)) {
        translated.push(...(await translateBatch(g.map((x) => x.value), tl)));
        await sleep(150);
      }
      todo.forEach((leaf, i) => setPath(result, leaf.path, translated[i]));
      fs.writeFileSync(file, JSON.stringify(result, null, 2) + '\n');
      console.log(`  ✓ ${lang.code} (${lang.name})${exists ? ` — added ${todo.length} key(s)` : ''}`);
    } catch (e) {
      console.log(`  ✗ ${lang.code} (${lang.name}): ${e.message}`);
    }
  }

  // index.ts — a static map of every translation file currently on disk.
  const done = fs
    .readdirSync(OUT_DIR)
    .filter((f) => f.endsWith('.json'))
    .map((f) => f.replace(/\.json$/, ''))
    .sort();
  const imports = done.map((c) => `import ${c.replace(/[^a-z0-9]/gi, '_')} from './${c}.json';`).join('\n');
  const entries = done.map((c) => `  '${c}': ${c.replace(/[^a-z0-9]/gi, '_')},`).join('\n');
  const indexTs =
    `/* AUTO-GENERATED by scripts/gen-i18n.mjs — do not edit by hand. */\n` +
    `${imports}\n\n` +
    `// Loosely typed on purpose: a translation may lag the English source by a few keys; i18n.ts\n` +
    `// deep-merges each over English, so any missing key falls back to English rather than erroring.\n` +
    `export const TRANSLATIONS: Record<string, unknown> = {\n${entries}\n};\n`;
  fs.writeFileSync(path.join(OUT_DIR, 'index.ts'), indexTs);
  console.log(`\nWrote ${done.length} languages + index.ts to src/translations/`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
