package io.github.hkleinkeane.spegen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import kotlinx.browser.window
import org.w3c.speech.SpeechSynthesisUtterance

class WebTtsEngine : TtsEngine {
    private val synth = window.speechSynthesis
    private var speechRate = 1.0f
    private var pitch = 1.0f

    override val isSpeaking get() = synth.speaking

    override fun speak(text: String, queueMode: Int, utteranceId: String) {
        if (queueMode == TtsEngine.QUEUE_FLUSH) synth.cancel()
        val utterance = SpeechSynthesisUtterance(text)
        utterance.rate = speechRate.toDouble()
        utterance.pitch = pitch.toDouble()
        synth.speak(utterance)
    }

    override fun stop() = synth.cancel()
    override fun setSpeechRate(rate: Float) { speechRate = rate }
    override fun setPitch(pitch: Float) { this.pitch = pitch }
    override fun playSilentUtterance(durationMs: Long, queueMode: Int, utteranceId: String) {
        // Web Speech API has no silent utterance; skip
    }
}

@Composable
actual fun rememberTtsEngine(): MutableState<TtsEngine?> {
    DisposableEffect(Unit) {
        ttsEngine.value = WebTtsEngine()
        tts_data_found.value = true
        onDispose { ttsEngine.value?.stop() }
    }
    return ttsEngine
}

actual fun openTtsSettings() {
    // No TTS settings page in browser
}
