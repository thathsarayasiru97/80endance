package com.example.ui.reports

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AcademicTerm
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceStatus
import com.example.ui.components.LiquidGlassButton
import com.example.ui.theme.LocalAttendanceColors
import com.example.viewmodel.StudentAppState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsScreen(
    state: StudentAppState,
    onToggleCountExcused: (Boolean) -> Unit = {},
    bottomBarPadding: Dp = 24.dp
) {
    val context = LocalContext.current
    val semantic = LocalAttendanceColors.current
    val profile = state.profile

    var selectedTerm by remember(state.terms) { mutableStateOf<AcademicTerm?>(null) } // null = Overall (G12 & 13)

    val stats = remember(state.attendanceRecords, selectedTerm, profile) {
        val threshold = selectedTerm?.minEligibilityPercent ?: state.appSettings.defaultEligibilityPercent
        AttendanceCalculator.calculateStats(
            records = state.attendanceRecords,
            thresholdPercent = threshold,
            countExcusedAsPresent = true,
            includePriorBalance = selectedTerm == null || selectedTerm?.id == state.activeTerm.id,
            priorPresent = profile?.priorPresentCount ?: 0,
            priorAbsent = profile?.priorAbsentCount ?: 0,
            termFilter = selectedTerm
        )
    }

    val subjectCounts = remember(state.attendanceRecords, selectedTerm) {
        val scoped = if (selectedTerm != null) {
            state.attendanceRecords.filter {
                it.termId == selectedTerm?.id ||
                    (it.dateKey >= selectedTerm!!.startDate && it.dateKey <= selectedTerm!!.endDate)
            }
        } else {
            state.attendanceRecords
        }
        val map = mutableMapOf<String, Pair<Int, Int>>() // subject -> (present, missed)
        scoped.forEach { rec ->
            rec.subjectTags.forEach { sub ->
                val cur = map[sub] ?: (0 to 0)
                when (rec.statusEnum) {
                    AttendanceStatus.PRESENT,
                    AttendanceStatus.SCHOOL_EVENT,
                    AttendanceStatus.MEDICAL -> map[sub] = (cur.first + 1) to cur.second
                    AttendanceStatus.ABSENT -> map[sub] = cur.first to (cur.second + 1)
                    AttendanceStatus.HOLIDAY -> Unit
                }
            }
        }
        map.entries.sortedByDescending { it.value.first + it.value.second }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("reports_lazy_column"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = bottomBarPadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Term & Eligibility Reports",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Inspect Grade 12 & 13 attendance and export summaries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Scope Chips
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTerm == null,
                        onClick = { selectedTerm = null },
                        label = { Text("Overall (G12 & 13)") }
                    )
                    state.terms.take(4).forEach { term ->
                        FilterChip(
                            selected = selectedTerm?.id == term.id,
                            onClick = { selectedTerm = term },
                            label = { Text(term.name) }
                        )
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedTerm?.name ?: "Overall GCE A/L",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${stats.effectiveAttended} of ${stats.totalCountedDays} counted school days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%.1f%%", stats.percentage),
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (stats.eligible) semantic.present else semantic.absent
                            )
                        }

                        LinearProgressIndicator(
                            progress = { (stats.percentage.toFloat() / 100f).coerceIn(0f, 1f) },
                            color = if (stats.eligible) semantic.present else semantic.absent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatTile("Present", stats.presentDays.toString(), semantic.presentContainer, semantic.onPresentContainer, Modifier.weight(1f))
                            StatTile("Absent", stats.absentDays.toString(), semantic.absentContainer, semantic.onAbsentContainer, Modifier.weight(1f))
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatTile("Medical", stats.medicalDays.toString(), semantic.medicalContainer, semantic.onMedicalContainer, Modifier.weight(1f))
                            StatTile("Excused", stats.schoolEventDays.toString(), semantic.schoolEventContainer, semantic.onSchoolEventContainer, Modifier.weight(1f))
                        }

                        if (stats.priorPresentDays > 0 || stats.priorAbsentDays > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Includes prior balance: +${stats.priorPresentDays} present, +${stats.priorAbsentDays} absent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Excused & Medical Policy Info Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = semantic.schoolEventContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = "College attendance rule",
                                    tint = semantic.onSchoolEventContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Excused & Medical Count as Present",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Official college sports, club/society events, and medical absences automatically count toward your 80% GCE A/L eligibility and streak.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Interactive "What-If" Skip Simulator
            item {
                WhatIfSkipSimulatorCard(
                    stats = stats,
                    scopeTitle = selectedTerm?.name ?: "Overall GCE A/L"
                )
            }

            // Monthly Attendance Chart & Weekday Habit Insights
            item {
                AttendancePatternsAndChartCard(
                    records = state.attendanceRecords,
                    thresholdPercent = stats.thresholdPercent
                )
            }

            // Subject Tags Summary
            if (subjectCounts.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Tagged A/L Subjects",
                                style = MaterialTheme.typography.titleMedium
                            )
                            subjectCounts.forEach { (subject, counts) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(subject, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${counts.first} attended • ${counts.second} missed",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Native Android Share Report Button
            item {
                LiquidGlassButton(
                    onClick = {
                        if (profile != null) {
                            val reportText = AttendanceCalculator.buildShareableReport(
                                profile = profile,
                                scopeTitle = selectedTerm?.name ?: "Overall (Grade 12 & 13)",
                                stats = stats,
                                streak = state.currentStreak
                            )
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "80endance Attendance Report")
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Attendance Report"))
                        }
                    },
                    tint = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    minHeight = 54.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("share_report_button")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Term Report")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fg.copy(alpha = 0.85f)
            )
        }
    }
}
