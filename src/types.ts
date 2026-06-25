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

// Core data model.
import * as Crypto from 'expo-crypto';
import { seedNgramModel, type NgramModel } from './ngram';

export type { NgramModel };

// MenuTemplate data.
export interface MenuTemplate {
  id: number; // ID of the current menu
  title: string; // Title of the current menu
  item_list: string[]; // names of all items (folders and symbols)
  pointers: (number | null)[]; // folder -> linked menu id; null for symbols
  tts: (number | null)[]; // 0=add to input, 1=speak, 2=both; null for folders
  item_type: boolean[]; // false=folder, true=symbol
  image_urls: string[]; // resolved OpenSymbols URLs
  item_uuids: string[]; // one UUID per item
  custom_image_paths: string[]; // custom image paths
  custom_audio_paths: string[]; // recorded/imported audio file path
  custom_audio_names: string[]; // recorded/imported audio file name
  pronunciation_overrides: string[]; // phonetic respelling text
  colors: string[]; // hex like "#FFEB3B" or category name; "" = default white
  item_locales: string[]; // "" = use default (image locale override)
  item_translations: Record<string, string>[]; // langCode -> label
  item_tts_locales: string[]; // langCode for TTS; "" = default
}

// Pad per-item lists.
export function padMenu(menu: MenuTemplate): MenuTemplate {
  const n = menu.item_list.length;
  const pad = <T>(arr: T[] | undefined, fill: () => T): T[] => {
    const out = (arr ?? []).slice(0, Math.max(arr?.length ?? 0, n));
    while (out.length < n) out.push(fill());
    return out;
  };
  return {
    ...menu,
    item_uuids: pad(menu.item_uuids, () => Crypto.randomUUID()),
    image_urls: pad(menu.image_urls, () => ''),
    custom_image_paths: pad(menu.custom_image_paths, () => ''),
    custom_audio_paths: pad(menu.custom_audio_paths, () => ''),
    custom_audio_names: pad(menu.custom_audio_names, () => ''),
    pronunciation_overrides: pad(menu.pronunciation_overrides, () => ''),
    colors: pad(menu.colors, () => ''),
    item_locales: pad(menu.item_locales, () => ''),
    item_translations: pad(menu.item_translations, () => ({})),
    item_tts_locales: pad(menu.item_tts_locales, () => ''),
  };
}

// MenuTemplate constructor.
export function makeMenu(
  id: number,
  title: string,
  item_list: string[],
  pointers: (number | null)[],
  tts: (number | null)[],
  item_type: boolean[],
  extra: Partial<MenuTemplate> = {}
): MenuTemplate {
  return padMenu({
    id,
    title,
    item_list,
    pointers,
    tts,
    item_type,
    image_urls: [],
    item_uuids: [],
    custom_image_paths: [],
    custom_audio_paths: [],
    custom_audio_names: [],
    pronunciation_overrides: [],
    colors: [],
    item_locales: [],
    item_translations: [],
    item_tts_locales: [],
    ...extra,
  });
}

export const DEFAULT_MENUS: MenuTemplate[] = [
  makeMenu(
    0,
    'Home',
    ['I', 'want', 'more', 'help', 'yes', 'no', 'stop', 'please', 'People', 'Actions', 'Food', 'Feelings'],
    [null, null, null, null, null, null, null, null, 1, 2, 3, 4],
    [2, 2, 2, 2, 2, 2, 2, 2, null, null, null, null],
    [true, true, true, true, true, true, true, true, false, false, false, false]
  ),
  makeMenu(
    1,
    'People',
    ['you', 'me', 'mom', 'dad', 'sister', 'brother', 'friend', 'teacher'],
    [null, null, null, null, null, null, null, null],
    [2, 2, 2, 2, 2, 2, 2, 2],
    [true, true, true, true, true, true, true, true]
  ),
  makeMenu(
    2,
    'Actions',
    ['eat', 'drink', 'play', 'go', 'sleep', 'read', 'watch', 'listen'],
    [null, null, null, null, null, null, null, null],
    [2, 2, 2, 2, 2, 2, 2, 2],
    [true, true, true, true, true, true, true, true]
  ),
  makeMenu(
    3,
    'Food',
    ['water', 'milk', 'apple', 'banana', 'sandwich', 'pizza', 'cookie', 'snack'],
    [null, null, null, null, null, null, null, null],
    [2, 2, 2, 2, 2, 2, 2, 2],
    [true, true, true, true, true, true, true, true]
  ),
  makeMenu(
    4,
    'Feelings',
    ['happy', 'sad', 'tired', 'sick', 'hungry', 'thirsty', 'scared', 'excited'],
    [null, null, null, null, null, null, null, null],
    [2, 2, 2, 2, 2, 2, 2, 2],
    [true, true, true, true, true, true, true, true]
  ),
];

// Fitzgerald color key (word categories). hex strings as stored.
export interface FitzgeraldCategory {
  name: string;
  colorHex: string;
}

