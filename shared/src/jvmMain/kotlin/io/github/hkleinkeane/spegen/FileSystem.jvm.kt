package io.github.hkleinkeane.spegen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.io.File

actual fun cleanOrphanedCustomImages() {
    val imageDir = File(System.getProperty("user.home"), ".spegen/custom_images")
    if (!imageDir.exists()) return
    val referenced = MenuList
        .flatMap { it.custom_image_paths }
        .filter { it.isNotBlank() }
        .map { File(it).name }
        .toSet()
    imageDir.listFiles()?.forEach { file ->
        if (file.name !in referenced) file.delete()
    }
}

@Composable
actual fun rememberPlatformImagePicker(
    itemKey: String,
    onImagePicked: (path: String) -> Unit
): () -> Unit {
    // Desktop file picker via AWT
    return {
        val chooser = javax.swing.JFileChooser()
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
            "Image files", "jpg", "jpeg", "png", "webp", "gif"
        )
        if (chooser.showOpenDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
            val src = chooser.selectedFile
            val destDir = File(System.getProperty("user.home"), ".spegen/custom_images")
                .also { it.mkdirs() }
            val dest = File(destDir, "$itemKey.${src.extension}")
            src.copyTo(dest, overwrite = true)
            onImagePicked(dest.absolutePath)
        }
    }
}

@Composable
actual fun BackupSection(onResult: (success: Boolean, message: String) -> Unit) {
    Button(onClick = { onResult(false, "Backup not yet implemented on desktop.") }) {
        Text("Export / Import (coming soon)")
    }
}
