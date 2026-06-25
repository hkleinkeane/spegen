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

// Tutorial spotlight walkthrough overlay.
import React, { useEffect, useMemo, useState } from 'react';
import { Pressable, View } from 'react-native';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Text } from './themed';
import { findMenu } from '../menus';
import { computeBoardGrid } from '../boardLayout';
import { useStrings } from '../i18n';

interface Rect {
  x: number;
  y: number;
  width: number;
  height: number;
}

interface Slide {
  title: string;
  body: string;
  highlight?: Rect | null;
  lookup?: 'folder' | 'symbol';
}

function clamp(v: number, lo: number, hi: number): number {
  if (hi < lo) return lo;
  return Math.max(lo, Math.min(hi, v));
}

function computeCardPos(
  h: Rect | null,
  sw: number,
  sh: number,
  cw: number,
  ch: number
): { x: number; y: number } {
  if (!h) return { x: (sw - cw) / 2, y: (sh - ch) / 2 };
  const gap = 16;
  const centeredX = clamp(h.x + h.width / 2 - cw / 2, gap, sw - cw - gap);
  const centeredY = clamp(h.y + h.height / 2 - ch / 2, gap, sh - ch - gap);
  const overlaps = (cx: number, cy: number) => {
    const noH = cx + cw <= h.x || cx >= h.x + h.width;
    const noV = cy + ch <= h.y || cy >= h.y + h.height;
    return !(noH || noV);
  };
  const fits = (cx: number, cy: number) =>
    cx >= gap && cy >= gap && cx + cw <= sw - gap && cy + ch <= sh - gap;
  const candidates: [number, number][] = [
    [centeredX, h.y + h.height + gap], // below
    [centeredX, h.y - ch - gap], // above
    [h.x + h.width + gap, centeredY], // right
    [h.x - cw - gap, centeredY], // left
  ];
  for (const [cx, cy] of candidates) if (fits(cx, cy) && !overlaps(cx, cy)) return { x: cx, y: cy };
  return { x: (sw - cw) / 2, y: (sh - ch) / 2 };
}

const RING = '#FFCC02';
const DIM = 'rgba(0,0,0,0.8)';

