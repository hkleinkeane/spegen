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

// Settings screen with tabs.
import React, { useEffect, useRef, useState } from 'react';
import { Linking, Modal, Platform, Pressable, ScrollView, Switch, View } from 'react-native';
import Slider from '@react-native-community/slider';
import * as DocumentPicker from 'expo-document-picker';
import * as IntentLauncher from 'expo-intent-launcher';
import { useTheme } from '../theme';
import { Text, TextInput } from './themed';
import { useStore } from '../store';
import { clearPersistedState } from '../persistence';
import { downloadArchiveWeb, importArchiveFile, saveArchiveToDevice, shareArchiveFile } from '../backup';
import { APP_LANGUAGES, BUTTON_SHAPES, SKIN_TONES } from '../types';
import { BOARD_DOTS_RESERVE } from '../boardLayout';
import { findMenu } from '../menus';
import { SECURITY_PRESET_QUESTIONS } from '../security';
import { fmt } from '../strings';
import { useStrings } from '../i18n';
import { getVoices, type TtsVoice } from '../tts';
import { LanguageDropdown } from './LanguageDropdown';
import { ReorderableTermList } from './ReorderableTermList';
import { Symbol } from './Symbol';
import { UnsavedChangesDialog } from './dialogs/UnsavedChangesDialog';

type TabKey = 'display' | 'voice' | 'vocabulary' | 'language' | 'data' | 'about' | 'feedback';

// Item preview symbol URL.
const WOMAN_PREVIEW_URL =
  'https://d18vdu4p71yql0.cloudfront.net/libraries/arasaac/woman_2.png.varianted-skin.png';

const TAB_KEYS: TabKey[] = ['display', 'voice', 'vocabulary', 'language', 'data', 'about', 'feedback'];

// SliderRow: label, value, reset, slider.
function SliderRow({
  label,
  value,
  min,
  max,
  step,
  onChange,
  format,
  onReset,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  step: number;
  onChange: (v: number) => void;
  format?: (v: number) => string;
  onReset?: () => void;
}) {
  const th = useTheme();
  const [local, setLocal] = useState(value);
  const dragging = useRef(false);
  // Sync to external changes (e.g. Reset / restore defaults) only when not actively dragging.
  useEffect(() => {
    if (!dragging.current) setLocal(value);
  }, [value]);
  return (
    <View style={{ marginTop: 16 }}>
      <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }}>
        <Text style={{ fontSize: 15, fontWeight: '500' }}>{label}</Text>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Text style={{ fontSize: 15 }}>{format ? format(local) : String(local)}</Text>
          {onReset && (
            <Pressable onPress={onReset} style={{ marginLeft: 12, padding: 4 }}>
              <Text style={{ color: th.primary }}>Reset</Text>
            </Pressable>
          )}
        </View>
      </View>
      <Slider
        style={{ width: '100%', height: 40 }}
        minimumValue={min}
        maximumValue={max}
        step={step}
        value={local}
        onValueChange={(v) => {
          dragging.current = true;
          setLocal(v);
        }}
        onSlidingComplete={(v) => {
          dragging.current = false;
          setLocal(v);
          onChange(v);
        }}
        minimumTrackTintColor={th.primary}
        maximumTrackTintColor={th.surfaceAlt}
        thumbTintColor={th.primary}
      />
    </View>
  );
}

function Chip({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  const th = useTheme();
  return (
    <Pressable
      onPress={onPress}
      style={{
        paddingHorizontal: 14,
        paddingVertical: 8,
        borderRadius: 20,
        backgroundColor: active ? th.primary : th.surfaceAlt,
        marginRight: 8,
        marginTop: 8,
      }}
    >
      <Text style={{ color: active ? th.onPrimary : th.text }}>{label}</Text>
    </Pressable>
  );
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <Text style={{ fontSize: 16, fontWeight: 'bold', marginTop: 20, marginBottom: 2 }}>{children}</Text>;
}

function ToggleRow({
  label,
  value,
  onValueChange,
  hint,
}: {
  label: string;
  value: boolean;
  onValueChange: (v: boolean) => void;
  hint?: string;
}) {
  const th = useTheme();
  return (
    <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 14 }}>
      <View style={{ flex: 1, paddingRight: 12 }}>
        <Text style={{ fontSize: 15 }}>{label}</Text>
        {hint ? <Text style={{ fontSize: 12, color: th.subtext }}>{hint}</Text> : null}
      </View>
      <Switch value={value} onValueChange={onValueChange} />
    </View>
  );
}

