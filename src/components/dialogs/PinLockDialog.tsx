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

// Caregiver PIN-lock screen.
import React, { useState } from 'react';
import { Modal, Pressable, ScrollView, View } from 'react-native';
import { useStore } from '../../store';
import { useTheme } from '../../theme';
import { Text, TextInput } from '../themed';
import { useStrings } from '../../i18n';

export function PinLockDialog() {
  const promptFor = useStore((s) => s.pinPromptFor);
  const questions = useStore((s) => s.security_questions);
  const submitPin = useStore((s) => s.submitPin);
  const cancelPinPrompt = useStore((s) => s.cancelPinPrompt);
  const verifySecurityAnswers = useStore((s) => s.verifySecurityAnswers);
  const resetPinViaRecovery = useStore((s) => s.resetPinViaRecovery);
  const t = useTheme();
  const S = useStrings();

  const [pin, setPin] = useState('');
  const [error, setError] = useState('');
  const [mode, setMode] = useState<'enter' | 'recover'>('enter');
  const [answers, setAnswers] = useState<string[]>([]);
  const [recovered, setRecovered] = useState(false); // answers verified -> choose a new PIN
  const [newPin, setNewPin] = useState('');
  const [newPin2, setNewPin2] = useState('');
  const [busy, setBusy] = useState(false);

  const reset = () => {
    setPin('');
    setError('');
    setMode('enter');
    setAnswers([]);
    setRecovered(false);
    setNewPin('');
    setNewPin2('');
    setBusy(false);
  };

  const close = () => {
    reset();
    cancelPinPrompt();
  };

  const onUnlock = async () => {
    setBusy(true);
    const ok = await submitPin(pin);
    setBusy(false);
    if (ok) reset(); // store clears pinPromptFor, so this dialog unmounts
    else setError(S.dialogs.pinLock.incorrectPin);
  };

  const startRecovery = () => {
    if (questions.length === 0) {
      setError(S.dialogs.pinLock.noQuestions);
      return;
    }
    setAnswers(questions.map(() => ''));
    setError('');
    setMode('recover');
  };

  const onVerify = async () => {
    setBusy(true);
    const ok = await verifySecurityAnswers(answers);
    setBusy(false);
    if (ok) {
      setRecovered(true);
      setError('');
    } else {
      setError(S.dialogs.pinLock.answersIncorrect);
    }
  };

  const onResetPin = async () => {
    if (newPin.length < 4) {
      setError(S.dialogs.pinLock.pinTooShort);
      return;
    }
    if (newPin !== newPin2) {
      setError(S.dialogs.pinLock.pinsDoNotMatch);
      return;
    }
    setBusy(true);
    await resetPinViaRecovery(newPin); // persists + opens Settings + clears pinPromptFor
    setBusy(false);
    reset();
  };

  return (
    <Modal visible={promptFor !== null} transparent animationType="fade" onRequestClose={close}>
      <View style={{ flex: 1, backgroundColor: t.scrim, justifyContent: 'center', padding: 24 }}>
        <View style={{ backgroundColor: t.surface, borderRadius: 12, padding: 20, maxHeight: '85%' }}>
          {mode === 'enter' ? (
            <>
              <Text style={{ fontSize: 18, fontWeight: 'bold' }}>{S.dialogs.pinLock.enterTitle}</Text>
              <Text style={{ fontSize: 13, color: t.subtext, marginTop: 4 }}>{S.dialogs.pinLock.lockedHint}</Text>
              <TextInput
                value={pin}
                onChangeText={(t) => {
                  setPin(t);
                  setError('');
                }}
                placeholder={S.dialogs.pinLock.pinPlaceholder}
                keyboardType="number-pad"
                secureTextEntry
                autoFocus
                maxLength={12}
                onSubmitEditing={onUnlock}
                style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 10, marginTop: 12, fontSize: 18, letterSpacing: 4 }}
              />
              {!!error && <Text style={{ color: t.danger, marginTop: 8 }}>{error}</Text>}
              <Pressable
                onPress={onUnlock}
                disabled={busy || !pin}
                style={{ marginTop: 14, backgroundColor: t.primary, opacity: busy || !pin ? 0.5 : 1, borderRadius: 8, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.dialogs.pinLock.unlock}</Text>
              </Pressable>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 14 }}>
                <Pressable onPress={startRecovery} style={{ padding: 6 }}>
                  <Text style={{ color: t.primary }}>{S.dialogs.pinLock.forgotPin}</Text>
                </Pressable>
                <Pressable onPress={close} style={{ padding: 6 }}>
                  <Text style={{ fontSize: 16 }}>{S.common.cancel}</Text>
                </Pressable>
              </View>
            </>
          ) : !recovered ? (
            <>
              <Text style={{ fontSize: 18, fontWeight: 'bold' }}>{S.dialogs.pinLock.answerToReset}</Text>
              <Text style={{ fontSize: 13, color: t.subtext, marginTop: 4 }}>{S.dialogs.pinLock.answerHint}</Text>
              <ScrollView style={{ maxHeight: 320, marginTop: 8 }} keyboardShouldPersistTaps="handled">
                {questions.map((q, i) => (
                  <View key={i} style={{ marginTop: 10 }}>
                    <Text style={{ fontSize: 14 }}>{q.question}</Text>
                    <TextInput
                      value={answers[i] ?? ''}
                      onChangeText={(t) => {
                        setAnswers((prev) => {
                          const next = [...prev];
                          next[i] = t;
                          return next;
                        });
                        setError('');
                      }}
                      placeholder={S.dialogs.pinLock.answerPlaceholder}
                      autoCapitalize="none"
                      style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 8, marginTop: 4 }}
                    />
                  </View>
                ))}
              </ScrollView>
              {!!error && <Text style={{ color: t.danger, marginTop: 8 }}>{error}</Text>}
              <Pressable
                onPress={onVerify}
                disabled={busy}
                style={{ marginTop: 14, backgroundColor: t.primary, opacity: busy ? 0.5 : 1, borderRadius: 8, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.dialogs.pinLock.verify}</Text>
              </Pressable>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 14 }}>
                <Pressable onPress={() => { setMode('enter'); setError(''); }} style={{ padding: 6 }}>
                  <Text style={{ color: t.primary }}>{S.common.back}</Text>
                </Pressable>
                <Pressable onPress={close} style={{ padding: 6 }}>
                  <Text style={{ fontSize: 16 }}>{S.common.cancel}</Text>
                </Pressable>
              </View>
            </>
          ) : (
            <>
              <Text style={{ fontSize: 18, fontWeight: 'bold' }}>{S.dialogs.pinLock.setNewPin}</Text>
              <TextInput
                value={newPin}
                onChangeText={(t) => { setNewPin(t); setError(''); }}
                placeholder={S.dialogs.pinLock.newPinPlaceholder}
                keyboardType="number-pad"
                secureTextEntry
                maxLength={12}
                style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 10, marginTop: 12, fontSize: 18, letterSpacing: 4 }}
              />
              <TextInput
                value={newPin2}
                onChangeText={(t) => { setNewPin2(t); setError(''); }}
                placeholder={S.dialogs.pinLock.confirmNewPinPlaceholder}
                keyboardType="number-pad"
                secureTextEntry
                maxLength={12}
                onSubmitEditing={onResetPin}
                style={{ borderWidth: 1, borderColor: t.inputBorder, borderRadius: 6, padding: 10, marginTop: 10, fontSize: 18, letterSpacing: 4 }}
              />
              {!!error && <Text style={{ color: t.danger, marginTop: 8 }}>{error}</Text>}
              <Pressable
                onPress={onResetPin}
                disabled={busy}
                style={{ marginTop: 14, backgroundColor: t.primary, opacity: busy ? 0.5 : 1, borderRadius: 8, padding: 12, alignItems: 'center' }}
              >
                <Text style={{ color: '#FFFFFF', fontWeight: 'bold' }}>{S.dialogs.pinLock.saveNewPin}</Text>
              </Pressable>
              <Pressable onPress={close} style={{ padding: 6, marginTop: 14, alignSelf: 'flex-end' }}>
                <Text style={{ fontSize: 16 }}>Cancel</Text>
              </Pressable>
            </>
          )}
        </View>
      </View>
    </Modal>
  );
}
