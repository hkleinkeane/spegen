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

// Editor toolbar.
import React, { useState } from 'react';
import { Platform, Pressable, ScrollView, StatusBar, Text, View } from 'react-native';
import { useStore } from '../store';
import { UnsavedChangesDialog } from './dialogs/UnsavedChangesDialog';
import { useStrings } from '../i18n';

function TBtn({ label, onPress, dark }: { label: string; onPress: () => void; dark?: boolean }) {
  return (
    <Pressable
      onPress={onPress}
      style={{
        backgroundColor: dark ? '#424242' : '#FFFFFF',
        borderRadius: 6,
        borderWidth: 1,
        borderColor: '#FF8F00',
        paddingHorizontal: 10,
        paddingVertical: 6,
        marginRight: 8,
      }}
    >
      <Text style={{ color: dark ? '#FFFFFF' : '#000000', fontSize: 13 }}>{label}</Text>
    </Pressable>
  );
}

export function EditorToolbar() {
  const openDialog = useStore((s) => s.openDialog);
  const applyChanges = useStore((s) => s.applyChanges);
  const discardChanges = useStore((s) => s.discardChanges);
  const hasUnsavedChanges = useStore((s) => s.hasUnsavedChanges);
  const [showUnsaved, setShowUnsaved] = useState(false);
  const S = useStrings();

  const exitEditor = () => useStore.setState({ editorMode: false });
  // Exit: prompt only when in-memory edits differ from what's saved.
  const onExit = () => {
    if (hasUnsavedChanges()) setShowUnsaved(true);
    else exitEditor();
  };

  return (
    <>
      <View
        style={{
          position: 'absolute',
          left: 0,
          top: 0,
          right: 0,
          backgroundColor: '#FFE082',
          borderWidth: 2,
          borderColor: '#FF8F00',
          // Pad below the Android status bar so the bar isn't hidden under the device toolbar.
          paddingTop: (Platform.OS === 'android' ? StatusBar.currentHeight ?? 0 : 0) + 8,
          paddingBottom: 8,
          zIndex: 800,
        }}
      >
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator
          // Android: keep the scroll indicator on screen at all times (not just while dragging) so
          // it's obvious the toolbar scrolls to reveal more actions. No-op on iOS.
          persistentScrollbar
          contentContainerStyle={{ alignItems: 'center', paddingHorizontal: 12 }}
        >
          <Text style={{ fontWeight: 'bold', fontSize: 16, marginRight: 12 }}>{S.editor.title}</Text>
          <TBtn label={S.editor.addItem} onPress={() => openDialog('addItem')} />
          <TBtn label={S.editor.addMenu} onPress={() => openDialog('newMenu')} />
          <TBtn label={S.editor.deleteMenu} onPress={() => openDialog('deleteMenu')} />
          <TBtn label={S.editor.gotoMenu} onPress={() => openDialog('gotoMenu')} />
          {/* Apply Changes = persist edits now without leaving editor mode. */}
          <TBtn label={S.editor.applyChanges} dark onPress={() => void applyChanges()} />
          <TBtn label={S.editor.exit} dark onPress={onExit} />
        </ScrollView>
      </View>
      <UnsavedChangesDialog
        visible={showUnsaved}
        onSave={() => {
          setShowUnsaved(false);
          void applyChanges().then(exitEditor);
        }}
        onDiscard={() => {
          setShowUnsaved(false);
          void discardChanges().then(exitEditor);
        }}
        onDismiss={() => setShowUnsaved(false)}
      />
    </>
  );
}
