package io.github.hkleinkeane.spegen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState

class JvmTtsEngine : TtsEngine {
    private val os = System.getProperty("os.name").lowercase()
    private var speechRate = 1.0f
    private var currentProcess: Process? = null

    override val isSpeaking get() = currentProcess?.isAlive == true

    override fun speak(text: String, queueMode: Int, utteranceId: String) {
        if (queueMode == TtsEngine.QUEUE_FLUSH) stop()
        val safe = text.replace("'", "").replace("\"", "").replace("`", "").replace("\\", "")
        currentProcess = try {
            when {
                "mac" in os -> ProcessBuilder("say", safe).start()
                "win" in os -> ProcessBuilder(
                    "powershell", "-NoProfile", "-Command",
                    "Add-Type -AssemblyName System.speech; " +
                    "\$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                    "\$s.Rate = ${((speechRate - 1f) * 5).toInt().coerceIn(-10, 10)}; " +
                    "\$s.Speak('$safe')"
                ).start()
                else -> ProcessBuilder("espeak", safe).start()
            }
        } catch (e: Exception) {
            println("TTS speak error: ${e.message}")
            null
        }
    }

    override fun stop() { currentProcess?.destroy(); currentProcess = null }
    override fun setSpeechRate(rate: Float) { speechRate = rate }
    override fun setPitch(pitch: Float) {}
    override fun playSilentUtterance(durationMs: Long, queueMode: Int, utteranceId: String) {
        Thread.sleep(durationMs)
    }
}

@Composable
actual fun rememberTtsEngine(): MutableState<TtsEngine?> {
    DisposableEffect(Unit) {
        ttsEngine.value = JvmTtsEngine()
        tts_data_found.value = true
        onDispose { ttsEngine.value?.stop() }
    }
    return ttsEngine
}

actual fun openTtsSettings() {
    // No system TTS settings to open on JVM desktop
}
