package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AcademicTerm
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceRecord
import com.example.data.CalendarEvent
import com.example.data.DateUtils
import com.example.data.UserProfile
import com.example.ui.theme.LocalAttendanceColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AttendanceBaselineDialog(
    profile: UserProfile?,
    activeTerm: AcademicTerm,
    terms: List<AcademicTerm>,
    calendarEvents: List<CalendarEvent>,
    existingRecords: List<AttendanceRecord>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        startDateKey: String,
        endDateKey: String,
        percentage: Double,
        presentDays: Int,
        absentDays: Int
    ) -> Unit,
    onClearBaseline: (() -> Unit)? = null
) {
    val semantic = LocalAttendanceColors.current
    val todayKey = remember { DateUtils.todayKey() }
    val yesterdayKey = remember(todayKey) { DateUtils.addDays(todayKey, -1) }
    val lastFridayKey = remember(todayKey) { DateUtils.lastFridayKey(todayKey) }
    val endOfLastMonthKey = remember(todayKey) { DateUtils.endOfPreviousMonthKey(todayKey) }

    val yearStartTerm = remember(terms, activeTerm) {
        terms.filter { it.academicYear == activeTerm.academicYear }.minByOrNull { it.startDate }
    }

    val defaultStart = remember(profile, activeTerm, todayKey) {
        val saved = profile?.baselineStartDate?.takeIf { it.length == 10 }
        val candidate = saved ?: activeTerm.startDate
        if (candidate > todayKey) DateUtils.addDays(todayKey, -30) else candidate
    }
    val defaultEnd = remember(profile, lastFridayKey, yesterdayKey, defaultStart) {
        val saved = profile?.baselineEndDate?.takeIf { it.length == 10 && it <= todayKey }
        val candidate = saved ?: if (lastFridayKey >= defaultStart) lastFridayKey else yesterdayKey
        if (candidate < defaultStart) defaultStart else candidate
    }

    var startDateKey by remember { mutableStateOf(defaultStart) }
    var endDateKey by remember { mutableStateOf(defaultEnd) }
    var percentText by remember {
        mutableStateOf(
            profile?.baselinePercent?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.1f", it)
            } ?: ""
        )
    }
    var showOverrideDays by remember { mutableStateOf(false) }
    var customSchoolDaysText by remember { mutableStateOf("") }
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

    val parsedPercent = percentText.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }
    val detectedSchoolDays = remember(startDateKey, endDateKey, calendarEvents) {
        AttendanceCalculator.countSchoolDaysInRange(startDateKey, endDateKey, calendarEvents)
    }
    val customOverrideDays = if (showOverrideDays) {
        customSchoolDaysText.toIntOrNull()?.takeIf { it in 1..600 }
    } else {
        null
    }
    val estimate = remember(startDateKey, endDateKey, parsedPercent, calendarEvents, customOverrideDays) {
        if (parsedPercent != null && startDateKey <= endDateKey && endDateKey <= todayKey) {
            AttendanceCalculator.estimateBaselineFromPercentage(
                startDateKey = startDateKey,
                endDateKey = endDateKey,
                percentage = parsedPercent,
                calendarEvents = calendarEvents,
                customSchoolDaysOverride = customOverrideDays
            )
        } else {
            null
        }
    }
    val overlappingCount = remember(existingRecords, startDateKey, endDateKey) {
        existingRecords.count { it.dateKey >= startDateKey && it.dateKey <= endDateKey }
    }

    val rangeValid = startDateKey.length == 10 &&
        endDateKey.length == 10 &&
        startDateKey <= endDateKey &&
        endDateKey <= todayKey

    val canSave = !isSaving &&
        rangeValid &&
        estimate != null &&
        estimate.totalSchoolDays > 0

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Percent,
                contentDescription = "Import attendance percentage",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Import Attendance %",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("baseline_import_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter your attendance percentage from Day 1 up to a past date (e.g., from your class register). Days in this range will be locked from individual editing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. From Date (Day 1)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "From (Day 1)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = { pickingStartDate = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("baseline_start_date_button")
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(DateUtils.formatReadableFull(startDateKey))
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (activeTerm.startDate <= todayKey) {
                            FilterChip(
                                selected = startDateKey == activeTerm.startDate,
                                onClick = {
                                    startDateKey = activeTerm.startDate
                                    if (endDateKey < startDateKey) endDateKey = startDateKey
                                },
                                label = { Text("${activeTerm.name} Start") }
                            )
                        }
                        if (yearStartTerm != null && yearStartTerm.startDate != activeTerm.startDate && yearStartTerm.startDate <= todayKey) {
                            FilterChip(
                                selected = startDateKey == yearStartTerm.startDate,
                                onClick = {
                                    startDateKey = yearStartTerm.startDate
                                    if (endDateKey < startDateKey) endDateKey = startDateKey
                                },
                                label = { Text("${yearStartTerm.academicYear} Day 1") }
                            )
                        }
                    }
                }

                // 2. Up To Date (End Date)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Up To Date (Included & Locked)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = { pickingEndDate = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("baseline_end_date_button")
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(DateUtils.formatReadableFull(endDateKey))
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (lastFridayKey >= startDateKey && lastFridayKey <= todayKey) {
                            FilterChip(
                                selected = endDateKey == lastFridayKey,
                                onClick = { endDateKey = lastFridayKey },
                                label = { Text("Last Friday") }
                            )
                        }
                        if (yesterdayKey >= startDateKey && yesterdayKey <= todayKey) {
                            FilterChip(
                                selected = endDateKey == yesterdayKey,
                                onClick = { endDateKey = yesterdayKey },
                                label = { Text("Yesterday") }
                            )
                        }
                        if (endOfLastMonthKey >= startDateKey && endOfLastMonthKey <= todayKey) {
                            FilterChip(
                                selected = endDateKey == endOfLastMonthKey,
                                onClick = { endDateKey = endOfLastMonthKey },
                                label = { Text("End of Last Month") }
                            )
                        }
                    }
                }

                // 3. Attendance Percentage Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = percentText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1 && filtered.length <= 6) {
                                percentText = filtered
                            }
                        },
                        label = { Text("Attendance Percentage (%) *") },
                        placeholder = { Text("e.g., 84.5") },
                        suffix = { Text("%") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = percentText.isNotBlank() && parsedPercent == null,
                        supportingText = if (percentText.isNotBlank() && parsedPercent == null) {
                            { Text("Enter a percentage between 0 and 100") }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("baseline_percent_input")
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("75", "80", "85", "90", "95").forEach { preset ->
                            FilterChip(
                                selected = percentText == preset,
                                onClick = { percentText = preset },
                                label = { Text("$preset%") }
                            )
                        }
                    }
                }

                // 4. Detected School Days + Optional Adjust
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$detectedSchoolDays school days detected",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Weekends & Poya/No School days automatically excluded",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    showOverrideDays = !showOverrideDays
                                    if (showOverrideDays && customSchoolDaysText.isEmpty() && detectedSchoolDays > 0) {
                                        customSchoolDaysText = detectedSchoolDays.toString()
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showOverrideDays) "Auto" else "Adjust")
                            }
                        }

                        AnimatedVisibility(visible = showOverrideDays) {
                            OutlinedTextField(
                                value = customSchoolDaysText,
                                onValueChange = { customSchoolDaysText = it.filter(Char::isDigit).take(3) },
                                label = { Text("Exact Total School Days (Register)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 5. Live Conversion Preview & Lock Notice
                if (!rangeValid) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Start date must be on or before the end date, and end date cannot be in the future.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else if (estimate != null && estimate.totalSchoolDays > 0) {
                    val isEligible = estimate.effectivePercent >= activeTerm.minEligibilityPercent
                    Surface(
                        color = if (isEligible) {
                            semantic.presentContainer.copy(alpha = 0.65f)
                        } else {
                            semantic.absentContainer.copy(alpha = 0.72f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("baseline_preview_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${estimate.presentDays} Present • ${estimate.absentDays} Absent (${estimate.totalSchoolDays} school days)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEligible) semantic.onPresentContainer else semantic.onAbsentContainer
                            )
                            Text(
                                text = "Calculated baseline: ${String.format(Locale.US, "%.1f", estimate.effectivePercent)}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isEligible) semantic.onPresentContainer else semantic.onAbsentContainer
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Locked period notice",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${DateUtils.formatReadableShort(startDateKey)} → ${DateUtils.formatReadableShort(endDateKey)} will be locked from individual day edits.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (overlappingCount > 0) {
                                    Text(
                                        text = "$overlappingCount existing day log${if (overlappingCount == 1) "" else "s"} inside this period will be consolidated so nothing is double-counted.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                } else if (detectedSchoolDays == 0 && rangeValid) {
                    Text(
                        text = "No school weekdays found in this date range. Expand your dates or tap Adjust.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (profile?.hasLockedBaseline == true && onClearBaseline != null) {
                    OutlinedButton(
                        onClick = {
                            onClearBaseline()
                            onDismiss()
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("baseline_clear_button")
                    ) {
                        Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear Imported % & Unlock Dates")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val est = estimate ?: return@Button
                    onSave(
                        est.startDateKey,
                        est.endDateKey,
                        est.enteredPercent,
                        est.presentDays,
                        est.absentDays
                    )
                },
                enabled = canSave,
                modifier = Modifier.testTag("baseline_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Lock")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )

    if (pickingStartDate) {
        val maxMillis = remember(todayKey) { isoDateToUtcMillis(todayKey) }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToUtcMillis(startDateKey),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return maxMillis == null || utcTimeMillis <= maxMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickingStartDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = utcMillisToIsoDate(millis)
                            startDateKey = picked
                            if (endDateKey < picked) {
                                endDateKey = picked
                            }
                        }
                        pickingStartDate = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { pickingStartDate = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (pickingEndDate) {
        val minMillis = remember(startDateKey) { isoDateToUtcMillis(startDateKey) }
        val maxMillis = remember(todayKey) { isoDateToUtcMillis(todayKey) }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToUtcMillis(endDateKey),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val afterStart = minMillis == null || utcTimeMillis >= minMillis
                    val notFuture = maxMillis == null || utcTimeMillis <= maxMillis
                    return afterStart && notFuture
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickingEndDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            endDateKey = utcMillisToIsoDate(millis)
                        }
                        pickingEndDate = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { pickingEndDate = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun isoDateToUtcMillis(dateKey: String): Long? {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.parse(dateKey)?.time
    } catch (_: Exception) {
        null
    }
}

private fun utcMillisToIsoDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(millis))
}
