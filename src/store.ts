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

// Zustand store.
import { create } from 'zustand';
import * as Crypto from 'expo-crypto';
import { hashSecret, normalizeAnswer } from './security';
import {
  defaultPersistedState,
  padMenu,
  type FitzgeraldCategory,
  type MenuTemplate,
  type PersistedState,
} from './types';
import { recordSentence } from './ngram';
import {
  deleteMenu as deleteMenuPure,
  findMenu,
  killDanglingPointers,
  nextMenuId,
  type SymbolPath,
} from './menus';
import { loadPersistedState, savePersistedState } from './persistence';
import { cachedTranslation, fetchTranslation, translationKey } from './translate';
import {
  isLanguageAvailable,
  loadVoices,
  playClip,
  playSentenceSequenced,
  setSelectedVoice,
  speak as _speak,
  stopSpeaking,
  type Utterance,
} from './tts';
import * as Speech from 'expo-speech';

export interface InputItem {
  text: string;
  hasSymbol: boolean;
  translations: Record<string, string>;
  audio: string;
  pron: string;
  imageUrl: string;
}

// Spec for adding/editing a board item.
export interface ItemSpec {
  name: string;
  isSymbol: boolean; // true=symbol, false=folder
  ttsMode: number | null; // 0/1/2 for symbols, null for folders
  pointer: number | null; // target menu id for folders, null for symbols
  color?: string;
  pron?: string;
  ttsLocale?: string;
  locale?: string;
  translations?: Record<string, string>;
  imageUrl?: string;
  customImagePath?: string;
  customAudioPath?: string;
  customAudioName?: string;
}

interface Dimensions {
  screenWidth: number;
  screenHeight: number;
  staticRowHeight: number;
  menuStaticRowHeight: number;
  buttonBoxesWidth: number;
  inputBoxHeight: number;
  menuHeight: number;
  menuWidth: number;
}

export interface WordFinderHighlight {
  menuId: number;
  index: number;
}

type SpeechMissingFn = (langCode: string) => void;

export interface AppState extends PersistedState, Dimensions {
  // lifecycle
  ready: boolean;

  // navigation / transient UI
  linkedMenu: number;
  menuHistory: number[];
  editorMode: boolean;
  refreshNonce: number; // bump to force the board to re-resolve images
  // Tutorial drives the board pager to the page holding the spotlighted folder/symbol.
  // -1 = no target / not in a board-spotlight slide.
  tutorialScrollToIndex: number;

  // overlays / dialogs
  showSettings: boolean;
  showTutorial: boolean;
  showAutocomplete: boolean;
  showWordFinder: boolean;
  showKeyboard: boolean;
  showEditItemDialog: boolean;
  showAddItemDialog: boolean;
  showNewMenuDialog: boolean;
  showDeleteMenuDialog: boolean;
  showGotoMenuDialog: boolean;
  editTargetMenuId: number;
  editTargetIndex: number;

  // input box
  inputItems: InputItem[];

  // tts banner + word finder
  ttsMissingLanguage: string | null;
  wordFinderHighlight: WordFinderHighlight | null;
  wordFinderPathIds: number[];
  wordFinderPathNames: string[];
  wordFinderTargetIsSymbol: boolean;

  // Which locked surface is awaiting a PIN. Only 'settings' (the sole path to editor mode).
  // null = no lock prompt showing. Volatile (not persisted).
  pinPromptFor: 'settings' | null;

  // Snapshot (JSON) of the last-persisted settings + menus, for unsaved-change detection.
  savedSnapshot: string;

  // ---- actions ----
  init: () => Promise<void>;
  save: () => Promise<void>;
  // Explicit-save model:
  applyChanges: () => Promise<void>;
  discardChanges: () => Promise<void>;
  hasUnsavedChanges: () => boolean;
  setDimensions: (width: number, height: number) => void;

  setSetting: <K extends keyof PersistedState>(key: K, value: PersistedState[K]) => void;

  navigateTo: (menuId: number) => void;
  back: () => void;
  goHome: () => void;

  selectSymbol: (menuId: number, index: number) => void;
  speakInputSentence: () => void;
  speakStaticTerm: (term: string) => void;
  testVoice: (text: string) => void;
  setVoice: (id: string) => void;
  addInputText: (text: string) => void;
  deleteLastInput: () => void;
  clearInput: () => void;

  toggleEditorMode: () => void;
  setMenuList: (menus: MenuTemplate[]) => void;
  updateMenu: (menuId: number, updater: (m: MenuTemplate) => MenuTemplate) => void;
  setItemImageUrl: (menuId: number, index: number, url: string) => void;
  setTranslation: (key: string, value: string) => void;
  reloadAllImages: () => void;
  addItem: (menuId: number, spec: ItemSpec) => void;
  editItem: (menuId: number, index: number, spec: ItemSpec) => void;
  deleteItem: (menuId: number, index: number) => void;
  addMenu: (title: string) => number;
  removeMenu: (menuId: number) => void;