export const DEFAULT_FITZGERALD_KEY: FitzgeraldCategory[] = [
  { name: 'Pronoun', colorHex: '#FFEB3B' }, // yellow
  { name: 'Noun', colorHex: '#FF9800' }, // orange
  { name: 'Verb', colorHex: '#4CAF50' }, // green
  { name: 'Adjective', colorHex: '#2196F3' }, // blue
  { name: 'Social', colorHex: '#E91E63' }, // pink
  { name: 'Question', colorHex: '#9C27B0' }, // purple
  { name: 'Adverb', colorHex: '#795548' }, // brown
  { name: 'Determiner', colorHex: '#FFFFFF' }, // white
  { name: 'Other', colorHex: '#BDBDBD' }, // gray
];

// Skin tones for OpenSymbols variant substitution.
export interface SkinTone {
  name: string;
  color: string; // swatch color
  hexCode: string; // "1f3fb", "" for default
  skinKey: string; // "light", "medium-light", etc.
}

export const SKIN_TONES: SkinTone[] = [
  { name: 'Light', color: '#F5DEB3', hexCode: '1f3fb', skinKey: 'light' },
  { name: 'Medium light', color: '#DDB892', hexCode: '1f3fc', skinKey: 'medium-light' },
  { name: 'Medium', color: '#C68863', hexCode: '1f3fd', skinKey: 'medium' },
  { name: 'Medium dark', color: '#8B5A3C', hexCode: '1f3fe', skinKey: 'medium-dark' },
  { name: 'Dark', color: '#5C4033', hexCode: '1f3ff', skinKey: 'dark' },
];

// Button shape options (each maps to a borderRadius value).
// Circle uses a large radius which, on a square button, renders as a circle.
export interface ButtonShapeOption {
  name: string;
  radius: number;
}

export const BUTTON_SHAPES: ButtonShapeOption[] = [
  { name: 'Square', radius: 0 },
  { name: 'Soft', radius: 15 },
  { name: 'Rounded', radius: 40 },
  { name: 'Circle', radius: 9999 },
];

// Languages for the picker — a curated list of common languages.
export interface AppLanguage {
  name: string;
  code: string;
}

export const APP_LANGUAGES: AppLanguage[] = [
  { name: 'English', code: 'en' },
  { name: 'Spanish', code: 'es' },
  { name: 'French', code: 'fr' },
  { name: 'German', code: 'de' },
  { name: 'Italian', code: 'it' },
  { name: 'Portuguese', code: 'pt' },
  { name: 'Dutch', code: 'nl' },
  { name: 'Polish', code: 'pl' },
  { name: 'Russian', code: 'ru' },
  { name: 'Ukrainian', code: 'uk' },
  { name: 'Czech', code: 'cs' },
  { name: 'Slovak', code: 'sk' },
  { name: 'Romanian', code: 'ro' },
  { name: 'Hungarian', code: 'hu' },
  { name: 'Greek', code: 'el' },
  { name: 'Turkish', code: 'tr' },
  { name: 'Arabic', code: 'ar' },
  { name: 'Hebrew', code: 'he' },
  { name: 'Hindi', code: 'hi' },
  { name: 'Bengali', code: 'bn' },
  { name: 'Urdu', code: 'ur' },
  { name: 'Persian', code: 'fa' },
  { name: 'Thai', code: 'th' },
  { name: 'Vietnamese', code: 'vi' },
  { name: 'Indonesian', code: 'id' },
  { name: 'Malay', code: 'ms' },
  { name: 'Filipino', code: 'fil' },
  { name: 'Chinese', code: 'zh' },
  { name: 'Japanese', code: 'ja' },
  { name: 'Korean', code: 'ko' },
  { name: 'Swedish', code: 'sv' },
  { name: 'Norwegian', code: 'no' },
  { name: 'Danish', code: 'da' },
  { name: 'Finnish', code: 'fi' },
  { name: 'Icelandic', code: 'is' },
  { name: 'Croatian', code: 'hr' },
  { name: 'Serbian', code: 'sr' },
  { name: 'Bulgarian', code: 'bg' },
  { name: 'Lithuanian', code: 'lt' },
  { name: 'Latvian', code: 'lv' },
  { name: 'Estonian', code: 'et' },
  { name: 'Catalan', code: 'ca' },
  { name: 'Afrikaans', code: 'af' },
  { name: 'Swahili', code: 'sw' },
];

export const DEFAULT_STATIC_TERMS: string[] = [
  'Yes',
  'No',
  'Thank you',
  'I need help',
  'Excuse me',
  'I use a talker to communicate',
];

/*
 * The persisted app state. Layout-derived dimensions (static_row_height, menu_static_row_height,
 * button_boxes_width, input_box_height) are recomputed from screen size at runtime, so they default
 * to 0 here. has_seen_tutorial true = the user has dismissed the tutorial; boot shows the tutorial
 * when it is false.
 */
