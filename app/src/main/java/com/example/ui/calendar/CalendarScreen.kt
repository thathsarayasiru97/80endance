package com.example.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.CalendarEventType
import com.example.data.DateUtils
import com.example.ui.components.LiquidGlassButton
import com.example.ui.theme.LocalAttendanceColors
import com.example.viewmodel.StudentAppState
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: StudentAppState,
    onEditDate: (String) -> Unit,
    onDeleteDate: (String) -> Unit,
    onOpenBaselineImport: (() -> Unit)? = null,
    bottomBarPadding: Dp = 24.dp
) {
    val semantic = LocalAttendanceColors.current
    val initialYm = remember { DateUtils.currentYearMonth() }
    var displayYear by remember { mutableIntStateOf(initialYm.first) }
    var displayMonth by remember { mutableIntStateOf(initialYm.second) }
    var selectedDateKey by remember { mutableStateOf<String?>(null) }

    val profile = state.profile
    val recordMap = remember(state.attendanceRecords) {
        state.attendanceRecords.associateBy { it.dateKey }
    }
    val eventMap = remember(state.calendarEvents) {
        state.calendarEvents.groupBy { it.dateKey }
    }
    val todayKey = remember { DateUtils.todayKey() }
    val monthPrefix = remember(displayYear, displayMonth) {
        String.format("%04d-%02d-", displayYear, displayMonth)
    }
    val monthEvents = remember(state.calendarEvents, monthPrefix) {
        state.calendarEvents.filter { it.dateKey.startsWith(monthPrefix) }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .testTag("calendar_lazy_column"),
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
                        text = "Calendar & Poya Days",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Tap any school day up to today to log or edit attendance • ${state.activeTerm.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onOpenBaselineImport != null) {
                item {
                    val hasBaseline = profile?.hasLockedBaseline == true
                    Surface(
                        color = if (hasBaseline) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBaselineImport() }
                            .testTag("calendar_baseline_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (hasBaseline) {
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (hasBaseline) Icons.Rounded.Lock else Icons.Rounded.Percent,
                                        contentDescription = "Attendance percentage baseline",
                                        tint = if (hasBaseline) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (hasBaseline) {
                                    val pctLabel = profile?.baselinePercent?.let {
                                        String.format(java.util.Locale.US, "%.1f%%", it)
                                    } ?: "${profile?.priorPresentCount}P / ${profile?.priorAbsentCount}A"
                                    Text(
                                        text = "Imported Period Locked • $pctLabel",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = "${DateUtils.formatReadableShort(profile!!.baselineStartDate!!)} → ${DateUtils.formatReadableShort(profile.baselineEndDate!!)} (${profile.priorPresentCount}P / ${profile.priorAbsentCount}A) • Tap to update",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                    )
                                } else {
                                    Text(
                                        text = "Enter Attendance % Up to a Date",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Import your % from Day 1 (e.g., from your class teacher) and lock those days",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Grid Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Month Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (displayMonth == 1) {
                                        displayMonth = 12
                                        displayYear--
                                    } else {
                                        displayMonth--
                                    }
                                },
                                modifier = Modifier.testTag("calendar_prev_month")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                            }
                            Text(
                                text = DateUtils.formatMonthYear(displayYear, displayMonth),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = {
                                    if (displayMonth == 12) {
                                        displayMonth = 1
                                        displayYear++
                                    } else {
                                        displayMonth++
                                    }
                                },
                                modifier = Modifier.testTag("calendar_next_month")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Weekday headers
                        val weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
                        Row(modifier = Modifier.fillMaxWidth()) {
                            weekHeaders.forEachIndexed { idx, label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (idx >= 5) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val firstDow = DateUtils.firstDayOfWeekMonZero(displayYear, displayMonth)
                        val daysCount = DateUtils.daysInMonth(displayYear, displayMonth)
                        val totalCells = ((firstDow + daysCount + 6) / 7) * 7

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in 0 until (totalCells / 7)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    for (col in 0 until 7) {
                                        val cellIdx = row * 7 + col
                                        val dayNum = cellIdx - firstDow + 1
                                        if (dayNum < 1 || dayNum > daysCount) {
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                        } else {
                                            val dateKey = DateUtils.formatDateKey(displayYear, displayMonth, dayNum)
                                            val isWeekend = col >= 5 || DateUtils.isWeekend(dateKey)
                                            val isFuture = dateKey > todayKey
                                            val isLocked = !isWeekend && profile?.isDateInLockedBaseline(dateKey) == true
                                            val rec = if (isWeekend || isFuture || isLocked) null else recordMap[dateKey]
                                            val dayEvents = eventMap[dateKey].orEmpty()
                                            val isPoya = dayEvents.any { it.eventTypeEnum == CalendarEventType.POYA }
                                            val isHoliday = dayEvents.any { it.nonSchoolDay }
                                            val isToday = dateKey == todayKey
                                            val isInteractive = !isWeekend && (!isFuture || dayEvents.isNotEmpty())

                                            val cellBg = when {
                                                isWeekend -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                                isPoya || isHoliday -> semantic.poyaContainer.copy(alpha = if (isFuture) 0.52f else 0.78f)
                                                isLocked -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
                                                rec?.statusEnum == AttendanceStatus.PRESENT -> semantic.presentContainer
                                                rec?.statusEnum == AttendanceStatus.ABSENT -> semantic.absentContainer
                                                rec?.statusEnum == AttendanceStatus.MEDICAL -> semantic.medicalContainer
                                                rec?.statusEnum == AttendanceStatus.SCHOOL_EVENT -> semantic.schoolEventContainer
                                                else -> Color.Transparent
                                            }
                                            val textColor = when {
                                                isWeekend -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                                                isPoya || isHoliday -> semantic.onPoyaContainer.copy(alpha = if (isFuture) 0.72f else 1f)
                                                isLocked -> MaterialTheme.colorScheme.onSecondaryContainer
                                                isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                                rec?.statusEnum == AttendanceStatus.PRESENT -> semantic.onPresentContainer
                                                rec?.statusEnum == AttendanceStatus.ABSENT -> semantic.onAbsentContainer
                                                rec?.statusEnum == AttendanceStatus.MEDICAL -> semantic.onMedicalContainer
                                                rec?.statusEnum == AttendanceStatus.SCHOOL_EVENT -> semantic.onSchoolEventContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(cellBg)
                                                    .then(
                                                        if (isToday) {
                                                            Modifier.border(
                                                                width = 2.dp,
                                                                color = if (isWeekend) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .then(
                                                        if (isInteractive) {
                                                            Modifier.clickable { selectedDateKey = dateKey }
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .testTag("calendar_day_$dateKey")
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = dayNum.toString(),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = textColor,
                                                        fontWeight = if (!isWeekend && !isFuture && (isToday || rec != null || isLocked)) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                    if (!isWeekend) {
                                                        when {
                                                            isPoya || isHoliday -> {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(semantic.poya)
                                                                )
                                                            }
                                                            isLocked -> {
                                                                Icon(
                                                                    imageVector = Icons.Rounded.Lock,
                                                                    contentDescription = "Locked imported attendance day",
                                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                                                                    modifier = Modifier.size(10.dp)
                                                                )
                                                            }
                                                            dayEvents.isNotEmpty() -> {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(MaterialTheme.colorScheme.primary)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Compact Color Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LegendItem("Present", semantic.present)
                            LegendItem("Absent", semantic.absent)
                            LegendItem("Excused", semantic.schoolEvent)
                            LegendItem("Medical", semantic.medical)
                            if (profile?.hasLockedBaseline == true) {
                                LegendItem("Locked", MaterialTheme.colorScheme.secondary)
                            } else {
                                LegendItem("No School", semantic.poya)
                            }
                        }
                    }
                }
            }

            // Sri Lankan Poya Days & Holidays this month
            item {
                Text(
                    text = "Sri Lankan Holidays & Milestones",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (monthEvents.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No Poya days or school holidays listed for this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(monthEvents, key = { it.id }) { ev ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDateKey = ev.dateKey }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = semantic.poyaContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (ev.eventTypeEnum == CalendarEventType.POYA) Icons.Rounded.NightsStay else Icons.Rounded.Event,
                                        contentDescription = ev.title,
                                        tint = semantic.onPoyaContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ev.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                val audienceSuffix = ev.targetDisplayLabel?.let { " • $it" }.orEmpty()
                                Text(
                                    text = "${DateUtils.formatReadableShort(ev.dateKey)} • ${ev.eventTypeEnum.label}$audienceSuffix",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Active School Terms Card
            item {
                Text(
                    text = "Sri Lankan School Terms",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(state.terms.filter { it.academicYear == displayYear }.ifEmpty { state.terms.take(3) }, key = { it.id }) { term ->
                Surface(
                    color = if (term.id == state.activeTerm.id) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(term.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${DateUtils.formatReadableShort(term.startDate)} → ${DateUtils.formatReadableShort(term.endDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${term.minEligibilityPercent}% rule",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // Day Detail Bottom Sheet
    selectedDateKey?.let { dateKey ->
        val record = recordMap[dateKey]
        val events = eventMap[dateKey].orEmpty()
        ModalBottomSheet(onDismissRequest = { selectedDateKey = null }) {
            val sheetBackdrop = rememberLayerBackdrop()
            val sheetSurface = MaterialTheme.colorScheme.surfaceContainerLow
            val sheetGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .layerBackdrop(sheetBackdrop)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(sheetSurface, sheetGlow, sheetSurface)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                Text(
                    text = DateUtils.formatReadableFull(dateKey),
                    style = MaterialTheme.typography.titleLarge
                )

                events.forEach { ev ->
                    Surface(
                        color = semantic.poyaContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NightsStay,
                                contentDescription = null,
                                tint = semantic.onPoyaContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(ev.title, style = MaterialTheme.typography.titleSmall, color = semantic.onPoyaContainer)
                                val audienceSuffix = ev.targetDisplayLabel?.let { " • $it" }.orEmpty()
                                Text(
                                    "${ev.eventTypeEnum.label}$audienceSuffix",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.onPoyaContainer
                                )
                            }
                        }
                    }
                }

                val isSelectedWeekend = DateUtils.isWeekend(dateKey)
                val isSelectedFuture = dateKey > todayKey
                val isSelectedLocked = !isSelectedWeekend && profile?.isDateInLockedBaseline(dateKey) == true

                when {
                    isSelectedWeekend -> {
                        Text(
                            text = "Weekend — no attendance is tracked on Saturdays or Sundays.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                    isSelectedFuture -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                .testTag("calendar_future_day_notice")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = "Future date",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Upcoming date — attendance can only be marked on or before today.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    isSelectedLocked -> {
                        val pctLabel = profile?.baselinePercent?.let {
                            String.format(java.util.Locale.US, "%.1f%%", it)
                        } ?: "imported attendance"
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calendar_locked_day_notice")
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Locked day",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Locked • Included in Imported $pctLabel",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Text(
                                    text = "Covered by ${DateUtils.formatReadableShort(profile!!.baselineStartDate!!)} → ${DateUtils.formatReadableShort(profile.baselineEndDate!!)} (${profile.priorPresentCount} Present, ${profile.priorAbsentCount} Absent). Individual days in this period cannot be edited.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f)
                                )
                            }
                        }
                        if (onOpenBaselineImport != null) {
                            OutlinedButton(
                                onClick = {
                                    selectedDateKey = null
                                    onOpenBaselineImport()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp)
                            ) {
                                Icon(Icons.Rounded.Percent, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manage Imported Period")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    else -> {
                        if (record != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Logged Status: ${record.statusEnum.label}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (record.subjectTags.isNotEmpty()) {
                                        Text(
                                            text = "Subjects: ${record.subjectTags.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!record.note.isNullOrBlank()) {
                                        Text(
                                            text = "Note: ${record.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No attendance recorded for this date yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            if (record != null) {
                                OutlinedButton(
                                    onClick = {
                                        onDeleteDate(dateKey)
                                        selectedDateKey = null
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear")
                                }
                            }
                            LiquidGlassButton(
                                onClick = {
                                    selectedDateKey = null
                                    onEditDate(dateKey)
                                },
                                backdrop = sheetBackdrop,
                                tint = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.primary,
                                minHeight = 48.dp,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1.4f)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (record == null) "Log Attendance" else "Edit Entry")
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
