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

// New-menu dialog.
import React, { useState } from 'react';
import { Modal, Pressable, View } from 'react-native';
import { useStore } from '../../store';
import { useTheme } from '../../theme';
import { Text, TextInput } from '../themed';
import { useStrings } from '../../i18n';

export function NewMenuDialog() {
  const show = useStore((s) => s.showNewMenuDialog);
  const addMenu = useStore((s) => s.addMenu);
  const closeDialogs = useStore((s) => s.closeDialogs);

  const [title, setTitle] = useState('');
  const t = useTheme();
  const S = useStrings();

  const cancel = () => {
    setTitle('');
    closeDialogs();
  };
  const create = () => {
    if (title.trim()) addMenu(title.trim());
    setTitle('');
    closeDialogs();
  };

  return (
    <Modal visible={show} transparent animationType="fade" onRequestClose={cancel}>
      <View style={{ flex: 1, backgroundColor: t.scrim, justifyContent: 'center', padding: 24 }}>
        <View style={{ backgroundColor: t.surface, borderRadius: 12, padding: 20 }}>
          <Text style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 12 }}>{S.dialogs.newMenu.title}</Text>
          <Text style={{ fontSize: 14 }}>{S.dialogs.newMenu.menuTitle}</Text>
          <TextInput
            value={title}
            onChangeText={setTitle}
            autoFocus
            style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 8, marginTop: 4 }}
          />
          <Text style={{ fontSize: 12, color: t.subtext, marginTop: 8 }}>{S.dialogs.newMenu.emptyNote}</Text>
          <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginTop: 16 }}>
            <Pressable onPress={cancel} style={{ padding: 10, marginRight: 8 }}>
              <Text style={{ fontSize: 16 }}>{S.common.cancel}</Text>
            </Pressable>
            <Pressable onPress={create} style={{ padding: 10 }}>
              <Text style={{ fontSize: 16, fontWeight: 'bold' }}>{S.dialogs.newMenu.create}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
