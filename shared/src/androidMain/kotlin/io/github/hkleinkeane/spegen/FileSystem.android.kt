package io.github.hkleinkeane.spegen

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

actual fun cleanOrphanedCustomImages() {
    val imageDir = File(androidAppContext.filesDir, "custom_images")
    if (imageDir.exists()) {
        val referencedImages = MenuList
            .flatMap { it.custom_image_paths }
            .filter { it.isNotBlank() }
            .map { File(it).name }
            .toSet()
        imageDir.listFiles()?.forEach { file ->
            if (file.name !in referencedImages) file.delete()
        }
    }

    val audioDir = File(androidAppContext.filesDir, "custom_audio")
    if (audioDir.exists()) {
        val referencedAudio = MenuList
            .flatMap { it.custom_audio_paths }
            .filter { it.isNotBlank() }
            .map { File(it).name }
            .toSet()
        audioDir.listFiles()?.forEach { file ->
            if (file.name !in referencedAudio) file.delete()
        }
    }
}

private fun copyImageToPrivateStorage(uri: Uri, itemKey: String): String {
    val context = androidAppContext
    return try {
        val dir = File(context.filesDir, "custom_images").also { it.mkdirs() }
        val dest = File(dir, "$itemKey.webp")
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            android.graphics.BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(uri)
            )
        }
        bitmap?.let {
            dest.outputStream().use { out -> it.compress(Bitmap.CompressFormat.WEBP, 90, out) }
        }
        dest.absolutePath
    } catch (e: Exception) {
        println("copyImageToPrivateStorage failed: ${e.message}")
        ""
    }
}

private fun exportToZip(outputUri: Uri) {
    val context = androidAppContext
    val imageDir = File(context.filesDir, "custom_images")
    val audioDir = File(context.filesDir, "custom_audio")
    val exportState = currentPersistedState().copy(
        menu_list = MenuList.map { menu ->
            menu.copy(
                custom_image_paths = menu.custom_image_paths.map { path ->
                    if (path.isNotBlank() && File(path).exists())
                        "custom_images/${File(path).name}"
                    else path
                },
                custom_audio_paths = menu.custom_audio_paths.map { path ->
                    if (path.isNotBlank() && File(path).exists())
                        "custom_audio/${File(path).name}"
                    else path
                }
            )
        }
    )
    context.contentResolver.openOutputStream(outputUri)?.use { out ->
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("state.json"))
            zip.write(Json.encodeToString(exportState).toByteArray())
            zip.closeEntry()
            imageDir.listFiles()?.forEach { f ->
                zip.putNextEntry(ZipEntry("custom_images/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            audioDir.listFiles()?.forEach { f ->
                zip.putNextEntry(ZipEntry("custom_audio/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}

private fun importFromZip(inputUri: Uri) {
    val context = androidAppContext
    val imageDir = File(context.filesDir, "custom_images").also { it.mkdirs() }
    val audioDir = File(context.filesDir, "custom_audio").also { it.mkdirs() }
    val input = context.contentResolver.openInputStream(inputUri)
        ?: throw Exception("Could not open the selected file.")
    input.use { inStream ->
        ZipInputStream(inStream.buffered()).use { zip ->
            var loadedState: PersistedState? = null
            val lenient = Json { ignoreUnknownKeys = true }
            var entry = zip.nextEntry
            while (entry != null) {
                when {
                    entry.name == "state.json" -> {
                        loadedState = lenient.decodeFromString<PersistedState>(
                            zip.readBytes().decodeToString()
                        )
                    }
                    entry.name.startsWith("custom_images/") -> {
                        val fileName = entry.name.removePrefix("custom_images/")
                        if (fileName.isNotBlank()) {
                            File(imageDir, fileName).outputStream().use { zip.copyTo(it) }
                        }
                    }
                    entry.name.startsWith("custom_audio/") -> {
                        val fileName = entry.name.removePrefix("custom_audio/")
                        if (fileName.isNotBlank()) {
                            File(audioDir, fileName).outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            val state = loadedState
                ?: throw Exception("Backup file has no state.json — not a valid SpeGen backup.")
            val padded = state.withPaddedLists()
            val resolved = padded.copy(
                menu_list = padded.menu_list.map { menu ->
                    menu.copy(
                        custom_image_paths = menu.custom_image_paths.map { path ->
                            if (path.startsWith("custom_images/")) {
                                val dest = File(imageDir, path.removePrefix("custom_images/"))
                                if (dest.exists()) dest.absolutePath else ""
                            } else path
                        },
                        custom_audio_paths = menu.custom_audio_paths.map { path ->
                            if (path.startsWith("custom_audio/")) {
                                val dest = File(audioDir, path.removePrefix("custom_audio/"))
                                if (dest.exists()) dest.absolutePath else ""
                            } else path
                        }
                    )
                }
            )
            load_vars(resolved)
            linked_menu.value = 0
            switchmenuparser.value++
        }
    }
}

@Composable
actual fun rememberPlatformImagePicker(
    itemKey: String,
    onImagePicked: (path: String) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = copyImageToPrivateStorage(uri, itemKey)
            if (path.isNotBlank()) onImagePicked(path)
        }
    }
    return { launcher.launch(arrayOf("image/*")) }
}

@Composable
actual fun BackupSection(onResult: (success: Boolean, message: String) -> Unit) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) { onResult(false, "Export cancelled."); return@rememberLauncherForActivityResult }
        try { exportToZip(uri); onResult(true, "Exported successfully.") }
        catch (e: Exception) { onResult(false, "Export failed: ${e.message}") }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) { onResult(false, "Import cancelled."); return@rememberLauncherForActivityResult }
        try { importFromZip(uri); onResult(true, "Imported successfully.") }
        catch (e: Exception) { onResult(false, "Import failed: ${e.message}") }
    }

    Button(
        onClick = {
            val ts = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(java.util.Date())
            exportLauncher.launch("spegen_backup_$ts.spegen")
        }
    ) { Text("Export to .zip file") }

    Button(
        onClick = {
            importLauncher.launch(
                arrayOf("application/zip", "application/octet-stream", "application/json", "*/*")
            )
        }
    ) { Text("Import from file") }
}
