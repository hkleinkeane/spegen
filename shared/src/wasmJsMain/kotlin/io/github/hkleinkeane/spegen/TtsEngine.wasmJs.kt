package io.github.hkleinkeane.spegen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import kotlinx.browser.window

// WebAssembly/JS target reuses browser Web Speech API via JS interop
@JsName("SpeechSynthesisUtterance")
external class JsSpeechSynthesisUtterance(text: String) {
    var rate: Double
    var pitch: Double
}

class WasmWebTtsEngine : TtsEngine {
    private val synth = window.speechSynthesis
    private var speechRate = 1.0f
    private var pitchVal = 1.0f

    override val isSpeaking get() = synth.speaking

    override fun speak(text: String, queueMode: Int, utteranceId: String) {
        if (queueMode == TtsEngine.QUEUE_FLUSH) synth.cancel()
        val utterance = JsSpeechSynthesisUtterance(text)
        utterance.rate = speechRate.toDouble()
        utterance.pitch = pitchVal.toDouble()
        synth.speak(utterance.unsafeCast())
    }

    override fun stop() = synth.cancel()
    override fun setSpeechRate(rate: Float) { speechRate = rate }
    override fun setPitch(pitch: Float) { pitchVal = pitch }
    override fun playSilentUtterance(durationMs: Long, queueMode: Int, utteranceId: String) {}
}

// Fallback: use org.w3c.speech if available; otherwise JsInterop
private fun org.w3c.speech.SpeechSynthesis.speaking(): Boolean = this.speaking

@Composable
actual fun rememberTtsEngine(): MutableState<TtsEngine?> {
    DisposableEffect(Unit) {
        ttsEngine.value = WasmWebTtsEngine()
        tts_data_found.value = true
        onDispose { ttsEngine.value?.stop() }
    }
    return ttsEngine
}

actual fun openTtsSettings() {}
