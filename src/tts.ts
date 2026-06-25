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

// Text-to-speech via expo-speech.
import * as Speech from 'expo-speech';
import { createAudioPlayer, setAudioModeAsync, type AudioPlayer } from 'expo-audio';
import { resolveSpeech } from './menus';
import type { MenuTemplate } from './types';

// Allow custom-audio clips to play in iOS silent mode (best-effort, once at startup).
void setAudioModeAsync({ playsInSilentMode: true }).catch(() => undefined);

export type TtsVoice = Speech.Voice;

let voiceLanguages: Set<string> | null = null;
let allVoices: Speech.Voice[] = [];
let selectedVoiceId = '';

export async function loadVoices(): Promise<void> {
  try {
    const voices = await Speech.getAvailableVoicesAsync();
    allVoices = voices;
    const set = new Set<string>();
    for (const v of voices) {
      if (v.language) {
        const lc = v.language.toLowerCase();
        set.add(lc);
        set.add(lc.split('-')[0]);
      }
    }
    voiceLanguages = set;
  } catch {
    voiceLanguages = null;
  }
}

// Fresh list of available voices for the picker. Also refreshes the cache (web loads voices late,
// so the first loadVoices() at startup can come back empty).
export async function getVoices(): Promise<Speech.Voice[]> {
  try {
    allVoices = await Speech.getAvailableVoicesAsync();
  } catch {
    // keep whatever we had
  }
  return allVoices;
}

// The user's chosen voice ('' = engine default). Set from the persisted tts_voice setting.
export function setSelectedVoice(id: string): void {
  selectedVoiceId = id || '';
}

// Returns the chosen voice id ONLY when its language matches the utterance language; otherwise
// undefined so the engine picks an appropriate default voice (keeps translated speech correct).
function voiceForLanguage(code: string): string | undefined {
  if (!selectedVoiceId) return undefined;
  const v = allVoices.find((x) => x.identifier === selectedVoiceId);
  if (!v || !v.language) return selectedVoiceId; // unknown language -> best-effort, just use it
  const a = v.language.toLowerCase().split('-')[0];
  const b = (code || '').toLowerCase().split('-')[0];
  return a === b ? selectedVoiceId : undefined;
}

// Returns true if unknown (voices not yet loaded) — assume available.
export function isLanguageAvailable(code: string): boolean {
  if (!code) return true;
  if (!voiceLanguages) return true;
  const lc = code.toLowerCase();
  return voiceLanguages.has(lc) || voiceLanguages.has(lc.split('-')[0]);
}

export interface SpeakConfig {
  rate: number;
  pitch: number;
  appLocale: string;
  multilingualLabels: boolean;
}

// Imperative player used by the sentence sequencer for custom-audio clips.
let seqPlayer: AudioPlayer | null = null;
function releaseSeqPlayer(): void {
  if (seqPlayer) {
    // Pause first: remove() alone can let an in-flight imported clip keep playing to the end,
    // so the Stop button appeared to do nothing for custom audio. Pause halts it immediately.
    try {
      seqPlayer.pause();
    } catch {
      // ignore: not playing / already released
    }
    try {
      seqPlayer.remove();
    } catch {
      // ignore: already released
    }
    seqPlayer = null;
  }
}

export function stopSpeaking(): void {
  seqToken++; // cancel any running sequence
  Speech.stop();
  releaseSeqPlayer();
}

// Play a single custom-audio clip on its own (a recorded/imported clip instead of the spoken
// label). Routed through the same seqPlayer the sentence sequencer uses, so stopSpeaking() / the
// Stop button halts it too. Best-effort: a missing/unreadable clip is silently skipped.
export function playClip(path: string): void {
  if (!path || !path.trim()) return;
  stopSpeaking(); // cancel any TTS / running sequence first (also bumps seqToken)
  const myToken = seqToken;
  try {
    const player = createAudioPlayer(path);
    seqPlayer = player;
    const sub = player.addListener('playbackStatusUpdate', (status) => {
      if (myToken !== seqToken) {
        sub.remove();
        return;
      }
      if (status.didJustFinish) {
        sub.remove();
        releaseSeqPlayer();
      }
    });
    player.play();
  } catch {
    // ignore: clip missing / unreadable
  }
}

