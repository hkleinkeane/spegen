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

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

// ---------------------------------------------------------------------------
// Module-level state (single active player / recorder at a time)
// ---------------------------------------------------------------------------

private var currentPlayer: MediaPlayer? = null
private var currentRecorder: MediaRecorder? = null
private var currentRecordingPath: String = ""

// ---------------------------------------------------------------------------
// playAudioFile
// ---------------------------------------------------------------------------

actual fun playAudioFile(path: String) {
    currentPlayer?.apply { try { stop() } catch (_: Exception) {}; release() }
    currentPlayer = null
    if (path.isBlank()) return
    try {
        val mp = MediaPlayer()
        mp.setDataSource(path)
        mp.prepare()
        mp.setOnCompletionListener { it.release(); if (currentPlayer == it) currentPlayer = null }
        mp.start()
        currentPlayer = mp
    } catch (e: Exception) {
        println("AudioEngine.android playAudioFile failed: ${e.message}")
    }
}

// ---------------------------------------------------------------------------
// Recording
// ---------------------------------------------------------------------------

actual fun startRecording(itemKey: String) {
      // Check mic permission first
      val granted = ContextCompat.checkSelfPermission(
        androidAppContext, Manifest.permission.RECORD_AUDIO
      ) == PackageManager.PERMISSION_GRANTED

      if (!granted) {
            // Show the system permission dialog. This is async so the current tap won't record, but once permission is granted the next one will.
            androidActivity?.let { activity ->
              ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1001
              )
            }
            println("AudioEngine.android startRecording: requesting mic permission, tap Record again after granting")
            currentRecordingPath = ""
            return
          }

      try {
            currentRecorder?.apply {
              try { stop() } catch (_: Exception) {}
              release()
            }
            currentRecorder = null

            val dir = File(androidAppContext.filesDir, "custom_audio").also { it.mkdirs() }
            currentRecordingPath = File(dir, "$itemKey.m4a").absolutePath

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              MediaRecorder(androidAppContext)
            } else {
              @Suppress("DEPRECATION") MediaRecorder()
            }

            rec.setAudioSource(MediaRecorder.AudioSource.MIC)
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            rec.setOutputFile(currentRecordingPath)
            rec.prepare()
            rec.start()
            currentRecorder = rec
          } catch (e: Exception) {
            println("AudioEngine.android startRecording failed: ${e.message}")
            currentRecordingPath = ""
          }
}

actual fun stopRecording(): String {
    return try {
        currentRecorder?.apply { stop(); release() }
        currentRecorder = null
        currentRecordingPath
    } catch (e: Exception) {
        println("AudioEngine.android stopRecording failed: ${e.message}")
        currentRecorder = null
        ""
    }
}

// ---------------------------------------------------------------------------
// Audio picker
// ---------------------------------------------------------------------------

private fun copyAudioToPrivateStorage(uri: Uri, itemKey: String): String {
    return try {
        val dir = File(androidAppContext.filesDir, "custom_audio").also { it.mkdirs() }
        val dest = File(dir, "$itemKey.m4a")
        androidAppContext.contentResolver.openInputStream(uri)?.use { it.copyTo(dest.outputStream()) }
        dest.absolutePath
    } catch (e: Exception) {
        println("AudioEngine.android copyAudioToPrivateStorage failed: ${e.message}")
        ""
    }
}

@Composable
actual fun rememberPlatformAudioPicker(
    itemKey: String,
    onAudioPicked: (path: String) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = copyAudioToPrivateStorage(uri, itemKey)
            if (path.isNotBlank()) onAudioPicked(path)
        }
    }
    return { launcher.launch(arrayOf("audio/*")) }
}

// ---------------------------------------------------------------------------
// Sentence sequencer
// ---------------------------------------------------------------------------

actual fun playSentenceSequenced(
    words: List<String>,
    audioPaths: List<String>,
    pauseMs: Long,
    onFinished: () -> Unit
) {
    stopSentenceSequenced()
    if (words.isEmpty()) { onFinished(); return }

    val hasAudio = audioPaths.any { it.isNotBlank() }
    val tts = ttsEngine.value
    val handler = Handler(Looper.getMainLooper())

    if (!hasAudio) {
        // Pure-TTS path: queue all words with optional silent pauses
        if (tts != null) {
            tts.speak(words[0], TtsEngine.QUEUE_FLUSH, "seq_0")
            for (i in 1 until words.size) {
                if (pauseMs > 0) tts.playSilentUtterance(pauseMs, TtsEngine.QUEUE_ADD, "pause_$i")
                tts.speak(words[i], TtsEngine.QUEUE_ADD, "seq_$i")
            }
        }
        onFinished()
        return
    }

    // Mixed audio + TTS: recursive chaining via MediaPlayer.onCompletionListener + Handler delay
    fun playNext(index: Int) {
        if (index >= words.size) { onFinished(); return }
        val audioPath = audioPaths.getOrNull(index)?.takeIf { it.isNotBlank() }

        fun scheduleNext() {
            if (pauseMs > 0) handler.postDelayed({ playNext(index + 1) }, pauseMs)
            else playNext(index + 1)
        }

        if (audioPath != null) {
            try {
                val mp = MediaPlayer()
                mp.setDataSource(audioPath)
                mp.prepare()
                mp.setOnCompletionListener {
                    it.release()
                    if (currentPlayer == it) currentPlayer = null
                    scheduleNext()
                }
                currentPlayer = mp
                mp.start()
            } catch (e: Exception) {
                println("AudioEngine.android playSentenceSequenced audio[$index] failed: ${e.message}")
                scheduleNext()
            }
        } else {
            // Estimate spoken duration: ~75 ms/char, minimum 400 ms
            val estimatedMs = maxOf(400L, words[index].length * 75L) + pauseMs
            tts?.speak(words[index], TtsEngine.QUEUE_FLUSH, "seq_$index")
            handler.postDelayed({ playNext(index + 1) }, estimatedMs)
        }
    }

    playNext(0)
}

actual fun stopSentenceSequenced() {
    currentPlayer?.apply { try { stop() } catch (_: Exception) {}; release() }
    currentPlayer = null
    ttsEngine.value?.stop()
}