// One caregiver-lock recovery question. The answer is stored only as a salted hash (see
// PersistedState.security_salt); the question text is plaintext so it can be displayed at recovery.
export interface SecurityQuestion {
  question: string;
  answerHash: string;
}

export type ThemeMode = 'light' | 'dark' | 'system';

export interface PersistedState {
  box_width_size_dp: number;
  box_height_size_dp: number;
  box_padding_dp: number;
  input_box_height_dp: number;
  item_text_padding_dp: number;
  has_seen_tutorial: boolean;
  tts_data_found: boolean;
  menu_list: MenuTemplate[];
  static_terms: string[];
  static_row_height: number;
  menu_static_row_height: number;
  button_boxes_width: number;
  menu_row_ids: number[];
  tts_speech_rate: number;
  tts_pitch: number;
  tts_pause_between_words: boolean;
  tts_pause_duration: number;
  // Chosen TTS voice identifier ('' = the engine/browser default). Applied only when the voice's
  // language matches the utterance, so translated multi-language speech still uses the right voice.
  tts_voice: string;
  static_row_text_size: number;
  static_row_text_padding: number;
  menu_row_text_size: number;
  menu_row_text_padding: number;
  ngram_model: NgramModel;
  fitzgerald_overrides: Record<string, string>;
  fitzgeraldKey: FitzgeraldCategory[];
  highcontrastmode: boolean;
  // Dark mode. 'system' follows the device. Themes app chrome only — the
  // board's AAC-coloured tiles and high-contrast mode are unaffected. Default 'light'.
  theme_mode: ThemeMode;
  skin_tone: string;
  text_location_bottom: boolean;
  button_shape_name: string;
  item_border_width_dp: number;
  app_locale: string;
  language_image_override: boolean;
  // Kept only to let users author alternate per-item TEXT labels per language in edit mode.
  // The board always shows the original (English-authored) label; a manual label overrides the
  // auto-translation for SPEECH when its language is the active input-box language.
  multilingual_labels: boolean;
  // Auto-translation toggles (default on). ui = translate the app interface (chrome); labels =
  // auto-translate board/menu/static labels; sentence = translate the whole spoken sentence at once
  // (better grammar than word-by-word). All only have effect for a non-English app language.
  auto_translate_ui: boolean;
  auto_translate_labels: boolean;
  auto_translate_sentence: boolean;
  // The currently-selected input-box language ('' = the app-language base). Decides which
  // language the input box speaks in. Volatile (changes during normal use), so excluded from the
  // unsaved-changes snapshot.
  current_board_language: string;
  // User-managed list of extra spoken languages shown in the input-box picker. The app
  // language is the implicit, non-removable base and is NOT stored here. Add/delete/reorder in
  // Settings → Language.
  language_list: string[];
  // Persisted auto-translation cache (`${targetLang}|${lowercased label}` -> translated
  // text). Filled lazily by the board (online, best-effort) so speech works offline afterwards.
  // Volatile, so excluded from the unsaved-changes snapshot.
  translations: Record<string, string>;
  // Caregiver lock. Empty hash = no PIN set (feature off). The PIN and each recovery answer
  // are stored only as salted SHA-256 hashes (never plaintext); the salt is a random per-install
  // value. It is a child-lock deterrent, not real security — a short PIN hash is brute-forceable.
  security_pin_hash: string;
  security_salt: string;
  security_questions: SecurityQuestion[];
}

export function defaultPersistedState(): PersistedState {
  return {
    box_width_size_dp: 100,
    box_height_size_dp: 100,
    box_padding_dp: 20,
    input_box_height_dp: 0,
    item_text_padding_dp: 20,
    has_seen_tutorial: false,
    tts_data_found: false,
    menu_list: DEFAULT_MENUS.map(padMenu),
    static_terms: [...DEFAULT_STATIC_TERMS],
    static_row_height: 0,
    menu_static_row_height: 0,
    button_boxes_width: 0,
    menu_row_ids: [0, 1, 2, 3, 4],
    tts_speech_rate: 1.0,
    tts_pitch: 1.0,
    tts_pause_between_words: false,
    tts_pause_duration: 500,
    tts_voice: '',
    static_row_text_size: 16,
    static_row_text_padding: 8,
    menu_row_text_size: 16,
    menu_row_text_padding: 8,
    ngram_model: seedNgramModel(),
    fitzgerald_overrides: {},
    fitzgeraldKey: DEFAULT_FITZGERALD_KEY.map((c) => ({ ...c })),
    highcontrastmode: false,
    theme_mode: 'light',
    skin_tone: '',
    text_location_bottom: true,
    button_shape_name: 'Rounded',
    item_border_width_dp: 4,
    app_locale: 'en',
    language_image_override: false,
    multilingual_labels: false,
    auto_translate_ui: true,
    auto_translate_labels: true,
    auto_translate_sentence: true,
    current_board_language: '',
    language_list: [],
    translations: {},
    security_pin_hash: '',
    security_salt: '',
    security_questions: [],
  };
}