  setFitzgeraldKey: (key: FitzgeraldCategory[]) => void;
  setFitzgeraldOverride: (name: string, hex: string) => void;
  setMenuRowIds: (ids: number[]) => void;
  setStaticTerms: (terms: string[]) => void;
  // Replace the user's extra spoken-language list (de-duped; the app locale can't be added).
  setLanguageList: (list: string[]) => void;

  openDialog: (which: DialogKey, menuId?: number, index?: number) => void;
  closeDialogs: () => void;
  setTtsMissingLanguage: (lang: string | null) => void;
  setWordFinderHighlight: (h: WordFinderHighlight | null) => void;
  // Word-finder guided path (path ids/names + button guide + highlight).
  startWordFinderPath: (path: SymbolPath, targetName: string, isSymbol: boolean) => void;
  handleWordFinderTap: (menuId: number, index: number) => void;
  cancelWordFinderPath: () => void;

  // Caregiver lock (PIN + recovery questions). Settings is the only entry to editor mode, so
  // gating it locks both. PIN/answers are stored as salted hashes; setup here is in-memory,
  // persisted on Apply/Done — except recovery, which persists immediately (no Apply on the lock).
  requestSettings: () => void;
  submitPin: (pin: string) => Promise<boolean>;
  cancelPinPrompt: () => void;
  setSecurityPin: (pin: string) => Promise<void>;
  removeSecurityPin: () => void;
  addSecurityQuestion: (question: string, answer: string) => Promise<void>;
  removeSecurityQuestion: (index: number) => void;
  verifySecurityAnswers: (answers: string[]) => Promise<boolean>;
  resetPinViaRecovery: (newPin: string) => Promise<void>;
}

type DialogKey =
  | 'editItem'
  | 'addItem'
  | 'newMenu'
  | 'deleteMenu'
  | 'gotoMenu'
  | 'settings'
  | 'autocomplete'
  | 'wordFinder'
  | 'keyboard'
  | 'tutorial';

// --- save debounce ---
let saveTimer: ReturnType<typeof setTimeout> | null = null;
function scheduleSave(get: () => AppState) {
  if (saveTimer) clearTimeout(saveTimer);
  saveTimer = setTimeout(() => {
    saveTimer = null;
    void get().save();
  }, 400);
}

// Extract the PersistedState subset from the store.
function extractPersisted(s: AppState): PersistedState {
  return {
    box_width_size_dp: s.box_width_size_dp,
    box_height_size_dp: s.box_height_size_dp,
    box_padding_dp: s.box_padding_dp,
    input_box_height_dp: s.input_box_height_dp,
    item_text_padding_dp: s.item_text_padding_dp,
    has_seen_tutorial: s.has_seen_tutorial,
    tts_data_found: s.tts_data_found,
    menu_list: s.menu_list,
    static_terms: s.static_terms,
    static_row_height: s.static_row_height,
    menu_static_row_height: s.menu_static_row_height,
    button_boxes_width: s.button_boxes_width,
    menu_row_ids: s.menu_row_ids,
    tts_speech_rate: s.tts_speech_rate,
    tts_pitch: s.tts_pitch,
    tts_pause_between_words: s.tts_pause_between_words,
    tts_pause_duration: s.tts_pause_duration,
    tts_voice: s.tts_voice,
    static_row_text_size: s.static_row_text_size,
    static_row_text_padding: s.static_row_text_padding,
    menu_row_text_size: s.menu_row_text_size,
    menu_row_text_padding: s.menu_row_text_padding,
    ngram_model: s.ngram_model,
    fitzgerald_overrides: s.fitzgerald_overrides,
    fitzgeraldKey: s.fitzgeraldKey,
    highcontrastmode: s.highcontrastmode,
    theme_mode: s.theme_mode,
    skin_tone: s.skin_tone,
    text_location_bottom: s.text_location_bottom,
    button_shape_name: s.button_shape_name,
    item_border_width_dp: s.item_border_width_dp,
    app_locale: s.app_locale,
    language_image_override: s.language_image_override,
    multilingual_labels: s.multilingual_labels,
    auto_translate_ui: s.auto_translate_ui,
    auto_translate_labels: s.auto_translate_labels,
    auto_translate_sentence: s.auto_translate_sentence,
    current_board_language: s.current_board_language,
    language_list: s.language_list,
    translations: s.translations,
    security_pin_hash: s.security_pin_hash,
    security_salt: s.security_salt,
    security_questions: s.security_questions,
  };
}

