package com.example.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AttendanceMilestone
import com.example.data.AttendancePatternInsights
import com.example.data.AttendanceStats
import com.example.data.UserProfile
import com.example.system.ShareableCardRenderer
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StatusBlue
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.StatusRed
import java.util.Locale

/**
 * Interactive preview + 1-tap WhatsApp / Share Sheet launcher for the "Monthly Wrapped" graphic card.
 */
@Composable
fun MonthlyWrappedPreviewDialog(
    profile: UserProfile?,
    monthLabel: String,
    stats: AttendanceStats,
    insights: AttendancePatternInsights,
    streak: Int,
    milestones: List<AttendanceMilestone>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val unlocked = milestones.filter { it.unlocked }
    val accent = if (stats.eligible) StatusGreen else StatusRed

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .testTag("monthly_wrapped_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = RoyalGold)
                        Text(
                            text = "Monthly Wrapped Card",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                // Live Preview of the WhatsApp-ready card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF081229), Color(0xFF132A56), Color(0xFF0A1836))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            color = RoyalGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, RoyalGold.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "80ENDANCE WRAPPED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                ),
                                color = RoyalGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Column {
                            Text(
                                text = monthLabel,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                            Text(
                                text = listOfNotNull(
                                    profile?.preferredDisplayName?.takeIf { it.isNotBlank() },
                                    profile?.className?.takeIf { it.isNotBlank() }
                                ).joinToString(" • ").ifBlank { "GCE A/L Attendance" },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA8C0E8)
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.09f),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (stats.totalCountedDays == 0) "--%" else String.format(Locale.US, "%.1f%%", stats.percentage),
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                    color = Color.White
                                )
                                Text(
                                    text = if (stats.eligible) {
                                        "✓ Eligible (${stats.thresholdPercent}%+) • ${stats.safeSkipsRemaining} safe skips left"
                                    } else {
                                        "Recovery Mode • Attend ${stats.daysNeededToRecover}d to reach ${stats.thresholdPercent}%"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = accent
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WrappedMiniTile(
                                label = "STREAK",
                                value = "${streak}d",
                                tint = RoyalGold,
                                modifier = Modifier.weight(1f)
                            )
                            WrappedMiniTile(
                                label = "SAFEST DAY",
                                value = insights.bestWeekday?.shortName ?: "--",
                                tint = StatusGreen,
                                modifier = Modifier.weight(1f)
                            )
                            WrappedMiniTile(
                                label = "BADGES",
                                value = "${unlocked.size}/${milestones.size}",
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (unlocked.isNotEmpty()) {
                            Text(
                                text = unlocked.take(3).joinToString("  •  ") { "★ ${it.title}" },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = RoyalGold
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val bmp = ShareableCardRenderer.renderMonthlyWrappedBitmap(
                            profile = profile,
                            monthLabel = monthLabel,
                            stats = stats,
                            insights = insights,
                            streak = streak,
                            milestones = milestones
                        )
                        val caption = "My $monthLabel 80endance Wrapped: ${String.format(Locale.US, "%.1f%%", stats.percentage)} (${streak}d streak)!"
                        ShareableCardRenderer.shareBitmapViaChooser(
                            context = context,
                            bitmap = bmp,
                            fileName = "80endance_wrapped.png",
                            chooserTitle = "Share Monthly Wrapped",
                            captionText = caption
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("share_wrapped_image_btn")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share to WhatsApp / Save Card", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WrappedMiniTile(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = tint
            )
        }
    }
}

/**
 * Clean, formal Parent-Friendly Summary Card preview + 1-tap PNG image share.
 * Shows Current %, 80% Eligibility badge, and Term totals while keeping private daily notes hidden.
 */
@Composable
fun ParentSummaryPreviewDialog(
    profile: UserProfile?,
    termName: String,
    stats: AttendanceStats,
    overallStats: AttendanceStats? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showOverall by remember { mutableStateOf(false) }
    val activeStats = if (showOverall && overallStats != null) overallStats else stats
    val activeScopeLabel = if (showOverall && overallStats != null) "Overall (Grade 12 & 13)" else termName
    val badgeColor = if (activeStats.eligible) StatusGreen else StatusRed

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .testTag("parent_summary_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.FamilyRestroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Parent Summary Card",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }

                if (overallStats != null && termName != "Overall (Grade 12 & 13)") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !showOverall,
                            onClick = { showOverall = false },
                            label = { Text("Current Term") },
                            modifier = Modifier.testTag("parent_summary_term_chip")
                        )
                        FilterChip(
                            selected = showOverall,
                            onClick = { showOverall = true },
                            label = { Text("Overall G12 & 13") },
                            modifier = Modifier.testTag("parent_summary_overall_chip")
                        )
                    }
                }

                // Clean formal paper-style preview card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0E2A5C))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = (profile?.schoolName ?: "ROYAL COLLEGE, COLOMBO").uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = RoyalGold
                                )
                                Text(
                                    text = "Student Attendance Summary",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White
                                )
                                Text(
                                    text = listOfNotNull(
                                        profile?.fullName?.takeIf { it.isNotBlank() },
                                        profile?.grade?.takeIf { it.isNotBlank() },
                                        profile?.className?.takeIf { it.isNotBlank() }
                                    ).joinToString(" • ").ifBlank { "GCE A/L Student" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD6E4FF)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = activeScopeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = if (activeStats.totalCountedDays == 0) "--%" else String.format(Locale.US, "%.1f%%", activeStats.percentage),
                                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "${activeStats.effectiveAttended} of ${activeStats.totalCountedDays} school days counted",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Surface(
                                    color = badgeColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (activeStats.eligible) "${activeStats.thresholdPercent}% Eligible" else "Below ${activeStats.thresholdPercent}%",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = badgeColor
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                ParentMetricCol("Attended", "${activeStats.effectiveAttended}d", StatusGreen)
                                ParentMetricCol("Absent", "${activeStats.absentDays + activeStats.priorAbsentDays + activeStats.periodAbsentDays}d", StatusRed)
                                ParentMetricCol("Medical", "${activeStats.medicalDays}d", StatusOrange)
                                ParentMetricCol("Excused", "${activeStats.schoolEventDays}d", StatusBlue)
                            }

                            Text(
                                text = "Private daily notes & simulator estimates are automatically hidden.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val bmp = ShareableCardRenderer.renderParentSummaryBitmap(
                            profile = profile,
                            termName = activeScopeLabel,
                            stats = activeStats
                        )
                        val caption = "${profile?.fullName ?: "Student"} — $activeScopeLabel Attendance: ${String.format(Locale.US, "%.1f%%", activeStats.percentage)}"
                        ShareableCardRenderer.shareBitmapViaChooser(
                            context = context,
                            bitmap = bmp,
                            fileName = "80endance_parent_summary.png",
                            chooserTitle = "Share Parent Summary Card",
                            captionText = caption
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("share_parent_summary_btn")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Clean Summary Card", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ParentMetricCol(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
    }
}
