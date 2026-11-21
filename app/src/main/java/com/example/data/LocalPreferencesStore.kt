package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
    val accentPaletteRaw: String = AppAccentPalette.ROYAL_BLUE.raw,
    /** IDs dismissed from the in-app Home broadcast banner. */
    val seenBroadcastIds: Set<String> = emptySet(),
    /** IDs that have ALREADY triggered a device status-bar notification (strictly once per device). */
    val notifiedBroadcastIds: Set<String> = emptySet(),
    /** Feedback report IDs whose "Resolved" in-app notification banner has been acknowledged. */
    val acknowledgedResolvedFeedbackIds: Set<String> = emptySet(),
    /** Last app versionName whose "What's New" dialog or initial setup was recorded. */
    val lastSeenAppVersion: String = "",
    /** Last app versionName for which the Feedback Encouragement popup was shown. */
    val lastFeedbackPromptVersion: String = ""
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
    private val keySeenBroadcastIds = stringSetPreferencesKey("seen_broadcast_ids")
    private val keyNotifiedBroadcastIds = stringSetPreferencesKey("notified_broadcast_ids")
    private val keyAckResolvedFeedbackIds = stringSetPreferencesKey("ack_resolved_feedback_ids")
    private val keyLastSeenAppVersion = stringPreferencesKey("last_seen_app_version")
    private val keyLastFeedbackPromptVersion = stringPreferencesKey("last_feedback_prompt_version")

    val prefsFlow: Flow<LocalReminderPrefs> = context.appPrefsDataStore.data.map { prefs ->
        LocalReminderPrefs(
            enabled = prefs[keyReminderEnabled] ?: true,
            hour = prefs[keyReminderHour] ?: AppConstants.DEFAULT_REMINDER_HOUR,
            minute = prefs[keyReminderMinute] ?: AppConstants.DEFAULT_REMINDER_MINUTE,
            lastDismissedDate = prefs[keyDismissedDate] ?: "",
            lastSyncedMillis = prefs[keyLastSynced] ?: 0L,
            sundayPrepEnabled = prefs[keySundayPrepEnabled] ?: true,
            themeModeRaw = prefs[keyThemeMode] ?: AppThemeMode.SYSTEM.raw,
            accentPaletteRaw = prefs[keyAccentPalette] ?: AppAccentPalette.ROYAL_BLUE.raw,
            seenBroadcastIds = prefs[keySeenBroadcastIds] ?: emptySet(),
            notifiedBroadcastIds = prefs[keyNotifiedBroadcastIds] ?: emptySet(),
            acknowledgedResolvedFeedbackIds = prefs[keyAckResolvedFeedbackIds] ?: emptySet(),
            lastSeenAppVersion = prefs[keyLastSeenAppVersion] ?: "",
            lastFeedbackPromptVersion = prefs[keyLastFeedbackPromptVersion] ?: ""
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

    suspend fun markBroadcastsSeen(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.appPrefsDataStore.edit { prefs ->
            val current = prefs[keySeenBroadcastIds]?.toMutableList() ?: mutableListOf()
            ids.forEach { id ->
                if (id.isNotBlank() && id !in current) current.add(id)
            }
            prefs[keySeenBroadcastIds] = current.takeLast(120).toSet()
        }
    }

    suspend fun markBroadcastsNotified(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.appPrefsDataStore.edit { prefs ->
            val current = prefs[keyNotifiedBroadcastIds]?.toMutableList() ?: mutableListOf()
            ids.forEach { id ->
                if (id.isNotBlank() && id !in current) current.add(id)
            }
            prefs[keyNotifiedBroadcastIds] = current.takeLast(150).toSet()
        }
    }

    suspend fun acknowledgeResolvedFeedback(ids: Collection<String>) {
        if (ids.isEmpty()) return
        context.appPrefsDataStore.edit { prefs ->
            val current = prefs[keyAckResolvedFeedbackIds]?.toMutableList() ?: mutableListOf()
            ids.forEach { id ->
                if (id.isNotBlank() && id !in current) current.add(id)
            }
            prefs[keyAckResolvedFeedbackIds] = current.takeLast(120).toSet()
        }
    }

    suspend fun markAppVersionSeen(versionName: String) {
        if (versionName.isBlank()) return
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyLastSeenAppVersion] = versionName.trim()
        }
    }

    suspend fun markFeedbackPromptShownForVersion(versionName: String) {
        if (versionName.isBlank()) return
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyLastFeedbackPromptVersion] = versionName.trim()
        }
    }
}
