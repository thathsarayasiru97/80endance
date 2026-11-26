package com.example.ui.settings

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.AmbientBackdropPreset
import com.example.data.AppAccentPalette
import com.example.data.AppThemeMode
import com.example.data.DateUtils
import com.example.data.FeedbackType
import com.example.data.GlassMaterialPreset
import com.example.data.WidgetThemeStyle
import com.example.system.AppUpdateChecker
import com.example.system.AttendanceWidgetProvider
import com.example.system.ReminderScheduler
import com.example.system.UpdateCheckStatusSummary
import com.example.ui.components.BubbleLookPreset
import com.example.ui.components.DevelopedByAtayaFooter
import com.example.ui.components.FeedbackReportDialog
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.LiquidGlassSegmentOption
import com.example.ui.components.LiquidSlider
import com.example.ui.components.LiquidToggle
import com.example.ui.components.LocalGlassBackdrop
import com.example.ui.components.MorphingThemeToggleButton
import com.example.ui.components.NoticeBoardSheet
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfilePhotoDialog
import com.example.ui.components.SlidableLiquidGlassSegmentedBar
import com.example.ui.components.SyncStatusPill
import com.example.ui.components.ThinFilmBubbleInteractiveCard
import com.example.ui.reports.RegisterVerificationDialog
import com.example.ui.theme.LocalAttendanceColors
import com.example.ui.theme.accentSwatchColor
import com.example.viewmodel.StudentAppState
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: StudentAppState,
    isVerifiedAdmin: Boolean,
    onEditProfile: () -> Unit,
    onUpdatePreferences: (enabled: Boolean, hour: Int, minute: Int, countExcused: Boolean) -> Unit,
    onPreviewCheckInPopup: () -> Unit,
    onManualSync: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit,
    onSaveCustomAvatar: (String?) -> Unit = {},
    onOpenBaselineImport: () -> Unit = {},
    onClearBaseline: () -> Unit = {},
    onOpenPeriodEntry: (() -> Unit)? = null,
    onToggleSundayPrep: (Boolean) -> Unit = {},
    onToggleAfterSchoolUnlockPrompt: (Boolean) -> Unit = {},
    onToggleDangerZoneNudge: (Boolean) -> Unit = {},
    onToggleInstantBackgroundAlerts: (Boolean) -> Unit = {},
    onCheckBackgroundNotificationsNow: () -> Unit = {},
    isCheckingBackgroundAlerts: Boolean = false,
    onUpdateAppearance: (AppThemeMode, AppAccentPalette) -> Unit = { _, _ -> },
    onUpdateLiquidGlassStyle: (
        preset: GlassMaterialPreset,
        intensity: Float,
        ambientBackdropPreset: AmbientBackdropPreset,
        ambientMotionEnabled: Boolean,
        frostedHeroCardsEnabled: Boolean
    ) -> Unit = { _, _, _, _, _ -> },
    onUpdateWidgetThemeStyle: (WidgetThemeStyle) -> Unit = {},
    onSubmitFeedback: (type: FeedbackType, title: String, message: String, onDone: () -> Unit) -> Unit = { _, _, _, _ -> },
    isSubmittingFeedback: Boolean = false,
    isCheckingUpdates: Boolean = false,
    updateStatusSummary: UpdateCheckStatusSummary? = null,
    onCheckForUpdates: () -> Unit = {},
    onOpenGitHubReleases: () -> Unit = {},
    bottomBarPadding: Dp = 24.dp
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val profile = state.profile
    val reminderEnabled = profile?.reminderEnabled ?: true
    val reminderHour = profile?.reminderHour ?: 14
    val reminderMinute = profile?.reminderMinute ?: 0
    val countExcused = profile?.countExcusedAsPresent ?: true

    var showTimePicker by remember { mutableStateOf(false) }
    var showNoticeSheet by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showRegisterVerify by remember { mutableStateOf(false) }
    var feedbackDialogInitialType by remember { mutableStateOf<FeedbackType?>(null) }

    var notificationsPermitted by remember {
        mutableStateOf(ReminderScheduler.hasNotificationPermission(context))
    }
    var batteryUnrestricted by remember {
        mutableStateOf(isBatteryOptimizationIgnored(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsPermitted = ReminderScheduler.hasNotificationPermission(context)
                batteryUnrestricted = isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        notificationsPermitted = ReminderScheduler.hasNotificationPermission(context)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("settings_lazy_column"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = bottomBarPadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Student Academic Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                ProfileAvatar(
                                    customAvatarBase64 = profile?.customAvatarBase64,
                                    googlePhotoUrl = state.googlePhotoUrl,
                                    displayName = profile?.fullName ?: "Student",
                                    size = 56.dp,
                                    showEditBadge = true,
                                    onClick = { showPhotoDialog = true },
                                    modifier = Modifier.testTag("settings_profile_avatar")
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = profile?.fullName ?: "Student",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            profile?.grade,
                                            profile?.className?.takeIf { it.isNotBlank() },
                                            profile?.alStream
                                        ).joinToString(" • "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Tap photo to change",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            TextButton(
                                onClick = onEditProfile,
                                modifier = Modifier.testTag("settings_edit_profile_button")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit")
                            }
                        }

                        if (!profile?.subjects.isNullOrEmpty()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = "Subjects: ${profile.subjects.joinToString(" • ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if ((profile?.priorPresentCount ?: 0) > 0 || (profile?.priorAbsentCount ?: 0) > 0) {
                            val lockSuffix = if (profile?.hasLockedBaseline == true) {
                                " (${DateUtils.formatReadableShort(profile.baselineStartDate!!)} – ${DateUtils.formatReadableShort(profile.baselineEndDate!!)} locked)"
                            } else {
                                ""
                            }
                            Text(
                                text = "Starting Balance: ${profile?.priorPresentCount} present, ${profile?.priorAbsentCount} absent$lockSuffix",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Import Attendance % From Day 1 Card
            item {
                val hasLocked = profile?.hasLockedBaseline == true
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_import_baseline_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = if (hasLocked) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (hasLocked) Icons.Rounded.Lock else Icons.Rounded.Percent,
                                        contentDescription = "Import attendance percentage",
                                        tint = if (hasLocked) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Import Attendance Percentage",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (hasLocked) {
                                        val pctText = profile.baselinePercent?.let {
                                            String.format(java.util.Locale.US, "%.1f%%", it)
                                        } ?: ""
                                        "Imported $pctText (${profile.priorPresentCount}P / ${profile.priorAbsentCount}A) • ${DateUtils.formatReadableShort(profile.baselineStartDate!!)} → ${DateUtils.formatReadableShort(profile.baselineEndDate!!)} locked"
                                    } else {
                                        "Enter your % from Day 1 up to a past date (e.g., from your class teacher). Included days are locked automatically."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (hasLocked) {
                                OutlinedButton(
                                    onClick = onClearBaseline,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_unlock_baseline_button")
                                ) {
                                    Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unlock")
                                }
                            }
                            LiquidGlassButton(
                                onClick = onOpenBaselineImport,
                                tint = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.primary,
                                minHeight = 48.dp,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("settings_import_baseline_button")
                            ) {
                                Icon(Icons.Rounded.Percent, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (hasLocked) "Update %" else "Enter Attendance %")
                            }
                        }

                        if (onOpenPeriodEntry != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            OutlinedButton(
                                onClick = onOpenPeriodEntry,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_lock_period_button")
                            ) {
                                Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (state.periodBlocks.isEmpty()) {
                                        "Lock a Custom Time Period (Days Missed / %)"
                                    } else {
                                        "Add Locked Time Period (${state.periodBlocks.size} active)"
                                    }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OutlinedButton(
                            onClick = { showRegisterVerify = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_verify_register_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify Against Class Register")
                        }
                    }
                }
            }

            // Daily Check-In Reminder Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Check-In Reminder",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Notification & popup on school days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LiquidToggle(
                                selected = { reminderEnabled },
                                onSelect = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onUpdatePreferences(enabled, reminderHour, reminderMinute, countExcused)
                                }
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                enabled = reminderEnabled,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(DateUtils.formatTime12Hour(reminderHour, reminderMinute))
                            }
                            LiquidGlassButton(
                                onClick = onPreviewCheckInPopup,
                                tint = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                minHeight = 48.dp,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_preview_popup_button")
                            ) {
                                Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Popup")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sunday Evening Weekly Digest",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Sunday 7:00 PM recap: last week's X/5, overall % trend, and next holiday",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            LiquidToggle(
                                selected = { state.sundayPrepEnabled },
                                onSelect = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onToggleSundayPrep(enabled)
                                },
                                modifier = Modifier.testTag("settings_sunday_prep_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "After-School Unlock Prompt",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Remind me on my first phone unlock after 1:45 PM on school days (instead of buzzing during class)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            LiquidToggle(
                                selected = { state.afterSchoolUnlockPromptEnabled },
                                onSelect = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onToggleAfterSchoolUnlockPrompt(enabled)
                                },
                                modifier = Modifier.testTag("settings_unlock_prompt_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "80% Edge Smart Nudge",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Only nudges you when sitting right on the edge (80%–82% or 1 safe skip left)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            LiquidToggle(
                                selected = { state.dangerZoneNudgeEnabled },
                                onSelect = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onToggleDangerZoneNudge(enabled)
                                },
                                modifier = Modifier.testTag("settings_danger_nudge_switch")
                            )
                        }
                    }
                }
            }

            // Background Notifications Control & Reliability Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_background_notifications_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = if (notificationsPermitted) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (notificationsPermitted) {
                                                Icons.Rounded.Bolt
                                            } else {
                                                Icons.Rounded.NotificationsOff
                                            },
                                            contentDescription = "Background notifications",
                                            tint = if (notificationsPermitted) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Background Notifications",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = when {
                                            !notificationsPermitted -> "Notifications are blocked on this device"
                                            state.instantBackgroundAlertsEnabled -> "Live listener + periodic wakeup active"
                                            else -> "Periodic background checks active"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (!notificationsPermitted) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                            Surface(
                                color = when {
                                    !notificationsPermitted -> MaterialTheme.colorScheme.errorContainer
                                    state.instantBackgroundAlertsEnabled -> Color(0xFF1B8A4B).copy(alpha = 0.16f)
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = when {
                                        !notificationsPermitted -> "Blocked"
                                        state.instantBackgroundAlertsEnabled -> "Instant"
                                        else -> "Periodic"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        !notificationsPermitted -> MaterialTheme.colorScheme.onErrorContainer
                                        state.instantBackgroundAlertsEnabled -> Color(0xFF15733E)
                                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (!notificationsPermitted) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Allow notifications so announcements pop up when the app is closed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            openAppNotificationSettings(context)
                                        },
                                        modifier = Modifier.testTag("settings_allow_notifications_button")
                                    ) {
                                        Text("Allow")
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Instant Background Alerts",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Keeps a silent listener running so school & class announcements arrive within seconds even after closing 80endance from Recent Apps.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            LiquidToggle(
                                selected = { state.instantBackgroundAlertsEnabled },
                                onSelect = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onToggleInstantBackgroundAlerts(enabled)
                                },
                                modifier = Modifier.testTag("settings_instant_bg_alerts_switch")
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Battery optimization status & OEM guidance
                        Surface(
                            color = if (batteryUnrestricted) {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BatteryChargingFull,
                                    contentDescription = "Battery optimization",
                                    tint = if (batteryUnrestricted) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.tertiary
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (batteryUnrestricted) {
                                            "Battery: Unrestricted (Best)"
                                        } else {
                                            "Battery: Restricted / Optimized"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (batteryUnrestricted) {
                                            "Android won't kill background alerts when you swipe the app away."
                                        } else {
                                            "On Xiaomi, Samsung, Oppo & Vivo, open Battery Settings and choose \"Unrestricted\" so swiped apps keep receiving alerts."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { openAppBatterySettings(context) },
                                    modifier = Modifier.testTag("settings_battery_settings_button")
                                ) {
                                    Text("Battery")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val lastCheckLabel = if (state.lastBackgroundCheckMillis > 0L) {
                                "Last checked: ${DateUtils.formatClockTime(state.lastBackgroundCheckMillis)}"
                            } else {
                                "Ready for background checks"
                            }
                            Text(
                                text = lastCheckLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = onCheckBackgroundNotificationsNow,
                                enabled = !isCheckingBackgroundAlerts,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("settings_check_bg_now_button")
                            ) {
                                if (isCheckingBackgroundAlerts) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isCheckingBackgroundAlerts) "Checking…" else "Check Now")
                            }
                        }
                    }
                }
            }

            // Appearance: Theme Mode (incl. True Black OLED) & Accent Palette Picker
            item {
                val systemDark = isSystemInDarkTheme()
                val previewDark = when (state.themeMode) {
                    AppThemeMode.SYSTEM -> systemDark
                    AppThemeMode.LIGHT -> false
                    AppThemeMode.DARK, AppThemeMode.OLED_BLACK -> true
                }
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_appearance_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Palette,
                                            contentDescription = "Appearance and theme",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Appearance & Accent", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Choose light, dark, or true-black OLED + custom accent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            MorphingThemeToggleButton(
                                isDarkTheme = previewDark,
                                onToggle = {
                                    val nextMode = if (previewDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                                    onUpdateAppearance(nextMode, state.accentPalette)
                                },
                                modifier = Modifier.testTag("settings_morphing_theme_toggle")
                            )
                        }

                        val themeModeOptions = remember {
                            AppThemeMode.entries.map { mode ->
                                LiquidGlassSegmentOption(
                                    label = mode.label,
                                    testTag = "theme_mode_${mode.raw}"
                                )
                            }
                        }
                        SlidableLiquidGlassSegmentedBar(
                            options = themeModeOptions,
                            selectedIndex = AppThemeMode.entries.indexOf(state.themeMode).coerceAtLeast(0),
                            onOptionSelected = { idx ->
                                AppThemeMode.entries.getOrNull(idx)?.let { mode ->
                                    onUpdateAppearance(mode, state.accentPalette)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_theme_mode_row")
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppAccentPalette.entries.forEach { palette ->
                                val selected = state.accentPalette == palette
                                val swatchColor = accentSwatchColor(palette, previewDark)
                                val scale by animateFloatAsState(
                                    targetValue = if (selected) 1.08f else 1.0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "swatch_scale_${palette.raw}"
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onUpdateAppearance(state.themeMode, palette) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .testTag("accent_swatch_${palette.raw}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            }
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(swatchColor)
                                            .border(
                                                width = if (selected) 3.dp else 1.dp,
                                                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            )
                                    ) {
                                        androidx.compose.animation.AnimatedVisibility(
                                            visible = selected,
                                            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                                            exit = scaleOut()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = "${palette.label} selected",
                                                tint = if (previewDark) Color.Black else Color.White,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = palette.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Liquid Glass 2.0 Personalization Studio
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Liquid Glass Studio",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val materialPresetOptions = remember {
                            GlassMaterialPreset.entries.map { preset ->
                                LiquidGlassSegmentOption(
                                    label = preset.label,
                                    testTag = "glass_preset_${preset.raw}"
                                )
                            }
                        }
                        SlidableLiquidGlassSegmentedBar(
                            options = materialPresetOptions,
                            selectedIndex = GlassMaterialPreset.entries.indexOf(state.glassMaterialPreset).coerceAtLeast(0),
                            onOptionSelected = { idx ->
                                GlassMaterialPreset.entries.getOrNull(idx)?.let { preset ->
                                    onUpdateLiquidGlassStyle(
                                        preset,
                                        preset.defaultIntensity,
                                        state.ambientBackdropPreset,
                                        state.ambientMotionEnabled,
                                        state.frostedHeroCardsEnabled
                                    )
                                }
                            },
                            barHeight = 42.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_glass_preset_bar")
                        )

                        // Live Glass Intensity Slider
                        val glassBackdrop = LocalGlassBackdrop.current
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Glass Refraction & Frost",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(state.glassIntensity * 100f).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (glassBackdrop != null) {
                                LiquidSlider(
                                    value = { state.glassIntensity },
                                    onValueChange = { nextVal ->
                                        onUpdateLiquidGlassStyle(
                                            state.glassMaterialPreset,
                                            nextVal.coerceIn(0f, 1f),
                                            state.ambientBackdropPreset,
                                            state.ambientMotionEnabled,
                                            state.frostedHeroCardsEnabled
                                        )
                                    },
                                    valueRange = 0f..1f,
                                    visibilityThreshold = 0.01f,
                                    backdrop = glassBackdrop,
                                    accentColorOverride = MaterialTheme.colorScheme.primary,
                                    snapToStepsOnRelease = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_glass_intensity_slider")
                                )
                            }
                        }

                        // 5 Ambient Light-Mesh Backdrops
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Ambient Light-Mesh Backdrop",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AmbientBackdropPreset.entries.forEach { preset ->
                                    val isSelected = state.ambientBackdropPreset == preset
                                    val swatchGradient = when (preset) {
                                        AmbientBackdropPreset.INDIGO_AURORA -> listOf(Color(0xFF3B82F6), Color(0xFFF59E0B))
                                        AmbientBackdropPreset.SUNSET_SAFFRON -> listOf(Color(0xFFF59E0B), Color(0xFFFB7185))
                                        AmbientBackdropPreset.EMERALD_LAGOON -> listOf(Color(0xFF10B981), Color(0xFF06B6D4))
                                        AmbientBackdropPreset.MIDNIGHT_PRISM -> listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                        AmbientBackdropPreset.PURE_MINIMAL -> listOf(Color(0xFF94A3B8), Color(0xFF64748B))
                                    }
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(50)
                                            )
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                onUpdateLiquidGlassStyle(
                                                    state.glassMaterialPreset,
                                                    state.glassIntensity,
                                                    preset,
                                                    state.ambientMotionEnabled,
                                                    state.frostedHeroCardsEnabled
                                                )
                                            }
                                            .testTag("ambient_preset_${preset.raw}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Brush.linearGradient(swatchGradient))
                                            )
                                            Text(
                                                text = preset.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Breathing Ambient Motion Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Breathing Ambient Motion", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Subtle 120Hz light-mesh drift behind glass surfaces",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LiquidToggle(
                                selected = { state.ambientMotionEnabled },
                                onSelect = { enabled ->
                                    onUpdateLiquidGlassStyle(
                                        state.glassMaterialPreset,
                                        state.glassIntensity,
                                        state.ambientBackdropPreset,
                                        enabled,
                                        state.frostedHeroCardsEnabled
                                    )
                                },
                                modifier = Modifier.testTag("settings_ambient_motion_toggle")
                            )
                        }

                        // Frosted Glass Hero Cards Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("Frosted Glass Hero Cards", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Apply refractive glass & specular rims to main summary cards",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LiquidToggle(
                                selected = { state.frostedHeroCardsEnabled },
                                onSelect = { enabled ->
                                    onUpdateLiquidGlassStyle(
                                        state.glassMaterialPreset,
                                        state.glassIntensity,
                                        state.ambientBackdropPreset,
                                        state.ambientMotionEnabled,
                                        enabled
                                    )
                                },
                                modifier = Modifier.testTag("settings_frosted_hero_cards_toggle")
                            )
                        }
                    }
                }
            }

            // Home Screen Widget Studio (Liquid Glass Widget Theme Sync + 1-Tap Pin)
            item(key = "settings_widget_studio_card") {
                val semantic = LocalAttendanceColors.current
                val widgetStyleOptions = remember {
                    WidgetThemeStyle.entries.map { style ->
                        LiquidGlassSegmentOption(
                            label = when (style) {
                                WidgetThemeStyle.AUTO_SYNC -> "Auto"
                                WidgetThemeStyle.FROSTED_LIGHT -> "Light"
                                WidgetThemeStyle.SMOKED_DARK -> "Dark"
                                WidgetThemeStyle.ACCENT_TINT -> "Accent"
                            },
                            testTag = "widget_style_${style.raw}"
                        )
                    }
                }
                val isPreviewLight = state.widgetThemeStyle == WidgetThemeStyle.FROSTED_LIGHT ||
                    (state.widgetThemeStyle == WidgetThemeStyle.AUTO_SYNC && state.themeMode == AppThemeMode.LIGHT)
                val previewGradient = when (state.widgetThemeStyle) {
                    WidgetThemeStyle.FROSTED_LIGHT -> listOf(Color(0xFFEEF4FF), Color(0xFFE2ECFF))
                    WidgetThemeStyle.SMOKED_DARK -> listOf(Color(0xFF0B1120), Color(0xFF141C2E))
                    WidgetThemeStyle.ACCENT_TINT -> listOf(Color(0xFF1D3B8A), Color(0xFF3B288A))
                    WidgetThemeStyle.AUTO_SYNC -> if (isPreviewLight) {
                        listOf(Color(0xFFEEF4FF), Color(0xFFE2ECFF))
                    } else {
                        listOf(Color(0xFF102347), Color(0xFF162E5E))
                    }
                }
                val previewPrimaryText = if (isPreviewLight) Color(0xFF0F172A) else Color.White
                val previewSubText = if (isPreviewLight) Color(0xFF475569) else Color(0xFFBFD7FF)

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_widget_studio_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Widgets,
                                        contentDescription = "Home Widget Studio",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Home Widget Studio", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "1-tap Present / Absent / Medical, 15s Undo & Evening Poll right on your home screen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Live miniature Smart Widget preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.linearGradient(previewGradient))
                                .border(
                                    width = 1.dp,
                                    color = if (isPreviewLight) Color.White else Color.White.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "80endance • ${state.activeTerm.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = previewPrimaryText.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = "${state.currentStreak}d streak",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = previewPrimaryText
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", state.currentTermStats.percentage),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (state.currentTermStats.eligible) {
                                            if (isPreviewLight) Color(0xFF0F766E) else Color(0xFF34D399)
                                        } else {
                                            if (isPreviewLight) Color(0xFFDC2626) else Color(0xFFFB7185)
                                        }
                                    )
                                    Column {
                                        Text(
                                            text = if (state.currentTermStats.eligible) "Eligible (>= ${state.currentTermStats.thresholdPercent}%)" else "Below ${state.currentTermStats.thresholdPercent}%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = previewPrimaryText
                                        )
                                        Text(
                                            text = "1-tap check-in • Evening Poll ready",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = previewSubText
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        color = Color(0xFF15803D),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.1f)
                                    ) {
                                        Text(
                                            text = "Present ✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    Surface(
                                        color = Color.White.copy(alpha = if (isPreviewLight) 0.55f else 0.16f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Absent ✕",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = previewPrimaryText,
                                            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFFD97706),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(0.95f)
                                    ) {
                                        Text(
                                            text = "Medical",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(vertical = 6.dp).fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        SlidableLiquidGlassSegmentedBar(
                            options = widgetStyleOptions,
                            selectedIndex = WidgetThemeStyle.entries.indexOf(state.widgetThemeStyle).coerceAtLeast(0),
                            onOptionSelected = { idx ->
                                WidgetThemeStyle.entries.getOrNull(idx)?.let(onUpdateWidgetThemeStyle)
                            },
                            barHeight = 42.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_widget_style_bar")
                        )

                        LiquidGlassButton(
                            onClick = {
                                val manager = AppWidgetManager.getInstance(context)
                                val provider = ComponentName(context, AttendanceWidgetProvider::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
                                    manager.requestPinAppWidget(provider, null, null)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Long-press your home screen and add the 80endance widget.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            tint = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.primary,
                            minHeight = 48.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_pin_smart_widget_button")
                        ) {
                            Icon(Icons.Rounded.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pin Smart Widget to Home Screen")
                        }
                    }
                }
            }

            // Offline Cache & Cloud Sync + Notices Archive
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Offline Cache & Cloud Sync", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Works offline; pending entries sync automatically in background.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            SyncStatusPill(syncStatus = state.syncStatus, onClick = onManualSync)
                        }

                        LiquidGlassButton(
                            onClick = onManualSync,
                            tint = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.primary,
                            minHeight = 48.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sync Now")
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNoticeSheet = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Campaign,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Notice Board Archive", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${state.visibleNotices.size} school & class updates",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isCheckingUpdates, onClick = onCheckForUpdates)
                                    .padding(vertical = 4.dp)
                                    .testTag("settings_check_updates_button"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SystemUpdate,
                                        contentDescription = stringResource(R.string.update_check_title),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        val ctx = LocalContext.current
                                        val installedVersion = remember(ctx) { AppUpdateChecker.getInstalledVersionName(ctx) }
                                        Text(
                                            text = stringResource(R.string.update_check_title),
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "Installed ${AppUpdateChecker.formatVersionLabel(installedVersion)} • GitHub Releases",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (isCheckingUpdates) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    TextButton(
                                        onClick = onCheckForUpdates,
                                        enabled = !isCheckingUpdates
                                    ) {
                                        Text(stringResource(R.string.update_check_action))
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = isCheckingUpdates || updateStatusSummary != null,
                                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(tween(240)),
                                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                            ) {
                                Surface(
                                    color = when {
                                        updateStatusSummary?.hasUpdate == true -> MaterialTheme.colorScheme.primaryContainer
                                        updateStatusSummary?.isError == true -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_update_status_banner")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isCheckingUpdates) {
                                                    "Checking GitHub Releases…"
                                                } else {
                                                    updateStatusSummary?.statusLine.orEmpty()
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = when {
                                                    updateStatusSummary?.hasUpdate == true -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    updateStatusSummary?.isError == true -> MaterialTheme.colorScheme.onErrorContainer
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            val detailText = updateStatusSummary?.detailLine
                                            if (!isCheckingUpdates && !detailText.isNullOrBlank()) {
                                                Text(
                                                    text = detailText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        TextButton(onClick = onOpenGitHubReleases) {
                                            Text("Releases", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Encouraging Bug Report & Feature Suggestion Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_feedback_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Share feedback",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Help Shape 80endance!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Found a sneaky bug or have a cool idea? Your feedback directly builds the next update for everyone!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { feedbackDialogInitialType = FeedbackType.BUG },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("settings_report_bug_button")
                            ) {
                                Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Report Bug", maxLines = 1)
                            }
                            LiquidGlassButton(
                                onClick = { feedbackDialogInitialType = FeedbackType.SUGGESTION },
                                tint = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.primary,
                                minHeight = 48.dp,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_suggest_idea_button")
                            ) {
                                Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Suggest Idea", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Discreet Admin Panel Entry — rendered ONLY for the verified developer Google account
            if (isVerifiedAdmin) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenAdminPanel)
                            .testTag("settings_admin_panel_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = "Manage workspace",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Admin Panel", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Notices, student fixes & SL calendar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            // Sign Out Button
            item {
                OutlinedButton(
                    onClick = { showSignOutConfirm = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("settings_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }

            item {
                DevelopedByAtayaFooter(
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Daily Reminder Time") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdatePreferences(reminderEnabled, pickerState.hour, pickerState.minute, countExcused)
                        showTimePicker = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showNoticeSheet) {
        NoticeBoardSheet(
            notices = state.visibleNotices,
            onDismiss = { showNoticeSheet = false }
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign Out?") },
            text = { Text("Your attendance history remains safely backed up to your Google account.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    }
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showPhotoDialog) {
        ProfilePhotoDialog(
            displayName = profile?.fullName ?: "Student",
            customAvatarBase64 = profile?.customAvatarBase64,
            googlePhotoUrl = state.googlePhotoUrl,
            onSaveCustomAvatar = onSaveCustomAvatar,
            onDismiss = { showPhotoDialog = false }
        )
    }

    if (showRegisterVerify) {
        RegisterVerificationDialog(
            termName = state.activeTerm.name,
            stats = state.overallStats,
            onDismiss = { showRegisterVerify = false }
        )
    }

    feedbackDialogInitialType?.let { initialType ->
        FeedbackReportDialog(
            initialType = initialType,
            isSubmitting = isSubmittingFeedback,
            onDismiss = { feedbackDialogInitialType = null },
            onSubmit = { type, title, message ->
                onSubmitFeedback(type, title, message) {
                    feedbackDialogInitialType = null
                }
            }
        )
    }
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    return runCatching {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }.getOrDefault(false)
}

private fun openAppNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(intent) }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}

private fun openAppBatterySettings(context: Context) {
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(detailsIntent) }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
