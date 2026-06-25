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

// Bundled prediction corpus: core vocabulary, common words, bigrams.
// AAC core vocabulary, roughly by usage frequency (pronouns, common verbs, descriptors, social).
export const CORE_WORDS: string[] = [
  'i', 'you', 'it', 'me', 'we', 'they', 'he', 'she', 'my', 'your',
  'a', 'the', 'this', 'that', 'is', 'are', 'was', 'do', 'did', 'can',
  'will', 'want', 'need', 'like', 'have', 'go', 'going', 'get', 'got', 'make',
  'help', 'stop', 'more', 'all', 'done', 'finished', 'no', 'yes', 'not', 'please',
  'thank', 'sorry', 'love', 'put', 'look', 'see', 'come', 'give', 'take', 'turn',
  'open', 'close', 'play', 'eat', 'drink', 'read', 'watch', 'listen', 'know', 'think',
  'feel', 'say', 'tell', 'ask', 'find', 'here', 'there', 'now', 'again', 'good',
  'bad', 'big', 'little', 'hot', 'cold', 'happy', 'sad', 'mad', 'tired', 'hungry',
  'thirsty', 'sick', 'what', 'where', 'who', 'when', 'why', 'how', 'some', 'on',
  'in', 'out', 'up', 'down', 'off', 'with', 'and', 'but', 'or', 'because',
  'to', 'too', 'home', 'school', 'work', 'time', 'today', 'mom', 'dad', 'friend',
  'water', 'food', 'snack', 'bathroom', 'break', 'music', 'book', 'show', 'game', 'outside',
  'okay', 'maybe', 'wait', 'ready', 'mine', 'yours', 'us', 'them', 'him', 'her',
];

// General high-frequency English words (deduped against CORE at build of RANKED_WORDS below).
export const COMMON_WORDS: string[] = [
  'of', 'for', 'be', 'at', 'by', 'from', 'as', 'so', 'if', 'about',
  'then', 'than', 'over', 'under', 'after', 'before', 'into', 'just', 'very', 'really',
  'also', 'only', 'much', 'many', 'most', 'one', 'two', 'three', 'first', 'next',
  'last', 'new', 'old', 'other', 'same', 'right', 'left', 'long', 'short', 'high',
  'low', 'fast', 'slow', 'nice', 'great', 'fun', 'every', 'each', 'any', 'no',
  'who', 'which', 'whose', 'would', 'could', 'should', 'may', 'might', 'must', 'shall',
  'am', 'been', 'being', 'has', 'had', 'does', 'made', 'said', 'went', 'gone',
  'come', 'came', 'see', 'saw', 'seen', 'looked', 'looking', 'gives', 'gave', 'taken',
  'let', 'keep', 'kept', 'leave', 'left', 'start', 'started', 'finish', 'use', 'used',
  'call', 'called', 'try', 'trying', 'show', 'showed', 'move', 'moving', 'run', 'walk',
  'sit', 'stand', 'sleep', 'wake', 'wash', 'clean', 'buy', 'bought', 'pay', 'cook',
  'morning', 'night', 'day', 'week', 'month', 'year', 'minute', 'hour', 'later', 'soon',
  'always', 'never', 'sometimes', 'often', 'still', 'yet', 'already', 'almost', 'around', 'away',
  'back', 'together', 'alone', 'fine', 'sure', 'true', 'real', 'better', 'best', 'worse',
  'people', 'person', 'man', 'woman', 'boy', 'girl', 'baby', 'kid', 'family', 'teacher',
  'house', 'room', 'door', 'car', 'bus', 'park', 'store', 'class', 'place', 'world',
  'thing', 'something', 'anything', 'nothing', 'everything', 'someone', 'anyone', 'everyone', 'name', 'word',
  'milk', 'juice', 'cookie', 'apple', 'pizza', 'bread', 'candy', 'lunch', 'dinner', 'breakfast',
  'dog', 'cat', 'ball', 'toy', 'phone', 'tv', 'computer', 'movie', 'song', 'color',
  'red', 'blue', 'green', 'yellow', 'black', 'white', 'please', 'thanks', 'hello', 'bye',
];

