package com.example.ui.admin

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppConstants
import com.example.data.CalendarEventTargetType
import com.example.data.DateUtils
import com.example.ui.theme.LocalAttendanceColors

private enum class NoSchoolDateMode(val label: String) {
    SINGLE("Single Day"),
    RANGE("Date Range"),
    MULTIPLE("Multi-Day")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminNoSchoolCard(
    isBusy: Boolean,
    onMarkNoSchoolDays: (
        dateKeys: List<String>,
        reason: String,
        targetType: CalendarEventTargetType,
        targetValue: String?,
        alsoPostNotice: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAttendanceColors.current
    val dateRegex = remember { Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$") }
    val todayKey = remember { DateUtils.todayKey() }
    val tomorrowKey = remember(todayKey) { DateUtils.addDays(todayKey, 1) }
    val inTwoDaysKey = remember(todayKey) { DateUtils.addDays(todayKey, 2) }

    var dateMode by remember { mutableStateOf(NoSchoolDateMode.SINGLE) }

    // Single day state
    var singleDateKey by remember { mutableStateOf(tomorrowKey) }

    // Range state
    var rangeStartKey by remember { mutableStateOf(tomorrowKey) }
    var rangeEndKey by remember { mutableStateOf(DateUtils.addDays(tomorrowKey, 4)) }
    var rangeSkipWeekends by remember { mutableStateOf(true) }

    // Multiple custom dates state
    var multiDateInput by remember { mutableStateOf(tomorrowKey) }
    var multiDates by remember { mutableStateOf(listOf(tomorrowKey)) }

    // Audience targeting state (Everyone / Grade / Stream / Specific Class)
    var targetType by remember { mutableStateOf(CalendarEventTargetType.ALL) }
    var targetValue by remember { mutableStateOf("") }

    // Reason preset + custom reason state
    val presetReasons = remember {
        listOf(
            "Exam Study Leave",
            "Interval Day Between Exams",
            "Term Vacation",
            "Special School Holiday",
            "Weather Closure"
        )
    }
    var useCustomReason by remember { mutableStateOf(false) }
    var reasonText by remember { mutableStateOf(presetReasons.first()) }
    var alsoPostNotice by remember { mutableStateOf(true) }

    val resolvedDates = remember(
        dateMode, singleDateKey, rangeStartKey, rangeEndKey, rangeSkipWeekends, multiDates
    ) {
        when (dateMode) {
            NoSchoolDateMode.SINGLE -> {
                val dk = singleDateKey.trim()
                if (dk.matches(dateRegex)) listOf(dk) else emptyList()
            }
            NoSchoolDateMode.RANGE -> {
                if (rangeStartKey.trim().matches(dateRegex) && rangeEndKey.trim().matches(dateRegex)) {
                    DateUtils.expandDateRange(
                        startDateKey = rangeStartKey.trim(),
                        endDateKey = rangeEndKey.trim(),
                        skipWeekends = rangeSkipWeekends,
                        maxDays = 60
                    )
                } else {
                    emptyList()
                }
            }
            NoSchoolDateMode.MULTIPLE -> multiDates.filter { it.matches(dateRegex) }.distinct().sorted()
        }
    }

    val targetValid = targetType == CalendarEventTargetType.ALL || targetValue.trim().isNotEmpty()
    val reasonValid = reasonText.trim().isNotEmpty()
    val canSubmit = !isBusy && resolvedDates.isNotEmpty() && targetValid && reasonValid

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = semantic.poyaContainer.copy(alpha = 0.65f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_no_school_card")
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
                    imageVector = Icons.Rounded.EventBusy,
                    contentDescription = null,
                    tint = semantic.onPoyaContainer
                )
                Text(
                    text = "Mark No School Day(s)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = semantic.onPoyaContainer
                )
            }
            Text(
                text = "Mark a single day, a holiday range, or specific exam-interval dates for everyone or a selected grade, stream, or class.",
                style = MaterialTheme.typography.bodySmall,
                color = semantic.onPoyaContainer.copy(alpha = 0.85f)
            )

            // 1. Date Mode Segmented Switcher
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_no_school_mode_selector")
            ) {
                NoSchoolDateMode.entries.forEachIndexed { idx, mode ->
                    SegmentedButton(
                        selected = dateMode == mode,
                        onClick = { dateMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = NoSchoolDateMode.entries.size),
                        modifier = Modifier.testTag("admin_no_school_mode_${mode.name.lowercase()}")
                    ) {
                        Text(mode.label, maxLines = 1)
                    }
                }
            }

            // Date Pickers per mode
            when (dateMode) {
                NoSchoolDateMode.SINGLE -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = singleDateKey == todayKey,
                            onClick = { singleDateKey = todayKey },
                            label = { Text("Today") }
                        )
                        FilterChip(
                            selected = singleDateKey == tomorrowKey,
                            onClick = { singleDateKey = tomorrowKey },
                            label = { Text("Tomorrow") }
                        )
                        FilterChip(
                            selected = singleDateKey == inTwoDaysKey,
                            onClick = { singleDateKey = inTwoDaysKey },
                            label = { Text("+2 Days") }
                        )
                    }
                    OutlinedTextField(
                        value = singleDateKey,
                        onValueChange = { singleDateKey = it.take(10) },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_no_school_date_input")
                    )
                }

                NoSchoolDateMode.RANGE -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("3 Days" to 2, "5 Days" to 4, "1 Week" to 6, "2 Weeks" to 13).forEach { (label, delta) ->
                            FilterChip(
                                selected = rangeEndKey == DateUtils.addDays(rangeStartKey, delta),
                                onClick = {
                                    if (rangeStartKey.matches(dateRegex)) {
                                        rangeEndKey = DateUtils.addDays(rangeStartKey, delta)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = rangeStartKey,
                            onValueChange = { rangeStartKey = it.take(10) },
                            label = { Text("Start (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_no_school_range_start")
                        )
                        OutlinedTextField(
                            value = rangeEndKey,
                            onValueChange = { rangeEndKey = it.take(10) },
                            label = { Text("End (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_no_school_range_end")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Skip Saturdays & Sundays",
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.onPoyaContainer
                        )
                        Switch(
                            checked = rangeSkipWeekends,
                            onCheckedChange = { rangeSkipWeekends = it }
                        )
                    }
                    Surface(
                        color = semantic.poya.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (resolvedDates.isEmpty()) {
                                "Enter a valid start & end date (end >= start)"
                            } else {
                                "${resolvedDates.size} school day${if (resolvedDates.size == 1) "" else "s"} will be marked (${DateUtils.formatReadableShort(resolvedDates.first())} → ${DateUtils.formatReadableShort(resolvedDates.last())})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = semantic.onPoyaContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                NoSchoolDateMode.MULTIPLE -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = multiDateInput,
                            onValueChange = { multiDateInput = it.take(10) },
                            label = { Text("Add Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_no_school_multi_input")
                        )
                        OutlinedButton(
                            onClick = {
                                val dk = multiDateInput.trim()
                                if (dk.matches(dateRegex) && dk !in multiDates) {
                                    multiDates = (multiDates + dk).sorted()
                                    multiDateInput = DateUtils.addDays(dk, 2)
                                }
                            },
                            enabled = multiDateInput.trim().matches(dateRegex),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("admin_no_school_add_date_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add date", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                    if (multiDates.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            multiDates.forEach { dk ->
                                InputChip(
                                    selected = true,
                                    onClick = { multiDates = multiDates - dk },
                                    label = { Text(DateUtils.formatReadableShort(dk)) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove $dk",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Audience Targeting (Everyone / Grade / Stream / Specific Class)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Applies To",
                    style = MaterialTheme.typography.titleSmall,
                    color = semantic.onPoyaContainer
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CalendarEventTargetType.entries.forEach { scope ->
                        FilterChip(
                            selected = targetType == scope,
                            onClick = {
                                targetType = scope
                                targetValue = when (scope) {
                                    CalendarEventTargetType.ALL -> ""
                                    CalendarEventTargetType.GRADE -> AppConstants.GRADES.last()
                                    CalendarEventTargetType.STREAM -> AppConstants.AL_STREAMS.first()
                                    CalendarEventTargetType.CLASS -> ""
                                }
                            },
                            label = { Text(scope.label) },
                            modifier = Modifier.testTag("admin_no_school_target_${scope.raw}")
                        )
                    }
                }

                when (targetType) {
                    CalendarEventTargetType.ALL -> Unit
                    CalendarEventTargetType.GRADE -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppConstants.GRADES.forEach { g ->
                                FilterChip(
                                    selected = targetValue == g,
                                    onClick = { targetValue = g },
                                    label = { Text(g) }
                                )
                            }
                        }
                    }
                    CalendarEventTargetType.STREAM -> {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AppConstants.AL_STREAMS.forEach { st ->
                                FilterChip(
                                    selected = targetValue == st,
                                    onClick = { targetValue = st },
                                    label = { Text(st) }
                                )
                            }
                        }
                    }
                    CalendarEventTargetType.CLASS -> {
                        OutlinedTextField(
                            value = targetValue,
                            onValueChange = { targetValue = it.uppercase().take(20) },
                            label = { Text("Class Code (e.g. 13M2, 12B1)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_no_school_target_class_input")
                        )
                    }
                }
            }

            // 3. Reason Presets + Custom Reason
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Reason Shown to Students",
                    style = MaterialTheme.typography.titleSmall,
                    color = semantic.onPoyaContainer
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetReasons.forEach { preset ->
                        FilterChip(
                            selected = !useCustomReason && reasonText == preset,
                            onClick = {
                                useCustomReason = false
                                reasonText = preset
                            },
                            label = { Text(preset) }
                        )
                    }
                    FilterChip(
                        selected = useCustomReason,
                        onClick = {
                            useCustomReason = true
                            if (reasonText in presetReasons) {
                                reasonText = ""
                            }
                        },
                        label = { Text("Custom Reason") },
                        modifier = Modifier.testTag("admin_no_school_custom_reason_chip")
                    )
                }

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = {
                        reasonText = it.take(100)
                        if (it !in presetReasons) {
                            useCustomReason = true
                        }
                    },
                    label = {
                        Text(if (useCustomReason) "Custom reason (required)" else "Reason (or edit to customize)")
                    },
                    placeholder = { Text("e.g., Physics Lab Maintenance, Big Match Holiday") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_no_school_reason_input")
                )
            }

            // 4. Pin Notice Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Also pin a notice on Home",
                        style = MaterialTheme.typography.titleSmall,
                        color = semantic.onPoyaContainer
                    )
                    Text(
                        text = if (targetType == CalendarEventTargetType.ALL) {
                            "Posts an urgent banner visible to all students"
                        } else {
                            "Posts an urgent banner targeted only to ${targetValue.ifBlank { targetType.label }}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.onPoyaContainer.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = alsoPostNotice,
                    onCheckedChange = { alsoPostNotice = it }
                )
            }

            // 5. Primary Submit CTA
            val audienceSummary = if (targetType == CalendarEventTargetType.ALL) {
                "Everyone"
            } else {
                targetValue.trim().ifEmpty { targetType.label }
            }
            val buttonLabel = when (resolvedDates.size) {
                0 -> "Select Valid Date(s)"
                1 -> "Mark No School ($audienceSummary)"
                else -> "Mark ${resolvedDates.size} No School Days ($audienceSummary)"
            }
            Button(
                onClick = {
                    onMarkNoSchoolDays(
                        resolvedDates,
                        reasonText.trim(),
                        targetType,
                        targetValue.trim().ifEmpty { null },
                        alsoPostNotice
                    )
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_mark_no_school_button")
            ) {
                Icon(Icons.Rounded.EventBusy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonLabel)
            }
        }
    }
}
