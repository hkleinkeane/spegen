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

/** Play a locally stored audio file at [path]. No-op on platforms without audio support. */
expect fun playAudioFile(path: String)

/** Start recording audio, using [itemKey] as the filename stem. No-op on unsupported platforms. */
expect fun startRecording(itemKey: String)

/**
 * Stop a recording started by [startRecording].
 * Returns the absolute path of the saved file, or "" if recording is unsupported or failed.
 */
expect fun stopRecording(): String

/**
 * Composable file picker that lets the user import an audio clip.
 * Returns a lambda that, when invoked, opens the platform file picker.
 * [itemKey] is used as the destination filename stem.
 * [onAudioPicked] is called with the absolute path of the imported file.
 */
@Composable
expect fun rememberPlatformAudioPicker(
    itemKey: String,
    onAudioPicked: (path: String) -> Unit
): () -> Unit

/**
 * Play a sequence of words, mixing custom audio files and TTS.
 *
 * For each index, if [audioPaths][i] is non-blank the audio file is played;
 * otherwise the word is spoken via [ttsEngine].
 * [pauseMs] is the inter-word gap (0 = none).
 * [onFinished] is called when the full sequence has been delivered.
 */
expect fun playSentenceSequenced(
    words: List<String>,
    audioPaths: List<String>,
    pauseMs: Long,
    onFinished: () -> Unit
)

/** Stop any in-progress sentence playback (both audio and TTS). */
expect fun stopSentenceSequenced()
