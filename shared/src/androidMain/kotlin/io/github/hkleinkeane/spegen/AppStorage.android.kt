package io.github.hkleinkeane.spegen

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import android.content.Context

private val Context.spegenDatastore by preferencesDataStore(name = "spegen_settings")
private val APP_STATE_KEY = stringPreferencesKey("app_state")

actual object AppStorage {
    actual suspend fun loadJson(): String? {
        val prefs = androidAppContext.spegenDatastore.data.first()
        return prefs[APP_STATE_KEY]
    }
    actual suspend fun saveJson(json: String) {
        androidAppContext.spegenDatastore.edit { prefs ->
            prefs[APP_STATE_KEY] = json
        }
    }
}
