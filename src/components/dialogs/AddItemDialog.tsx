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

// Add-item dialog.
import React, { useState } from 'react';
import { Modal, Pressable, ScrollView, View } from 'react-native';
import { useStore } from '../../store';
import { useTheme } from '../../theme';
import { Text, TextInput } from '../themed';
import { findMenu } from '../../menus';
import { fmt } from '../../strings';
import { useStrings } from '../../i18n';
import { useAutoTranslate } from '../../useAutoTranslate';

export function AddItemDialog() {
  const show = useStore((s) => s.showAddItemDialog);
  const linkedMenu = useStore((s) => s.linkedMenu);
  const menuList = useStore((s) => s.menu_list);
  const addItem = useStore((s) => s.addItem);
  const closeDialogs = useStore((s) => s.closeDialogs);

  const [name, setName] = useState('');
  const [isSymbol, setIsSymbol] = useState(true);
  const [ttsType, setTtsType] = useState(2);
  const [folderTarget, setFolderTarget] = useState<number>(menuList[0]?.id ?? 0);
  const t = useTheme();
  const S = useStrings();
  const tr = useAutoTranslate(menuList.map((m) => m.title));

  const reset = () => {
    setName('');
    setIsSymbol(true);
    setTtsType(2);
    setFolderTarget(menuList[0]?.id ?? 0);
  };
  const cancel = () => {
    reset();
    closeDialogs();
  };
  const add = () => {
    if (name.trim()) {
      addItem(linkedMenu, {
        name: name.trim(),
        isSymbol,
        ttsMode: isSymbol ? ttsType : null,
        pointer: isSymbol ? null : folderTarget,
      });
    }
    reset();
    closeDialogs();
  };

  return (
    <Modal visible={show} transparent animationType="fade" onRequestClose={cancel}>
      <View style={{ flex: 1, backgroundColor: t.scrim, justifyContent: 'center', padding: 24 }}>
        <View style={{ backgroundColor: t.surface, borderRadius: 12, padding: 20, maxHeight: '85%' }}>
          <Text style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 12 }}>
            {fmt(S.dialogs.addItem.title, { menuTitle: findMenu(menuList, linkedMenu).title })}
          </Text>
          <Text style={{ fontSize: 14 }}>{S.dialogs.addItem.name}</Text>
          <TextInput
            value={name}
            onChangeText={setName}
            autoFocus
            style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 8, marginTop: 4 }}
          />
          <View style={{ flexDirection: 'row', marginTop: 12 }}>
            <Pressable onPress={() => setIsSymbol(true)} style={{ padding: 8 }}>
              <Text>{isSymbol ? `● ${S.dialogs.addItem.symbol}` : `○ ${S.dialogs.addItem.symbol}`}</Text>
            </Pressable>
            <Pressable onPress={() => setIsSymbol(false)} style={{ padding: 8 }}>
              <Text>{!isSymbol ? `● ${S.dialogs.addItem.folder}` : `○ ${S.dialogs.addItem.folder}`}</Text>
            </Pressable>
          </View>

          {isSymbol ? (
            <View style={{ flexDirection: 'row', marginTop: 8, flexWrap: 'wrap' }}>
              {[
                [S.dialogs.ttsTypeLabels[0], 0],
                [S.dialogs.ttsTypeLabels[1], 1],
                [S.dialogs.ttsTypeLabels[2], 2],
              ].map(([label, value]) => (
                <Pressable key={String(value)} onPress={() => setTtsType(value as number)} style={{ padding: 8 }}>
                  <Text style={{ fontSize: 13 }}>{ttsType === value ? `● ${label}` : `○ ${label}`}</Text>
                </Pressable>
              ))}
            </View>
          ) : (
            <View style={{ marginTop: 8 }}>
              <Text style={{ fontSize: 14 }}>{S.dialogs.addItem.folderTargetMenu}</Text>
              <ScrollView style={{ maxHeight: 150 }}>
                {menuList.map((m) => (
                  <Pressable key={m.id} onPress={() => setFolderTarget(m.id)} style={{ padding: 4 }}>
                    <Text style={{ fontSize: 13 }}>{folderTarget === m.id ? `● ${tr(m.title)}` : `○ ${tr(m.title)}`}</Text>
                  </Pressable>
                ))}
              </ScrollView>
            </View>
          )}

          <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginTop: 16 }}>
            <Pressable onPress={cancel} style={{ padding: 10, marginRight: 8 }}>
              <Text style={{ fontSize: 16 }}>{S.common.cancel}</Text>
            </Pressable>
            <Pressable onPress={add} style={{ padding: 10 }}>
              <Text style={{ fontSize: 16, fontWeight: 'bold' }}>{S.common.add}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