// JSON snapshot of the persisted state for unsaved-change detection. Excludes fields that
// change during *normal* use (not via Settings/Editor) so they never read as unsaved edits:
// the lazily-filled OpenSymbols image cache (Board fills image_urls while rendering), the learned
// ngram model, the current board/spoken language, and the lazily-filled translation cache.
function snapshotOf(s: AppState): string {
  const p = extractPersisted(s);
  const rest = {
    ...p,
    menu_list: p.menu_list.map((m) => ({ ...m, image_urls: [] })),
  } as Partial<PersistedState>;
  delete rest.ngram_model;
  delete rest.current_board_language;
  delete rest.translations;
  return JSON.stringify(rest);
}

function boardLang(s: AppState): string {
  return s.current_board_language || s.app_locale;
}

// A non-empty, non-English active language means the (English-authored) labels must be
// translated for speech. English / the base language => speak the original label.
function needsTranslation(lang: string): boolean {
  return !!lang && !lang.toLowerCase().startsWith('en');
}

// Resolve what to SPEAK for an item in the active input-box language.
//   - base / English active -> pronunciation override or the original label, in the app locale.
//   - other language        -> a manual per-item label (multilingual editor) if present, else the
//                              cached auto-translation, else the original label; in that language.
function resolveSpoken(
  label: string,
  manual: Record<string, string>,
  pron: string,
  cache: Record<string, string>,
  lang: string,
  appLocale: string
): { text: string; language: string } {
  if (!needsTranslation(lang)) {
    return { text: pron && pron.trim() ? pron : label, language: appLocale };
  }
  const m = manual[lang];
  if (m && m.trim()) return { text: m, language: lang };
  return { text: cache[translationKey(label, lang)] || label, language: lang };
}

function capitalizeFirst(s: string): string {
  return s.length > 0 ? s[0].toUpperCase() + s.slice(1) : s;
}

/*
 * Word-finder highlight: which item in the *current* path menu to ring next. The current menu is
 * pathIds[0].
 *   - Intermediate steps (pathIds.length > 1): ring the folder whose pointer is the next menu
 *     in the path (pathIds[1]). Pointer identity is exact and immune to duplicate menu titles.
 *   - Final step (pathIds.length === 1): ring the target item by name (pathNames[1]) and type
 *     (targetIsSymbol). Name match is case-insensitive (names are user-entered so we relax it).
 */
function computeWfHighlight(
  menuList: MenuTemplate[],
  pathIds: number[],
  pathNames: string[],
  targetIsSymbol: boolean
): WordFinderHighlight | null {
  if (pathIds.length === 0) return null;
  const currentId = pathIds[0];
  const menu = findMenu(menuList, currentId);

  if (pathIds.length > 1) {
    const nextMenu = pathIds[1];
    for (let i = 0; i < menu.item_list.length; i++) {
      if (!menu.item_type[i] && menu.pointers[i] === nextMenu) return { menuId: currentId, index: i };
    }
    return null;
  }

  const lookupName = (pathNames.length > 1 ? pathNames[1] : pathNames[0] ?? '').trim().toLowerCase();
  if (!lookupName) return null;
  let index = -1;
  for (let i = 0; i < menu.item_list.length; i++) {
    if ((menu.item_list[i] ?? '').trim().toLowerCase() !== lookupName) continue;
    if (menu.item_type[i] === targetIsSymbol) index = i; // prefer the expected type (last match)
  }
  if (index < 0) {
    for (let i = 0; i < menu.item_list.length; i++) {
      if ((menu.item_list[i] ?? '').trim().toLowerCase() === lookupName) index = i;
    }
  }
  return index >= 0 ? { menuId: currentId, index } : null;
}

