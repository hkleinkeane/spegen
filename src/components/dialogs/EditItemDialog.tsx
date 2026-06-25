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

// Edit-item dialog (name, image, color, audio, locale, labels).
import React, { useState } from 'react';
import { Modal, Pressable, ScrollView, View } from 'react-native';
import { Image } from 'expo-image';
import * as ImagePicker from 'expo-image-picker';
import {
  useAudioRecorder,
  useAudioPlayer,
  RecordingPresets,
  setAudioModeAsync,
  requestRecordingPermissionsAsync,
  getRecordingPermissionsAsync,
} from 'expo-audio';
import { File } from 'expo-file-system';
import { useStore } from '../../store';
import { useTheme } from '../../theme';
import { Text, TextInput } from '../themed';
import { findMenu } from '../../menus';
import { searchSymbols, resolveImageUrl } from '../../opensymbols';
import { HC_IMAGE_FILTER } from '../../hc';
import { resolveItemColor, effectiveCategoryColor } from '../../colors';
import { copyImageToStorage, copyAudioToStorage } from '../../media';
import { LanguageDropdown } from '../LanguageDropdown';
import { ColorPickerDialog } from './ColorPickerDialog';
import { useStrings } from '../../i18n';
import type { MenuTemplate } from '../../types';

function capitalize(s: string): string {
  return s.length > 0 ? s[0].toUpperCase() + s.slice(1) : s;
}

