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

// AsyncStorage persistence.
import AsyncStorage from '@react-native-async-storage/async-storage';
import { defaultPersistedState, padMenu, type PersistedState } from './types';
import { seedNgramModel } from './ngram';

const APP_STATE_KEY = 'app_state';

// Load persisted state, null on fresh install.
export async function loadPersistedState(): Promise<PersistedState | null> {
  let json: string | null;
  try {
    json = await AsyncStorage.getItem(APP_STATE_KEY);
  } catch (e) {
    console.warn(`Failed to read preferences: ${String(e)}`);
    return null;
  }
  if (json == null) return null;

  try {
    const parsed = JSON.parse(json) as Partial<PersistedState>;
    // Migrate v1 -> v2.
    const oldBoxSize = (parsed as Record<string, unknown>)['box_size_dp'] as number | undefined;
    if (oldBoxSize != null && parsed.box_width_size_dp == null) {
      parsed.box_width_size_dp = oldBoxSize;
      parsed.box_height_size_dp = oldBoxSize;
    }
    delete (parsed as Record<string, unknown>)['box_size_dp'];
    // Merge with defaults.
    const merged: PersistedState = { ...defaultPersistedState(), ...parsed };
    // Pad menu lists.
    merged.menu_list = (merged.menu_list ?? []).map(padMenu);
    // Reseed empty n-gram.
    if (!merged.ngram_model || Object.keys(merged.ngram_model.bigrams ?? {}).length === 0) {
      merged.ngram_model = seedNgramModel();
    }
    return merged;
  } catch (e) {
    console.warn(`Failed to load preferences: ${String(e)}`);
    return null;
  }
}

export async function savePersistedState(state: PersistedState): Promise<void> {
  try {
    const padded: PersistedState = { ...state, menu_list: state.menu_list.map(padMenu) };
    await AsyncStorage.setItem(APP_STATE_KEY, JSON.stringify(padded));
  } catch (e) {
    console.warn(`Failed to save preferences: ${String(e)}`);
  }
}

export async function clearPersistedState(): Promise<void> {
  try {
    await AsyncStorage.removeItem(APP_STATE_KEY);
  } catch (e) {
    console.warn(`Failed to clear preferences: ${String(e)}`);
  }
}
