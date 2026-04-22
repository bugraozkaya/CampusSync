package com.bugra.campussync.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bugra.campussync.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Uygulama düzeyinde tek bir DataStore örneği
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "campus_sync_settings")

class ThemePreferences(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_mode")
        private val LANGUAGE_KEY = stringPreferencesKey("language_code")
    }

    // Kaydedilmiş tema modunu döner (varsayılan: SYSTEM)
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "LIGHT"  -> ThemeMode.LIGHT
            "DARK"   -> ThemeMode.DARK
            else     -> ThemeMode.SYSTEM
        }
    }

    // Kaydedilmiş dil kodunu döner (varsayılan: "tr")
    val languageCode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE_KEY] ?: "tr"
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME_KEY] = mode.name }
    }

    suspend fun setLanguageCode(code: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = code }
    }
}
