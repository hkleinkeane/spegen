package io.github.hkleinkeane.spegen

import java.io.File

actual object AppStorage {
    private val file = File(System.getProperty("user.home"), ".spegen/app_state.json")

    actual suspend fun loadJson(): String? = try {
        if (file.exists()) file.readText() else null
    } catch (e: Exception) {
        println("AppStorage load failed: ${e.message}")
        null
    }

    actual suspend fun saveJson(json: String) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(json)
        } catch (e: Exception) {
            println("AppStorage save failed: ${e.message}")
        }
    }
}