export function SettingsScreen() {
  const st = useStore();
  const th = useTheme();
  const S = useStrings();
  const [tab, setTab] = useState<TabKey>('display');
  const [status, setStatus] = useState('');
  const [showUnsaved, setShowUnsaved] = useState(false);
  const [newTerm, setNewTerm] = useState('');
  const [addMenuOpen, setAddMenuOpen] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);
  
  // Security UI state.
  const [pinModalOpen, setPinModalOpen] = useState(false);
  const [pin1, setPin1] = useState('');
  const [pin2, setPin2] = useState('');
  const [pinErr, setPinErr] = useState('');
  const [qOpen, setQOpen] = useState(false);
  const [qText, setQText] = useState('');
  const [qAnswer, setQAnswer] = useState('');
  
  // True while a row in a ReorderableTermList is being dragged, so we can freeze the outer
  // ScrollView (otherwise it steals the vertical pan and the dragged row snaps back).
  const [dragging, setDragging] = useState(false);

  // Feedback form state variables
  const [fbTitle, setFbTitle] = useState('');
  const [fbBody, setFbBody] = useState('');
  const [fbCategory, setFbCategory] = useState<string>(S.settings.feedback.categories[0]);
  const [fbSubmitting, setFbSubmitting] = useState(false);

  // Voice picker. Voices can load asynchronously (notably on web), so we also refresh on open.
  const [voices, setVoices] = useState<TtsVoice[]>([]);
  const [voiceOpen, setVoiceOpen] = useState(false);
  const [gridCols, setGridCols] = useState('');
  const [gridRows, setGridRows] = useState('');
  useEffect(() => {
    void getVoices().then(setVoices);
  }, []);

  if (!st.showSettings) return null;

  const close = () => useStore.setState({ showSettings: false });

  // Done prompts to save when there are in-memory edits, otherwise just closes.
  const onDone = () => {
    if (st.hasUnsavedChanges()) setShowUnsaved(true);
    else close();
  };

  const doSaveToDevice = async () => {
    try {
      await st.save();
      if (Platform.OS === 'web') {
        // Web: hand the browser a .spegen file download instead of the Android folder picker.
        setStatus(S.settings.data.preparingDownload);
        await downloadArchiveWeb();
        setStatus(S.settings.data.backupDownloaded);
        return;
      }
      if (Platform.OS === 'ios') {
        // iOS: open the share sheet so the user can save the .spegen file (e.g. to Files).
        setStatus(S.settings.data.openingShareSheet);
        const shared = await shareArchiveFile();
        setStatus(shared ? S.settings.data.shareSheetHint : S.settings.data.sharingUnavailable);
        return;
      }
      setStatus(S.settings.data.chooseFolder);
      const uri = await saveArchiveToDevice();
      setStatus(uri ? S.settings.data.savedToFolder : S.settings.data.saveCanceled);
    } catch (e) {
      setStatus(fmt(S.settings.data.saveFailed, { e: String(e) }));
    }
  };

  const doImport = async () => {
    try {
      setStatus(S.settings.data.chooseBackupFile);
      const res = await DocumentPicker.getDocumentAsync({
        type: ['application/zip', 'application/octet-stream', '*/*'],
        copyToCacheDirectory: true,
      });

      if (res.canceled || !res.assets || res.assets.length === 0) {
        setStatus(S.settings.data.importCanceled);
        return;
      }

      const asset = res.assets[0];

      if (Platform.OS === 'web') {
        if (!asset.file) {
          setStatus(S.settings.data.importWebFileMissing);
          return;
        }
        await importArchiveFile(asset.file as any);
      } else {
        await importArchiveFile(asset as any);
      }

      await st.init();
      setStatus(S.settings.data.importDone);
    } catch (e) {
      setStatus(fmt(S.settings.data.importFailed, { e: String(e) }));
    }
  };

  const doReset = async () => {
    if (!confirmReset) {
      setConfirmReset(true);
      setStatus(S.settings.data.confirmReset);
      return;
    }
    try {
      await clearPersistedState();
      await st.init();
      setConfirmReset(false);
      setStatus(S.settings.data.resetDone);
    } catch (e) {
      setStatus(fmt(S.settings.data.resetFailed, { e: String(e) }));
    }
  };

  // "Spoken languages" list management. setLanguageList dedupes, drops the app locale, and
  // clears current_board_language if it no longer points at a listed language.
  const addLang = (code: string) => st.setLanguageList([...st.language_list, code]);
  const removeLang = (code: string) => st.setLanguageList(st.language_list.filter((c) => c !== code));
  const move = (i: number, dir: number) => {
    const l = [...st.language_list];
    const j = i + dir;
    if (j < 0 || j >= l.length) return;
    [l[i], l[j]] = [l[j], l[i]];
    st.setLanguageList(l);
  };

  // Security helpers (in-memory; persisted on Apply / Done-save like other settings).
  const openSetPin = () => {
    setPin1('');
    setPin2('');
    setPinErr('');
    setPinModalOpen(true);
  };
  const saveSetPin = async () => {
    if (pin1.length < 4) {
      setPinErr(S.settings.data.pinTooShort);
      return;
    }
    if (pin1 !== pin2) {
      setPinErr(S.settings.data.pinsDoNotMatch);
      return;
    }
    await st.setSecurityPin(pin1);
    setPinModalOpen(false);
  };
  const addQuestion = async () => {
    if (!qText.trim() || !qAnswer.trim()) return;
    await st.addSecurityQuestion(qText, qAnswer);
    setQText('');
    setQAnswer('');
    setQOpen(false);
  };

  const submitFeedback = async () => {
  if (!fbTitle.trim() || !fbBody.trim()) {
    setStatus(S.settings.feedback.missingFields);
    return;
  }

  setFbSubmitting(true);
  setStatus(S.settings.feedback.submittingStatus);

  try {
    const response = await fetch('https://spegen.vercel.app/api/github-issue', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({
        title: `${fbTitle.trim()} (${fbCategory.toUpperCase()})`,
        body: fbBody.trim(),
      }),
    });

    if (response.ok) {
      setStatus(S.settings.feedback.submitted);
      setFbTitle('');
      setFbBody('');
    } else {
      const errData = await response.json().catch(() => ({}));
      setStatus(fmt(S.settings.feedback.submitFailed, { msg: errData.message || response.statusText }));
    }
  } catch (e) {
    setStatus(fmt(S.settings.feedback.networkError, { e: String(e) }));
  } finally {
    setFbSubmitting(false);
  }
  };

  return (
    <>
    <Modal visible transparent animationType="slide" onRequestClose={onDone}>
      <View style={{ flex: 1, backgroundColor: th.bg, paddingTop: 40 }}>
        {/* header */}
        <View
          style={{
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
            paddingHorizontal: 16,
            paddingBottom: 8,
            borderBottomWidth: 1,
            borderColor: th.divider,
          }}
        >
          <Text style={{ fontSize: 20, fontWeight: 'bold' }}>{S.settings.title}</Text>
          {/* Apply persists now; Done prompts on unsaved edits. */}
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Pressable
              onPress={() => void st.applyChanges()}
              style={{
                paddingHorizontal: 12,
                paddingVertical: 8,
                backgroundColor: th.primary,
                borderRadius: 8,
                marginRight: 8,
              }}
            >
              <Text style={{ fontSize: 15, color: '#FFFFFF', fontWeight: 'bold' }}>{S.settings.apply}</Text>
            </Pressable>
            <Pressable onPress={onDone} style={{ padding: 8 }}>
              <Text style={{ fontSize: 16, color: th.primary, fontWeight: 'bold' }}>{S.settings.done}</Text>
            </Pressable>
          </View>
        </View>

        {/* tab strip */}
        <View style={{ borderBottomWidth: 1, borderColor: th.divider }}>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ padding: 8 }}>
            {TAB_KEYS.map((key) => (
              <Chip key={key} label={S.settings.tabs[key]} active={tab === key} onPress={() => setTab(key)} />
            ))}
          </ScrollView>
        </View>

        <ScrollView
          style={{ flex: 1 }}
          contentContainerStyle={{ padding: 16, paddingBottom: 48 }}
          keyboardShouldPersistTaps="handled"
          scrollEnabled={!dragging}
        >
          {tab === 'display' && (
            <View>
              {/* Live preview: reflects item size,
                  padding, border, shape, label position, skin tone and high contrast as you edit. */}
              <SectionTitle>{S.settings.display.preview}</SectionTitle>
              <View
                style={{
                  alignItems: 'center',
                  marginTop: 8,
                  paddingVertical: 12,
                  backgroundColor: th.surfaceAlt,
                  borderRadius: 8,
                  borderWidth: 1,
                  borderColor: th.panelBorder,
                }}
              >
                <Symbol
                  label="Woman"
                  imageUrl={WOMAN_PREVIEW_URL}
                  bgColor="#FFFFFF"
                  cellWidth={st.box_width_size_dp + st.box_padding_dp * 2}
                  cellHeight={Math.min(st.box_height_size_dp + st.box_padding_dp * 3, 240)}
                  boxWidth={st.box_width_size_dp}
                  boxHeight={st.box_height_size_dp}
                  boxPadding={st.box_padding_dp}
                  borderRadius={BUTTON_SHAPES.find((b) => b.name === st.button_shape_name)?.radius ?? 40}
                  borderWidth={st.item_border_width_dp}
                  highContrast={st.highcontrastmode}
                  textBottom={st.text_location_bottom}
                  skinTone={st.skin_tone}
                  onPress={() => undefined}
                />
              </View>

              {/* Live previews of the static-words row and menu row, mirroring StaticRow /
                  MenuRow on the board so the text-size / padding sliders below show their effect. */}
              <Text style={{ fontSize: 13, color: th.subtext, marginTop: 14 }}>{S.settings.display.staticRowLabel}</Text>
              <View style={{ flexDirection: 'row', height: 64, marginTop: 6, borderRadius: 6, overflow: 'hidden' }}>
                {(st.static_terms.length > 0 ? st.static_terms.slice(0, 4) : [...S.settings.display.previewStaticTerms]).map((term, i) => (
                  <View
                    key={`${i}-${term}`}
                    style={{
                      flex: 1,
                      backgroundColor: th.surface,
                      borderWidth: 2,
                      borderColor: th.border,
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <Text
                      numberOfLines={1}
                      style={{ color: th.text, fontSize: st.static_row_text_size, padding: st.static_row_text_padding }}
                    >
                      {term}
                    </Text>
                  </View>
                ))}
              </View>

              <Text style={{ fontSize: 13, color: th.subtext, marginTop: 12 }}>{S.settings.display.menuRowLabel}</Text>
              <View style={{ flexDirection: 'row', height: 64, marginTop: 6, borderRadius: 6, overflow: 'hidden' }}>
                {(st.menu_row_ids.length > 0
                  ? st.menu_row_ids.slice(0, 3).map((id) => findMenu(st.menu_list, id).title)
                  : [...S.settings.display.previewMenuTitles]
                ).map((title, i) => (
                  <View
                    key={`${i}-${title}`}
                    style={{
                      flex: 1,
                      backgroundColor: th.surface,
                      borderWidth: 2,
                      borderColor: th.border,
                      alignItems: 'center',
                      justifyContent: 'center',
                      overflow: 'hidden',
                    }}
                  >
                    <Text
                      numberOfLines={1}
                      style={{ color: th.text, fontSize: st.menu_row_text_size, padding: st.menu_row_text_padding }}
                    >
                      {title}
                    </Text>
                    {/* folded-corner (folder) indicator, as drawn by MenuRow */}
                    <View
                      style={{
                        position: 'absolute',
                        top: 0,
                        right: 0,
                        width: 0,
                        height: 0,
                        borderTopWidth: 16,
                        borderLeftWidth: 16,
                        borderTopColor: st.highcontrastmode ? '#FFFFFF' : th.text,
                        borderLeftColor: 'transparent',
                      }}
                    />
                  </View>
                ))}
              </View>

              <SectionTitle>{S.settings.display.itemSizing}</SectionTitle>
              <SliderRow
                label={S.settings.display.itemWidthSize}
                value={st.box_width_size_dp}
                min={40}
                max={180}
                step={1}
                format={(v) => `${Math.round(v)} dp`}
                onChange={(v) => st.setSetting('box_width_size_dp', Math.round(v))}
                onReset={() => st.setSetting('box_width_size_dp', 100)}
              />
			  <SliderRow
                label={S.settings.display.itemHeightSize}
                value={st.box_height_size_dp}
                min={40}
                max={180}
                step={1}
                format={(v) => `${Math.round(v)} dp`}
                onChange={(v) => st.setSetting('box_height_size_dp', Math.round(v))}
                onReset={() => st.setSetting('box_height_size_dp', 100)}
              />
              <Text style={{ fontSize: 13, fontWeight: '500', marginTop: 16 }}>{S.settings.display.gridSize}</Text>
              <Text style={{ fontSize: 12, color: th.subtext }}>{S.settings.display.gridHint}</Text>
              <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 8 }}>
                <TextInput
                  value={gridCols}
                  onChangeText={(v) => setGridCols(v.replace(/[^0-9]/g, ''))}
                  keyboardType="number-pad"
                  placeholder={S.settings.display.gridCols}
                  style={{ width: 64, borderWidth: 1, borderColor: th.inputBorder, borderRadius: 6, padding: 8, textAlign: 'center' }}
                />
                <Text style={{ marginHorizontal: 10, fontSize: 16 }}>×</Text>
                <TextInput
                  value={gridRows}
                  onChangeText={(v) => setGridRows(v.replace(/[^0-9]/g, ''))}
                  keyboardType="number-pad"
                  placeholder={S.settings.display.gridRows}
                  style={{ width: 64, borderWidth: 1, borderColor: th.inputBorder, borderRadius: 6, padding: 8, textAlign: 'center' }}
                />
                <Pressable
                  onPress={() => {
                    const cols = Math.max(parseInt(gridCols, 10) || 0, 1);
                    const rows = Math.max(parseInt(gridRows, 10) || 0, 1);
                    const pad = st.box_padding_dp;
                    const avail = Math.max(st.menuHeight - BOARD_DOTS_RESERVE, 1);
                    const scaleWidth = Math.max(1, Math.floor(st.menuWidth / cols - pad * 2));
                    const scaleHeight = Math.max(1, Math.floor(avail / rows - pad * 3));
                    st.setSetting('box_width_size_dp', scaleWidth);
                    st.setSetting('box_height_size_dp', scaleHeight);
                  }}
                  style={{ marginLeft: 10, backgroundColor: th.primary, borderRadius: 6, paddingHorizontal: 16, paddingVertical: 10 }}
                >
                  <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.settings.display.gridApply}</Text>
                </Pressable>
              </View>

              <SectionTitle>{S.settings.display.bordersSpacing}</SectionTitle>
              <SliderRow
                label={S.settings.display.borderWidth}
                value={st.item_border_width_dp}
                min={0}
                max={12}
                step={1}
                format={(v) => `${Math.round(v)} dp`}
                onChange={(v) => st.setSetting('item_border_width_dp', Math.round(v))}
              />
              <SliderRow
                label={S.settings.display.itemPadding}
                value={st.box_padding_dp}
                min={4}
                max={40}
                step={1}
                format={(v) => `${Math.round(v)} dp`}
                onChange={(v) => st.setSetting('box_padding_dp', Math.round(v))}
              />

              <SectionTitle>{S.settings.display.staticRowStyle}</SectionTitle>
              <SliderRow
                label={S.settings.display.textSize}
                value={st.static_row_text_size}
                min={8}
                max={32}
                step={1}
                format={(v) => `${Math.round(v)} sp`}
                onChange={(v) => st.setSetting('static_row_text_size', Math.round(v))}
              />
              <SliderRow
                label={S.settings.display.textPadding}
                value={st.static_row_text_padding}
                min={0}
                max={24}
                step={1}
                format={(v) => `${Math.round(v)} dp`}
                onChange={(v) => st.setSetting('static_row_text_padding', Math.round(v))}
              />

              <SectionTitle>{S.settings.display.menuRowStyle}</SectionTitle>
              <SliderRow
                label={S.settings.display.textSize}
                value={st.menu_row_text_size}
                min={8}
                max={32}
                step={1}
                format={(v) => `${Math.round(v)} sp`}
                onChange={(v) => st.setSetting('menu_row_text_size', Math.round(v))}
              />
              <SliderRow
                label={S.settings.display.textPadding}
                value={st.menu_row_text_padding}
                min={0}
                max={24}
                step={1}
                format={(v) => `${Math.round(v)} dp`}
                onChange={(v) => st.setSetting('menu_row_text_padding', Math.round(v))}
              />

              <SectionTitle>{S.settings.display.buttonShape}</SectionTitle>
              <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
                {BUTTON_SHAPES.map((b) => (
                  <Chip
                    key={b.name}
                    label={b.name}
                    active={st.button_shape_name === b.name}
                    onPress={() => st.setSetting('button_shape_name', b.name)}
                  />
                ))}
              </View>

              <SectionTitle>{S.settings.display.labelPosition}</SectionTitle>
              <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
                <Chip label={S.settings.display.top} active={!st.text_location_bottom} onPress={() => st.setSetting('text_location_bottom', false)} />
                <Chip label={S.settings.display.bottom} active={st.text_location_bottom} onPress={() => st.setSetting('text_location_bottom', true)} />
              </View>

              <SectionTitle>{S.settings.display.skinTone}</SectionTitle>
              <View style={{ flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center' }}>
                <Pressable
                  onPress={() => st.setSetting('skin_tone', '')}
                  style={{
                    width: 40,
                    height: 40,
                    borderRadius: 20,
                    backgroundColor: th.surface,
                    borderWidth: st.skin_tone === '' ? 4 : 2,
                    borderColor: st.skin_tone === '' ? th.primary : '#000000',
                    marginRight: 8,
                    marginTop: 8,
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <Text style={{ fontSize: 10 }}>—</Text>
                </Pressable>
                {SKIN_TONES.map((t) => (
                  <Pressable
                    key={t.hexCode}
                    onPress={() => st.setSetting('skin_tone', t.hexCode)}
                    style={{
                      width: 40,
                      height: 40,
                      borderRadius: 20,
                      backgroundColor: t.color,
                      borderWidth: st.skin_tone === t.hexCode ? 4 : 2,
                      borderColor: st.skin_tone === t.hexCode ? th.primary : '#000000',
                      marginRight: 8,
                      marginTop: 8,
                    }}
                  />
                ))}
              </View>

              <SectionTitle>{S.settings.display.theme}</SectionTitle>
              <View style={{ flexDirection: 'row', flexWrap: 'wrap' }}>
                <Chip label={S.settings.display.themeLight} active={st.theme_mode === 'light'} onPress={() => st.setSetting('theme_mode', 'light')} />
                <Chip label={S.settings.display.themeDark} active={st.theme_mode === 'dark'} onPress={() => st.setSetting('theme_mode', 'dark')} />
                <Chip label={S.settings.display.themeSystem} active={st.theme_mode === 'system'} onPress={() => st.setSetting('theme_mode', 'system')} />
              </View>
              <Text style={{ fontSize: 12, color: th.subtext, marginTop: 4 }}>{S.settings.display.themeNote}</Text>

              <ToggleRow
                label={S.settings.display.highContrast}
                hint={S.settings.display.highContrastHint}
                value={st.highcontrastmode}
                onValueChange={(v) => {
                  st.setSetting('highcontrastmode', v);
                  if (v) st.setSetting('skin_tone', '');
                }}
              />
            </View>
          )}

          {tab === 'voice' && (
            <View>
              <SliderRow
                label={S.settings.voice.speechRate}
                value={st.tts_speech_rate}
                min={0.25}
                max={2.0}
                step={0.05}
                format={(v) => `${v.toFixed(2)}×`}
                onChange={(v) => st.setSetting('tts_speech_rate', Math.round(v * 100) / 100)}
                onReset={() => st.setSetting('tts_speech_rate', 1.0)}
              />
              <SliderRow
                label={S.settings.voice.pitch}
                value={st.tts_pitch}
                min={0.5}
                max={2.0}
                step={0.05}
                format={(v) => v.toFixed(2)}
                onChange={(v) => st.setSetting('tts_pitch', Math.round(v * 100) / 100)}
                onReset={() => st.setSetting('tts_pitch', 1.0)}
              />
              <ToggleRow
                label={S.settings.voice.pauseBetweenWords}
                value={st.tts_pause_between_words}
                onValueChange={(v) => st.setSetting('tts_pause_between_words', v)}
              />
              <SliderRow
                label={S.settings.voice.pauseDuration}
                value={st.tts_pause_duration}
                min={100}
                max={2000}
                step={50}
                format={(v) => `${Math.round(v)} ms`}
                onChange={(v) => st.setSetting('tts_pause_duration', Math.round(v))}
              />

              {/* In-app voice picker. The chosen voice is used for speech in the app language;
                  translated other-language speech keeps the engine's default. Works on all platforms
                  (the device-TTS-settings button above is Android-only). */}
              <SectionTitle>{S.settings.voice.voiceLabel}</SectionTitle>
              <Text style={{ fontSize: 12, color: th.subtext }}>{S.settings.voice.voiceHint}</Text>
              <Pressable
                onPress={() => {
                  const next = !voiceOpen;
                  setVoiceOpen(next);
                  if (next) void getVoices().then(setVoices);
                }}
                style={{
                  marginTop: 10,
                  backgroundColor: th.surfaceAlt,
                  borderRadius: 8,
                  paddingHorizontal: 12,
                  paddingVertical: 12,
                  flexDirection: 'row',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <Text style={{ fontSize: 15, flex: 1 }} numberOfLines={1}>
                  {voices.find((v) => v.identifier === st.tts_voice)?.name ?? S.settings.voice.voiceDefault}
                </Text>
                <Text style={{ fontSize: 12, color: th.subtext }}>{voiceOpen ? '▲' : '▼'}</Text>
              </Pressable>
              {voiceOpen && (
                <View
                  style={{
                    borderWidth: 1,
                    borderColor: th.panelBorder,
                    borderTopWidth: 0,
                    borderBottomLeftRadius: 8,
                    borderBottomRightRadius: 8,
                  }}
                >
                  <ScrollView style={{ maxHeight: 240 }} keyboardShouldPersistTaps="handled">
                    <Pressable
                      onPress={() => {
                        st.setVoice('');
                        setVoiceOpen(false);
                      }}
                      style={{ paddingHorizontal: 12, paddingVertical: 12 }}
                    >
                      <Text style={{ fontSize: 15, color: st.tts_voice === '' ? th.primary : th.text }}>
                        {S.settings.voice.voiceDefault}
                      </Text>
                    </Pressable>
                    {(() => {
                      const base = (st.app_locale || 'en').toLowerCase().split('-')[0];
                      const list = voices.filter(
                        (v) => (v.language || '').toLowerCase().split('-')[0] === base
                      );
                      if (list.length === 0) {
                        return (
                          <Text style={{ fontSize: 13, color: th.subtext, padding: 12 }}>
                            {S.settings.voice.noVoices}
                          </Text>
                        );
                      }
                      return list.map((v) => (
                        <Pressable
                          key={v.identifier}
                          onPress={() => {
                            st.setVoice(v.identifier);
                            setVoiceOpen(false);
                          }}
                          style={{
                            paddingHorizontal: 12,
                            paddingVertical: 12,
                            borderTopWidth: 1,
                            borderTopColor: th.divider,
                          }}
                        >
                          <Text style={{ fontSize: 15, color: st.tts_voice === v.identifier ? th.primary : th.text }}>
                            {v.name}
                            {v.language ? ` (${v.language})` : ''}
                          </Text>
                        </Pressable>
                      ));
                    })()}
                  </ScrollView>
                </View>
              )}

              <ToggleRow
                label={S.settings.voice.translateSentence}
                hint={S.settings.voice.translateSentenceHint}
                value={st.auto_translate_sentence}
                onValueChange={(v) => st.setSetting('auto_translate_sentence', v)}
              />
              <Pressable
                onPress={() => st.testVoice(S.settings.voice.testPhrase)}
                style={{ marginTop: 20, backgroundColor: th.primary, borderRadius: 8, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.settings.voice.testVoice}</Text>
              </Pressable>
              {Platform.OS === 'android' && (
                <Pressable
                  onPress={async () => {
                    try {
                      await IntentLauncher.startActivityAsync(IntentLauncher.ActivityAction.TTS_SETTINGS);
                    } catch (e) {
                      setStatus(fmt(S.settings.voice.ttsOpenFailed, { e: String(e) }));
                    }
                  }}
                  style={{ marginTop: 10, backgroundColor: th.surfaceAlt, borderRadius: 8, padding: 12, alignItems: 'center' }}
                >
                  <Text style={{ fontWeight: 'bold' }}>{S.settings.voice.openDeviceTts}</Text>
                </Pressable>
              )}
              <Pressable
                onPress={() => {
                  st.setSetting('tts_speech_rate', 1.0);
                  st.setSetting('tts_pitch', 1.0);
                  st.setSetting('tts_pause_between_words', false);
                  st.setSetting('tts_pause_duration', 500);
                  st.setVoice('');
                  setStatus(S.settings.voice.resetDone);
                }}
                style={{ marginTop: 10, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ color: th.primary }}>{S.settings.voice.resetToDefaults}</Text>
              </Pressable>
              {!!status && <Text style={{ marginTop: 8, color: th.subtext }}>{status}</Text>}
            </View>
          )}

          {tab === 'vocabulary' && (
            <View>
              <SectionTitle>{S.settings.vocabulary.editorMode}</SectionTitle>
              <Text style={{ fontSize: 12, color: th.subtext }}>{S.settings.vocabulary.editorModeHint}</Text>
              <Pressable
                onPress={() => useStore.setState({ editorMode: true, showSettings: false })}
                style={{ marginTop: 10, backgroundColor: th.primary, borderRadius: 8, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.settings.vocabulary.enableEditorMode}</Text>
              </Pressable>

              <SectionTitle>{S.settings.vocabulary.staticRow}</SectionTitle>
              <Text style={{ fontSize: 12, color: th.subtext }}>{S.settings.vocabulary.staticRowHint}</Text>
              <View style={{ marginTop: 8 }}>
                <ReorderableTermList
                  items={st.static_terms}
                  onReorder={(next) => st.setStaticTerms(next)}
                  onDragActiveChange={setDragging}
                  renderContent={(i, term) => (
                    <TextInput
                      value={term}
                      onChangeText={(v) => {
                        const next = [...st.static_terms];
                        next[i] = v;
                        st.setStaticTerms(next);
                      }}
                      style={{ flex: 1, borderWidth: 1, borderColor: th.panelBorder, borderRadius: 6, padding: 8 }}
                    />
                  )}
                  renderTrailing={(i) => (
                    <Pressable
                      onPress={() => st.setStaticTerms(st.static_terms.filter((_, j) => j !== i))}
                      style={{ paddingHorizontal: 12, paddingVertical: 10 }}
                    >
                      <Text style={{ fontSize: 16 }}>✕</Text>
                    </Pressable>
                  )}
                />
              </View>
              <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 12 }}>
                <TextInput
                  value={newTerm}
                  onChangeText={setNewTerm}
                  placeholder={S.settings.vocabulary.addPhrasePlaceholder}
                  style={{ flex: 1, borderWidth: 1, borderColor: th.inputBorder, borderRadius: 6, padding: 8 }}
                />
                <Pressable
                  onPress={() => {
                    if (newTerm.trim()) {
                      st.setStaticTerms([...st.static_terms, newTerm.trim()]);
                      setNewTerm('');
                    }
                  }}
                  style={{ marginLeft: 8, backgroundColor: th.primary, borderRadius: 6, paddingHorizontal: 16, paddingVertical: 10 }}
                >
                  <Text style={{ color: '#FFFFFF' }}>{S.settings.vocabulary.addPhrase}</Text>
                </Pressable>
              </View>

              <SectionTitle>{S.settings.vocabulary.menuRow}</SectionTitle>
              <Text style={{ fontSize: 12, color: th.subtext }}>{S.settings.vocabulary.menuRowHint}</Text>
              <View style={{ marginTop: 8 }}>
                <ReorderableTermList
                  items={st.menu_row_ids}
                  onReorder={(next) => st.setMenuRowIds(next)}
                  onDragActiveChange={setDragging}
                  renderContent={(_i, id) => (
                    <Text style={{ flex: 1, fontSize: 16 }}>{findMenu(st.menu_list, id).title}</Text>
                  )}
                  renderTrailing={(_i, id) => (
                    <Pressable
                      onPress={() => st.setMenuRowIds(st.menu_row_ids.filter((m) => m !== id))}
                      style={{ paddingHorizontal: 12, paddingVertical: 10 }}
                    >
                      <Text style={{ fontSize: 16 }}>✕</Text>
                    </Pressable>
                  )}
                />
              </View>
              {st.menu_row_ids.length === 0 && (
                <Text style={{ fontSize: 12, color: th.subtext, marginTop: 4 }}>{S.settings.vocabulary.menuRowEmpty}</Text>
              )}
              <Pressable
                onPress={() => setAddMenuOpen((v) => !v)}
                style={{
                  marginTop: 10,
                  backgroundColor: th.surfaceAlt,
                  borderRadius: 8,
                  paddingHorizontal: 12,
                  paddingVertical: 12,
                  flexDirection: 'row',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                }}
              >
                <Text style={{ fontSize: 14, fontWeight: '500' }}>{S.settings.vocabulary.addMenuToRow}</Text>
                <Text style={{ fontSize: 12, color: th.subtext }}>{addMenuOpen ? '▲' : '▼'}</Text>
              </Pressable>
              {addMenuOpen && (
                <View style={{ borderWidth: 1, borderColor: th.panelBorder, borderTopWidth: 0, borderBottomLeftRadius: 8, borderBottomRightRadius: 8 }}>
                  {st.menu_list.filter((m) => !st.menu_row_ids.includes(m.id)).length === 0 ? (
                    <Text style={{ fontSize: 13, color: th.subtext, padding: 12 }}>{S.settings.vocabulary.allMenusInRow}</Text>
                  ) : (
                    st.menu_list
                      .filter((m) => !st.menu_row_ids.includes(m.id))
                      .map((m) => (
                        <Pressable
                          key={m.id}
                          onPress={() => {
                            st.setMenuRowIds([...st.menu_row_ids, m.id]);
                            setAddMenuOpen(false);
                          }}
                          style={{ paddingHorizontal: 12, paddingVertical: 12, borderTopWidth: 1, borderTopColor: '#EEEEEE' }}
                        >
                          <Text style={{ fontSize: 15 }}>{m.title}</Text>
                        </Pressable>
                      ))
                  )}
                </View>
              )}
            </View>
          )}

          {tab === 'language' && (
            <View>
              <SectionTitle>{S.settings.language.appLanguage}</SectionTitle>
              <Text style={{ fontSize: 12, color: th.subtext, marginBottom: 8 }}>{S.settings.language.appLanguageHint}</Text>
              <LanguageDropdown
                selectedCode={st.app_locale}
                onSelected={(code) => {
                  st.setSetting('app_locale', code);
                  st.reloadAllImages();
                }}
              />
              <Text style={{ fontSize: 12, color: th.subtext, marginTop: 8 }}>{S.settings.language.translationDisclaimer}</Text>

              <SectionTitle>{S.settings.language.autoTranslate}</SectionTitle>
              <ToggleRow
                label={S.settings.language.translateUi}
                hint={S.settings.language.translateUiHint}
                value={st.auto_translate_ui}
                onValueChange={(v) => st.setSetting('auto_translate_ui', v)}
              />
              <ToggleRow
                label={S.settings.language.translateLabels}
                hint={S.settings.language.translateLabelsHint}
                value={st.auto_translate_labels}
                onValueChange={(v) => st.setSetting('auto_translate_labels', v)}
              />

              <SectionTitle>{S.settings.language.multilingual}</SectionTitle>
              <ToggleRow
                label={S.settings.language.perItemImageLanguage}
                hint={S.settings.language.perItemImageLanguageHint}
                value={st.language_image_override}
                onValueChange={(v) => st.setSetting('language_image_override', v)}
              />
              <ToggleRow
                label={S.settings.language.multilingualLabels}
                hint={S.settings.language.multilingualLabelsHint}
                value={st.multilingual_labels}
                onValueChange={(v) => st.setSetting('multilingual_labels', v)}
              />

              <SectionTitle>{S.settings.language.spokenLanguages}</SectionTitle>
              <Text style={{ fontSize: 12, color: th.subtext, marginBottom: 4 }}>{S.settings.language.spokenLanguagesHint}</Text>
              <View
                style={{
                  flexDirection: 'row',
                  alignItems: 'center',
                  paddingVertical: 10,
                  borderBottomWidth: 1,
                  borderColor: th.divider,
                }}
              >
                <Text style={{ flex: 1, fontSize: 15 }}>
                  {APP_LANGUAGES.find((l) => l.code === st.app_locale)?.name ?? st.app_locale}
                </Text>
                <Text style={{ fontSize: 12, color: th.subtext }}>{S.settings.language.base}</Text>
              </View>
              {st.language_list.map((code, i) => (
                <View
                  key={code}
                  style={{
                    flexDirection: 'row',
                    alignItems: 'center',
                    paddingVertical: 8,
                    borderBottomWidth: 1,
                    borderColor: th.divider,
                  }}
                >
                  <Text style={{ flex: 1, fontSize: 15 }}>
                    {APP_LANGUAGES.find((l) => l.code === code)?.name ?? code}
                  </Text>
                  <Pressable
                    onPress={() => move(i, -1)}
                    disabled={i === 0}
                    style={{ padding: 8, opacity: i === 0 ? 0.3 : 1 }}
                  >
                    <Text style={{ fontSize: 16 }}>▲</Text>
                  </Pressable>
                  <Pressable
                    onPress={() => move(i, 1)}
                    disabled={i === st.language_list.length - 1}
                    style={{ padding: 8, opacity: i === st.language_list.length - 1 ? 0.3 : 1 }}
                  >
                    <Text style={{ fontSize: 16 }}>▼</Text>
                  </Pressable>
                  <Pressable onPress={() => removeLang(code)} style={{ padding: 8 }}>
                    <Text style={{ fontSize: 16, color: th.danger }}>✕</Text>
                  </Pressable>
                </View>
              ))}
              <View style={{ marginTop: 10 }}>
                <LanguageDropdown selectedCode="" label={S.settings.language.addLanguage} onSelected={(code) => addLang(code)} />
              </View>
            </View>
          )}

          {tab === 'data' && (
            <View>
              <SectionTitle>{S.settings.data.backupRestore}</SectionTitle>
              <Text style={{ fontSize: 12, color: th.subtext }}>{S.settings.data.backupRestoreHint}</Text>
              <Pressable
                onPress={doSaveToDevice}
                style={{ marginTop: 14, backgroundColor: th.primary, borderRadius: 8, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>
                  {Platform.OS === 'web'
                    ? S.settings.data.downloadBackup
                    : Platform.OS === 'ios'
                      ? S.settings.data.exportBackup
                      : S.settings.data.saveBackupToDevice}
                </Text>
              </Pressable>
              <Pressable
                onPress={doImport}
                style={{ marginTop: 10, backgroundColor: th.surfaceAlt, borderRadius: 8, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ fontWeight: 'bold' }}>{S.settings.data.importBackup}</Text>
              </Pressable>

              <SectionTitle>{S.settings.data.security}</SectionTitle>
              <Text style={{ fontSize: 12, color: th.subtext }}>{S.settings.data.securityHint}</Text>
              {st.security_pin_hash ? (
                <View style={{ flexDirection: 'row', gap: 8, marginTop: 10 }}>
                  <Pressable
                    onPress={openSetPin}
                    style={{ flex: 1, backgroundColor: th.primary, borderRadius: 8, padding: 12, alignItems: 'center' }}
                  >
                    <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.settings.data.changePin}</Text>
                  </Pressable>
                  <Pressable
                    onPress={() => st.removeSecurityPin()}
                    style={{ flex: 1, backgroundColor: th.neutral, borderRadius: 8, padding: 12, alignItems: 'center' }}
                  >
                    <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.settings.data.removePin}</Text>
                  </Pressable>
                </View>
              ) : (
                <Pressable
                  onPress={openSetPin}
                  style={{ marginTop: 10, backgroundColor: th.primary, borderRadius: 8, padding: 12, alignItems: 'center' }}
                >
                  <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.settings.data.setPin}</Text>
                </Pressable>
              )}

              {st.security_pin_hash ? (
                <View>
                  <Text style={{ fontSize: 14, fontWeight: '500', marginTop: 16 }}>{S.settings.data.recoveryQuestions}</Text>
                  <Text style={{ fontSize: 12, color: th.subtext }}>{S.settings.data.recoveryQuestionsHint}</Text>
                  {st.security_questions.map((q, i) => (
                    <View
                      key={i}
                      style={{ flexDirection: 'row', alignItems: 'center', paddingVertical: 8, borderBottomWidth: 1, borderColor: th.divider }}
                    >
                      <Text style={{ flex: 1, fontSize: 14 }}>{q.question}</Text>
                      <Pressable onPress={() => st.removeSecurityQuestion(i)} style={{ padding: 8 }}>
                        <Text style={{ fontSize: 16, color: th.danger }}>✕</Text>
                      </Pressable>
                    </View>
                  ))}
                  {qOpen ? (
                    <View style={{ marginTop: 8, borderWidth: 1, borderColor: th.panelBorder, borderRadius: 8, padding: 10 }}>
                      <Text style={{ fontSize: 13, fontWeight: '500' }}>{S.settings.data.question}</Text>
                      {SECURITY_PRESET_QUESTIONS.map((p) => (
                        <Pressable key={p} onPress={() => setQText(p)} style={{ paddingVertical: 4 }}>
                          <Text style={{ fontSize: 13 }}>{qText === p ? `● ${p}` : `○ ${p}`}</Text>
                        </Pressable>
                      ))}
                      <TextInput
                        value={qText}
                        onChangeText={setQText}
                        placeholder={S.settings.data.ownQuestionPlaceholder}
                        style={{ borderWidth: 1, borderColor: th.inputBorder, borderRadius: 6, padding: 8, marginTop: 6 }}
                      />
                      <Text style={{ fontSize: 13, fontWeight: '500', marginTop: 8 }}>{S.settings.data.answer}</Text>
                      <TextInput
                        value={qAnswer}
                        onChangeText={setQAnswer}
                        placeholder={S.settings.data.answer}
                        autoCapitalize="none"
                        style={{ borderWidth: 1, borderColor: th.inputBorder, borderRadius: 6, padding: 8, marginTop: 4 }}
                      />
                      <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginTop: 10 }}>
                        <Pressable
                          onPress={() => {
                            setQOpen(false);
                            setQText('');
                            setQAnswer('');
                          }}
                          style={{ padding: 10, marginRight: 8 }}
                        >
                          <Text>{S.common.cancel}</Text>
                        </Pressable>
                        <Pressable onPress={addQuestion} style={{ padding: 10 }}>
                          <Text style={{ fontWeight: 'bold', color: th.primary }}>{S.common.add}</Text>
                        </Pressable>
                      </View>
                    </View>
                  ) : (
                    <Pressable
                      onPress={() => {
                        setQText('');
                        setQAnswer('');
                        setQOpen(true);
                      }}
                      style={{ marginTop: 8 }}
                    >
                      <Text style={{ color: th.primary }}>{S.settings.data.addQuestion}</Text>
                    </Pressable>
                  )}
                </View>
              ) : null}

              {/* Set / Change PIN modal */}
              <Modal visible={pinModalOpen} transparent animationType="fade" onRequestClose={() => setPinModalOpen(false)}>
                <View style={{ flex: 1, backgroundColor: th.scrim, justifyContent: 'center', padding: 24 }}>
                  <View style={{ backgroundColor: th.surface, borderRadius: 12, padding: 20 }}>
                    <Text style={{ fontSize: 18, fontWeight: 'bold' }}>
                      {st.security_pin_hash ? S.settings.data.changePin : S.settings.data.setPin}
                    </Text>
                    <TextInput
                      value={pin1}
                      onChangeText={(t) => {
                        setPin1(t);
                        setPinErr('');
                      }}
                      placeholder={S.settings.data.pinPlaceholder}
                      keyboardType="number-pad"
                      secureTextEntry
                      maxLength={12}
                      style={{ borderWidth: 1, borderColor: th.inputBorder, borderRadius: 6, padding: 10, marginTop: 12, fontSize: 18, letterSpacing: 4 }}
                    />
                    <TextInput
                      value={pin2}
                      onChangeText={(t) => {
                        setPin2(t);
                        setPinErr('');
                      }}
                      placeholder={S.settings.data.confirmPinPlaceholder}
                      keyboardType="number-pad"
                      secureTextEntry
                      maxLength={12}
                      onSubmitEditing={saveSetPin}
                      style={{ borderWidth: 1, borderColor: th.inputBorder, borderRadius: 6, padding: 10, marginTop: 10, fontSize: 18, letterSpacing: 4 }}
                    />
                    {!!pinErr && <Text style={{ color: th.danger, marginTop: 8 }}>{pinErr}</Text>}
                    <View style={{ flexDirection: 'row', justifyContent: 'flex-end', marginTop: 16 }}>
                      <Pressable onPress={() => setPinModalOpen(false)} style={{ padding: 10, marginRight: 8 }}>
                        <Text style={{ fontSize: 16 }}>{S.common.cancel}</Text>
                      </Pressable>
                      <Pressable onPress={saveSetPin} style={{ padding: 10 }}>
                        <Text style={{ fontSize: 16, fontWeight: 'bold', color: th.primary }}>{S.common.save}</Text>
                      </Pressable>
                    </View>
                  </View>
                </View>
              </Modal>

              <SectionTitle>{S.settings.data.reset}</SectionTitle>
              <Pressable
                onPress={doReset}
                style={{ marginTop: 8, borderWidth: 1, borderColor: th.danger, borderRadius: 8, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ color: th.danger, fontWeight: 'bold' }}>{S.settings.data.restoreDefaults}</Text>
              </Pressable>

              {!!status && <Text style={{ marginTop: 14, color: th.subtext }}>{status}</Text>}
              <Text style={{ fontSize: 12, color: th.subtext, marginTop: 16 }}>{S.settings.data.backupFormatNote}</Text>
            </View>
          )}

          {tab === 'about' && (
            <View>
              <Text style={{ fontSize: 22, fontWeight: 'bold', marginTop: 8 }}>{S.settings.about.appName}</Text>
              <Text style={{ fontSize: 14, color: th.subtext, marginTop: 8 }}>
                {S.settings.about.intro}
                <Text
                  style={{ color: th.primary, textDecorationLine: 'underline' }}
                  onPress={() => void Linking.openURL(S.settings.about.githubUrl)}
                >
                  {S.settings.about.githubLabel}
                </Text>
                {S.settings.about.websiteIntro}
                <Text
                  style={{ color: th.primary, textDecorationLine: 'underline' }}
                  onPress={() => void Linking.openURL(S.settings.about.websiteUrl)}
                >
                  {S.settings.about.websiteLabel}
                </Text>
                {S.settings.about.end}
              </Text>
              <Text style={{ fontSize: 13, color: th.subtext, marginTop: 16 }}>{S.settings.about.license}</Text>
              <Pressable
                onPress={() => {
                  st.setSetting('has_seen_tutorial', false);
                  useStore.setState({ showTutorial: true, showSettings: false });
                }}
                style={{ marginTop: 20, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ color: th.primary }}>{S.settings.about.showTutorialAgain}</Text>
              </Pressable>
              <Pressable
                onPress={() => void Linking.openURL('https://spegen.vercel.app/privacy.html')}
                style={{ marginTop: 4, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ color: th.primary }}>{S.settings.about.privacyPolicy}</Text>
              </Pressable>
            </View>
          )}

          {tab === 'feedback' && (
            <View>
              <SectionTitle>{S.settings.feedback.title}</SectionTitle>
              <Text style={{ fontSize: 12, color: th.subtext, marginBottom: 12 }}>{S.settings.feedback.intro}</Text>

              {/* Category selector chips */}
              <Text style={{ fontSize: 14, fontWeight: '500', marginTop: 8 }}>{S.settings.feedback.category}</Text>
              <View style={{ flexDirection: 'row', flexWrap: 'wrap', marginBottom: 8 }}>
                {[...S.settings.feedback.categories].map((cat) => (
                  <Chip
                    key={cat}
                    label={cat}
                    active={fbCategory === cat}
                    onPress={() => setFbCategory(cat)}
                  />
                ))}
              </View>

              {/* Title Input */}
              <Text style={{ fontSize: 14, fontWeight: '500', marginTop: 12 }}>{S.settings.feedback.header}</Text>
              <TextInput
                value={fbTitle}
                onChangeText={setFbTitle}
                placeholder={S.settings.feedback.headerPlaceholder}
                style={{ borderWidth: 1, borderColor: th.inputBorder, borderRadius: 6, padding: 8, marginTop: 4, backgroundColor: th.surface }}
              />

              {/* Body Input */}
              <Text style={{ fontSize: 14, fontWeight: '500', marginTop: 16 }}>{S.settings.feedback.details}</Text>
              <TextInput
                value={fbBody}
                onChangeText={setFbBody}
                placeholder={S.settings.feedback.detailsPlaceholder}
                multiline
                numberOfLines={6}
                style={{ 
                  borderWidth: 1, 
                  borderColor: th.inputBorder, 
                  borderRadius: 6, 
                  padding: 8, 
                  marginTop: 4, 
                  minHeight: 140, 
                  textAlignVertical: 'top', 
                  backgroundColor: th.surface 
                }}
              />

              {/* Submit Button */}
              <Pressable
                onPress={submitFeedback}
                disabled={fbSubmitting}
                style={{
                  marginTop: 24,
                  backgroundColor: fbSubmitting ? '#A0A0A0' : th.primary,
                  borderRadius: 8,
                  padding: 14,
                  alignItems: 'center',
                }}
              >
                <Text style={{ color: '#FFFFFF', fontWeight: 'bold', fontSize: 15 }}>
                  {fbSubmitting ? S.settings.feedback.submitting : S.settings.feedback.submit}
                </Text>
              </Pressable>

              {/* Local status preview message */}
              {!!status && <Text style={{ marginTop: 14, color: th.subtext }}>{status}</Text>}
            </View>
          )}
        </ScrollView>
      </View>
    </Modal>

    <UnsavedChangesDialog
      visible={showUnsaved}
      onSave={() => {
        setShowUnsaved(false);
        void st.applyChanges().then(close);
      }}
      onDiscard={() => {
        setShowUnsaved(false);
        void st.discardChanges().then(close);
      }}
      onDismiss={() => setShowUnsaved(false)}
    />
    </>
  );
}