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

// N-gram model for word prediction.
import { COMMON_BIGRAMS, RANKED_WORDS } from './corpus';

export interface NgramModel {
  // last word -> next word -> count
  bigrams: Record<string, Record<string, number>>;
}

export function emptyNgramModel(): NgramModel {
  return { bigrams: {} };
}

export function recordSentence(model: NgramModel, sentence: string[]): void {
  for (let i = 0; i < sentence.length - 1; i++) {
    const cur = sentence[i].toLowerCase().trim();
    const next = sentence[i + 1].toLowerCase().trim();
    if (!cur || !next) continue;
    const row = (model.bigrams[cur] ??= {});
    row[next] = (row[next] ?? 0) + 1;
  }
}

// Predict next words: learned bigrams, curated transitions, frequency fallback.
export function predict(model: NgramModel, lastWord: string, limit = 8): string[] {
  const lw = lastWord.toLowerCase().trim();
  const scores = new Map<string, number>();
  const bump = (word: string, weight: number) => {
    const w = word.toLowerCase().trim();
    if (!w || w === lw) return;
    scores.set(w, (scores.get(w) ?? 0) + weight);
  };

  if (lw) {
    const learned = model.bigrams[lw];
    if (learned) for (const [w, c] of Object.entries(learned)) bump(w, 1000 + c);
    const seeded = COMMON_BIGRAMS[lw];
    if (seeded) seeded.forEach((w, i) => bump(w, 200 - i));
  }

  // Top up with frequency-ranked words.
  if (scores.size < limit) {
    RANKED_WORDS.forEach((w, i) => bump(w, (RANKED_WORDS.length - i) / RANKED_WORDS.length));
  }

  return [...scores.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, limit)
    .map(([w]) => w);
}

// Word completion by prefix.
export function completeWord(prefix: string, limit = 8): string[] {
  const p = prefix.toLowerCase().trim();
  if (!p) return [];
  const out: string[] = [];
  for (const w of RANKED_WORDS) {
    if (w !== p && w.startsWith(p)) {
      out.push(w);
      if (out.length >= limit) break;
    }
  }
  return out;
}

export function seedNgramModel(): NgramModel {
  const m = emptyNgramModel();
  const seedSentences = [
    // wants & needs
    'i want more', 'i want to play', 'i want to go', 'i want to eat',
    'i want to watch', 'i want that', 'i want it', 'i want a snack',
    'i need help', 'i need to go', 'i need a break', 'i need water',
    'i need more time', 'i would like more', 'i would like to play',
    // feelings
    'i feel happy', 'i feel sad', 'i feel sick', 'i feel tired',
    'i feel scared', 'i feel hungry', 'i feel angry', 'i feel good',
    'i am happy', 'i am sad', 'i am tired', 'i am hungry', 'i am done',
    'i am okay', 'i am not okay', 'i am excited',
    // likes
    'i like that', 'i like it', 'i like this', 'i like you',
    'i do not like that', 'i do not want that', 'i do not know',
    // actions
    'let us go', 'let us play', 'go home', 'go outside', 'go to bed',
    'come here', 'stop it', 'stop please', 'all done', 'more please',
    'play with me', 'read a book', 'watch a show', 'listen to music',
    // social
    'thank you', 'thank you very much', 'i love you', 'good morning',
    'good night', 'see you later', 'how are you', 'i am fine',
    'yes please', 'no thank you', 'excuse me', 'i am sorry',
    // food & drink
    'i want water', 'i want milk', 'i want a cookie', 'i want pizza',
    'more food please', 'i am thirsty', 'i am still hungry',
    // questions
    'can i have more', 'can i go', 'can we play', 'what is that',
    'where is it', 'i want to know', 'help me please',
    // people
    'where is mom', 'where is dad', 'i want mom', 'i want dad',
    'my turn', 'your turn', 'with my friend',
  ];
  for (const sentence of seedSentences) {
    recordSentence(m, sentence.split(' '));
  }
  return m;
}
