package io.github.hkleinkeane.spegen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSFileManager

actual fun cleanOrphanedCustomImages() {
    val imageDir = "${NSHomeDirectory()}/Documents/custom_images"
    val manager = NSFileManager.defaultManager
    val files = manager.contentsOfDirectoryAtPath(imageDir, error = null) ?: return
    val referenced = MenuList
        .flatMap { it.custom_image_paths }
        .filter { it.isNotBlank() }
        .map { it.substringAfterLast('/') }
        .toSet()
    @Suppress("UNCHECKED_CAST")
    (files as List<String>).forEach { name ->
        if (name !in referenced) {
            manager.removeItemAtPath("$imageDir/$name", error = null)
        }
    }
}

@Composable
actual fun rememberPlatformImagePicker(
    itemKey: String,
    onImagePicked: (path: String) -> Unit
): () -> Unit {
    // iOS image picker requires UIImagePickerController / PHPickerViewController
    // Full implementation in a future milestone
    return {}
}

@Composable
actual fun BackupSection(onResult: (success: Boolean, message: String) -> Unit) {
    Button(onClick = { onResult(false, "Backup not yet implemented on iOS.") }) {
        Text("Export / Import (coming soon)")
    }
}