export const useStore = create<AppState>((set, get) => ({
  ...defaultPersistedState(),

  // dimensions (recomputed on layout)
  screenWidth: 0,
  screenHeight: 0,
  staticRowHeight: 0,
  menuStaticRowHeight: 0,
  buttonBoxesWidth: 0,
  inputBoxHeight: 0,
  menuHeight: 0,
  menuWidth: 0,

  ready: false,
  linkedMenu: 0,
  menuHistory: [],
  editorMode: false,
  refreshNonce: 0,
  tutorialScrollToIndex: -1,
  savedSnapshot: '',

  showSettings: false,
  showTutorial: false,
  showAutocomplete: false,
  showWordFinder: false,
  showKeyboard: false,
  showEditItemDialog: false,
  showAddItemDialog: false,
  showNewMenuDialog: false,
  showDeleteMenuDialog: false,
  showGotoMenuDialog: false,
  editTargetMenuId: -1,
  editTargetIndex: -1,

  inputItems: [],

  ttsMissingLanguage: null,
  wordFinderHighlight: null,
  wordFinderPathIds: [],
  wordFinderPathNames: [],
  wordFinderTargetIsSymbol: false,
  pinPromptFor: null,

  init: async () => {
    void loadVoices();
    const loaded = await loadPersistedState();
    if (loaded) {
      set({ ...loaded, ready: true, linkedMenu: 0, menuHistory: [], showTutorial: !loaded.has_seen_tutorial });
    } else {
      // fresh install: defaults + show tutorial
      set({ ...defaultPersistedState(), ready: true, linkedMenu: 0, menuHistory: [], showTutorial: true });
    }
    // Baseline for unsaved detection: what we just loaded IS the saved state.
    set({ savedSnapshot: snapshotOf(get()) });
    // Push the persisted voice choice into the TTS engine.
    setSelectedVoice(get().tts_voice);
  },

  save: async () => {
    await savePersistedState(extractPersisted(get()));
    // Persisting makes the current state the new baseline.
    set({ savedSnapshot: snapshotOf(get()) });
  },

  // Apply = persist now + re-parse the board.
  applyChanges: async () => {
    await get().save();
    set((s) => ({ refreshNonce: s.refreshNonce + 1 }));
  },

  // Discard = reload the persisted state, dropping in-memory edits.
  discardChanges: async () => {
    const loaded = await loadPersistedState();
    const base = loaded ?? defaultPersistedState();
    set({ ...base, refreshNonce: get().refreshNonce + 1 });
    set({ savedSnapshot: snapshotOf(get()) });
  },

  hasUnsavedChanges: () => snapshotOf(get()) !== get().savedSnapshot,

  setDimensions: (width, height) => {
    const eighth = height / 8;
    const inputBoxHeight = height / 4;
    const staticRowHeight = eighth;
    const menuStaticRowHeight = eighth;
    const buttonBoxesWidth = eighth;
    const menuHeight = height - menuStaticRowHeight - staticRowHeight - inputBoxHeight;
    const menuWidth = width - buttonBoxesWidth * 2;
    set({
      screenWidth: width,
      screenHeight: height,
      staticRowHeight,
      menuStaticRowHeight,
      buttonBoxesWidth,
      inputBoxHeight,
      menuHeight,
      menuWidth,
    });
  },

  setSetting: (key, value) => {
    // In-memory only — persisted explicitly via Apply / Save-on-exit, not auto-saved.
    set({ [key]: value } as Partial<AppState>);
  },

  navigateTo: (menuId) => {
    set((s) => ({
      menuHistory: [...s.menuHistory, s.linkedMenu],
      linkedMenu: menuId,
      refreshNonce: s.refreshNonce + 1,
      showAutocomplete: false,
    }));
  },

  back: () => {
    set((s) => {
      if (s.menuHistory.length === 0) return { linkedMenu: 0, refreshNonce: s.refreshNonce + 1 };
      const history = [...s.menuHistory];
      const prev = history.pop()!;
      return { menuHistory: history, linkedMenu: prev, refreshNonce: s.refreshNonce + 1, showAutocomplete: false };
    });
  },

  goHome: () => set((s) => ({ linkedMenu: 0, menuHistory: [], refreshNonce: s.refreshNonce + 1 })),

  selectSymbol: (menuId, index) => {
    const s = get();
    if (s.editorMode) {
      get().openDialog('editItem', menuId, index);
      return;
    }
    const menu = findMenu(s.menu_list, menuId);
    const mode = menu.tts[index];
    const label = capitalizeFirst(menu.item_list[index] ?? '');

    const addToInput = () => {
      const item: InputItem = {
        text: label,
        hasSymbol: true,
        translations: menu.item_translations[index] ?? {},
        audio: menu.custom_audio_paths[index] ?? '',
        pron: menu.pronunciation_overrides[index] ?? '',
        imageUrl: menu.image_urls[index] ?? '',
      };
      set((st) => ({ inputItems: [...st.inputItems, item] }));
    };
    const speak = async () => {
      // Custom audio (B4): if this item has a recorded/imported clip, PLAY THE CLIP instead of
      // speaking the label. Applied to a direct symbol tap, so an imported/recorded clip is heard
      // on a direct tap outside editor mode
      // (tts mode 1 "speak" and 2 "both"). Pronunciation/translation only apply to TTS.
      const clip = menu.custom_audio_paths[index];
      if (clip && clip.trim()) {
        playClip(clip);
        return;
      }
      // Speak in the active input-box language: a manual per-item label, else the cached
      // auto-translation, else fetch it on demand (so a just-tapped item speaks translated instead
      // of briefly playing English first). English/base: pronunciation override or the label.
      const lang = boardLang(s);
      const label = menu.item_list[index] ?? '';
      if (needsTranslation(lang)) {
        if (!isLanguageAvailable(lang)) get().setTtsMissingLanguage(lang);
        const manual = (menu.item_translations[index] ?? {})[lang];
        let text = manual && manual.trim() ? manual : s.translations[translationKey(label, lang)];
        if (!text) {
          text = (await fetchTranslation(label, lang)) || label;
          get().setTranslation(translationKey(label, lang), text);
        }
        _speak(text, { language: lang, rate: s.tts_speech_rate, pitch: s.tts_pitch });
        return;
      }
      const pron = menu.pronunciation_overrides[index] ?? '';
      _speak(pron && pron.trim() ? pron : label, { language: s.app_locale, rate: s.tts_speech_rate, pitch: s.tts_pitch });
    };

    if (mode === 0) addToInput();
    else if (mode === 1) void speak();
    else if (mode === 2) {
      addToInput();
      void speak();
    }
  },

  speakInputSentence: () => {
    const s = get();
    void Speech.isSpeakingAsync().then(async (speaking) => {
      if (speaking) {
        stopSpeaking();
        return;
      }
      const cur = get();
      const lang = boardLang(cur);
      if (needsTranslation(lang) && !isLanguageAvailable(lang)) {
        get().setTtsMissingLanguage(lang);
      }
      // Fix 1: when sentence translation is on (non-English, no custom-audio items), translate the
      // WHOLE assembled English sentence as one unit so grammar/word order is correct, instead of
      // concatenating individually-translated words. Otherwise speak each item (manual label, else
      // cached per-item auto-translation, else the original).
      const hasAudio = cur.inputItems.some((it) => it.audio && it.audio.trim());
      let utterances: Utterance[];
      if (cur.auto_translate_sentence && needsTranslation(lang) && cur.inputItems.length > 0 && !hasAudio) {
        const english = cur.inputItems.map((it) => it.text).join(' ').trim();
        let translated = cachedTranslation(english, lang) || cur.translations[translationKey(english, lang)];
        if (!translated) {
          translated = (await fetchTranslation(english, lang)) || english;
          get().setTranslation(translationKey(english, lang), translated);
        }
        utterances = [{ text: translated || english, language: lang, audio: '' }];
      } else {
        utterances = cur.inputItems.map((it) => {
          const { text, language } = resolveSpoken(it.text, it.translations, it.pron, cur.translations, lang, cur.app_locale);
          return { text, language, audio: it.audio };
        });
      }
      playSentenceSequenced(utterances, {
        rate: cur.tts_speech_rate,
        pitch: cur.tts_pitch,
        pauseBetween: cur.tts_pause_between_words,
        pauseMs: cur.tts_pause_duration,
      });
      if (cur.inputItems.length > 0) {
        const model = { bigrams: { ...cur.ngram_model.bigrams } };
        recordSentence(model, cur.inputItems.map((it) => it.text.toLowerCase()));
        set({ ngram_model: model });
        scheduleSave(get);
      }
    });
  },

  setVoice: (id) => {
    get().setSetting('tts_voice', id);
    setSelectedVoice(id);
  },

  speakStaticTerm: (term) => {
    const s = get();
    const lang = boardLang(s);
    // Auto-translate the spoken term to the active language when label translation is enabled. Uses
    // the cached translation the StaticRow prefetched; falls back to the original until it arrives.
    if (s.auto_translate_labels && needsTranslation(lang)) {
      if (!isLanguageAvailable(lang)) get().setTtsMissingLanguage(lang);
      const text = s.translations[translationKey(term, lang)] || term;
      _speak(text, { language: lang, rate: s.tts_speech_rate, pitch: s.tts_pitch });
      return;
    }
    _speak(term, { language: s.app_locale, rate: s.tts_speech_rate, pitch: s.tts_pitch });
  },

  // Test the voice settings (B3). Unlike a single utterance, this splits the sentence into
  // words and runs them through the sentence sequencer so the "pause between words" / "pause
  // duration" settings are actually demonstrated. Real input-sentence playback already pauses via
  // the sequencer.
  testVoice: (text) => {
    const s = get();
    const words = text.split(/\s+/).filter((w) => w.length > 0);
    const utterances: Utterance[] = words.map((w) => ({ text: w, language: s.app_locale, audio: '' }));
    playSentenceSequenced(utterances, {
      rate: s.tts_speech_rate,
      pitch: s.tts_pitch,
      pauseBetween: s.tts_pause_between_words,
      pauseMs: s.tts_pause_duration,
    });
  },

  addInputText: (text) => {
    if (!text.trim()) return;
    const item: InputItem = { text, hasSymbol: false, translations: {}, audio: '', pron: '', imageUrl: '' };
    set((s) => ({ inputItems: [...s.inputItems, item] }));
  },

  deleteLastInput: () => set((s) => ({ inputItems: s.inputItems.slice(0, -1) })),

  clearInput: () => set({ inputItems: [] }),

  toggleEditorMode: () => set((s) => ({ editorMode: !s.editorMode })),

  setMenuList: (menus) => {
    set({ menu_list: menus.map(padMenu) });
  },

  updateMenu: (menuId, updater) => {
    set((s) => ({
      menu_list: s.menu_list.map((m) => (m.id === menuId ? padMenu(updater(m)) : m)),
    }));
  },

  setItemImageUrl: (menuId, index, url) => {
    set((s) => ({
      menu_list: s.menu_list.map((m) => {
        if (m.id !== menuId) return m;
        const image_urls = m.image_urls.slice();
        image_urls[index] = url;
        return { ...m, image_urls };
      }),
    }));
  },

  setTranslation: (key, value) => {
    // No-op (no re-render) when unchanged, so the board's fetch loop doesn't churn the store.
    set((s) => (s.translations[key] === value ? {} : { translations: { ...s.translations, [key]: value } }));
  },

  reloadAllImages: () => {
    set((s) => ({
      menu_list: s.menu_list.map((m) => ({ ...m, image_urls: m.image_urls.map(() => '') })),
      refreshNonce: s.refreshNonce + 1,
    }));
  },

  addItem: (menuId, spec) => {
    get().updateMenu(menuId, (m) => ({
      ...m,
      item_list: [...m.item_list, spec.name],
      pointers: [...m.pointers, spec.isSymbol ? null : spec.pointer ?? null],
      tts: [...m.tts, spec.isSymbol ? spec.ttsMode ?? 2 : null],
      item_type: [...m.item_type, spec.isSymbol],
      image_urls: [...m.image_urls, spec.imageUrl ?? ''],
      item_uuids: [...m.item_uuids, Crypto.randomUUID()],
      custom_image_paths: [...m.custom_image_paths, spec.customImagePath ?? ''],
      custom_audio_paths: [...m.custom_audio_paths, ''],
      custom_audio_names: [...m.custom_audio_names, ''],
      pronunciation_overrides: [...m.pronunciation_overrides, spec.pron ?? ''],
      colors: [...m.colors, spec.color ?? ''],
      item_locales: [...m.item_locales, spec.locale ?? ''],
      item_translations: [...m.item_translations, spec.translations ?? {}],
      item_tts_locales: [...m.item_tts_locales, spec.ttsLocale ?? ''],
    }));
  },

  editItem: (menuId, index, spec) => {
    get().updateMenu(menuId, (m) => {
      const setAt = <T>(arr: T[], v: T): T[] => {
        const out = arr.slice();
        out[index] = v;
        return out;
      };
      const nameChanged = m.item_list[index] !== spec.name;
      // Clear the cached search URL when the per-item locale changes
      // (the OpenSymbols result depends on locale), so a re-fetch happens next render.
      const localeChanged = (m.item_locales[index] ?? '') !== (spec.locale ?? '');
      return {
        ...m,
        item_list: setAt(m.item_list, spec.name),
        pointers: setAt(m.pointers, spec.isSymbol ? null : spec.pointer ?? null),
        tts: setAt(m.tts, spec.isSymbol ? spec.ttsMode ?? 2 : null),
        item_type: setAt(m.item_type, spec.isSymbol),
        // clear cached image if the name or locale changed and no explicit url/custom given
        image_urls: setAt(
          m.image_urls,
          spec.imageUrl ?? (nameChanged || localeChanged ? '' : m.image_urls[index] ?? '')
        ),
        custom_image_paths: setAt(m.custom_image_paths, spec.customImagePath ?? m.custom_image_paths[index] ?? ''),
        custom_audio_paths: setAt(m.custom_audio_paths, spec.customAudioPath ?? m.custom_audio_paths[index] ?? ''),
        custom_audio_names: setAt(m.custom_audio_names, spec.customAudioName ?? m.custom_audio_names[index] ?? ''),
        pronunciation_overrides: setAt(m.pronunciation_overrides, spec.pron ?? ''),
        colors: setAt(m.colors, spec.color ?? ''),
        item_locales: setAt(m.item_locales, spec.locale ?? ''),
        item_translations: setAt(m.item_translations, spec.translations ?? {}),
        item_tts_locales: setAt(m.item_tts_locales, spec.ttsLocale ?? ''),
      };
    });
  },

  deleteItem: (menuId, index) => {
    get().updateMenu(menuId, (m) => {
      const drop = <T>(arr: T[]): T[] => arr.filter((_, i) => i !== index);
      return {
        ...m,
        item_list: drop(m.item_list),
        pointers: drop(m.pointers),
        tts: drop(m.tts),
        item_type: drop(m.item_type),
        image_urls: drop(m.image_urls),
        item_uuids: drop(m.item_uuids),
        custom_image_paths: drop(m.custom_image_paths),
        custom_audio_paths: drop(m.custom_audio_paths),
        custom_audio_names: drop(m.custom_audio_names),
        pronunciation_overrides: drop(m.pronunciation_overrides),
        colors: drop(m.colors),
        item_locales: drop(m.item_locales),
        item_translations: drop(m.item_translations),
        item_tts_locales: drop(m.item_tts_locales),
      };
    });
    // a deleted folder may orphan menus
    set((s) => ({ menu_list: killDanglingPointers(s.menu_list) }));
  },

  addMenu: (title) => {
    const id = nextMenuId(get().menu_list);
    const newMenu = padMenu({
      id,
      title,
      item_list: [],
      pointers: [],
      tts: [],
      item_type: [],
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
    });
    set((s) => ({ menu_list: [...s.menu_list, newMenu] }));
    return id;
  },

  removeMenu: (menuId) => {
    set((s) => {
      const res = deleteMenuPure(s.menu_list, s.menu_row_ids, menuId);
      const onDeleted = s.linkedMenu === menuId;
      return {
        menu_list: res.menuList,
        menu_row_ids: res.menuRowIds,
        linkedMenu: onDeleted ? 0 : s.linkedMenu,
        refreshNonce: s.refreshNonce + 1,
      };
    });
  },

  setFitzgeraldKey: (key) => {
    set({ fitzgeraldKey: key });
  },

  setFitzgeraldOverride: (name, hex) => {
    set((s) => ({ fitzgerald_overrides: { ...s.fitzgerald_overrides, [name]: hex } }));
  },

  setMenuRowIds: (ids) => {
    // When the row empties out, collapse its reserved height to 0 so the board reclaims the
    // space. Re-expand to the standard 1/8 when non-empty.
    set((s) => {
      if (s.screenHeight <= 0) return { menu_row_ids: ids };
      const eighth = s.screenHeight / 8;
      const menuStaticRowHeight = ids.length === 0 ? 0 : eighth;
      const menuHeight = s.screenHeight - menuStaticRowHeight - s.staticRowHeight - s.inputBoxHeight;
      return { menu_row_ids: ids, menuStaticRowHeight, menuHeight };
    });
  },

  setStaticTerms: (terms) => {
    set({ static_terms: terms });
  },

  // Replace the extra spoken-language list. De-dupes, drops blanks and the app locale (it's
  // the implicit base), and resets the active language if it was removed. In-memory only.
  setLanguageList: (list) => {
    set((s) => {
      const cleaned = list.filter(
        (c, i) => !!c && c !== s.app_locale && list.indexOf(c) === i
      );
      const stillValid = !s.current_board_language || cleaned.includes(s.current_board_language);
      return {
        language_list: cleaned,
        current_board_language: stillValid ? s.current_board_language : '',
      };
    });
  },

  openDialog: (which, menuId, index) => {
    const patch: Partial<AppState> = {};
    if (menuId !== undefined) patch.editTargetMenuId = menuId;
    if (index !== undefined) patch.editTargetIndex = index;
    switch (which) {
      case 'editItem': patch.showEditItemDialog = true; break;
      case 'addItem': patch.showAddItemDialog = true; break;
      case 'newMenu': patch.showNewMenuDialog = true; break;
      case 'deleteMenu': patch.showDeleteMenuDialog = true; break;
      case 'gotoMenu': patch.showGotoMenuDialog = true; break;
      case 'settings': patch.showSettings = true; break;
      case 'autocomplete': patch.showAutocomplete = true; break;
      case 'wordFinder': patch.showWordFinder = true; break;
      case 'keyboard': patch.showKeyboard = true; break;
      case 'tutorial': patch.showTutorial = true; break;
    }
    set(patch);
  },

  closeDialogs: () =>
    set({
      showEditItemDialog: false,
      showAddItemDialog: false,
      showNewMenuDialog: false,
      showDeleteMenuDialog: false,
      showGotoMenuDialog: false,
      showKeyboard: false,
    }),

  setTtsMissingLanguage: (lang) => {
    set({ ttsMissingLanguage: lang });
    if (lang) {
      setTimeout(() => {
        if (get().ttsMissingLanguage === lang) set({ ttsMissingLanguage: null });
      }, 6000);
    }
  },

  setWordFinderHighlight: (h) => set({ wordFinderHighlight: h }),

  // Begin a guided path from the word finder's "Find" button. Every menuPath
  // from allPathsToMenu starts at home (id 0), so we navigate there and ring the first step.
  startWordFinderPath: (path, targetName, isSymbol) => {
    const names = [...path.menuNames];
    if (names[names.length - 1] !== targetName) names.push(targetName);
    const ids = [...path.menuPath];
    set((s) => ({
      showWordFinder: false,
      showAutocomplete: false,
      menuHistory: ids.length > 0 && ids[0] !== s.linkedMenu ? [...s.menuHistory, s.linkedMenu] : s.menuHistory,
      linkedMenu: ids.length > 0 ? ids[0] : s.linkedMenu,
      refreshNonce: s.refreshNonce + 1,
      wordFinderPathIds: ids,
      wordFinderPathNames: names,
      wordFinderTargetIsSymbol: isSymbol,
    }));
    const s2 = get();
    set({ wordFinderHighlight: computeWfHighlight(s2.menu_list, ids, names, isSymbol) });
  },

  // A board tap while a guided path is active. Only the ringed (expected) item advances; other
  // taps are ignored so the guide stays on track. A folder target navigates into it and then
  // ends the guide.
  handleWordFinderTap: (menuId, index) => {
    const s = get();
    const menu = findMenu(s.menu_list, menuId);
    const hi = s.wordFinderHighlight;
    const isExpected = !!hi && hi.menuId === menuId && hi.index === index;
    if (!isExpected) return;

    if (menu.item_type[index]) {
      // Final target symbol: run its normal add/speak action, then end the guide.
      get().selectSymbol(menuId, index);
      get().cancelWordFinderPath();
      return;
    }

    // Folder step: drop the current menu from the path and navigate into the folder.
    const target = menu.pointers[index];
    if (target == null) {
      get().cancelWordFinderPath();
      return;
    }
    const newIds = s.wordFinderPathIds.slice(1);
    const newNames = s.wordFinderPathNames.slice(1);
    set((st) => ({
      menuHistory: [...st.menuHistory, st.linkedMenu],
      linkedMenu: target,
      refreshNonce: st.refreshNonce + 1,
      showAutocomplete: false,
      wordFinderPathIds: newIds,
      wordFinderPathNames: newNames,
    }));
    if (newIds.length === 0) {
      get().cancelWordFinderPath(); // folder target reached and opened
    } else {
      const ns = get();
      set({ wordFinderHighlight: computeWfHighlight(ns.menu_list, newIds, newNames, ns.wordFinderTargetIsSymbol) });
    }
  },

  cancelWordFinderPath: () =>
    set({
      wordFinderHighlight: null,
      wordFinderPathIds: [],
      wordFinderPathNames: [],
      wordFinderTargetIsSymbol: false,
    }),

  // ---- caregiver lock ----
  // Settings is the only entry to editor mode, so gating it locks both. With no PIN set the
  // feature is off and Settings opens directly. "Each time" model: no session-unlock flag.
  requestSettings: () => {
    if (get().security_pin_hash) set({ pinPromptFor: 'settings' });
    else set({ showSettings: true });
  },

  submitPin: async (pin) => {
    const s = get();
    if (!s.security_pin_hash) return false;
    const ok = (await hashSecret(s.security_salt, 'pin', pin)) === s.security_pin_hash;
    if (!ok) return false;
    set({ pinPromptFor: null, showSettings: true });
    return true;
  },

  cancelPinPrompt: () => set({ pinPromptFor: null }),

  // Set/change the PIN from Settings. In-memory only: persisted on Apply / Done-save like
  // every other setting. The salt is generated once and reused for the PIN and all answer hashes.
  setSecurityPin: async (pin) => {
    const s = get();
    const salt = s.security_salt || Crypto.randomUUID();
    const hash = await hashSecret(salt, 'pin', pin);
    set({ security_salt: salt, security_pin_hash: hash });
  },

  // Remove the PIN (in-memory; persisted on Apply). Recovery questions are kept so re-enabling the
  // lock later doesn't lose them.
  removeSecurityPin: () => set({ security_pin_hash: '' }),

  addSecurityQuestion: async (question, answer) => {
    const q = question.trim();
    if (!q || !answer.trim()) return;
    const s = get();
    const salt = s.security_salt || Crypto.randomUUID();
    const answerHash = await hashSecret(salt, 'ans', normalizeAnswer(answer));
    set({ security_salt: salt, security_questions: [...s.security_questions, { question: q, answerHash }] });
  },

  removeSecurityQuestion: (index) =>
    set((s) => ({ security_questions: s.security_questions.filter((_, i) => i !== index) })),

  // Recovery (from the lock screen): every stored question must be answered correctly.
  verifySecurityAnswers: async (answers) => {
    const s = get();
    if (s.security_questions.length === 0) return false;
    for (let i = 0; i < s.security_questions.length; i++) {
      const h = await hashSecret(s.security_salt, 'ans', normalizeAnswer(answers[i] ?? ''));
      if (h !== s.security_questions[i].answerHash) return false;
    }
    return true;
  },

  // After successful recovery, set the new PIN and persist immediately (the lock screen has no
  // Apply button, and no Settings edits are in flight here), then open the gated surface.
  resetPinViaRecovery: async (newPin) => {
    const s = get();
    const salt = s.security_salt || Crypto.randomUUID();
    const hash = await hashSecret(salt, 'pin', newPin);
    set({ security_salt: salt, security_pin_hash: hash });
    await get().save();
    set({ pinPromptFor: null, showSettings: true });
  },
}));
