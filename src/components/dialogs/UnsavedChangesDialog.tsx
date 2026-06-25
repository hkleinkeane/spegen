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

// Unsaved-changes confirmation dialog.
import React from 'react';
import { Modal, Pressable, View } from 'react-native';
import { useTheme } from '../../theme';
import { Text } from '../themed';
import { useStrings } from '../../i18n';

export function UnsavedChangesDialog({
  visible,
  onSave,
  onDiscard,
  onDismiss,
}: {
  visible: boolean;
  onSave: () => void;
  onDiscard: () => void;
  onDismiss: () => void;
}) {
  const t = useTheme();
  const S = useStrings();
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onDismiss}>
      <Pressable
        onPress={onDismiss}
        style={{
          flex: 1,
          backgroundColor: t.scrim,
          justifyContent: 'center',
          alignItems: 'center',
          padding: 24,
        }}
      >
        {/* Inner press is swallowed so tapping the card doesn't dismiss the dialog. */}
        <Pressable
          onPress={() => undefined}
          style={{ width: '100%', maxWidth: 360, backgroundColor: t.surface, borderRadius: 12, padding: 20 }}
        >
          <Text style={{ fontSize: 18, fontWeight: 'bold', color: t.text, marginBottom: 8 }}>{S.dialogs.unsaved.title}</Text>
          <Text style={{ fontSize: 14, color: t.text, marginBottom: 20 }}>{S.dialogs.unsaved.body}</Text>
          <View style={{ flexDirection: 'row', justifyContent: 'flex-end', alignItems: 'center' }}>
            <Pressable onPress={onDiscard} style={{ paddingHorizontal: 14, paddingVertical: 10, marginRight: 8 }}>
              <Text style={{ color: t.danger, fontWeight: 'bold' }}>{S.dialogs.unsaved.dontSave}</Text>
            </Pressable>
            <Pressable
              onPress={onSave}
              style={{ paddingHorizontal: 16, paddingVertical: 10, backgroundColor: t.primary, borderRadius: 8 }}
            >
              <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.dialogs.unsaved.saveChanges}</Text>
            </Pressable>
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}