export function Tutorial() {
  const show = useStore((s) => s.showTutorial);
  const setSetting = useStore((s) => s.setSetting);
  const menuList = useStore((s) => s.menu_list);
  const screenWidth = useStore((s) => s.screenWidth);
  const screenHeight = useStore((s) => s.screenHeight);
  const bbw = useStore((s) => s.buttonBoxesWidth);
  const inputBoxHeight = useStore((s) => s.inputBoxHeight);
  const menuStaticRowHeight = useStore((s) => s.menuStaticRowHeight);
  const staticRowHeight = useStore((s) => s.staticRowHeight);
  const menuWidth = useStore((s) => s.menuWidth);
  const menuHeight = useStore((s) => s.menuHeight);
  const boxWidth = useStore((s) => s.box_width_size_dp);
  const boxHeight = useStore((s) => s.box_height_size_dp);
  const boxPadding = useStore((s) => s.box_padding_dp);
  const t = useTheme();
  const S = useStrings();

  const [currentSlide, setCurrentSlide] = useState(0);

  const xRight = screenWidth - bbw;
  const xLeft = xRight - bbw;

  const slides = useMemo<Slide[]>(
    () => [
      { ...S.tutorial.slides[0] },
      { ...S.tutorial.slides[1], lookup: 'folder' },
      { ...S.tutorial.slides[2], lookup: 'symbol' },
      {
        ...S.tutorial.slides[3],
        highlight: { x: 0, y: 0, width: screenWidth - bbw * 2, height: inputBoxHeight },
      },
      {
        ...S.tutorial.slides[4],
        highlight: {
          x: 0,
          y: screenHeight - staticRowHeight - menuStaticRowHeight,
          width: screenWidth,
          height: menuStaticRowHeight,
        },
      },
      {
        ...S.tutorial.slides[5],
        highlight: { x: 0, y: screenHeight - staticRowHeight, width: screenWidth, height: staticRowHeight },
      },
      {
        ...S.tutorial.slides[6],
        highlight: { x: xLeft, y: 0, width: bbw, height: bbw },
      },
      {
        ...S.tutorial.slides[7],
        highlight: { x: xLeft, y: bbw, width: bbw, height: bbw },
      },
      {
        ...S.tutorial.slides[8],
        highlight: { x: xLeft, y: bbw * 2, width: bbw, height: bbw },
      },
      {
        ...S.tutorial.slides[9],
        highlight: { x: xRight, y: bbw * 2, width: bbw, height: bbw },
      },
      {
        ...S.tutorial.slides[10],
        highlight: { x: xRight, y: bbw, width: bbw, height: bbw },
      },
      {
        ...S.tutorial.slides[11],
        highlight: { x: xRight, y: 0, width: bbw, height: bbw },
      },
      {
        ...S.tutorial.slides[12],
        highlight: { x: xLeft, y: bbw * 3, width: bbw * 2, height: bbw },
      },
      {
        ...S.tutorial.slides[13],
        highlight: {
          x: xLeft,
          y: bbw * 4,
          width: bbw * 2,
          height: screenHeight - bbw * 4 - menuStaticRowHeight - staticRowHeight,
        },
      },
    ],
    [screenWidth, screenHeight, bbw, inputBoxHeight, menuStaticRowHeight, staticRowHeight, xLeft, xRight, S]
  );

  // Index of the first folder/symbol on the home board (-1 if none). Drives both the spotlight
  // rect and the board page-jump (tutorialScrollToIndex).
  const findHomeItemIndex = (isSymbol: boolean): number => {
    const home = findMenu(menuList, 0);
    for (let i = 0; i < home.item_type.length; i++) {
      if (home.item_type[i] === isSymbol) return i;
    }
    return -1;
  };

  const itemHighlight = (isSymbol: boolean): Rect => {
    const fallback: Rect = { x: 0, y: inputBoxHeight, width: menuWidth, height: menuHeight };
    const home = findMenu(menuList, 0);
    const idx = findHomeItemIndex(isSymbol);
    if (idx < 0 || menuWidth <= 0 || menuHeight <= 0) return fallback;
    const grid = computeBoardGrid(boxWidth, boxHeight, boxPadding, menuWidth, menuHeight, home.item_list.length);
    // Spotlight the cell at the item's LOCAL position within its page (the board pager scrolls
    // to that page via tutorialScrollToIndex), so items past page 0 are highlighted correctly
    // instead of bailing to the whole-board fallback.
    const localIdx = idx % grid.itemsPerPage;
    const row = Math.floor(localIdx / grid.itemsPerRow);
    const col = localIdx % grid.itemsPerRow;
    return {
      x: col * grid.cellWidth,
      y: inputBoxHeight + row * grid.cellHeight,
      width: grid.cellWidth,
      height: grid.cellHeight,
    };
  };

  const maxSlide = slides.length - 1;
  const current = slides[clamp(currentSlide, 0, maxSlide)];
  const lookupIndex = current.lookup ? findHomeItemIndex(current.lookup === 'symbol') : -1;

  // Publish the spotlighted item's index so the board pager scrolls to its page.
  // -1 on non-lookup slides / when the tutorial is hidden.
  useEffect(() => {
    useStore.setState({ tutorialScrollToIndex: show ? lookupIndex : -1 });
  }, [show, lookupIndex]);

  if (!show || screenWidth <= 0 || screenHeight <= 0) return null;

  const highlight: Rect | null = current.lookup
    ? itemHighlight(current.lookup === 'symbol')
    : current.highlight ?? null;

  const cardWidth = Math.min(screenWidth * 0.85, 420);
  const cardHeight = 220;
  const card = computeCardPos(highlight, screenWidth, screenHeight, cardWidth, cardHeight);

  const finish = () => {
    setSetting('has_seen_tutorial', true);
    // Settings no longer auto-save, so persist this flag now (and update the saved baseline)
    // — otherwise the tutorial re-shows next launch and reads as an unsaved change in Settings.
    void useStore.getState().save();
    setCurrentSlide(0);
    useStore.setState({ showTutorial: false, tutorialScrollToIndex: -1 });
  };

  const swallow = (extra?: object) => (
    <Pressable onPress={() => undefined} style={{ position: 'absolute', ...extra }} />
  );

  return (
    <View
      style={{ position: 'absolute', left: 0, top: 0, width: screenWidth, height: screenHeight, zIndex: 2000 }}
    >
      {highlight == null ? (
        <Pressable
          onPress={() => undefined}
          style={{ position: 'absolute', left: 0, top: 0, width: screenWidth, height: screenHeight, backgroundColor: DIM }}
        />
      ) : (
        <>
          {/* Top strip */}
          {swallow({ left: 0, top: 0, width: screenWidth, height: Math.max(highlight.y, 0), backgroundColor: DIM })}
          {/* Bottom strip */}
          {swallow({
            left: 0,
            top: highlight.y + highlight.height,
            width: screenWidth,
            height: Math.max(screenHeight - (highlight.y + highlight.height), 0),
            backgroundColor: DIM,
          })}
          {/* Left strip */}
          {swallow({
            left: 0,
            top: highlight.y,
            width: Math.max(highlight.x, 0),
            height: highlight.height,
            backgroundColor: DIM,
          })}
          {/* Right strip */}
          {swallow({
            left: highlight.x + highlight.width,
            top: highlight.y,
            width: Math.max(screenWidth - (highlight.x + highlight.width), 0),
            height: highlight.height,
            backgroundColor: DIM,
          })}
          {/* Ring (non-interactive) */}
          <View
            pointerEvents="none"
            style={{
              position: 'absolute',
              left: highlight.x,
              top: highlight.y,
              width: highlight.width,
              height: highlight.height,
              borderWidth: 4,
              borderColor: RING,
              borderRadius: 8,
            }}
          />
          {/* Swallow taps over the hole so the board isn't interactive mid-tour */}
          {swallow({ left: highlight.x, top: highlight.y, width: highlight.width, height: highlight.height })}
        </>
      )}

      {/* Info card */}
      <View
        style={{
          position: 'absolute',
          left: card.x,
          top: card.y,
          width: cardWidth,
          backgroundColor: t.surface,
          borderRadius: 16,
          borderWidth: 2,
          borderColor: RING,
          padding: 20,
          alignItems: 'center',
        }}
      >
        <Text style={{ fontSize: 22, fontWeight: 'bold', textAlign: 'center' }}>{current.title}</Text>
        <Text style={{ fontSize: 14, textAlign: 'center', marginTop: 8 }}>{current.body}</Text>

        {/* Progress dots */}
        <View style={{ flexDirection: 'row', justifyContent: 'center', marginTop: 12 }}>
          {slides.map((_, i) => (
            <View
              key={i}
              style={{
                marginHorizontal: 2,
                width: i === currentSlide ? 8 : 5,
                height: i === currentSlide ? 8 : 5,
                borderRadius: 50,
                backgroundColor: i === currentSlide ? t.text : 'rgba(128,128,128,0.4)',
              }}
            />
          ))}
        </View>

        <View style={{ flexDirection: 'row', marginTop: 12 }}>
          {currentSlide > 0 && (
            <Pressable
              onPress={() => setCurrentSlide((s) => Math.max(0, s - 1))}
              style={{ backgroundColor: t.primary, borderRadius: 6, paddingHorizontal: 14, paddingVertical: 8, marginHorizontal: 4 }}
            >
              <Text style={{ color: '#FFFFFF' }}>{S.common.back}</Text>
            </Pressable>
          )}
          <Pressable
            onPress={() => (currentSlide < maxSlide ? setCurrentSlide((s) => s + 1) : finish())}
            style={{ backgroundColor: t.primary, borderRadius: 6, paddingHorizontal: 14, paddingVertical: 8, marginHorizontal: 4 }}
          >
            <Text style={{ color: '#FFFFFF' }}>{currentSlide < maxSlide ? S.common.next : S.common.done}</Text>
          </Pressable>
          {currentSlide < maxSlide && (
            <Pressable
              onPress={finish}
              style={{ backgroundColor: t.neutral, borderRadius: 6, paddingHorizontal: 14, paddingVertical: 8, marginHorizontal: 4 }}
            >
              <Text style={{ color: '#FFFFFF' }}>{S.common.skip}</Text>
            </Pressable>
          )}
        </View>
      </View>
    </View>
  );
}
