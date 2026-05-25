package io.github.hkleinkeane.spegen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

class IosTtsEngine : TtsEngine {
    private val synthesizer = AVSpeechSynthesizer()
    private var speechRate: Float = 0.5f
    private var pitch: Float = 1.0f

    override fun speak(text: String, queueMode: Int, utteranceId: String) {
        if (queueMode == TtsEngine.QUEUE_FLUSH) synthesizer.stopSpeakingAtBoundary(
            platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate
        )
        val utterance = AVSpeechUtterance(string = text)
        utterance.rate = speechRate
        utterance.pitchMultiplier = pitch
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(null)
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(
            platform.AVFAudio.AVSpeechBoundary.AVSpeechBoundaryImmediate
        )
    }

    override fun setSpeechRate(rate: Float) {
        // Android rate 1.0 = normal; iOS AVSpeechUtteranceDefaultSpeechRate ≈ 0.5
        speechRate = (rate * 0.5f).coerceIn(0.1f, 1.0f)
    }

    override fun setPitch(pitch: Float) { this.pitch = pitch.coerceIn(0.5f, 2.0f) }

    override fun playSilentUtterance(durationMs: Long, queueMode: Int, utteranceId: String) {
        // iOS has no built-in silent utterance; approximate with a pause utterance
        val utterance = AVSpeechUtterance(string = " ")
        utterance.rate = AVSpeechUtteranceMinimumSpeechRate
        synthesizer.speakUtterance(utterance)
    }

    override val isSpeaking get() = synthesizer.isSpeaking()
}

@Composable
actual fun rememberTtsEngine(): MutableState<TtsEngine?> {
    DisposableEffect(Unit) {
        ttsEngine.value = IosTtsEngine()
        tts_data_found.value = true
        onDispose { ttsEngine.value?.stop() }
    }
    return ttsEngine
}

actual fun openTtsSettings() {
    // No direct TTS settings page on iOS; open Settings app root
    val url = platform.Foundation.NSURL.URLWithString("App-Prefs:") ?: return
    platform.UIKit.UIApplication.sharedApplication.openURL(url)
}
