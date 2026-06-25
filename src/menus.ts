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

// Menu logic.
import type { MenuTemplate } from './types';

// Find menu by id, fallback to home.
export function findMenu(menuList: MenuTemplate[], menuId: number | null | undefined): MenuTemplate {
  const home = menuList.find((m) => m.id === 0) ?? menuList[0];
  if (typeof menuId !== 'number') return home;
  return menuList.find((m) => m.id === menuId) ?? home;
}

export function parentsOf(menuList: MenuTemplate[], menuId: number): number[] {
  if (menuId === 0) return [];
  return menuList.filter((menu) => menu.pointers.includes(menuId)).map((m) => m.id);
}

export function primaryParentOf(menuList: MenuTemplate[], menuId: number): number | null {
  const parents = parentsOf(menuList, menuId);
  return parents.length > 0 ? parents[0] : null;
}

export function allPathsToMenu(menuList: MenuTemplate[], targetId: number): number[][] {
  const results: number[][] = [];
  const dfs = (currentId: number, pathSoFar: number[]) => {
    if (pathSoFar.includes(currentId)) return;
    const newPath = [...pathSoFar, currentId];
    if (currentId === targetId) {
      results.push(newPath);
      return;
    }
    const menu = menuList.find((m) => m.id === currentId);
    if (!menu) return;
    const children = [...new Set(menu.pointers.filter((p): p is number => p != null))];
    for (const childId of children) dfs(childId, newPath);
  };
  dfs(0, []);
  return results;
}

export interface SymbolPath {
  menuPath: number[];
  menuNames: string[];
  containingMenuId: number;
  itemIndex: number;
}

export function allPathsToSymbol(
  menuList: MenuTemplate[],
  menuListIndex: number,
  itemIndex: number
): SymbolPath[] {
  const containingMenuId = menuList[menuListIndex].id;
  return allPathsToMenu(menuList, containingMenuId).map((menuIdPath) => ({
    menuPath: menuIdPath,
    menuNames: menuIdPath.map((id) => menuList.find((m) => m.id === id)?.title ?? '?'),
    containingMenuId,
    itemIndex,
  }));
}

// Remove indices from menu lists.
function removeIndices(menu: MenuTemplate, dead: Set<number>): MenuTemplate {
  const keep = <T>(arr: T[]): T[] => arr.filter((_, idx) => !dead.has(idx));
  return {
    ...menu,
    item_list: keep(menu.item_list),
    pointers: keep(menu.pointers),
    tts: keep(menu.tts),
    item_type: keep(menu.item_type),
    image_urls: keep(menu.image_urls),
    item_uuids: keep(menu.item_uuids),
    custom_image_paths: keep(menu.custom_image_paths),
    custom_audio_paths: keep(menu.custom_audio_paths),
    custom_audio_names: keep(menu.custom_audio_names),
    pronunciation_overrides: keep(menu.pronunciation_overrides),
    colors: keep(menu.colors),
    item_locales: keep(menu.item_locales),
    item_translations: keep(menu.item_translations),
    item_tts_locales: keep(menu.item_tts_locales),
  };
}

// Remove folders pointing to dead menus.
export function killDanglingPointers(menuList: MenuTemplate[]): MenuTemplate[] {
  const liveIds = new Set(menuList.map((m) => m.id));
  return menuList.map((m) => {
    const dead = new Set<number>();
    m.pointers.forEach((ptr, idx) => {
      if (ptr != null && !liveIds.has(ptr)) dead.add(idx);
    });
    return dead.size > 0 ? removeIndices(m, dead) : m;
  });
}

export interface DeleteMenuResult {
  menuList: MenuTemplate[];
  menuRowIds: number[];
}

// Delete a menu and prune pointers.
export function deleteMenu(
  menuList: MenuTemplate[],
  menuRowIds: number[],
  menuId: number
): DeleteMenuResult {
  if (menuId === 0) return { menuList, menuRowIds };

  let next = menuList.filter((m) => m.id !== menuId);

  next = next.map((m) => {
    const dead = new Set<number>();
    m.pointers.forEach((ptr, idx) => {
      if (ptr === menuId) dead.add(idx);
    });
    return dead.size > 0 ? removeIndices(m, dead) : m;
  });

  next = killDanglingPointers(next);

  return {
    menuList: next,
    menuRowIds: menuRowIds.filter((id) => id !== menuId),
  };
}

// Custom image path wins over URL.
export function displayUrl(menu: MenuTemplate, idx: number): string {
  const custom = menu.custom_image_paths[idx];
  if (custom && custom.trim()) return custom;
  return menu.image_urls[idx] ?? '';
}

// Resolve speech text and locale.
export function resolveSpeech(
  menu: MenuTemplate,
  index: number,
  multilingualLabels: boolean
): [string, string | null] {
  const defaultLabel = menu.item_list[index] ?? '';
  if (!multilingualLabels) return [defaultLabel, null];
  const loc = menu.item_tts_locales[index];
  if (!loc || !loc.trim()) return [defaultLabel, null];
  const translated = menu.item_translations[index]?.[loc] || defaultLabel;
  return [translated, loc];
}

// Next free menu id.
export function nextMenuId(menuList: MenuTemplate[]): number {
  return menuList.reduce((max, m) => Math.max(max, m.id), 0) + 1;
}
