package io.github.hkleinkeane.spegen

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SpeGen",
    ) {
        App()
    }
}