// Curated next-word transitions (last word -> likely next words, most likely first).
export const COMMON_BIGRAMS: Record<string, string[]> = {
  i: ['want', 'need', 'like', 'am', 'feel', 'have', 'can', 'will', 'do', 'love', 'see', 'know', 'think'],
  you: ['are', 'want', 'can', 'need', 'have', 'like', 'do', 'too', 'know'],
  we: ['can', 'are', 'want', 'will', 'need', 'go', 'should', 'have'],
  they: ['are', 'want', 'will', 'have', 'can', 'do'],
  he: ['is', 'was', 'wants', 'has', 'can', 'will'],
  she: ['is', 'was', 'wants', 'has', 'can', 'will'],
  it: ['is', 'was', 'looks', 'feels', 'hurts', 'works'],
  want: ['to', 'a', 'more', 'that', 'it', 'some', 'the'],
  need: ['to', 'a', 'help', 'more', 'some', 'the', 'water'],
  like: ['to', 'that', 'it', 'this', 'the', 'a'],
  love: ['you', 'it', 'that', 'this'],
  feel: ['happy', 'sad', 'sick', 'tired', 'good', 'bad', 'better', 'scared', 'hungry'],
  am: ['happy', 'sad', 'tired', 'hungry', 'done', 'okay', 'sorry', 'not', 'fine', 'ready', 'thirsty'],
  are: ['you', 'we', 'they', 'not', 'going', 'here', 'okay'],
  is: ['it', 'that', 'this', 'not', 'the', 'good', 'mine'],
  to: ['go', 'eat', 'play', 'the', 'see', 'do', 'drink', 'watch', 'read', 'school', 'bed', 'know'],
  go: ['to', 'home', 'outside', 'there', 'now', 'away'],
  going: ['to', 'home', 'outside', 'there'],
  can: ['i', 'you', 'we', 'have', 'help'],
  will: ['you', 'be', 'go', 'help'],
  do: ['you', 'not', 'it', 'that', 'this'],
  did: ['you', 'not', 'it'],
  have: ['a', 'to', 'more', 'it', 'some', 'fun'],
  get: ['a', 'it', 'more', 'the', 'up', 'ready'],
  let: ['us', 'me', 'go'],
  come: ['here', 'with', 'back', 'on'],
  stop: ['it', 'please', 'now', 'that'],
  more: ['please', 'food', 'time', 'water', 'of'],
  all: ['done', 'gone', 'of', 'right'],
  thank: ['you'],
  thanks: ['for'],
  good: ['morning', 'night', 'job', 'idea'],
  my: ['turn', 'mom', 'dad', 'friend', 'name', 'book', 'toy'],
  your: ['turn', 'name', 'help', 'book'],
  this: ['is', 'one', 'please'],
  that: ['is', 'one', 'please', 'too'],
  the: ['book', 'bathroom', 'door', 'car', 'tv', 'food', 'water', 'park', 'store', 'same'],
  a: ['little', 'lot', 'break', 'snack', 'book', 'drink', 'toy', 'turn'],
  what: ['is', 'do', 'are', 'time'],
  where: ['is', 'are', 'do'],
  how: ['are', 'do', 'much', 'many'],
  please: ['help', 'stop', 'come'],
  not: ['now', 'okay', 'yet', 'sure'],
  no: ['thank', 'more', 'thanks'],
  yes: ['please'],
  help: ['me', 'please'],
  give: ['me', 'it'],
  put: ['it', 'down', 'on'],
  look: ['at', 'here'],
  see: ['you', 'it', 'that'],
};

// One frequency-ordered, de-duplicated word list (CORE first) for completion + cold-start fallback.
export const RANKED_WORDS: string[] = (() => {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const w of [...CORE_WORDS, ...COMMON_WORDS]) {
    const lw = w.toLowerCase();
    if (!seen.has(lw)) {
      seen.add(lw);
      out.push(lw);
    }
  }
  return out;
})();
