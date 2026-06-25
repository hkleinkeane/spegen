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

/*
 * App root. Measures the screen via
 * onLayout (feeding the absolute-positioned layout), boots persisted state on mount, and
 * saves when the app is backgrounded. All regions position themselves from the store's
 * Dimensions fields, so they are simply rendered as siblings here.
 */
import React, { useEffect } from 'react';
import { AppState as RNAppState, LayoutChangeEvent, Platform, Pressable, View } from 'react-native';
import * as IntentLauncher from 'expo-intent-launcher';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';
import { useStore } from './src/store';
import { registerPWA } from './src/pwa';
import { useTheme } from './src/theme';
import { Text } from './src/components/themed';
import { APP_LANGUAGES } from './src/types';
import { fmt } from './src/strings';
import { useStrings } from './src/i18n';
import { InputBox } from './src/components/InputBox';
import { Board } from './src/components/Board';
import { AutocompleteMenu } from './src/components/AutocompleteMenu';
import { MenuRow } from './src/components/MenuRow';
import { StaticRow } from './src/components/StaticRow';
import { ButtonBoxes } from './src/components/ButtonBoxes';
import { EditorOverlay } from './src/components/EditorOverlay';
import { EditorToolbar } from './src/components/EditorToolbar';
import { Tutorial } from './src/components/Tutorial';
import { WordFinder, WordFinderGuide } from './src/components/WordFinder';
import { WordFinderSpotlight } from './src/components/WordFinderSpotlight';
import { UpdateBanner } from './src/components/UpdateBanner';
import { SettingsScreen } from './src/components/SettingsScreen';
import { AddItemDialog } from './src/components/dialogs/AddItemDialog';
import { EditItemDialog } from './src/components/dialogs/EditItemDialog';
import { NewMenuDialog } from './src/components/dialogs/NewMenuDialog';
import { DeleteMenuDialog } from './src/components/dialogs/DeleteMenuDialog';
import { GotoMenuDialog } from './src/components/dialogs/GotoMenuDialog';
import { PinLockDialog } from './src/components/dialogs/PinLockDialog';

// Banner shown when speech is requested for a language with no installed voice. The sentence is
// still spoken with the default voice.
function TtsBanner() {
  const lang = useStore((s) => s.ttsMissingLanguage);
  const clear = useStore((s) => s.setTtsMissingLanguage);
  const S = useStrings();
  if (!lang) return null;
  const name = APP_LANGUAGES.find((l) => l.code === lang)?.name ?? lang;
  // Platform-specific guidance: Android can deep-link into the device speech settings (where voice
  // data is installed); iOS/web can't, so we tell the user where to go.
  const hint =
    Platform.OS === 'android'
      ? S.banners.ttsInstallAndroid
      : Platform.OS === 'ios'
        ? S.banners.ttsInstallIos
        : S.banners.ttsInstallWeb;
  const onPress = async () => {
    if (Platform.OS === 'android') {
      try {
        await IntentLauncher.startActivityAsync(IntentLauncher.ActivityAction.TTS_SETTINGS);
      } catch {
        // ignore — fall through to dismiss
      }
    }
    clear(null);
  };
  return (
    <Pressable
      onPress={() => void onPress()}
      style={{
        position: 'absolute',
        top: 8,
        left: 12,
        right: 12,
        backgroundColor: '#323232',
        borderRadius: 8,
        padding: 12,
        zIndex: 1000,
      }}
    >
      <Text style={{ color: '#FFFFFF' }}>{fmt(S.banners.ttsMissingVoice, { name, code: lang }) + hint}</Text>
    </Pressable>
  );
}

export default function App() {
  return (
    <SafeAreaProvider>
      <AppContent />
    </SafeAreaProvider>
  );
}

function AppContent() {
  const init = useStore((s) => s.init);
  const save = useStore((s) => s.save);
  const setDimensions = useStore((s) => s.setDimensions);
  const ready = useStore((s) => s.ready);
  const editorMode = useStore((s) => s.editorMode);
  const showAutocomplete = useStore((s) => s.showAutocomplete);
  const t = useTheme();
  const insets = useSafeAreaInsets();

  useEffect(() => {
    void init();
  }, [init]);

  // Register the PWA manifest + service worker (web only) so the site is installable and works
  // offline after the first load. No-op on native.
  useEffect(() => {
    registerPWA();
  }, []);

  useEffect(() => {
    const sub = RNAppState.addEventListener('change', (state) => {
      if (state !== 'active') void save();
    });
    return () => sub.remove();
  }, [save]);

  const onLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (width > 0 && height > 0) setDimensions(width, height);
  };

  return (
    <View
      style={{
        flex: 1,
        backgroundColor: t.bg,
        paddingTop: insets.top,
        paddingBottom: insets.bottom,
        paddingLeft: insets.left,
        paddingRight: insets.right,
      }}
    >
      <StatusBar hidden />
      {/* Measured content area = the safe area (avoids the notch / home indicator on iOS). The
          absolute-positioned regions below lay out within this box. */}
      <View onLayout={onLayout} style={{ flex: 1 }}>
        {ready && (
          <>
          <InputBox />
          <Board />
          {showAutocomplete && <AutocompleteMenu />}
          <MenuRow />
          <StaticRow />
          <ButtonBoxes />
          {/* Editor scrims (z700/750) dim everything but the board; render before the toolbar
              (z800) so the toolbar stays tappable above them. */}
          {editorMode && <EditorOverlay />}
          {editorMode && <EditorToolbar />}
          <TtsBanner />
          {/* Word-finder grey spotlight (greys everything but the next item to tap). */}
          <WordFinderSpotlight />
          <WordFinderGuide />

          {/* overlays / dialogs (each gates on its own store flag) */}
          <SettingsScreen />
          <WordFinder />
          <Tutorial />
          <AddItemDialog />
          <EditItemDialog />
          <NewMenuDialog />
          <DeleteMenuDialog />
          <GotoMenuDialog />
          {/* Caregiver PIN lock — gates Settings (and thus the editor). */}
          <PinLockDialog />
          {/* Web/PWA: prompt to reload when a newer build is deployed. */}
          <UpdateBanner />
          </>
        )}
      </View>
    </View>
  );
}
