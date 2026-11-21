package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "eightyendance_prefs"
)

data class LocalReminderPrefs(
    val enabled: Boolean = true,
    val hour: Int = AppConstants.DEFAULT_REMINDER_HOUR,
    val minute: Int = AppConstants.DEFAULT_REMINDER_MINUTE,
    val lastDismissedDate: String = "",
    val lastSyncedMillis: Long = 0L,
    val sundayPrepEnabled: Boolean = true,
    val themeModeRaw: String = AppThemeMode.SYSTEM.raw,
    val accentPaletteRaw: String = AppAccentPalette.ROYAL_BLUE.raw
) {
    val themeMode: AppThemeMode get() = AppThemeMode.fromRaw(themeModeRaw)
    val accentPalette: AppAccentPalette get() = AppAccentPalette.fromRaw(accentPaletteRaw)
}

class LocalPreferencesStore(private val context: Context) {
    private val keyReminderEnabled = booleanPreferencesKey("reminder_enabled")
    private val keyReminderHour = intPreferencesKey("reminder_hour")
    private val keyReminderMinute = intPreferencesKey("reminder_minute")
    private val keyDismissedDate = stringPreferencesKey("last_dismissed_date")
    private val keyLastSynced = longPreferencesKey("last_synced_millis")
    private val keySundayPrepEnabled = booleanPreferencesKey("sunday_prep_enabled")
    private val keyThemeMode = stringPreferencesKey("app_theme_mode")
    private val keyAccentPalette = stringPreferencesKey("app_accent_palette")

    val prefsFlow: Flow<LocalReminderPrefs> = context.appPrefsDataStore.data.map { prefs ->
        LocalReminderPrefs(
            enabled = prefs[keyReminderEnabled] ?: true,
            hour = prefs[keyReminderHour] ?: AppConstants.DEFAULT_REMINDER_HOUR,
            minute = prefs[keyReminderMinute] ?: AppConstants.DEFAULT_REMINDER_MINUTE,
            lastDismissedDate = prefs[keyDismissedDate] ?: "",
            lastSyncedMillis = prefs[keyLastSynced] ?: 0L,
            sundayPrepEnabled = prefs[keySundayPrepEnabled] ?: true,
            themeModeRaw = prefs[keyThemeMode] ?: AppThemeMode.SYSTEM.raw,
            accentPaletteRaw = prefs[keyAccentPalette] ?: AppAccentPalette.ROYAL_BLUE.raw
        )
    }

    suspend fun getSnapshot(): LocalReminderPrefs = prefsFlow.first()

    suspend fun saveReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyReminderEnabled] = enabled
            prefs[keyReminderHour] = hour
            prefs[keyReminderMinute] = minute
        }
    }

    suspend fun saveSundayPrepEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keySundayPrepEnabled] = enabled
        }
    }

    suspend fun saveAppearance(themeMode: AppThemeMode, accentPalette: AppAccentPalette) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyThemeMode] = themeMode.raw
            prefs[keyAccentPalette] = accentPalette.raw
        }
    }

    suspend fun markPromptDismissedForDate(dateKey: String) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyDismissedDate] = dateKey
        }
    }

    suspend fun markSyncedNow() {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyLastSynced] = System.currentTimeMillis()
        }
    }
}
