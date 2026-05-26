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

import androidx.compose.runtime.Composable

actual fun playAudioFile(path: String) {
    // TODO: javax.sound.sampled / JavaFX MediaPlayer implementation
}

actual fun startRecording(itemKey: String) {
    // TODO: javax.sound.sampled TargetDataLine implementation
}

actual fun stopRecording(): String = ""

@Composable
actual fun rememberPlatformAudioPicker(
    itemKey: String,
    onAudioPicked: (path: String) -> Unit
): () -> Unit {
    return {
        val chooser = javax.swing.JFileChooser()
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
            "Audio files", "mp3", "m4a", "wav", "ogg", "aac"
        )
        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            val src = chooser.selectedFile
            val destDir = java.io.File(System.getProperty("user.home"), ".spegen/custom_audio")
                .also { it.mkdirs() }
            val dest = java.io.File(destDir, "$itemKey.${src.extension}")
            src.copyTo(dest, overwrite = true)
            onAudioPicked(dest.absolutePath)
        }
    }
}

actual fun playSentenceSequenced(
    words: List<String>,
    audioPaths: List<String>,
    pauseMs: Long,
    onFinished: () -> Unit
) {
    val tts = ttsEngine.value ?: run { onFinished(); return }
    if (words.isEmpty()) { onFinished(); return }
    tts.speak(words[0], TtsEngine.QUEUE_FLUSH, "seq_0")
    for (i in 1 until words.size) {
        if (pauseMs > 0) tts.playSilentUtterance(pauseMs, TtsEngine.QUEUE_ADD, "pause_$i")
        tts.speak(words[i], TtsEngine.QUEUE_ADD, "seq_$i")
    }
    onFinished()
}

actual fun stopSentenceSequenced() {
    ttsEngine.value?.stop()
}
