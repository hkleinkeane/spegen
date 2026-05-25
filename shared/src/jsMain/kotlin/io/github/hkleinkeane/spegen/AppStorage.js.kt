package io.github.hkleinkeane.spegen

import kotlinx.browser.localStorage

actual object AppStorage {
    private const val KEY = "spegen_app_state"

    actual suspend fun loadJson(): String? = localStorage.getItem(KEY)

    actual suspend fun saveJson(json: String) { localStorage.setItem(KEY, json) }
}
