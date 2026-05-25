package io.github.hkleinkeane.spegen

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

actual fun cleanOrphanedCustomImages() {
    // Web: custom images are stored as data URIs or object URLs; orphan cleanup is a no-op for now
}

@Composable
actual fun rememberPlatformImagePicker(
    itemKey: String,
    onImagePicked: (path: String) -> Unit
): () -> Unit {
    // Web file picker via <input type=file> — full implementation in a future milestone
    return {}
}

@Composable
actual fun BackupSection(onResult: (success: Boolean, message: String) -> Unit) {
    Button(onClick = { onResult(false, "Backup not yet implemented on web.") }) {
        Text("Export / Import (coming soon)")
    }
}
