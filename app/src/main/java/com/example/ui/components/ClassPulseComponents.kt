package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ClassPodiumEntry
import com.example.data.ClassPulseSummary
import com.example.data.ClassStreakEntry
import com.example.data.ClassWeeklyChallenge
import com.example.ui.theme.LocalAttendanceColors
import com.example.ui.theme.RoyalGold
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Compact Class Pulse card shown on Home & Reports summarizing Weekly Squad Goal,
 * Grade-Level Class Rank, and Streak Arena participation.
 */
@Composable
fun ClassPulseBannerCard(
    summary: ClassPulseSummary,
    optedInArena: Boolean,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAttendanceColors.current
    val challenge = summary.weeklyChallenge
    val progressRatio by animateFloatAsState(
        targetValue = if (challenge != null) {
            (challenge.currentPercent / challenge.targetPercent.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
        } else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "class_pulse_progress"
    )

    FrostedHeroCard(
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable(pressedScale = 0.975f, onClick = onOpenSheet)
            .testTag("class_pulse_banner_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.16f)
                        )
                    )
                )
                .padding(18.dp),
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = "Class Pulse & Challenges",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Class Pulse",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (summary.myClassName.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = summary.myClassName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (summary.myClassRank != null && summary.myGrade.isNotBlank()) {
                                "Ranked #${summary.myClassRank} in ${summary.myGrade} • Streak Arena & Squad Goal"
                            } else {
                                "Weekly Squad Goal, Grade Podium & Streak Arena"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Open Class Pulse",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (challenge != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = challenge.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        AnimatedOdometerText(
                            text = "${challenge.currentPercent.roundToInt()}% / ${challenge.targetPercent.roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (challenge.isCompleted) semantic.present else MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        color = if (challenge.isCompleted) semantic.present else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LivingStreakFlameIcon(
                        streak = summary.streakLeaderboard.maxOfOrNull { it.currentStreak } ?: 1,
                        size = 18.dp
                    )
                    Text(
                        text = when {
                            summary.streakLeaderboard.isNotEmpty() ->
                                "${summary.streakLeaderboard.size} classmate${if (summary.streakLeaderboard.size == 1) "" else "s"} in Streak Arena"
                            optedInArena -> "You're live in Streak Arena — invite classmates!"
                            else -> "Opt-in Streak Arena • Cheer classmates"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Full Class Pulse Bottom Sheet featuring:
 * 1) Weekly Class Squad Goal
 * 2) Friendly Grade-Level Class Podium
 * 3) Opt-In Class Streak Arena with zero-PII aliases & 1-Tap Cheer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassPulseBottomSheet(
    summary: ClassPulseSummary,
    currentUserId: String,
    optedInArena: Boolean,
    useAnonymousAlias: Boolean,
    cheeredPeerIdsToday: Set<String>,
    onUpdateArenaPrefs: (optedIn: Boolean, useAnonymousAlias: Boolean) -> Unit,
    onCheerClassmate: (userId: String, displayAlias: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = LocalAttendanceColors.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Streak Arena, 1 = Grade Podium

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("class_pulse_bottom_sheet")
    ) {
        val sectionOptions = remember {
            listOf(
                LiquidGlassSegmentOption(label = "Streak Arena", testTag = "class_pulse_tab_arena"),
                LiquidGlassSegmentOption(label = "Grade Podium", testTag = "class_pulse_tab_podium")
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Class Pulse • ${summary.myClassName.ifBlank { "Your Class" }}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Friendly streaks, weekly squad goals, and zero-PII class momentum.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Weekly Squad Goal Hero
            summary.weeklyChallenge?.let { challenge ->
                item {
                    WeeklySquadChallengeCard(challenge = challenge)
                }
            }

            // Section switcher
            item {
                SlidableLiquidGlassSegmentedBar(
                    options = sectionOptions,
                    selectedIndex = selectedSection,
                    onOptionSelected = { selectedSection = it.coerceIn(0, 1) },
                    barHeight = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (selectedSection == 0) {
                // Opt-in privacy card
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Join Class Streak Arena",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Shares only your streak & nickname with classmates — never your exact absences.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                LiquidToggle(
                                    selected = { optedInArena },
                                    onSelect = { enabled ->
                                        onUpdateArenaPrefs(enabled, useAnonymousAlias)
                                    },
                                    modifier = Modifier.testTag("streak_arena_opt_in_toggle")
                                )
                            }

                            if (optedInArena) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                        Text(
                                            text = "Show as Anonymous Student",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Hide your nickname while still joining the streak board.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LiquidToggle(
                                        selected = { useAnonymousAlias },
                                        onSelect = { anon ->
                                            onUpdateArenaPrefs(optedInArena, anon)
                                        },
                                        modifier = Modifier.testTag("streak_arena_anonymous_toggle")
                                    )
                                }
                            }
                        }
                    }
                }

                if (summary.streakLeaderboard.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = semantic.medical,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Be the first streak spark in ${summary.myClassName.ifBlank { "your class" }}!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Turn on Join Class Streak Arena above to showcase your streak and let classmates cheer you on.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = summary.streakLeaderboard,
                        key = { _, entry -> entry.userId }
                    ) { index, entry ->
                        StreakArenaRow(
                            rank = index + 1,
                            entry = entry,
                            isMe = entry.userId == currentUserId,
                            alreadyCheeredToday = entry.userId in cheeredPeerIdsToday,
                            onCheer = { onCheerClassmate(entry.userId, entry.displayAlias) }
                        )
                    }
                }
            } else {
                // Grade-Level Class Podium
                if (summary.gradeClassPodium.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Groups,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Grade Podium Warming Up",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Class rankings appear once students in ${summary.myGrade.ifBlank { "your grade" }} log attendance.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = summary.gradeClassPodium,
                        key = { _, entry -> "${entry.grade}_${entry.className}" }
                    ) { index, entry ->
                        GradePodiumRow(
                            rank = index + 1,
                            entry = entry
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun WeeklySquadChallengeCard(challenge: ClassWeeklyChallenge) {
    val semantic = LocalAttendanceColors.current
    val progress by animateFloatAsState(
        targetValue = (challenge.currentPercent / challenge.targetPercent.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "squad_challenge_progress"
    )
    Surface(
        color = if (challenge.isCompleted) semantic.presentContainer.copy(alpha = 0.72f)
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (challenge.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = if (challenge.isCompleted) semantic.present else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = String.format(Locale.US, "%.1f%%", challenge.currentPercent),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (challenge.isCompleted) semantic.present else MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                color = if (challenge.isCompleted) semantic.present else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape)
            )
            Text(
                text = challenge.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreakArenaRow(
    rank: Int,
    entry: ClassStreakEntry,
    isMe: Boolean,
    alreadyCheeredToday: Boolean,
    onCheer: () -> Unit
) {
    val semantic = LocalAttendanceColors.current
    val haptic = LocalHapticFeedback.current
    var cheerTrigger by remember { mutableIntStateOf(0) }

    val medalColor = when (rank) {
        1 -> RoyalGold
        2 -> Color(0xFF94A3B8)
        3 -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Surface(
        color = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isMe) 1.4.dp else 1.dp,
            color = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("streak_arena_row_$rank")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (rank <= 3) medalColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (rank <= 3) medalColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.displayAlias.ifBlank { "Student" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isMe) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "You",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "Best: ${entry.bestStreak}d • ${entry.cheerCount} cheer${if (entry.cheerCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Streak badge
            Surface(
                color = semantic.medicalContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    LivingStreakFlameIcon(
                        streak = entry.currentStreak,
                        size = 17.dp
                    )
                    Text(
                        text = "${entry.currentStreak}d",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = semantic.onMedicalContainer
                    )
                }
            }

            if (!isMe) {
                Box(contentAlignment = Alignment.Center) {
                    CheerSparkBurstOverlay(
                        triggerKey = cheerTrigger,
                        accentColor = semantic.medical
                    )
                    CheerBurstBounce(triggerKey = cheerTrigger) {
                        LiquidGlassButton(
                            onClick = {
                                if (!alreadyCheeredToday) {
                                    cheerTrigger++
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCheer()
                                }
                            },
                            enabled = !alreadyCheeredToday,
                            tint = if (alreadyCheeredToday) semantic.present else semantic.medical,
                            contentColor = if (alreadyCheeredToday) semantic.present else semantic.onMedicalContainer,
                            minHeight = 40.dp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("cheer_classmate_btn_${entry.userId}")
                        ) {
                            Icon(
                                imageVector = if (alreadyCheeredToday) Icons.Rounded.CheckCircle else Icons.Rounded.ThumbUp,
                                contentDescription = if (alreadyCheeredToday) "Cheered" else "Cheer classmate",
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (alreadyCheeredToday) "Cheered" else "Cheer",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradePodiumRow(
    rank: Int,
    entry: ClassPodiumEntry
) {
    val semantic = LocalAttendanceColors.current
    val medalColor = when (rank) {
        1 -> RoyalGold
        2 -> Color(0xFF94A3B8)
        3 -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = if (entry.isMyClass) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.44f)
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (entry.isMyClass) 1.4.dp else 1.dp,
            color = if (entry.isMyClass) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grade_podium_row_$rank")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (rank <= 3) medalColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (rank <= 3) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Rank $rank",
                            tint = medalColor,
                            modifier = Modifier.size(19.dp)
                        )
                    } else {
                        Text(
                            text = "#$rank",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.className,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.isMyClass) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Your Class",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${entry.alStream} • ${entry.studentCount} student${if (entry.studentCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = String.format(Locale.US, "%.1f%%", entry.averagePercent),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (entry.averagePercent >= 80.0) semantic.present else semantic.medical
            )
        }
    }
}
