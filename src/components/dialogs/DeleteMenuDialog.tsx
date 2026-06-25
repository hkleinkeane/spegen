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

// Delete-menu dialog.
import React from 'react';
import { Modal, Pressable, ScrollView, View } from 'react-native';
import { useStore } from '../../store';
import { useTheme } from '../../theme';
import { Text } from '../themed';
import { useStrings } from '../../i18n';
import { useAutoTranslate } from '../../useAutoTranslate';

export function DeleteMenuDialog() {
  const show = useStore((s) => s.showDeleteMenuDialog);
  const menuList = useStore((s) => s.menu_list);
  const removeMenu = useStore((s) => s.removeMenu);
  const closeDialogs = useStore((s) => s.closeDialogs);
  const t = useTheme();
  const S = useStrings();
  const tr = useAutoTranslate(menuList.map((m) => m.title));

  return (
    <Modal visible={show} transparent animationType="fade" onRequestClose={closeDialogs}>
      <View style={{ flex: 1, backgroundColor: t.scrim, justifyContent: 'center', padding: 24 }}>
        <View style={{ backgroundColor: t.surface, borderRadius: 12, padding: 20, maxHeight: '85%' }}>
          <Text style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 8 }}>{S.dialogs.deleteMenu.title}</Text>
          <Text style={{ fontSize: 12, color: t.subtext, marginBottom: 8 }}>{S.dialogs.deleteMenu.hint}</Text>
          <ScrollView style={{ maxHeight: 300 }}>
            {menuList
              .filter((m) => m.id !== 0)
              .map((m) => (
                <View
                  key={m.id}
                  style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 4 }}
                >
                  <Text style={{ flex: 1, fontSize: 16 }}>{tr(m.title)}</Text>
                  <Pressable
                    onPress={() => removeMenu(m.id)}
                    style={{ backgroundColor: t.danger, borderRadius: 6, paddingHorizontal: 12, paddingVertical: 6 }}
                  >
                    <Text style={{ color: '#FFFFFF' }}>{S.dialogs.deleteMenu.delete}</Text>
                  </Pressable>
                </View>
              ))}
          </ScrollView>
          <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginTop: 16 }}>
            <Pressable onPress={closeDialogs} style={{ padding: 10 }}>
              <Text style={{ fontSize: 16, fontWeight: 'bold' }}>{S.common.done}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
