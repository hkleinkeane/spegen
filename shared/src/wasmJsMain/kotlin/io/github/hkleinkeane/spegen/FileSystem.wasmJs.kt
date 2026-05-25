package io.github.hkleinkeane.spegen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

actual fun cleanOrphanedCustomImages() {}

@Composable
actual fun rememberPlatformImagePicker(
    itemKey: String,
    onImagePicked: (path: String) -> Unit
): () -> Unit = {}

@Composable
actual fun BackupSection(onResult: (success: Boolean, message: String) -> Unit) {
    Button(onClick = { onResult(false, "Backup not yet implemented on web.") }) {
        Text("Export / Import (coming soon)")
    }
}
