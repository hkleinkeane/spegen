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

// Searchable language picker.
import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, View } from 'react-native';
import { APP_LANGUAGES } from '../types';
import { useTheme } from '../theme';
import { Text, TextInput } from './themed';
import { useStrings } from '../i18n';

export function LanguageDropdown({
  selectedCode,
  onSelected,
  label = 'Language',
}: {
  selectedCode: string;
  onSelected: (code: string) => void;
  label?: string;
}) {
  const [expanded, setExpanded] = useState(false);
  const [query, setQuery] = useState('');
  const t = useTheme();
  const S = useStrings();
  const current = APP_LANGUAGES.find((l) => l.code === selectedCode);
  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return APP_LANGUAGES;
    return APP_LANGUAGES.filter((l) => l.name.toLowerCase().includes(q) || l.code.toLowerCase().includes(q));
  }, [query]);

  return (
    <View style={{ width: '100%' }}>
      <Pressable
        onPress={() => setExpanded((e) => !e)}
        style={{
          flexDirection: 'row',
          alignItems: 'center',
          backgroundColor: expanded ? t.panelBorder : t.surfaceAlt,
          borderRadius: 8,
          paddingHorizontal: 12,
          paddingVertical: 12,
        }}
      >
        <Text style={{ fontSize: 14, fontWeight: '500', marginRight: 12 }}>{label}</Text>
        <Text style={{ fontSize: 14, flex: 1 }}>
          {current ? `${current.name} (${current.code})` : S.languageDropdown.select}
        </Text>
        <Text style={{ fontSize: 12, color: t.subtext }}>{expanded ? '▲' : '▼'}</Text>
      </Pressable>
      {expanded && (
        <View style={{ borderWidth: 1, borderColor: t.panelBorder, backgroundColor: t.surface }}>
          <TextInput
            value={query}
            onChangeText={setQuery}
            placeholder={S.languageDropdown.searchPlaceholder}
            style={{ borderBottomWidth: 1, borderColor: t.divider, padding: 8 }}
          />
          <ScrollView
            style={{ maxHeight: 240 }}
            keyboardShouldPersistTaps="handled"
            nestedScrollEnabled
            showsVerticalScrollIndicator
          >
            {filtered.map((lang) => (
              <Pressable
                key={lang.code}
                onPress={() => {
                  onSelected(lang.code);
                  setExpanded(false);
                  setQuery('');
                }}
                style={{ flexDirection: 'row', paddingHorizontal: 12, paddingVertical: 10 }}
              >
                <Text style={{ flex: 1, fontSize: 14 }}>{`${lang.name} (${lang.code})`}</Text>
                {lang.code === selectedCode && <Text style={{ color: t.primary }}>✓</Text>}
              </Pressable>
            ))}
          </ScrollView>
        </View>
      )}
    </View>
  );
}
