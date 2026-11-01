package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fashion_aura_prefs")

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val DARK_MODE = stringPreferencesKey("dark_mode_state") // "system", "dark", "light"
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val USE_BANGLA_DIGITS = booleanPreferencesKey("use_bangla_digits")
        val STEADFAST_API_KEY = stringPreferencesKey("steadfast_api_key")
        val STEADFAST_SECRET_KEY = stringPreferencesKey("steadfast_secret_key")
        val AUTO_SYNC_COURIER = booleanPreferencesKey("auto_sync_courier")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val darkModeStr = preferences[PreferencesKeys.DARK_MODE] ?: "system"
            val darkModeBool = when (darkModeStr) {
                "dark" -> true
                "light" -> false
                else -> null
            }
            val langCode = preferences[PreferencesKeys.APP_LANGUAGE] ?: "bn"
            val appLanguage = if (langCode == "en") AppLanguage.ENGLISH else AppLanguage.BANGLA
            val useBanglaDigits = preferences[PreferencesKeys.USE_BANGLA_DIGITS] ?: true
            val apiKey = preferences[PreferencesKeys.STEADFAST_API_KEY] ?: ""
            val secretKey = preferences[PreferencesKeys.STEADFAST_SECRET_KEY] ?: ""
            val autoSync = preferences[PreferencesKeys.AUTO_SYNC_COURIER] ?: true

            UserPreferences(
                isDarkMode = darkModeBool,
                appLanguage = appLanguage,
                useBanglaDigits = useBanglaDigits,
                steadfastApiKey = apiKey,
                steadfastSecretKey = secretKey,
                autoSyncCourier = autoSync
            )
        }

    suspend fun setDarkMode(isDark: Boolean?) {
        context.dataStore.edit { preferences ->
            val value = when (isDark) {
                true -> "dark"
                false -> "light"
                null -> "system"
            }
            preferences[PreferencesKeys.DARK_MODE] = value
        }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language.code
        }
    }

    suspend fun setUseBanglaDigits(useBangla: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_BANGLA_DIGITS] = useBangla
        }
    }

    suspend fun setSteadfastCredentials(apiKey: String, secretKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STEADFAST_API_KEY] = apiKey.trim()
            preferences[PreferencesKeys.STEADFAST_SECRET_KEY] = secretKey.trim()
        }
    }

    suspend fun setAutoSyncCourier(autoSync: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SYNC_COURIER] = autoSync
        }
    }
}
