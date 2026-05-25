package io.github.hkleinkeane.spegen

import platform.Foundation.NSUserDefaults

actual object AppStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private const val KEY = "spegen_app_state"

    actual suspend fun loadJson(): String? = defaults.stringForKey(KEY)

    actual suspend fun saveJson(json: String) {
        defaults.setObject(json, KEY)
        defaults.synchronize()
    }
}
