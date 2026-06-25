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

// Button boxes: right-hand control cluster.
import React, { useMemo, useState } from 'react';
import { Modal, Pressable, ScrollView, View } from 'react-native';
import { useStore } from '../store';
import { useTheme } from '../theme';
import { Text, TextInput } from './themed';
import { completeWord } from '../ngram';
import { stopSpeaking } from '../tts';
import { useStrings } from '../i18n';

function Btn({
  label,
  left,
  top,
  width,
  height,
  onPress,
}: {
  label: string;
  left: number;
  top: number;
  width: number;
  height: number;
  onPress: () => void;
}) {
  const t = useTheme();
  return (
    <Pressable
      onPress={onPress}
      style={{
        position: 'absolute',
        left,
        top,
        width,
        height,
        backgroundColor: t.surface,
        borderWidth: 2,
        borderColor: t.border,
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <Text style={{ color: t.text, padding: 3, textAlign: 'center' }}>{label}</Text>
    </Pressable>
  );
}

export function ButtonBoxes() {
  const screenWidth = useStore((s) => s.screenWidth);
  const screenHeight = useStore((s) => s.screenHeight);
  const bbw = useStore((s) => s.buttonBoxesWidth);
  const menuStaticRowHeight = useStore((s) => s.menuStaticRowHeight);
  const staticRowHeight = useStore((s) => s.staticRowHeight);
  const showKeyboard = useStore((s) => s.showKeyboard);

  const openDialog = useStore((s) => s.openDialog);
  const requestSettings = useStore((s) => s.requestSettings);
  const closeDialogs = useStore((s) => s.closeDialogs);
  const S = useStrings();
  const deleteLastInput = useStore((s) => s.deleteLastInput);
  const clearInput = useStore((s) => s.clearInput);
  const back = useStore((s) => s.back);
  const addInputText = useStore((s) => s.addInputText);
  const t = useTheme();

  const [typed, setTyped] = useState('');
  // Word completion (B-upgrade): frequency-ranked words starting with what's typed.
  const completions = useMemo(() => completeWord(typed, 8), [typed]);

  if (screenWidth <= 0 || bbw <= 0) return null;

  const xRight = screenWidth - bbw;
  const xLeft = screenWidth - bbw * 2;
  const autocompleteHeight = screenHeight - bbw * 4 - menuStaticRowHeight - staticRowHeight;

  const submitKeyboard = () => {
    const trimmed = typed.trim();
    if (trimmed) addInputText(trimmed);
    setTyped('');
    closeDialogs();
  };
  // Tap a completion: add that single word and clear the field so the next word can be typed.
  const pickCompletion = (word: string) => {
    addInputText(word.charAt(0).toUpperCase() + word.slice(1));
    setTyped('');
  };

  return (
    <>
      {/* right column */}
      {/* Settings is the only path to editor mode, so it is the single PIN choke point. */}
      <Btn label={S.board.settings} left={xRight} top={0} width={bbw} height={bbw} onPress={requestSettings} />
      <Btn label={S.board.search} left={xRight} top={bbw} width={bbw} height={bbw} onPress={() => openDialog('wordFinder')} />
      <Btn label={S.board.stop} left={xRight} top={bbw * 2} width={bbw} height={bbw} onPress={() => stopSpeaking()} />

      {/* left column */}
      <Btn label={S.board.keyboard} left={xLeft} top={0} width={bbw} height={bbw} onPress={() => openDialog('keyboard')} />
      <Btn label={S.board.delete} left={xLeft} top={bbw} width={bbw} height={bbw} onPress={deleteLastInput} />
      <Btn label={S.board.clear} left={xLeft} top={bbw * 2} width={bbw} height={bbw} onPress={clearInput} />

      {/* spanning buttons */}
      <Btn label={S.board.back} left={xLeft} top={bbw * 3} width={bbw * 2} height={bbw} onPress={back} />
      <Btn
        label={S.board.autocomplete}
        left={xLeft}
        top={bbw * 4}
        width={bbw * 2}
        height={Math.max(autocompleteHeight, 0)}
        onPress={() => openDialog('autocomplete')}
      />

      <Modal visible={showKeyboard} transparent animationType="fade" onRequestClose={closeDialogs}>
        <View style={{ flex: 1, backgroundColor: t.scrim, justifyContent: 'center', padding: 24 }}>
          <View style={{ backgroundColor: t.surface, borderRadius: 12, padding: 20 }}>
            <Text style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 12 }}>{S.board.addText}</Text>
            <TextInput
              value={typed}
              onChangeText={setTyped}
              autoFocus
              onSubmitEditing={submitKeyboard}
              style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 10, fontSize: 16 }}
            />
            {completions.length > 0 && (
              <ScrollView
                horizontal
                keyboardShouldPersistTaps="handled"
                showsHorizontalScrollIndicator={false}
                style={{ marginTop: 8 }}
                contentContainerStyle={{ gap: 8 }}
              >
                {completions.map((w) => (
                  <Pressable
                    key={w}
                    onPress={() => pickCompletion(w)}
                    style={{ backgroundColor: t.surfaceAlt, borderRadius: 16, paddingHorizontal: 14, paddingVertical: 8 }}
                  >
                    <Text style={{ fontSize: 15 }}>{w}</Text>
                  </Pressable>
                ))}
              </ScrollView>
            )}
            <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginTop: 16 }}>
              <Pressable onPress={closeDialogs} style={{ padding: 10, marginRight: 8 }}>
                <Text style={{ fontSize: 16 }}>{S.common.cancel}</Text>
              </Pressable>
              <Pressable onPress={submitKeyboard} style={{ padding: 10 }}>
                <Text style={{ fontSize: 16, fontWeight: 'bold' }}>{S.common.add}</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </>
  );
}
