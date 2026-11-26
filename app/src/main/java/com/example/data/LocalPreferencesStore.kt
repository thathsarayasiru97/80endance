package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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
    val lastFeedbackPromptVersion: String = "",
    /** Keeps the real-time silent background listener active for instant notifications even when swiped away. */
    val instantBackgroundAlertsEnabled: Boolean = true,
    /** Epoch millis of the most recent successful background announcement check. */
    val lastBackgroundCheckMillis: Long = 0L,
    /** Cached student audience attributes so cold-process background checks work even if profile fetch lags. */
    val cachedUserId: String = "",
    val cachedGrade: String = "",
    val cachedStream: String = "",
    val cachedClassName: String = "",
    val cachedSetupCompleted: Boolean = false,
    /** Fires the daily check-in reminder the first time the user unlocks their phone after 1:45 PM on a school day. */
    val afterSchoolUnlockPromptEnabled: Boolean = false,
    /** Fires a calm once-a-day nudge when attendance sits right on the 80% edge (80%-82% or <=1 safe skip). */
    val dangerZoneNudgeEnabled: Boolean = true,
    val lastUnlockPromptDate: String = "",
    val lastDangerNudgeDate: String = "",
    val lastCelebratedWeekKey: String = "",
    val lastCelebratedStreak: Int = 0,
    val wasBelowThreshold: Boolean = false,
    val cachedNextDayPollEnabled: Boolean = false,
    val glassMaterialPresetRaw: String = GlassMaterialPreset.BALANCED.raw,
    val glassIntensity: Float = GlassMaterialPreset.BALANCED.defaultIntensity,
    val ambientBackdropPresetRaw: String = AmbientBackdropPreset.INDIGO_AURORA.raw,
    val ambientMotionEnabled: Boolean = true,
    val frostedHeroCardsEnabled: Boolean = false,
    val widgetThemeStyleRaw: String = WidgetThemeStyle.AUTO_SYNC.raw,
    val streakArenaOptIn: Boolean = false,
    val streakArenaUseAnonymousAlias: Boolean = false,
    val cheeredPeerTokens: Set<String> = emptySet()
) {
    val themeMode: AppThemeMode get() = AppThemeMode.fromRaw(themeModeRaw)
    val accentPalette: AppAccentPalette get() = AppAccentPalette.fromRaw(accentPaletteRaw)
    val glassMaterialPreset: GlassMaterialPreset get() = GlassMaterialPreset.fromRaw(glassMaterialPresetRaw)
    val ambientBackdropPreset: AmbientBackdropPreset get() = AmbientBackdropPreset.fromRaw(ambientBackdropPresetRaw)
    val widgetThemeStyle: WidgetThemeStyle get() = WidgetThemeStyle.fromRaw(widgetThemeStyleRaw)

    fun toCachedUserProfile(): UserProfile? {
        if (cachedUserId.isBlank() || !cachedSetupCompleted) return null
        return UserProfile(
            id = cachedUserId,
            userId = cachedUserId,
            grade = cachedGrade,
            alStream = cachedStream,
            className = cachedClassName,
            setupCompleted = true
        )
    }
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
    private val keyInstantBgAlerts = booleanPreferencesKey("instant_background_alerts_enabled")
    private val keyLastBgCheckMillis = longPreferencesKey("last_background_check_millis")
    private val keyCachedUserId = stringPreferencesKey("cached_user_id")
    private val keyCachedGrade = stringPreferencesKey("cached_student_grade")
    private val keyCachedStream = stringPreferencesKey("cached_student_stream")
    private val keyCachedClassName = stringPreferencesKey("cached_student_class")
    private val keyCachedSetupCompleted = booleanPreferencesKey("cached_setup_completed")
    private val keyAfterSchoolUnlockPrompt = booleanPreferencesKey("after_school_unlock_prompt_enabled")
    private val keyDangerZoneNudge = booleanPreferencesKey("danger_zone_nudge_enabled")
    private val keyLastUnlockPromptDate = stringPreferencesKey("last_unlock_prompt_date")
    private val keyLastDangerNudgeDate = stringPreferencesKey("last_danger_nudge_date")
    private val keyLastCelebratedWeekKey = stringPreferencesKey("last_celebrated_week_key")
    private val keyLastCelebratedStreak = intPreferencesKey("last_celebrated_streak")
    private val keyWasBelowThreshold = booleanPreferencesKey("was_below_threshold")
    private val keyCachedNextDayPollEnabled = booleanPreferencesKey("cached_next_day_poll_enabled")
    private val keyGlassMaterialPreset = stringPreferencesKey("glass_material_preset")
    private val keyGlassIntensity = floatPreferencesKey("glass_intensity")
    private val keyAmbientBackdropPreset = stringPreferencesKey("ambient_backdrop_preset")
    private val keyAmbientMotionEnabled = booleanPreferencesKey("ambient_motion_enabled")
    private val keyFrostedHeroCardsEnabled = booleanPreferencesKey("frosted_hero_cards_enabled")
    private val keyWidgetThemeStyle = stringPreferencesKey("widget_theme_style")
    private val keyStreakArenaOptIn = booleanPreferencesKey("streak_arena_opt_in")
    private val keyStreakArenaAnon = booleanPreferencesKey("streak_arena_use_anonymous_alias")
    private val keyCheeredPeerTokens = stringSetPreferencesKey("cheered_peer_tokens")

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
            lastFeedbackPromptVersion = prefs[keyLastFeedbackPromptVersion] ?: "",
            instantBackgroundAlertsEnabled = prefs[keyInstantBgAlerts] ?: true,
            lastBackgroundCheckMillis = prefs[keyLastBgCheckMillis] ?: 0L,
            cachedUserId = prefs[keyCachedUserId] ?: "",
            cachedGrade = prefs[keyCachedGrade] ?: "",
            cachedStream = prefs[keyCachedStream] ?: "",
            cachedClassName = prefs[keyCachedClassName] ?: "",
            cachedSetupCompleted = prefs[keyCachedSetupCompleted] ?: false,
            afterSchoolUnlockPromptEnabled = prefs[keyAfterSchoolUnlockPrompt] ?: false,
            dangerZoneNudgeEnabled = prefs[keyDangerZoneNudge] ?: true,
            lastUnlockPromptDate = prefs[keyLastUnlockPromptDate] ?: "",
            lastDangerNudgeDate = prefs[keyLastDangerNudgeDate] ?: "",
            lastCelebratedWeekKey = prefs[keyLastCelebratedWeekKey] ?: "",
            lastCelebratedStreak = prefs[keyLastCelebratedStreak] ?: 0,
            wasBelowThreshold = prefs[keyWasBelowThreshold] ?: false,
            cachedNextDayPollEnabled = prefs[keyCachedNextDayPollEnabled] ?: false,
            glassMaterialPresetRaw = prefs[keyGlassMaterialPreset] ?: GlassMaterialPreset.BALANCED.raw,
            glassIntensity = (prefs[keyGlassIntensity] ?: GlassMaterialPreset.BALANCED.defaultIntensity).coerceIn(0.35f, 1.0f),
            ambientBackdropPresetRaw = prefs[keyAmbientBackdropPreset] ?: AmbientBackdropPreset.INDIGO_AURORA.raw,
            ambientMotionEnabled = prefs[keyAmbientMotionEnabled] ?: true,
            frostedHeroCardsEnabled = prefs[keyFrostedHeroCardsEnabled] ?: false,
            widgetThemeStyleRaw = prefs[keyWidgetThemeStyle] ?: WidgetThemeStyle.AUTO_SYNC.raw,
            streakArenaOptIn = prefs[keyStreakArenaOptIn] ?: false,
            streakArenaUseAnonymousAlias = prefs[keyStreakArenaAnon] ?: false,
            cheeredPeerTokens = prefs[keyCheeredPeerTokens] ?: emptySet()
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

    suspend fun saveInstantBackgroundAlertsEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyInstantBgAlerts] = enabled
        }
    }

    suspend fun markBackgroundCheckNow() {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyLastBgCheckMillis] = System.currentTimeMillis()
        }
    }

    suspend fun saveCachedStudentAudience(
        userId: String,
        grade: String,
        stream: String,
        className: String,
        setupCompleted: Boolean
    ) {
        if (userId.isBlank()) return
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyCachedUserId] = userId.trim()
            prefs[keyCachedGrade] = grade.trim()
            prefs[keyCachedStream] = stream.trim()
            prefs[keyCachedClassName] = className.trim()
            prefs[keyCachedSetupCompleted] = setupCompleted
        }
    }

    suspend fun clearCachedStudentAudience() {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyCachedUserId] = ""
            prefs[keyCachedGrade] = ""
            prefs[keyCachedStream] = ""
            prefs[keyCachedClassName] = ""
            prefs[keyCachedSetupCompleted] = false
        }
    }

    suspend fun saveAfterSchoolUnlockPromptEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyAfterSchoolUnlockPrompt] = enabled
        }
    }

    suspend fun saveDangerZoneNudgeEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyDangerZoneNudge] = enabled
        }
    }

    suspend fun markUnlockPromptFiredForDate(dateKey: String) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyLastUnlockPromptDate] = dateKey
        }
    }

    suspend fun markDangerNudgeFiredForDate(dateKey: String) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyLastDangerNudgeDate] = dateKey
        }
    }

    suspend fun markCelebratedWeek(weekKey: String) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyLastCelebratedWeekKey] = weekKey
        }
    }

    suspend fun markCelebratedStreak(streak: Int) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyLastCelebratedStreak] = streak
        }
    }

    suspend fun saveWasBelowThreshold(below: Boolean) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyWasBelowThreshold] = below
        }
    }

    suspend fun saveCachedNextDayPollEnabled(enabled: Boolean) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyCachedNextDayPollEnabled] = enabled
        }
    }

    suspend fun saveLiquidGlassStyle(
        preset: GlassMaterialPreset,
        intensity: Float,
        ambientBackdropPreset: AmbientBackdropPreset,
        ambientMotionEnabled: Boolean,
        frostedHeroCardsEnabled: Boolean
    ) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyGlassMaterialPreset] = preset.raw
            prefs[keyGlassIntensity] = intensity.coerceIn(0.35f, 1.0f)
            prefs[keyAmbientBackdropPreset] = ambientBackdropPreset.raw
            prefs[keyAmbientMotionEnabled] = ambientMotionEnabled
            prefs[keyFrostedHeroCardsEnabled] = frostedHeroCardsEnabled
        }
    }

    suspend fun saveWidgetThemeStyle(style: WidgetThemeStyle) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyWidgetThemeStyle] = style.raw
        }
    }

    suspend fun saveStreakArenaPreferences(optedIn: Boolean, useAnonymousAlias: Boolean) {
        context.appPrefsDataStore.edit { prefs ->
            prefs[keyStreakArenaOptIn] = optedIn
            prefs[keyStreakArenaAnon] = useAnonymousAlias
        }
    }

    suspend fun markPeerCheeredToday(dateKey: String, targetUserId: String) {
        val token = "$dateKey:$targetUserId"
        context.appPrefsDataStore.edit { prefs ->
            val current = prefs[keyCheeredPeerTokens]
                ?.filter { it.startsWith("$dateKey:") }
                ?.toMutableSet()
                ?: mutableSetOf()
            current.add(token)
            prefs[keyCheeredPeerTokens] = current.take(80).toSet()
        }
    }
}

