package com.example.ui.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Block
import com.example.BuildConfig
import com.example.data.AppSettings
import com.example.data.BlockedEmailEntry
import com.example.data.UserProfile
import com.example.system.AppUpdateChecker
import com.example.ui.components.AnimatedOdometerText
import com.example.ui.components.StaggeredReveal
import com.example.ui.components.springPressScale
import com.example.ui.theme.LocalAttendanceColors
import kotlin.math.roundToInt

/**
 * Dedicated Admin Panel "Config" tab housing release distribution controls (Direct APK Override &
 * live version adoption pulse), remote Feature Flags (Next-Day School Poll), and Global Policy /
 * A/L Exam Countdown settings.
 */
@Composable
fun AdminConfigTab(
    appSettings: AppSettings,
    students: List<UserProfile>,
    blockedEmails: List<BlockedEmailEntry> = emptyList(),
    latestGithubVersion: String?,
    isCheckingGithubRelease: Boolean,
    isBusy: Boolean,
    onRefreshGithubRelease: () -> Unit,
    onSaveDirectApkOverride: (url: String, version: String) -> Unit,
    onSetNextDayPollEnabled: (Boolean) -> Unit,
    onSaveGlobalSettings: (AppSettings) -> Unit,
    onUnblockEmail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val semantic = LocalAttendanceColors.current

    var directApkUrlInput by remember(appSettings.directApkDownloadUrl) {
        mutableStateOf(appSettings.directApkDownloadUrl.orEmpty())
    }
    var directApkVersionInput by remember(appSettings.directApkVersion) {
        mutableStateOf(appSettings.directApkVersion.orEmpty())
    }
    var thresholdText by remember(appSettings) {
        mutableStateOf(appSettings.defaultEligibilityPercent.toString())
    }
    var exam2026 by remember(appSettings) { mutableStateOf(appSettings.al2026ExamDate) }
    var exam2027 by remember(appSettings) { mutableStateOf(appSettings.al2027ExamDate) }
    var exam2028 by remember(appSettings) { mutableStateOf(appSettings.al2028ExamDate) }

    val targetLatestVersion = latestGithubVersion?.trim()?.removePrefix("v")?.removePrefix("V")
        ?.takeIf { it.isNotBlank() } ?: BuildConfig.VERSION_NAME
    val studentsWithVersion = remember(students) {
        students.filter { !it.guestObserver && !it.appVersionName.isNullOrBlank() }
    }
    val studentsOnLatest = remember(studentsWithVersion, targetLatestVersion) {
        studentsWithVersion.count {
            it.appVersionName.orEmpty().trim().removePrefix("v").removePrefix("V") == targetLatestVersion
        }
    }
    val adoptionRatio by animateFloatAsState(
        targetValue = if (studentsWithVersion.isNotEmpty()) {
            (studentsOnLatest.toFloat() / studentsWithVersion.size.toFloat()).coerceIn(0f, 1f)
        } else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "admin_version_adoption_ratio"
    )

    val savedUrl = appSettings.validDirectApkDownloadUrl
    val savedVersion = appSettings.cleanDirectApkVersion
    val isOutdated = savedUrl != null && AppUpdateChecker.isAdminApkOverrideOutdated(
        configuredVersion = savedVersion,
        latestGithubVersion = latestGithubVersion
    )
    val deliveryModeLabel = when {
        savedUrl != null && !isOutdated -> "Admin Direct APK"
        latestGithubVersion != null -> "GitHub Auto-APK"
        else -> "Release Page"
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.testTag("admin_config_tab_list")
    ) {
        // 1. Release & Version Adoption Pulse Card
        item(key = "config_release_pulse") {
            StaggeredReveal(index = 0) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("admin_release_pulse_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.RocketLaunch,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Release & Update Pulse",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Delivery route: $deliveryModeLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            IconButton(
                                onClick = onRefreshGithubRelease,
                                enabled = !isCheckingGithubRelease,
                                modifier = Modifier.testTag("admin_refresh_github_release_btn")
                            ) {
                                if (isCheckingGithubRelease) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh GitHub latest release"
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Text(
                                        text = "THIS BUILD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "v${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Text(
                                        text = "GITHUB LATEST",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = latestGithubVersion?.let { AppUpdateChecker.formatVersionLabel(it) } ?: "Checking…",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (studentsWithVersion.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Students on v$targetLatestVersion",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    AnimatedOdometerText(
                                        text = "$studentsOnLatest / ${studentsWithVersion.size} (${(adoptionRatio * 100f).roundToInt()}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = semantic.present
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { adoptionRatio },
                                    color = semantic.present,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Direct APK Download Override Card
        item(key = "config_direct_apk") {
            StaggeredReveal(index = 1) {
                val (statusText, rawStatusBg, rawStatusFg) = when {
                    savedUrl == null -> Triple(
                        "Not configured — auto-using GitHub release APK",
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    isOutdated -> Triple(
                        "Outdated (${savedVersion ?: "no version"}) vs ${latestGithubVersion?.let { AppUpdateChecker.formatVersionLabel(it) } ?: "latest"} — falling back to GitHub",
                        semantic.medicalContainer,
                        semantic.onMedicalContainer
                    )
                    else -> Triple(
                        "Active Override (${savedVersion?.let { AppUpdateChecker.formatVersionLabel(it) } ?: "latest"})",
                        semantic.presentContainer,
                        semantic.onPresentContainer
                    )
                }
                val statusBg by animateColorAsState(
                    targetValue = rawStatusBg,
                    animationSpec = tween(240),
                    label = "apk_status_bg"
                )
                val statusFg by animateColorAsState(
                    targetValue = rawStatusFg,
                    animationSpec = tween(240),
                    label = "apk_status_fg"
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("admin_direct_apk_override_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Direct APK Download Override",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Paste a direct HTTPS .apk link & version. Falls back to GitHub automatically if blank or outdated.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = statusBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = statusFg,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = directApkUrlInput,
                            onValueChange = { directApkUrlInput = it.take(512) },
                            label = { Text("Direct APK Download URL (https://...)") },
                            placeholder = { Text("https://github.com/.../80endance-v4.0.apk") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_direct_apk_url_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = directApkVersionInput,
                                onValueChange = { directApkVersionInput = it.take(32) },
                                label = { Text("APK Version / Code") },
                                placeholder = { Text(latestGithubVersion ?: BuildConfig.VERSION_NAME) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_direct_apk_version_input")
                            )
                            if (!latestGithubVersion.isNullOrBlank()) {
                                TextButton(
                                    onClick = { directApkVersionInput = latestGithubVersion.removePrefix("v").removePrefix("V") },
                                    modifier = Modifier.testTag("admin_fill_latest_version_btn")
                                ) {
                                    Text("Use ${AppUpdateChecker.formatVersionLabel(latestGithubVersion)}")
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    onSaveDirectApkOverride(directApkUrlInput, directApkVersionInput)
                                },
                                enabled = !isBusy,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .springPressScale(enabled = !isBusy)
                                    .testTag("admin_save_direct_apk_button")
                            ) {
                                Text("Save Link")
                            }
                            OutlinedButton(
                                onClick = {
                                    directApkUrlInput = ""
                                    directApkVersionInput = ""
                                    onSaveDirectApkOverride("", "")
                                },
                                enabled = !isBusy && (savedUrl != null || directApkUrlInput.isNotBlank()),
                                modifier = Modifier
                                    .weight(0.9f)
                                    .springPressScale(enabled = !isBusy)
                                    .testTag("admin_clear_direct_apk_button")
                            ) {
                                Text("Clear")
                            }
                            OutlinedButton(
                                onClick = {
                                    val testUrl = directApkUrlInput.trim()
                                    if (testUrl.startsWith("https://", ignoreCase = true)) {
                                        AppUpdateChecker.openSafeHttpsUrl(context, testUrl)
                                    }
                                },
                                enabled = directApkUrlInput.trim().startsWith("https://", ignoreCase = true),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .springPressScale(enabled = directApkUrlInput.trim().startsWith("https://", ignoreCase = true))
                                    .testTag("admin_test_direct_apk_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = "Test direct APK link",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Remote Feature Flags Card (Next-Day School Poll Dev Toggle)
        item(key = "config_feature_flags") {
            StaggeredReveal(index = 2) {
                val pollEnabled = appSettings.nextDayPollEnabled
                val badgeBg by animateColorAsState(
                    targetValue = if (pollEnabled) semantic.presentContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    animationSpec = tween(220),
                    label = "poll_flag_bg"
                )
                val badgeFg by animateColorAsState(
                    targetValue = if (pollEnabled) semantic.onPresentContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "poll_flag_fg"
                )

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_next_day_poll_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Remote Feature Flags",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Next-Day School Poll",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Surface(
                                        color = badgeBg,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (pollEnabled) "LIVE" else "OFF",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeFg,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "8:00 PM – 5:00 AM daily \"Going to school tomorrow?\" notification, Home poll & widget Evening Mode.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = pollEnabled,
                                onCheckedChange = onSetNextDayPollEnabled,
                                modifier = Modifier.testTag("admin_next_day_poll_switch")
                            )
                        }
                    }
                }
            }
        }

        // 4. Global Policy & A/L Exam Countdowns Card
        item(key = "config_global_policy") {
            StaggeredReveal(index = 3) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_global_policy_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Global Policy & A/L Exam Countdowns",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedTextField(
                            value = thresholdText,
                            onValueChange = { thresholdText = it.filter(Char::isDigit).take(3) },
                            label = { Text("Min Eligibility % (default 80)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = exam2026,
                            onValueChange = { exam2026 = it.take(10) },
                            label = { Text("2026 A/L Exam Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = exam2027,
                            onValueChange = { exam2027 = it.take(10) },
                            label = { Text("2027 A/L Exam Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = exam2028,
                            onValueChange = { exam2028 = it.take(10) },
                            label = { Text("2028 A/L Exam Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val dateRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
                                if (exam2026.matches(dateRegex) && exam2027.matches(dateRegex) && exam2028.matches(dateRegex)) {
                                    onSaveGlobalSettings(
                                        appSettings.copy(
                                            defaultEligibilityPercent = (thresholdText.toIntOrNull() ?: 80).coerceIn(50, 100),
                                            al2026ExamDate = exam2026,
                                            al2027ExamDate = exam2027,
                                            al2028ExamDate = exam2028
                                        )
                                    )
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .springPressScale(enabled = !isBusy)
                                .testTag("admin_save_global_defaults_button")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Global Defaults")
                        }
                    }
                }
            }
        }

        // 5. Blocked Emails & Removed Users Card
        item(key = "config_blocked_emails") {
            StaggeredReveal(index = 4) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("admin_blocked_emails_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Blocked Emails (${blockedEmails.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Emails removed & blocked from Student Details cannot sign back in or write data until unblocked here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (blockedEmails.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No blocked emails.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            blockedEmails.forEach { entry ->
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = entry.email,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val subtitle = listOfNotNull(
                                                entry.displayName?.takeIf { it.isNotBlank() },
                                                entry.reason?.takeIf { it.isNotBlank() }
                                            ).joinToString(" • ")
                                            if (subtitle.isNotBlank()) {
                                                Text(
                                                    text = subtitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        OutlinedButton(
                                            onClick = { onUnblockEmail(entry.email) },
                                            enabled = !isBusy,
                                            modifier = Modifier.testTag("admin_unblock_${entry.email}")
                                        ) {
                                            Text("Unblock")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

