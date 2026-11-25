package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
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
import com.example.data.AttendanceCalculator
import com.example.data.AttendancePeriodBlock
import com.example.data.AttendancePeriodEntryMode
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
fun PeriodAttendanceDialog(
    profile: UserProfile?,
    calendarEvents: List<CalendarEvent>,
    existingRecords: List<AttendanceRecord>,
    existingPeriodBlocks: List<AttendancePeriodBlock>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        startDateKey: String,
        endDateKey: String,
        totalSchoolDays: Int,
        presentDays: Int,
        absentDays: Int,
        percentage: Double,
        entryMode: AttendancePeriodEntryMode,
        label: String?
    ) -> Unit
) {
    val semantic = LocalAttendanceColors.current
    val todayKey = remember { DateUtils.todayKey() }
    val endOfLastMonthKey = remember(todayKey) { DateUtils.endOfPreviousMonthKey(todayKey) }
    val startOfLastMonthKey = remember(endOfLastMonthKey) {
        if (endOfLastMonthKey.length == 10) "${endOfLastMonthKey.substring(0, 7)}-01" else DateUtils.addDays(todayKey, -30)
    }
    val startOfThisMonthKey = remember(todayKey) { "${todayKey.substring(0, 7)}-01" }
    val twoWeeksAgoKey = remember(todayKey) { DateUtils.addDays(todayKey, -13) }

    var startDateKey by remember { mutableStateOf(startOfLastMonthKey) }
    var endDateKey by remember { mutableStateOf(endOfLastMonthKey) }
    var entryMode by remember { mutableStateOf(AttendancePeriodEntryMode.MISSED_DAYS) }
    var valueText by remember { mutableStateOf("") }
    var labelText by remember { mutableStateOf("") }
    var showOverrideDays by remember { mutableStateOf(false) }
    var customSchoolDaysText by remember { mutableStateOf("") }

    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }

    val rangeValid = startDateKey <= endDateKey && endDateKey <= todayKey
    val detectedSchoolDays = remember(startDateKey, endDateKey, calendarEvents, rangeValid) {
        if (rangeValid) {
            AttendanceCalculator.countSchoolDaysInRange(startDateKey, endDateKey, calendarEvents)
        } else {
            0
        }
    }
    val overrideDays = if (showOverrideDays) {
        customSchoolDaysText.toIntOrNull()?.takeIf { it in 1..400 }
    } else {
        null
    }
    val effectiveSchoolDays = overrideDays ?: detectedSchoolDays

    val overlappingBlocks = remember(startDateKey, endDateKey, existingPeriodBlocks, rangeValid) {
        if (!rangeValid) emptyList()
        else existingPeriodBlocks.filter { it.overlapsRange(startDateKey, endDateKey) }
    }
    val overlapsBaseline = remember(startDateKey, endDateKey, profile, rangeValid) {
        if (!rangeValid) false
        else {
            val baseRange = profile?.lockedBaselineRange
            baseRange != null && startDateKey <= baseRange.second && endDateKey >= baseRange.first
        }
    }
    val overlappingRecordsCount = remember(startDateKey, endDateKey, existingRecords, rangeValid) {
        if (!rangeValid) 0
        else existingRecords.count { it.dateKey in startDateKey..endDateKey }
    }

    val parsedInt = valueText.trim().toIntOrNull()
    val parsedDouble = valueText.trim().toDoubleOrNull()

    val inputError: String? = when {
        !rangeValid -> "End date must be on or after start date (and not in the future)."
        effectiveSchoolDays <= 0 -> "No school days found in this range. Pick another range or tap Adjust."
        overlappingBlocks.isNotEmpty() -> {
            val first = overlappingBlocks.first()
            "Overlaps existing locked period (${DateUtils.formatReadableShort(first.startDate)} – ${DateUtils.formatReadableShort(first.endDate)}). Unlock it first or pick non-overlapping dates."
        }
        overlapsBaseline -> "Overlaps your imported baseline period. Pick dates after ${DateUtils.formatReadableShort(profile!!.baselineEndDate!!)}."
        valueText.isBlank() -> null
        entryMode == AttendancePeriodEntryMode.MISSED_DAYS -> {
            if (parsedInt == null || parsedInt < 0) "Enter a valid number of missed days (0 or more)."
            else if (parsedInt > effectiveSchoolDays) "Missed days ($parsedInt) can't exceed total school days ($effectiveSchoolDays)."
            else null
        }
        entryMode == AttendancePeriodEntryMode.ATTENDED_DAYS -> {
            if (parsedInt == null || parsedInt < 0) "Enter a valid number of attended days (0 or more)."
            else if (parsedInt > effectiveSchoolDays) "Attended days ($parsedInt) can't exceed total school days ($effectiveSchoolDays)."
            else null
        }
        entryMode == AttendancePeriodEntryMode.PERCENTAGE -> {
            if (parsedDouble == null || parsedDouble !in 0.0..100.0) "Enter a percentage between 0 and 100."
            else null
        }
        else -> null
    }

    val estimate = remember(
        rangeValid,
        startDateKey,
        endDateKey,
        entryMode,
        parsedInt,
        parsedDouble,
        effectiveSchoolDays,
        calendarEvents,
        inputError
    ) {
        if (!rangeValid || inputError != null || effectiveSchoolDays <= 0) return@remember null
        when (entryMode) {
            AttendancePeriodEntryMode.MISSED_DAYS -> {
                val missed = parsedInt ?: return@remember null
                AttendanceCalculator.estimatePeriodFromMissedDays(
                    startDateKey = startDateKey,
                    endDateKey = endDateKey,
                    missedDays = missed,
                    calendarEvents = calendarEvents,
                    customSchoolDaysOverride = overrideDays
                )
            }
            AttendancePeriodEntryMode.ATTENDED_DAYS -> {
                val attended = parsedInt ?: return@remember null
                AttendanceCalculator.estimatePeriodFromAttendedDays(
                    startDateKey = startDateKey,
                    endDateKey = endDateKey,
                    attendedDays = attended,
                    calendarEvents = calendarEvents,
                    customSchoolDaysOverride = overrideDays
                )
            }
            AttendancePeriodEntryMode.PERCENTAGE -> {
                val pct = parsedDouble ?: return@remember null
                AttendanceCalculator.estimateBaselineFromPercentage(
                    startDateKey = startDateKey,
                    endDateKey = endDateKey,
                    percentage = pct,
                    calendarEvents = calendarEvents,
                    customSchoolDaysOverride = overrideDays
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Rounded.DateRange,
                contentDescription = "Lock period attendance",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Lock Period Attendance",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Can't remember which exact day you missed? Pick a date range and enter days missed, days attended, or a %. That period locks automatically in Calendar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Period Presets
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = startDateKey == startOfLastMonthKey && endDateKey == endOfLastMonthKey,
                        onClick = {
                            startDateKey = startOfLastMonthKey
                            endDateKey = endOfLastMonthKey
                        },
                        label = { Text("Last Month") },
                        modifier = Modifier.testTag("period_preset_last_month")
                    )
                    FilterChip(
                        selected = startDateKey == startOfThisMonthKey && endDateKey == todayKey,
                        onClick = {
                            startDateKey = startOfThisMonthKey
                            endDateKey = todayKey
                        },
                        label = { Text("This Month") },
                        modifier = Modifier.testTag("period_preset_this_month")
                    )
                    FilterChip(
                        selected = startDateKey == twoWeeksAgoKey && endDateKey == todayKey,
                        onClick = {
                            startDateKey = twoWeeksAgoKey
                            endDateKey = todayKey
                        },
                        label = { Text("Last 2 Weeks") },
                        modifier = Modifier.testTag("period_preset_two_weeks")
                    )
                }

                // Start & End Date Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickingStartDate = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("period_start_date_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "From",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateUtils.formatReadableShort(startDateKey),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { pickingEndDate = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("period_end_date_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "To",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateUtils.formatReadableShort(endDateKey),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Detected School Days Row + Optional Override
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$effectiveSchoolDays school days in range",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            TextButton(
                                onClick = {
                                    showOverrideDays = !showOverrideDays
                                    if (showOverrideDays && customSchoolDaysText.isBlank() && detectedSchoolDays > 0) {
                                        customSchoolDaysText = detectedSchoolDays.toString()
                                    }
                                },
                                modifier = Modifier.testTag("period_override_days_button")
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (showOverrideDays) "Auto" else "Adjust")
                            }
                        }
                        AnimatedVisibility(visible = showOverrideDays) {
                            OutlinedTextField(
                                value = customSchoolDaysText,
                                onValueChange = { customSchoolDaysText = it.filter(Char::isDigit).take(3) },
                                label = { Text("Actual school days held in period") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("period_custom_school_days_input")
                            )
                        }
                    }
                }

                // 3 Entry Modes: Days Missed | Days Attended | Percentage %
                val entryModes = remember {
                    listOf(
                        AttendancePeriodEntryMode.MISSED_DAYS,
                        AttendancePeriodEntryMode.ATTENDED_DAYS,
                        AttendancePeriodEntryMode.PERCENTAGE
                    )
                }
                val entryModeOptions = remember {
                    listOf(
                        LiquidGlassSegmentOption(
                            label = "Missed",
                            testTag = "period_mode_missed",
                            icon = Icons.Rounded.EventBusy
                        ),
                        LiquidGlassSegmentOption(
                            label = "Attended",
                            testTag = "period_mode_attended",
                            icon = Icons.Rounded.CheckCircle
                        ),
                        LiquidGlassSegmentOption(
                            label = "Percent %",
                            testTag = "period_mode_percent",
                            icon = Icons.Rounded.Percent
                        )
                    )
                }
                SlidableLiquidGlassSegmentedBar(
                    options = entryModeOptions,
                    selectedIndex = entryModes.indexOf(entryMode).coerceAtLeast(0),
                    onOptionSelected = { idx ->
                        val nextMode = entryModes.getOrNull(idx) ?: return@SlidableLiquidGlassSegmentedBar
                        if (entryMode != nextMode) {
                            entryMode = nextMode
                            valueText = ""
                        }
                    },
                    barHeight = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                // Value Input Field
                val fieldLabel = when (entryMode) {
                    AttendancePeriodEntryMode.MISSED_DAYS -> "How many days did you miss?"
                    AttendancePeriodEntryMode.ATTENDED_DAYS -> "How many days did you attend?"
                    AttendancePeriodEntryMode.PERCENTAGE -> "Attendance percentage (%)"
                }
                val fieldPlaceholder = when (entryMode) {
                    AttendancePeriodEntryMode.MISSED_DAYS -> "e.g., 1"
                    AttendancePeriodEntryMode.ATTENDED_DAYS -> "e.g., ${maxOf(0, effectiveSchoolDays - 1)}"
                    AttendancePeriodEntryMode.PERCENTAGE -> "e.g., 95.0"
                }
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { raw ->
                        valueText = if (entryMode == AttendancePeriodEntryMode.PERCENTAGE) {
                            raw.filter { it.isDigit() || it == '.' }.take(6)
                        } else {
                            raw.filter(Char::isDigit).take(3)
                        }
                    },
                    label = { Text(fieldLabel) },
                    placeholder = { Text(fieldPlaceholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (entryMode == AttendancePeriodEntryMode.PERCENTAGE) {
                            KeyboardType.Decimal
                        } else {
                            KeyboardType.Number
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("period_value_input")
                )

                // Optional Period Name / Label
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it.take(48) },
                    label = { Text("Period note (optional)") },
                    placeholder = { Text("e.g., October Attendance") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("period_label_input")
                )

                // Validation Error Banner
                if (inputError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "Validation error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = inputError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Live Preview Card + Lock Notice
                if (estimate != null) {
                    val isEligiblePct = estimate.effectivePercent >= 80.0
                    Surface(
                        color = if (isEligiblePct) {
                            semantic.presentContainer.copy(alpha = 0.72f)
                        } else {
                            semantic.absentContainer.copy(alpha = 0.8f)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("period_preview_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val fg = if (isEligiblePct) semantic.onPresentContainer else semantic.onAbsentContainer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${estimate.presentDays} Present • ${estimate.absentDays} Missed",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = fg
                                )
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", estimate.effectivePercent),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = fg
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = fg,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${DateUtils.formatReadableShort(startDateKey)} → ${DateUtils.formatReadableShort(endDateKey)} (${estimate.totalSchoolDays} school days) will be locked in Calendar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = fg.copy(alpha = 0.9f)
                                )
                            }
                            if (overlappingRecordsCount > 0) {
                                Text(
                                    text = "$overlappingRecordsCount individual day log${if (overlappingRecordsCount == 1) "" else "s"} in this range will be consolidated automatically so nothing is double-counted.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fg.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val est = estimate ?: return@Button
                    onSave(
                        startDateKey,
                        endDateKey,
                        est.totalSchoolDays,
                        est.presentDays,
                        est.absentDays,
                        est.effectivePercent,
                        entryMode,
                        labelText.trim().takeIf { it.isNotEmpty() }
                    )
                },
                enabled = !isSaving && estimate != null && inputError == null,
                modifier = Modifier.testTag("period_save_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Save & Lock Period")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )

    if (pickingStartDate) {
        PeriodDatePickerDialog(
            initialDateKey = startDateKey,
            maxDateKey = todayKey,
            onDismiss = { pickingStartDate = false },
            onDateSelected = { chosen ->
                startDateKey = chosen
                if (endDateKey < chosen) endDateKey = chosen
                pickingStartDate = false
            }
        )
    }

    if (pickingEndDate) {
        PeriodDatePickerDialog(
            initialDateKey = endDateKey,
            maxDateKey = todayKey,
            onDismiss = { pickingEndDate = false },
            onDateSelected = { chosen ->
                endDateKey = chosen
                if (startDateKey > chosen) startDateKey = chosen
                pickingEndDate = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodDatePickerDialog(
    initialDateKey: String,
    maxDateKey: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val maxMillis = remember(maxDateKey) { periodDateKeyToUtcMillis(maxDateKey) ?: System.currentTimeMillis() }
    val initMillis = remember(initialDateKey) { periodDateKeyToUtcMillis(initialDateKey) }
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= maxMillis
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        onDateSelected(utcMillisToPeriodDateKey(millis))
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

private fun periodDateKeyToUtcMillis(dateKey: String): Long? = runCatching {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    fmt.parse(dateKey)?.time
}.getOrNull()

private fun utcMillisToPeriodDateKey(millis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(Date(millis))
}
