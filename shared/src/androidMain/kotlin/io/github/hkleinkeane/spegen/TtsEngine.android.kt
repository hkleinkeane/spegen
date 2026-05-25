/*
 * Copyright 2026 Harper Klein Keane
 *
 * This file is part of SpeGen.
 *
 * SpeGen is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * SpeGen is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SpeGen. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.hkleinkeane.spegen

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class AndroidTtsEngine(private val tts: TextToSpeech) : TtsEngine {
    override fun speak(text: String, queueMode: Int, utteranceId: String) {
        tts.speak(text, queueMode, null, utteranceId)
    }
    override fun stop() { tts.stop() }
    override fun setSpeechRate(rate: Float) { tts.setSpeechRate(rate) }
    override fun setPitch(pitch: Float) { tts.setPitch(pitch) }
    override fun playSilentUtterance(durationMs: Long, queueMode: Int, utteranceId: String) {
        tts.playSilentUtterance(durationMs, queueMode, utteranceId)
    }
    override val isSpeaking get() = tts.isSpeaking
}

@Composable
actual fun rememberTtsEngine(): MutableState<TtsEngine?> {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tt = instance ?: return@TextToSpeech
                val result = tt.setLanguage(Locale.getDefault())
                if ((result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) && !tts_data_found.value
                ) {
                    context.startActivity(Intent().apply {
                        action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
                if (result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    tts_data_found.value = true
                }
                tt.setSpeechRate(tts_speech_rate.value)
                tt.setPitch(tts_pitch.value)
                ttsEngine.value = AndroidTtsEngine(tt)
            }
        }
        onDispose { instance?.stop() }
    }
    return ttsEngine
}

actual fun openTtsSettings() {
    try {
        androidAppContext.startActivity(
            Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    } catch (e: Exception) {
        println("TTS settings not available: ${e.message}")
    }
}
