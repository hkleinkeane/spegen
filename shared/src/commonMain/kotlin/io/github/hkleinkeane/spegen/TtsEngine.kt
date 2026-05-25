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
import androidx.compose.runtime.MutableState

interface TtsEngine {
    fun speak(text: String, queueMode: Int, utteranceId: String)
    fun stop()
    fun setSpeechRate(rate: Float)
    fun setPitch(pitch: Float)
    fun playSilentUtterance(durationMs: Long, queueMode: Int, utteranceId: String)
    val isSpeaking: Boolean

    companion object {
        const val QUEUE_FLUSH = 0
        const val QUEUE_ADD = 1
    }
}

@Composable
expect fun rememberTtsEngine(): MutableState<TtsEngine?>

expect fun openTtsSettings()
