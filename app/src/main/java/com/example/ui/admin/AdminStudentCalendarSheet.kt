package com.example.ui.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AcademicTerm
import com.example.data.AppSettings
import com.example.data.AttendanceCalculator
import com.example.data.AttendancePeriodBlock
import com.example.data.AttendanceRecord
import com.example.data.AttendanceStatus
import com.example.data.CalendarEvent
import com.example.data.CalendarEventType
import com.example.data.DateUtils
import com.example.data.UserProfile
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.LocalAttendanceColors
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminStudentCalendarSheet(
    student: UserProfile,
    records: List<AttendanceRecord>,
    periodBlocks: List<AttendancePeriodBlock>,
    calendarEvents: List<CalendarEvent>,
    terms: List<AcademicTerm>,
    appSettings: AppSettings,
    presenceNowMillis: Long,
    onAvatarClick: () -> Unit,
    onEditProfile: () -> Unit,
    onToggleGuestObserver: (Boolean) -> Unit,
    onLogOrOverrideDate: (String) -> Unit,
    onDeleteDateAttendance: (String) -> Unit,
    onDeletePeriodBlock: (String) -> Unit,
    onRemoveAndBlockStudent: (String) -> Unit
) {
    val semantic = LocalAttendanceColors.current
    val initialYm = remember { DateUtils.currentYearMonth() }
    var displayYear by remember { mutableIntStateOf(initialYm.first) }
    var displayMonth by remember { mutableIntStateOf(initialYm.second) }
    val todayKey = remember { DateUtils.todayKey() }
    var selectedDateKey by remember { mutableStateOf(todayKey) }
    var showRemoveBlockDialog by remember { mutableStateOf(false) }

    val isCurrentMonth = displayYear == initialYm.first && displayMonth == initialYm.second
    val goToPrevMonth: () -> Unit = {
        if (displayMonth == 1) {
            displayMonth = 12
            displayYear--
        } else {
            displayMonth--
        }
    }
    val goToNextMonth: () -> Unit = {
        if (displayMonth == 12) {
            displayMonth = 1
            displayYear++
        } else {
            displayMonth++
        }
    }

    val filteredEvents = remember(calendarEvents, student) {
        calendarEvents.filter { it.appliesToStudent(student) }
    }

    val effectiveSchoolStartDate = remember(terms, appSettings, student.alExamYear) {
        val earliestTermStart = terms
            .map { it.startDate }
            .filter { it.length == 10 }
            .minOrNull()
        listOfNotNull(
            appSettings.effectiveSchoolStartDate(student.alExamYear).takeIf { it.isNotBlank() },
            earliestTermStart
        ).minOrNull() ?: "2026-01-05"
    }

    val liveStats = remember(
        records,
        periodBlocks,
        filteredEvents,
        student.priorPresentCount,
        student.priorAbsentCount,
        student.lockedBaselineRange,
        student.countExcusedAsPresent,
        appSettings.defaultEligibilityPercent,
        effectiveSchoolStartDate
    ) {
        AttendanceCalculator.calculateStats(
            records = records,
            thresholdPercent = appSettings.defaultEligibilityPercent,
            countExcusedAsPresent = student.countExcusedAsPresent,
            includePriorBalance = true,
            priorPresent = student.priorPresentCount,
            priorAbsent = student.priorAbsentCount,
            lockedRange = student.lockedBaselineRange,
            periodBlocks = periodBlocks,
            calendarEvents = filteredEvents,
            schoolStartDate = effectiveSchoolStartDate
        )
    }

    val recordsByDate = remember(records) { records.associateBy { it.dateKey } }
    val eventsByDate = remember(filteredEvents) { filteredEvents.groupBy { it.dateKey } }
    val lockedRange = student.lockedBaselineRange

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Student Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                val isOnline = !student.guestObserver && student.isCurrentlyOnline(presenceNowMillis)
                val lastSeenLabel = DateUtils.formatLastSeen(
                    timestampMillis = student.lastSeenAt?.toDate()?.time,
                    isOnline = isOnline,
                    nowMillis = presenceNowMillis
                )
                Box {
                    ProfileAvatar(
                        customAvatarBase64 = student.customAvatarBase64,
                        googlePhotoUrl = student.photoUrl,
                        displayName = student.fullName,
                        size = 54.dp,
                        onClick = onAvatarClick,
                        modifier = Modifier.testTag("admin_detail_student_avatar")
                    )
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(semantic.present)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (student.guestObserver) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "GUEST / DEMO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${student.className} • ${student.alStream} • ${student.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (student.guestObserver) {
                            "Detached Observer Mode • $lastSeenLabel"
                        } else {
                            lastSeenLabel
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (student.guestObserver) {
                            MaterialTheme.colorScheme.tertiary
                        } else if (isOnline) {
                            semantic.present
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (liveStats.eligible) semantic.presentContainer else semantic.absentContainer
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f%%", liveStats.percentage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (liveStats.eligible) semantic.onPresentContainer else semantic.onAbsentContainer
                    )
                    Text(
                        text = "${liveStats.effectiveAttended}/${liveStats.totalCountedDays}d",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (liveStats.eligible) semantic.onPresentContainer else semantic.onAbsentContainer
                    )
                }
            }
        }

        // 2. Summary Breakdown Pills (Present, Absent, Excused, Medical, Locked Periods, Baseline)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatMiniChip(
                label = "Present: ${liveStats.presentDays + liveStats.priorPresentDays + liveStats.periodPresentDays}",
                bg = semantic.presentContainer,
                fg = semantic.onPresentContainer
            )
            StatMiniChip(
                label = "Absent: ${liveStats.absentDays + liveStats.priorAbsentDays + liveStats.periodAbsentDays}",
                bg = semantic.absentContainer,
                fg = semantic.onAbsentContainer
            )
            if (liveStats.schoolEventDays > 0) {
                StatMiniChip(
                    label = "Excused: ${liveStats.schoolEventDays}",
                    bg = semantic.schoolEventContainer,
                    fg = semantic.onSchoolEventContainer
                )
            }
            if (liveStats.medicalDays > 0) {
                StatMiniChip(
                    label = "Medical: ${liveStats.medicalDays}",
                    bg = semantic.medicalContainer,
                    fg = semantic.onMedicalContainer
                )
            }
            if (liveStats.periodPresentDays > 0 || liveStats.periodAbsentDays > 0) {
                StatMiniChip(
                    label = "Locked Periods: +${liveStats.periodPresentDays}P / +${liveStats.periodAbsentDays}A",
                    bg = MaterialTheme.colorScheme.secondaryContainer,
                    fg = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            if (liveStats.priorPresentDays > 0 || liveStats.priorAbsentDays > 0) {
                StatMiniChip(
                    label = "Baseline: +${liveStats.priorPresentDays}P / +${liveStats.priorAbsentDays}A",
                    bg = MaterialTheme.colorScheme.surfaceVariant,
                    fg = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 3. Guest / Observer Mode Switch Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_guest_observer_card"),
            shape = RoundedCornerShape(16.dp),
            color = if (student.guestObserver) {
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            border = BorderStroke(
                1.dp,
                if (student.guestObserver) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Science,
                        contentDescription = null,
                        tint = if (student.guestObserver) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Guest / Observer Demo Account",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "For teachers & old boys trying the app. Everything works for them, but their info is detached from school stats, polls & streaks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = student.guestObserver,
                    onCheckedChange = onToggleGuestObserver,
                    modifier = Modifier.testTag("admin_guest_observer_switch")
                )
            }
        }

        // 4. Full Monthly Attendance Calendar (just like Calendar tab)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_student_calendar_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            var dragAccumulator by remember { mutableFloatStateOf(0f) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(displayYear, displayMonth) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                dragAccumulator += dragAmount
                            },
                            onDragEnd = {
                                if (dragAccumulator > 110f) {
                                    goToPrevMonth()
                                } else if (dragAccumulator < -110f) {
                                    goToNextMonth()
                                }
                            }
                        )
                    }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Month navigation header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = goToPrevMonth,
                        modifier = Modifier.testTag("admin_cal_prev_month")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Previous Month"
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = DateUtils.formatMonthYear(displayYear, displayMonth),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (!isCurrentMonth) {
                            Surface(
                                onClick = {
                                    displayYear = initialYm.first
                                    displayMonth = initialYm.second
                                    selectedDateKey = todayKey
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Today,
                                        contentDescription = "Jump to Today",
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Jump to Today",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Tap any date to inspect or override",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = goToNextMonth,
                        modifier = Modifier.testTag("admin_cal_next_month")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Next Month"
                        )
                    }
                }

                // Weekday headers (Mon - Sun)
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { idx, day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (idx >= 5) MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Animated calendar grid
                AnimatedContent(
                    targetState = displayYear * 100 + displayMonth,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(tween(220)) { it / 3 } + fadeIn(tween(180)))
                                .togetherWith(slideOutHorizontally(tween(180)) { -it / 3 } + fadeOut(tween(140)))
                        } else {
                            (slideInHorizontally(tween(220)) { -it / 3 } + fadeIn(tween(180)))
                                .togetherWith(slideOutHorizontally(tween(180)) { it / 3 } + fadeOut(tween(140)))
                        }
                    },
                    label = "AdminStudentMonthGrid"
                ) { ymCode ->
                    val yr = ymCode / 100
                    val mo = ymCode % 100
                    val leadingEmptyCells = DateUtils.firstDayOfWeekMonZero(yr, mo)
                    val daysInMonth = DateUtils.daysInMonth(yr, mo)
                    val totalCells = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (week in 0 until (totalCells / 7)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (dayOfWeek in 0..6) {
                                    val cellIndex = week * 7 + dayOfWeek
                                    val dayNumber = cellIndex - leadingEmptyCells + 1
                                    if (dayNumber in 1..daysInMonth) {
                                        val dateKey = DateUtils.formatDateKey(yr, mo, dayNumber)
                                        val record = recordsByDate[dateKey]
                                        val dayEvents = eventsByDate[dateKey].orEmpty()
                                        val isPoya = dayEvents.any { it.eventTypeEnum == CalendarEventType.POYA }
                                        val isNonSchoolEvent = dayEvents.any { it.nonSchoolDay }
                                        val isBeforeSchoolStart = effectiveSchoolStartDate.isNotBlank() &&
                                            dateKey < effectiveSchoolStartDate
                                        val isBaselineLocked = student.isDateInLockedBaseline(dateKey)
                                        val matchingPeriodBlock = periodBlocks.firstOrNull { it.containsDate(dateKey) }
                                        val isLockedDate = isBaselineLocked || matchingPeriodBlock != null

                                        AdminCalendarDayCell(
                                            modifier = Modifier.weight(1f),
                                            dayNumber = dayNumber,
                                            isToday = dateKey == todayKey,
                                            isSelected = dateKey == selectedDateKey,
                                            isWeekend = dayOfWeek >= 5,
                                            isBeforeSchoolStart = isBeforeSchoolStart,
                                            isLocked = isLockedDate,
                                            record = record,
                                            hasPoya = isPoya,
                                            hasNonSchoolEvent = isNonSchoolEvent && !isPoya,
                                            onClick = {
                                                if (selectedDateKey == dateKey && !isLockedDate) {
                                                    onLogOrOverrideDate(dateKey)
                                                } else {
                                                    selectedDateKey = dateKey
                                                }
                                            }
                                        )
                                    } else {
                                        Spacer(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Color Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AdminLegendDot(color = semantic.present, label = "Present")
                    AdminLegendDot(color = semantic.absent, label = "Absent")
                    AdminLegendDot(color = semantic.schoolEvent, label = "Excused")
                    AdminLegendDot(color = semantic.medical, label = "Medical")
                    AdminLegendDot(color = MaterialTheme.colorScheme.secondary, label = "Locked")
                    AdminLegendDot(color = semantic.poya, label = "No School")
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

                // Selected Day Inspector & Quick Action Row
                val selectedRecord = recordsByDate[selectedDateKey]
                val selectedEvents = eventsByDate[selectedDateKey].orEmpty()
                val selectedPeriodBlock = periodBlocks.firstOrNull { it.containsDate(selectedDateKey) }
                val isSelectedBaselineLocked = student.isDateInLockedBaseline(selectedDateKey)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = DateUtils.formatReadableFull(selectedDateKey),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                val statusSubtitle = when {
                                    selectedPeriodBlock != null -> {
                                        val title = selectedPeriodBlock.label?.takeIf { it.isNotBlank() } ?: "Locked Time Period"
                                        "Covered by $title (${selectedPeriodBlock.presentDays}P / ${selectedPeriodBlock.absentDays}A)"
                                    }
                                    isSelectedBaselineLocked -> "Covered by Imported Baseline (${student.priorPresentCount}P / ${student.priorAbsentCount}A)"
                                    selectedRecord != null -> {
                                        val label = selectedRecord.statusEnum.label
                                        val note = selectedRecord.note?.takeIf { it.isNotBlank() }
                                        if (note != null) "$label • $note" else label
                                    }
                                    selectedEvents.isNotEmpty() -> {
                                        val firstEv = selectedEvents.first()
                                        "${firstEv.title} (${firstEv.eventTypeEnum.label})"
                                    }
                                    else -> "No attendance logged for this date"
                                }
                                Text(
                                    text = statusSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (selectedRecord != null) {
                                    IconButton(
                                        onClick = { onDeleteDateAttendance(selectedDateKey) },
                                        modifier = Modifier.testTag("admin_clear_selected_date")
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Clear Date Attendance",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { onLogOrOverrideDate(selectedDateKey) },
                                    modifier = Modifier.testTag("admin_override_selected_date")
                                ) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (selectedRecord != null) "Edit" else "Log Day")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Locked Baseline & Locked Time Periods Section (matching Calendar tab)
        if (student.hasLockedBaseline || periodBlocks.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_student_locked_periods_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Locked Time Periods & Baseline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    if (student.hasLockedBaseline && lockedRange != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
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
                                        text = "Imported Baseline (${DateUtils.formatReadableShort(lockedRange.first)} → ${DateUtils.formatReadableShort(lockedRange.second)})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${student.priorPresentCount} Present • ${student.priorAbsentCount} Absent",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = onEditProfile) {
                                    Text("Edit Baseline")
                                }
                            }
                        }
                    }

                    periodBlocks.forEach { block ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val rangeLabel = "${DateUtils.formatReadableShort(block.startDate)} → ${DateUtils.formatReadableShort(block.endDate)}"
                                    val blockTitle = block.label?.takeIf { it.isNotBlank() }?.let { "$it ($rangeLabel)" } ?: rangeLabel
                                    Text(
                                        text = blockTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f%%", block.percentage)} • ${block.presentDays} Present / ${block.absentDays} Absent (${block.totalSchoolDays} school days)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onDeletePeriodBlock(block.id) },
                                    modifier = Modifier.testTag("admin_unlock_period_${block.id}")
                                ) {
                                    Icon(
                                        Icons.Rounded.LockOpen,
                                        contentDescription = "Unlock Period",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Bottom Admin Actions (Edit Balance / Class + Remove & Block User)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_edit_student_profile_button")
            ) {
                Icon(Icons.Rounded.HistoryEdu, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit Balance / Class")
            }

            Button(
                onClick = { showRemoveBlockDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_remove_block_student_button")
            ) {
                Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Remove & Block")
            }
        }
    }

    if (showRemoveBlockDialog) {
        var reason by remember { mutableStateOf("Removed by administrator") }
        AlertDialog(
            onDismissRequest = { showRemoveBlockDialog = false },
            icon = {
                Icon(
                    Icons.Rounded.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remove User & Block Email?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This will permanently remove ${student.fullName} from 80endance, delete their attendance & poll records, and block their email (${student.email.lowercase()}) from accessing the app.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Note / Reason (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveBlockDialog = false
                        onRemoveAndBlockStudent(reason)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("admin_confirm_remove_block_button")
                ) {
                    Text("Remove & Block Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatMiniChip(
    label: String,
    bg: Color,
    fg: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AdminCalendarDayCell(
    modifier: Modifier = Modifier,
    dayNumber: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isWeekend: Boolean,
    isBeforeSchoolStart: Boolean,
    isLocked: Boolean,
    record: AttendanceRecord?,
    hasPoya: Boolean,
    hasNonSchoolEvent: Boolean,
    onClick: () -> Unit
) {
    val semantic = LocalAttendanceColors.current
    val status = record?.statusEnum

    val targetBgColor = when {
        status == AttendanceStatus.PRESENT -> semantic.presentContainer
        status == AttendanceStatus.ABSENT -> semantic.absentContainer
        status == AttendanceStatus.SCHOOL_EVENT -> semantic.schoolEventContainer
        status == AttendanceStatus.MEDICAL -> semantic.medicalContainer
        hasPoya -> semantic.poyaContainer
        hasNonSchoolEvent || status == AttendanceStatus.HOLIDAY -> MaterialTheme.colorScheme.tertiaryContainer
        isBeforeSchoolStart -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        isLocked && !isWeekend -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        isWeekend -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    val bgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "adminDayBgColor"
    )

    val textColor = when {
        status == AttendanceStatus.PRESENT -> semantic.onPresentContainer
        status == AttendanceStatus.ABSENT -> semantic.onAbsentContainer
        status == AttendanceStatus.SCHOOL_EVENT -> semantic.onSchoolEventContainer
        status == AttendanceStatus.MEDICAL -> semantic.onMedicalContainer
        hasPoya -> semantic.onPoyaContainer
        hasNonSchoolEvent || status == AttendanceStatus.HOLIDAY -> MaterialTheme.colorScheme.onTertiaryContainer
        isBeforeSchoolStart -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        isLocked && !isWeekend -> MaterialTheme.colorScheme.onSecondaryContainer
        isWeekend -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    val dotColor = when {
        status == AttendanceStatus.PRESENT -> semantic.present
        status == AttendanceStatus.ABSENT -> semantic.absent
        status == AttendanceStatus.SCHOOL_EVENT -> semantic.schoolEvent
        status == AttendanceStatus.MEDICAL -> semantic.medical
        hasPoya -> semantic.poya
        hasNonSchoolEvent || status == AttendanceStatus.HOLIDAY -> MaterialTheme.colorScheme.tertiary
        isLocked && !isWeekend -> MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .then(
                when {
                    isSelected -> Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp)
                    )
                    isToday -> Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
                    else -> Modifier
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected || record != null) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor
            )
            if (isLocked && record == null && !hasPoya && !hasNonSchoolEvent && !isWeekend && !isBeforeSchoolStart) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked Period",
                    modifier = Modifier.size(10.dp),
                    tint = textColor.copy(alpha = 0.75f)
                )
            } else if (dotColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

@Composable
private fun AdminLegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