function EditItemContent({ menu, idx, onClose }: { menu: MenuTemplate; idx: number; onClose: () => void }) {
  const editItem = useStore((s) => s.editItem);
  const deleteItem = useStore((s) => s.deleteItem);
  const skinTone = useStore((s) => s.skin_tone);
  const highContrast = useStore((s) => s.highcontrastmode);
  const appLocale = useStore((s) => s.app_locale);
  const languageImageOverride = useStore((s) => s.language_image_override);
  const multilingual = useStore((s) => s.multilingual_labels);
  const textBottom = useStore((s) => s.text_location_bottom);
  const fitzgeraldKey = useStore((s) => s.fitzgeraldKey);
  const overrides = useStore((s) => s.fitzgerald_overrides);
  const t = useTheme();
  const S = useStrings();

  const originalIsSymbol = menu.item_type[idx];
  const existingPointer = menu.pointers[idx] ?? null;

  const [name, setName] = useState(menu.item_list[idx] ?? '');
  const [ttsType, setTtsType] = useState(menu.tts[idx] ?? 2);
  const [pron, setPron] = useState(menu.pronunciation_overrides[idx] ?? '');
  const [color, setColor] = useState(menu.colors[idx] ?? '');
  const [locale, setLocale] = useState(menu.item_locales[idx] ?? '');
  const [translations, setTranslations] = useState<Record<string, string>>(menu.item_translations[idx] ?? {});
  const [ttsLocale, setTtsLocale] = useState(menu.item_tts_locales[idx] ?? '');

  const itemUuid = menu.item_uuids[idx] || `${menu.id}-${idx}`;

  const [customPath, setCustomPath] = useState(menu.custom_image_paths[idx] ?? '');
  const initialImage = menu.custom_image_paths[idx] || menu.image_urls[idx] || '';
  const [images, setImages] = useState<string[]>(initialImage ? [initialImage] : []);
  const [loading, setLoading] = useState(false);
  const [addingLanguage, setAddingLanguage] = useState(false);

  const [audioPath, setAudioPath] = useState(menu.custom_audio_paths[idx] ?? '');
  const [audioName, setAudioName] = useState(menu.custom_audio_names[idx] ?? '');
  const [useCustomAudio, setUseCustomAudio] = useState((menu.custom_audio_paths[idx] ?? '').trim() !== '');
  const [isRecording, setIsRecording] = useState(false);
  const [audioError, setAudioError] = useState('');
  const [showRename, setShowRename] = useState(false);
  const [tempName, setTempName] = useState('');
  const [showColorPicker, setShowColorPicker] = useState(false);

  const recorder = useAudioRecorder(RecordingPresets.HIGH_QUALITY);
  const player = useAudioPlayer();

  const loadImages = async () => {
    if (!name.trim()) return;
    setLoading(true);
    const loc = languageImageOverride ? locale || appLocale : appLocale;
    const results = await searchSymbols(name.trim(), loc, highContrast, 9, images.length);
    const next = [...images];
    for (const r of results) if (r.image_url && !next.includes(r.image_url)) next.push(r.image_url);
    setImages(next);
    setLoading(false);
  };

  const pickCustomImage = async () => {
    try {
      const perm = await ImagePicker.requestMediaLibraryPermissionsAsync();
      if (!perm.granted) return;
      const res = await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 1 });
      if (res.canceled || !res.assets?.[0]) return;
      const stored = await copyImageToStorage(res.assets[0].uri, itemUuid);
      if (stored) {
        setCustomPath(stored);
        if (!images.includes(stored)) setImages((prev) => [stored, ...prev]);
      }
    } catch (e) {
      console.warn('pickCustomImage failed', e);
    }
  };

  const toggleRecord = async () => {
    setAudioError('');
    if (isRecording) {
      try {
        await recorder.stop();
        const uri = recorder.uri;
        if (uri) {
          const stored = await copyAudioToStorage(uri, itemUuid);
          if (stored) {
            setAudioPath(stored);
            setAudioName('Recording');
          }
        }
      } catch (e) {
        console.warn('stopRecording failed', e);
        setAudioError(`Stop recording failed: ${String(e)}`);
      }
      await setAudioModeAsync({ playsInSilentMode: true, allowsRecording: false }).catch(() => undefined);
      setIsRecording(false);
      return;
    }
    try {
      let granted = (await getRecordingPermissionsAsync()).granted;
      if (!granted) granted = (await requestRecordingPermissionsAsync()).granted;
      if (!granted) {
        setAudioError('Microphone permission denied.');
        return;
      }
      await setAudioModeAsync({ playsInSilentMode: true, allowsRecording: true });
      await recorder.prepareToRecordAsync();
      recorder.record();
      setIsRecording(true);
    } catch (e) {
      console.warn('startRecording failed', e);
      setAudioError(`Recording failed: ${String(e)}`);
    }
  };

  const importAudio = async () => {
    setAudioError('');
    try {
      const res = await File.pickFileAsync({ mimeTypes: 'audio/*' });
      if (res.canceled || !res.result) return;
      const stored = await copyAudioToStorage(res.result, itemUuid);
      if (stored) setAudioPath(stored);
    } catch (e) {
      console.warn('importAudio failed', e);
      setAudioError(`Import failed: ${String(e)}`);
    }
  };

  const previewAudio = () => {
    if (!audioPath) return;
    setAudioError('');
    try {
      player.replace(audioPath);
      void player.seekTo(0);
      player.play();
    } catch (e) {
      console.warn('previewAudio failed', e);
      setAudioError(`Preview failed: ${String(e)}`);
    }
  };

  const save = () => {
    editItem(menu.id, idx, {
      name: name.trim(),
      isSymbol: originalIsSymbol,
      ttsMode: originalIsSymbol ? ttsType : null,
      pointer: originalIsSymbol ? null : existingPointer,
      color,
      pron: useCustomAudio ? '' : pron.trim(),
      locale,
      translations,
      ttsLocale,
      customImagePath: customPath,
      customAudioPath: useCustomAudio ? audioPath : '',
      customAudioName: useCustomAudio ? audioName.trim() : '',
    });
    onClose();
  };

  const remove = () => {
    deleteItem(menu.id, idx);
    onClose();
  };

  const previewColor = resolveItemColor(color, fitzgeraldKey, overrides);
  const previewUrl = resolveImageUrl(customPath || (menu.image_urls[idx] ?? ''), skinTone);

  // SVG verification for the dialog's top main item preview window
  const isPreviewSvg = typeof previewUrl === 'string' && (previewUrl.toLowerCase().includes('.svg') || previewUrl.toLowerCase().includes('data:image/svg'));

  return (
    <View style={{ backgroundColor: t.surface, borderRadius: 12, padding: 16, height: '90%', width: '95%' }}>
      <Text style={{ fontSize: 18, fontWeight: 'bold', marginBottom: 8 }}>
        Edit {originalIsSymbol ? 'symbol' : 'folder'}
      </Text>
      <ScrollView keyboardShouldPersistTaps="handled">
        {/* preview */}
        <View
          style={{
            alignSelf: 'center',
            width: 120,
            height: 120,
            backgroundColor: highContrast ? '#000000' : '#FFFFFF',
            borderWidth: 4,
            borderColor: highContrast ? '#FFFFFF' : '#000000',
            borderRadius: 16,
            marginBottom: 12,
            overflow: 'hidden',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          {previewUrl ? (
            <Image
              source={previewUrl}
              style={[
                { flex: 1, width: '100%', height: '100%', padding: 8 },
                highContrast && !isPreviewSvg && HC_IMAGE_FILTER
              ]}
              tintColor={highContrast && isPreviewSvg ? '#FFFFFF' : undefined}
              contentFit="contain"
            />
          ) : (
            <View style={{ flex: 1 }} />
          )}
          <Text
            style={{
              position: 'absolute',
              left: 4,
              right: 4,
              [textBottom ? 'bottom' : 'top']: 6,
              textAlign: 'center',
              color: highContrast ? '#FFFFFF' : '#000000',
              fontSize: 12,
            }}
          >
            {capitalize(name)}
          </Text>
        </View>

        <Text style={{ fontSize: 14 }}>{S.dialogs.editItem.name}</Text>
        <TextInput
          value={name}
          onChangeText={setName}
          style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 8, marginTop: 4 }}
        />

        {languageImageOverride && (
          <View style={{ marginTop: 12 }}>
            <Text style={{ fontSize: 14, fontWeight: '500', marginBottom: 4 }}>{S.dialogs.editItem.itemLanguage}</Text>
            <LanguageDropdown selectedCode={locale || appLocale} onSelected={setLocale} label={S.dialogs.editItem.language} />
          </View>
        )}

        {multilingual && (
          <View style={{ marginTop: 12 }}>
            <Text style={{ fontSize: 14, fontWeight: '500' }}>{S.dialogs.editItem.labelsByLanguage}</Text>
            {Object.entries(translations).map(([code, value]) => (
              <View key={code} style={{ flexDirection: 'row', alignItems: 'center', marginTop: 4 }}>
                <Text style={{ width: 60, fontSize: 13 }}>{code}:</Text>
                <TextInput
                  value={value}
                  onChangeText={(t) => setTranslations({ ...translations, [code]: t })}
                  style={{ flex: 1, borderWidth: 1, borderColor: t.panelBorder, borderRadius: 6, padding: 6 }}
                />
                <Pressable
                  onPress={() => {
                    const next = { ...translations };
                    delete next[code];
                    setTranslations(next);
                    if (ttsLocale === code) setTtsLocale('');
                  }}
                  style={{ padding: 8 }}
                >
                  <Text>✕</Text>
                </Pressable>
              </View>
            ))}
            {addingLanguage ? (
              <LanguageDropdown
                selectedCode=""
                label={S.dialogs.editItem.add}
                onSelected={(code) => {
                  if (!(code in translations)) setTranslations({ ...translations, [code]: '' });
                  setAddingLanguage(false);
                }}
              />
            ) : (
              <Pressable onPress={() => setAddingLanguage(true)} style={{ marginTop: 8 }}>
                <Text style={{ color: t.primary }}>{S.dialogs.editItem.addLanguage}</Text>
              </Pressable>
            )}
          </View>
        )}

        {/* image picker */}
        <Text style={{ fontSize: 14, fontWeight: '500', marginTop: 12 }}>{S.dialogs.editItem.image}</Text>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 8 }}>
          {images.map((url, i) => {
            const selected = url === customPath;
            const resolvedGridUrl = resolveImageUrl(url, skinTone);
            // SVG verification for search result grids
            const isGridSvg = typeof resolvedGridUrl === 'string' && (resolvedGridUrl.toLowerCase().includes('.svg') || resolvedGridUrl.toLowerCase().includes('data:image/svg'));

            return (
              <Pressable
                key={`${i}-${url}`}
                onPress={() => setCustomPath(url)}
                style={{
                  width: 90,
                  height: 90,
                  backgroundColor: highContrast ? '#000000' : '#FFFFFF',
                  borderWidth: selected ? 4 : 2,
                  borderColor: highContrast ? '#FFFFFF' : selected ? '#1976D2' : '#000000',
                  borderRadius: 12,
                  overflow: 'hidden',
                  justifyContent: 'center',
                  alignItems: 'center',
                }}
              >
                <Image 
                  source={resolvedGridUrl} 
                  style={[
                    { flex: 1, width: '100%', height: '100%', padding: 4 },
                    highContrast && !isGridSvg && HC_IMAGE_FILTER
                  ]} 
                  tintColor={highContrast && isGridSvg ? '#FFFFFF' : undefined}
                  contentFit="contain" 
                />
              </Pressable>
            );
          })}
        </View>
        <Pressable
          onPress={loadImages}
          disabled={loading}
          style={{ marginTop: 8, backgroundColor: t.surfaceAlt, borderRadius: 6, padding: 10, alignItems: 'center' }}
        >
          <Text>{loading ? S.dialogs.editItem.loading : images.length > 0 ? S.dialogs.editItem.loadMoreImages : S.dialogs.editItem.findImages}</Text>
        </Pressable>
        <Pressable
          onPress={pickCustomImage}
          style={{ marginTop: 8, backgroundColor: t.primary, borderRadius: 6, padding: 10, alignItems: 'center' }}
        >
          <Text style={{ color: '#FFFFFF' }}>{S.dialogs.editItem.chooseCustomImage}</Text>
        </Pressable>
        {customPath.trim() !== '' && (
          <Pressable
            onPress={() => setCustomPath('')}
            style={{ marginTop: 8, backgroundColor: t.neutral, borderRadius: 6, padding: 10, alignItems: 'center' }}
          >
            <Text style={{ color: '#FFFFFF' }}>{S.dialogs.editItem.resetToDefault}</Text>
          </Pressable>
        )}

        {/* audio components */}
        <Text style={{ fontSize: 14, fontWeight: '500', marginTop: 12 }}>{S.dialogs.editItem.audio}</Text>
        <View style={{ flexDirection: 'row', marginTop: 4 }}>
          <Pressable onPress={() => setUseCustomAudio(false)} style={{ padding: 8 }}>
            <Text style={{ fontSize: 13 }}>{!useCustomAudio ? `● ${S.dialogs.editItem.useItemName}` : `○ ${S.dialogs.editItem.useItemName}`}</Text>
          </Pressable>
          <Pressable onPress={() => setUseCustomAudio(true)} style={{ padding: 8 }}>
            <Text style={{ fontSize: 13 }}>{useCustomAudio ? `● ${S.dialogs.editItem.useCustomAudio}` : `○ ${S.dialogs.editItem.useCustomAudio}`}</Text>
          </Pressable>
        </View>
        {audioError ? (
          <Text style={{ fontSize: 12, color: t.danger, marginTop: 4 }}>{audioError}</Text>
        ) : null}

        {useCustomAudio ? (
          <View style={{ marginTop: 4 }}>
            {audioPath.trim() !== '' && (
              <View
                style={{
                  flexDirection: 'row',
                  alignItems: 'center',
                  alignSelf: 'flex-start',
                  marginVertical: 4,
                  borderWidth: 2,
                  borderColor: t.border,
                  borderRadius: 50,
                  backgroundColor: t.surfaceAlt,
                  paddingHorizontal: 12,
                  paddingVertical: 6,
                }}
              >
                <Text style={{ fontSize: 13, marginRight: 8 }}>{audioName.trim() || S.dialogs.editItem.audioClip}</Text>
                <Pressable onPress={() => { setTempName(audioName); setShowRename(true); }} style={{ padding: 2 }}>
                  <Text style={{ fontSize: 14 }}>✎</Text>
                </Pressable>
              </View>
            )}
            <View style={{ flexDirection: 'row', gap: 8 }}>
              <Pressable
                onPress={toggleRecord}
                style={{ backgroundColor: isRecording ? t.danger : t.primary, borderRadius: 6, paddingHorizontal: 14, paddingVertical: 10 }}
              >
                <Text style={{ color: '#FFFFFF' }}>{isRecording ? S.dialogs.editItem.stop : S.dialogs.editItem.record}</Text>
              </Pressable>
              <Pressable
                onPress={importAudio}
                style={{ backgroundColor: t.primary, borderRadius: 6, paddingHorizontal: 14, paddingVertical: 10 }}
              >
                <Text style={{ color: '#FFFFFF' }}>{S.dialogs.editItem.importFromDevice}</Text>
              </Pressable>
            </View>
            {audioPath.trim() !== '' && (
              <View style={{ flexDirection: 'row', gap: 8, marginTop: 8 }}>
                <Pressable
                  onPress={previewAudio}
                  style={{ backgroundColor: t.primary, borderRadius: 6, paddingHorizontal: 14, paddingVertical: 10 }}
                >
                  <Text style={{ color: '#FFFFFF' }}>{S.dialogs.editItem.preview}</Text>
                </Pressable>
                <Pressable
                  onPress={() => setAudioPath('')}
                  style={{ backgroundColor: t.neutral, borderRadius: 6, paddingHorizontal: 14, paddingVertical: 10 }}
                >
                  <Text style={{ color: '#FFFFFF' }}>{S.dialogs.editItem.clearAudio}</Text>
                </Pressable>
              </View>
            )}
            <Modal visible={showRename} transparent animationType="fade" onRequestClose={() => setShowRename(false)}>
              <View style={{ flex: 1, backgroundColor: t.scrim, justifyContent: 'center', padding: 24 }}>
                <View style={{ backgroundColor: t.surface, borderRadius: 12, padding: 20 }}>
                  <Text style={{ fontSize: 18, fontWeight: 'bold' }}>{S.dialogs.editItem.renameAudioClip}</Text>
                  <TextInput
                    value={tempName}
                    onChangeText={setTempName}
                    placeholder={S.dialogs.editItem.clipNamePlaceholder}
                    style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 8, marginTop: 12 }}
                  />
                  <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginTop: 16 }}>
                    <Pressable onPress={() => setShowRename(false)} style={{ padding: 10, marginRight: 8 }}>
                      <Text style={{ fontSize: 16 }}>{S.common.cancel}</Text>
                    </Pressable>
                    <Pressable
                      onPress={() => { setAudioName(tempName.trim()); setShowRename(false); }}
                      style={{ padding: 10 }}
                    >
                      <Text style={{ fontSize: 16, fontWeight: 'bold', color: t.primary }}>{S.common.save}</Text>
                    </Pressable>
                  </View>
                </View>
              </View>
            </Modal>
          </View>
        ) : (
          <View style={{ marginTop: 4 }}>
            <Text style={{ fontSize: 12, color: t.subtext }}>{S.dialogs.editItem.pronHint}</Text>
            <TextInput
              value={pron}
              onChangeText={setPron}
              placeholder={S.dialogs.editItem.pronPlaceholder}
              style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 8, marginTop: 4 }}
            />
          </View>
        )}

        {/* color picker options */}
        <Text style={{ fontSize: 14, fontWeight: '500', marginTop: 12 }}>{S.dialogs.editItem.color}</Text>
        <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 4 }}>
          <View style={{ width: 36, height: 36, backgroundColor: previewColor, borderWidth: 2, borderColor: t.border, borderRadius: 4 }} />
          <Text style={{ marginLeft: 8 }}>{color.startsWith('#') ? S.dialogs.editItem.custom : color || S.dialogs.editItem.default}</Text>
        </View>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 8 }}>
          <Pressable
            onPress={() => setColor('')}
            style={{
              width: 36,
              height: 36,
              backgroundColor: '#FFFFFF',
              borderWidth: color === '' ? 4 : 2,
              borderColor: color === '' ? '#1976D2' : '#000000',
              borderRadius: 4,
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Text style={{ fontSize: 10 }}>—</Text>
          </Pressable>
          {fitzgeraldKey.map((cat) => {
            const selected = color === cat.name;
            return (
              <Pressable
                key={cat.name}
                onPress={() => setColor(cat.name)}
                style={{
                  width: 36,
                  height: 36,
                  backgroundColor: effectiveCategoryColor(cat, overrides),
                  borderWidth: selected ? 4 : 2,
                  borderColor: selected ? '#1976D2' : '#000000',
                  borderRadius: 4,
                }}
              />
            );
          })}
        </View>

        <Pressable
          onPress={() => setShowColorPicker(true)}
          style={{ marginTop: 8, backgroundColor: t.primary, borderRadius: 6, padding: 10, alignItems: 'center' }}
        >
          <Text style={{ color: '#FFFFFF' }}>{color.startsWith('#') ? S.dialogs.editItem.editCustomColor : S.dialogs.editItem.pickCustomColor}</Text>
        </Pressable>
        {showColorPicker && (
          <ColorPickerDialog
            initialColor={previewColor}
            onDismiss={() => setShowColorPicker(false)}
            onConfirm={(hex) => {
              setColor(hex);
              setShowColorPicker(false);
            }}
          />
        )}

        {originalIsSymbol && (
          <View style={{ marginTop: 12 }}>
            <Text style={{ fontSize: 14, fontWeight: '500' }}>{S.dialogs.editItem.ttsBehavior}</Text>
            <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
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
          </View>
        )}
      </ScrollView>

      <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 12 }}>
        <Pressable onPress={remove} style={{ backgroundColor: t.danger, borderRadius: 6, paddingHorizontal: 14, paddingVertical: 10 }}>
          <Text style={{ color: '#FFFFFF' }}>{S.dialogs.editItem.delete}</Text>
        </Pressable>
        <View style={{ flexDirection: 'row' }}>
          <Pressable onPress={onClose} style={{ padding: 10, marginRight: 8 }}>
            <Text style={{ fontSize: 16 }}>{S.common.cancel}</Text>
          </Pressable>
          <Pressable onPress={save} style={{ padding: 10 }}>
            <Text style={{ fontSize: 16, fontWeight: 'bold' }}>{S.common.done}</Text>
          </Pressable>
        </View>
      </View>
    </View>
  );
}

export function EditItemDialog() {
  const show = useStore((s) => s.showEditItemDialog);
  const menuId = useStore((s) => s.editTargetMenuId);
  const idx = useStore((s) => s.editTargetIndex);
  const menuList = useStore((s) => s.menu_list);
  const closeDialogs = useStore((s) => s.closeDialogs);
  const t = useTheme();

  const menu = findMenu(menuList, menuId);
  const valid = show && idx >= 0 && idx < menu.item_list.length;

  return (
    <Modal visible={show} transparent animationType="fade" onRequestClose={closeDialogs}>
      <View style={{ flex: 1, backgroundColor: t.scrim, justifyContent: 'center', alignItems: 'center' }}>
        {valid && <EditItemContent key={`${menuId}-${idx}`} menu={menu} idx={idx} onClose={closeDialogs} />}
      </View>
    </Modal>
  );
}