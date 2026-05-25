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

/** Delete custom image files that are no longer referenced by any menu item. */
expect fun cleanOrphanedCustomImages()

/**
 * Platform file picker that returns the destination path of the copied image, or "" on cancel/error.
 * [itemKey] is the stable UUID used as the filename stem.
 */
@Composable
expect fun rememberPlatformImagePicker(
    itemKey: String,
    onImagePicked: (path: String) -> Unit
): () -> Unit

/**
 * Backup section composable — contains platform-specific file-picker UI for export/import.
 * Calls [onResult] with success flag and a user-facing status message.
 */
@Composable
expect fun BackupSection(onResult: (success: Boolean, message: String) -> Unit)
