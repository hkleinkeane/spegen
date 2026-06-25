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

// Drag-to-reorder list.
import React, { useRef, useState } from 'react';
import { Animated, PanResponder, View } from 'react-native';
import { useTheme } from '../theme';
import { Text } from './themed';

export const REORDER_ROW_HEIGHT = 56;

interface Props<T> {
  items: T[];
  onReorder: (next: T[]) => void;
  renderContent: (index: number, item: T) => React.ReactNode;
  renderTrailing: (index: number, item: T) => React.ReactNode;
  // Fired with true when a drag starts and false when it ends, so a parent ScrollView can set
  // scrollEnabled={false} during the drag (otherwise the parent steals the vertical pan and the
  // grabbed row snaps back to its origin — the core cause of the reported "reorder doesn't work").
  onDragActiveChange?: (active: boolean) => void;
}

export function ReorderableTermList<T>({
  items,
  onReorder,
  renderContent,
  renderTrailing,
  onDragActiveChange,
}: Props<T>) {
  const t = useTheme();
  const [startIndex, setStartIndex] = useState(-1); // slot grabbed at drag start (fixed for the gesture)
  const [targetIndex, setTargetIndex] = useState(-1); // current drop slot (drives the gap preview)
  const dragY = useRef(new Animated.Value(0)).current;

  // PanResponder closures are recreated each render but read live values from refs so an
  // in-flight gesture (whose responder node persists by index key) stays consistent across
  // the re-renders triggered by the gap preview.
  const itemsRef = useRef(items);
  itemsRef.current = items;
  const onReorderRef = useRef(onReorder);
  onReorderRef.current = onReorder;
  const onDragActiveChangeRef = useRef(onDragActiveChange);
  onDragActiveChangeRef.current = onDragActiveChange;
  const startIndexRef = useRef(-1);
  const targetIndexRef = useRef(-1);

  const endDrag = () => {
    const start = startIndexRef.current;
    const target = targetIndexRef.current;
    // Commit the move exactly once, here on release/terminate (not mid-drag).
    if (start >= 0 && target >= 0 && target !== start) {
      const next = itemsRef.current.slice();
      const moved = next.splice(start, 1)[0];
      next.splice(target, 0, moved);
      onReorderRef.current(next);
    }
    startIndexRef.current = -1;
    targetIndexRef.current = -1;
    setStartIndex(-1);
    setTargetIndex(-1);
    dragY.setValue(0);
    onDragActiveChangeRef.current?.(false);
  };

  const makeResponder = (index: number) =>
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      // Capture the move on the handle BEFORE an ancestor ScrollView can claim it, and once we
      // hold the responder, refuse to hand it back. Without these, a parent vertical ScrollView
      // terminates our gesture mid-drag (onPanResponderTerminate), snapping the row back.
      onMoveShouldSetPanResponderCapture: () => true,
      onPanResponderTerminationRequest: () => false,
      onShouldBlockNativeResponder: () => true,
      onPanResponderGrant: () => {
        startIndexRef.current = index;
        targetIndexRef.current = index;
        dragY.setValue(0);
        setStartIndex(index);
        setTargetIndex(index);
        onDragActiveChangeRef.current?.(true);
      },
      onPanResponderMove: (_e, g) => {
        const start = startIndexRef.current;
        if (start < 0) return;
        const n = itemsRef.current.length;
        const movedRows = Math.round(g.dy / REORDER_ROW_HEIGHT);
        let target = start + movedRows;
        if (target < 0) target = 0;
        if (target > n - 1) target = n - 1;
        if (target !== targetIndexRef.current) {
          targetIndexRef.current = target;
          setTargetIndex(target);
        }
        // The grabbed row tracks the finger directly; the list order stays put until release.
        dragY.setValue(g.dy);
      },
      onPanResponderRelease: endDrag,
      onPanResponderTerminate: endDrag,
    });

  // Memoize one PanResponder per row index. Recreating it every render (as before) handed the
  // grabbed node a fresh responder mid-gesture on each gap-preview re-render, which could drop
  // the in-flight touch; a stable responder (reading live values from refs) avoids that.
  const respondersRef = useRef(new Map<number, ReturnType<typeof PanResponder.create>>());
  const getResponder = (index: number) => {
    let r = respondersRef.current.get(index);
    if (!r) {
      r = makeResponder(index);
      respondersRef.current.set(index, r);
    }
    return r;
  };

  return (
    <View>
      {items.map((item, index) => {
        const isDragging = index === startIndex;
        // Preview the drop: rows between the grabbed slot and the current target slide one row
        // to open a gap at `targetIndex` (the grabbed row will land there on release).
        let shift = 0;
        if (startIndex >= 0 && !isDragging) {
          if (startIndex < targetIndex && index > startIndex && index <= targetIndex) {
            shift = -REORDER_ROW_HEIGHT;
          } else if (startIndex > targetIndex && index >= targetIndex && index < startIndex) {
            shift = REORDER_ROW_HEIGHT;
          }
        }
        const responder = getResponder(index);
        return (
          <Animated.View
            key={index}
            style={[
              {
                flexDirection: 'row',
                alignItems: 'center',
                height: REORDER_ROW_HEIGHT,
                backgroundColor: isDragging ? t.surfaceAlt : t.surface,
                zIndex: isDragging ? 1 : 0,
              },
              isDragging
                ? { transform: [{ translateY: dragY }], opacity: 0.9 }
                : { transform: [{ translateY: shift }] },
            ]}
          >
            <View
              style={{ height: '100%', justifyContent: 'center', paddingHorizontal: 12 }}
              {...responder.panHandlers}
            >
              <Text style={{ fontSize: 20, color: t.subtext }}>≡</Text>
            </View>
            {renderContent(index, item)}
            {renderTrailing(index, item)}
          </Animated.View>
        );
      })}
    </View>
  );
}