// One-off utterance (used for static-row terms).
export function speak(
  text: string,
  opts: { language: string; rate: number; pitch: number }
): void {
  stopSpeaking();
  Speech.speak(text, { language: opts.language, rate: opts.rate, pitch: opts.pitch, voice: voiceForLanguage(opts.language) });
}

// Speak an item: honors pronunciation override and per-item locale.
export function speakItem(
  menu: MenuTemplate,
  index: number,
  cfg: SpeakConfig,
  onMissingLang?: (langCode: string) => void
): void {
  const [textRaw, loc] = resolveSpeech(menu, index, cfg.multilingualLabels);
  const pron = menu.pronunciation_overrides[index];
  const text = pron && pron.trim() ? pron : textRaw;
  const code = loc ?? cfg.appLocale;
  if (!isLanguageAvailable(code)) onMissingLang?.(code);
  stopSpeaking();
  Speech.speak(text, { language: code, rate: cfg.rate, pitch: cfg.pitch, voice: voiceForLanguage(code) });
}

export interface Utterance {
  text: string;
  language: string;
  audio: string; // custom audio path; "" if none
}

let seqToken = 0;

// Play the sentence as a sequence of steps: a custom-audio item is its own step (expo-audio); text
// items are spoken via expo-speech.
//
// When "pause between words" is OFF we MERGE consecutive text items in the same language
// into a single utterance so the sentence is spoken continuously. Speaking word-by-word leaves an
// audible gap between every word (the engine's stop/restart latency) even with no explicit delay —
// that's the unwanted pause users hear. So the whole sentence is spoken in one go (whole sentence
// in one speak when the toggle is off). When the toggle is ON we keep one item per step and insert
// pauseMs between steps. Custom-audio clips always stay their own step.
type Step = { kind: 'audio'; path: string } | { kind: 'text'; text: string; language: string };

export function playSentenceSequenced(
  utterances: Utterance[],
  opts: { rate: number; pitch: number; pauseBetween: boolean; pauseMs: number },
  callbacks: { onFinished?: () => void } = {}
): void {
  Speech.stop();
  releaseSeqPlayer();
  const myToken = ++seqToken;

  const steps: Step[] = [];
  for (const u of utterances) {
    if (u.audio && u.audio.trim()) {
      steps.push({ kind: 'audio', path: u.audio });
      continue;
    }
    const prev = steps[steps.length - 1];
    if (!opts.pauseBetween && prev && prev.kind === 'text' && prev.language === u.language) {
      prev.text = `${prev.text} ${u.text}`.trim();
    } else {
      steps.push({ kind: 'text', text: u.text, language: u.language });
    }
  }

  let i = 0;
  const advance = () => {
    if (myToken !== seqToken) return;
    i++;
    if (opts.pauseBetween && opts.pauseMs > 0) {
      setTimeout(next, opts.pauseMs);
    } else {
      next();
    }
  };

  const next = () => {
    if (myToken !== seqToken) return; // superseded or stopped
    if (i >= steps.length) {
      callbacks.onFinished?.();
      return;
    }
    const s = steps[i];
    if (s.kind === 'audio') {
      try {
        releaseSeqPlayer();
        const player = createAudioPlayer(s.path);
        seqPlayer = player;
        const sub = player.addListener('playbackStatusUpdate', (status) => {
          if (myToken !== seqToken) {
            sub.remove();
            return;
          }
          if (status.didJustFinish) {
            sub.remove();
            advance();
          }
        });
        player.play();
      } catch {
        advance();
      }
      return;
    }
    Speech.speak(s.text, {
      language: s.language,
      rate: opts.rate,
      pitch: opts.pitch,
      voice: voiceForLanguage(s.language),
      onDone: advance,
      onError: advance,
    });
  };

  next();
}
