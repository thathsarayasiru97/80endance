package com.example.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilterChip
import com.example.data.AttendanceStatus
import com.example.data.CalendarEvent
import com.example.data.CalendarEventType
import com.example.data.DateUtils
import com.example.data.PersonalAcademicMarker
import com.example.data.PersonalMarkerCategory
import com.example.ui.admin.AdminCalendarEventEditorDialog
import com.example.ui.components.LiquidGlassButton
import com.example.ui.components.PersonalMarkerDialog
import com.example.ui.components.StaggeredReveal
import com.example.ui.components.bouncyClickable
import com.example.ui.theme.LocalAttendanceColors
import com.example.ui.theme.RoyalGold
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
    onOpenPeriodEntry: (() -> Unit)? = null,
    onDeletePeriodBlock: ((String) -> Unit)? = null,
    onBulkLogAttendance: ((dateKeys: List<String>, status: AttendanceStatus, note: String?) -> Unit)? = null,
    onSavePersonalMarker: ((dateKey: String, title: String, category: PersonalMarkerCategory, note: String?, existingId: String?) -> Unit)? = null,
    onDeletePersonalMarker: ((String) -> Unit)? = null,
    isVerifiedAdmin: Boolean = false,
    isSaving: Boolean = false,
    onAdminSaveCalendarEvent: ((
        eventId: String?,
        title: String,
        dateKey: String,
        eventType: CalendarEventType,
        description: String?,
        targetAll: Boolean,
        targetGrades: List<String>,
        targetStreams: List<String>,
        targetClasses: List<String>,
        targetUserIds: List<String>,
        audienceSummary: String,
        alsoSendNotification: Boolean
    ) -> Unit)? = null,
    onAdminDeleteCalendarEvent: ((String) -> Unit)? = null,
    bottomBarPadding: Dp = 24.dp
) {
    val semantic = LocalAttendanceColors.current
    val initialYm = remember { DateUtils.currentYearMonth() }
    var displayYear by remember { mutableIntStateOf(initialYm.first) }
    var displayMonth by remember { mutableIntStateOf(initialYm.second) }
    var selectedDateKey by remember { mutableStateOf<String?>(null) }
    var adminEventDialogDate by remember { mutableStateOf<String?>(null) }
    var editingCalendarEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    // Multi-Day Bulk Select state
    var bulkSelectMode by remember { mutableStateOf(false) }
    var selectedBulkDates by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Personal Academic Marker dialog state
    var markerDialogDate by remember { mutableStateOf<String?>(null) }
    var editingPersonalMarker by remember { mutableStateOf<PersonalAcademicMarker?>(null) }

    val profile = state.profile
    val displayedEvents = remember(isVerifiedAdmin, state.allCalendarEventsForAdmin, state.calendarEvents) {
        if (isVerifiedAdmin && state.allCalendarEventsForAdmin.isNotEmpty()) {
            state.allCalendarEventsForAdmin
        } else {
            state.calendarEvents
        }
    }
    val recordMap = remember(state.attendanceRecords) {
        state.attendanceRecords.associateBy { it.dateKey }
    }
    val eventMap = remember(displayedEvents) {
        displayedEvents.groupBy { it.dateKey }
    }
    val personalMarkerMap = remember(state.personalMarkers) {
        state.personalMarkers.groupBy { it.dateKey }
    }
    val todayKey = remember { DateUtils.todayKey() }
    val monthPrefix = remember(displayYear, displayMonth) {
        String.format("%04d-%02d-", displayYear, displayMonth)
    }
    val monthEvents = remember(displayedEvents, monthPrefix) {
        displayedEvents.filter { it.dateKey.startsWith(monthPrefix) }
    }
    val monthPersonalMarkers = remember(state.personalMarkers, monthPrefix) {
        state.personalMarkers.filter { it.dateKey.startsWith(monthPrefix) }.sortedBy { it.dateKey }
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
                        text = if (isVerifiedAdmin) {
                            "Tap any date to manage school updates or log attendance • ${state.activeTerm.name}"
                        } else {
                            "Tap any school day up to today to log or edit attendance • ${state.activeTerm.name}"
                        },
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
                                    val baselineStart = profile.baselineStartDate
                                    val baselineEnd = profile.baselineEndDate
                                    val pctLabel = profile.baselinePercent?.let {
                                        String.format(java.util.Locale.US, "%.1f%%", it)
                                    } ?: "${profile.priorPresentCount}P / ${profile.priorAbsentCount}A"
                                    Text(
                                        text = "Imported Period Locked • $pctLabel",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    val rangeLabel = if (baselineStart != null && baselineEnd != null) {
                                        "${DateUtils.formatReadableShort(baselineStart)} → ${DateUtils.formatReadableShort(baselineEnd)} "
                                    } else {
                                        ""
                                    }
                                    Text(
                                        text = "$rangeLabel(${profile.priorPresentCount}P / ${profile.priorAbsentCount}A) • Tap to update",
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

            if (onOpenPeriodEntry != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenPeriodEntry() }
                            .testTag("calendar_add_period_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.DateRange,
                                        contentDescription = "Lock attendance for a custom time period",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lock Attendance for a Time Period",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pick any start & end date and enter days missed, days attended, or %",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (state.periodBlocks.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_period_blocks_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Locked Time Periods (${state.periodBlocks.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.periodBlocks.forEach { block ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("calendar_period_block_${block.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Locked period",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        val titleText = block.label?.takeIf { it.isNotBlank() }
                                            ?: "${DateUtils.formatReadableShort(block.startDate)} → ${DateUtils.formatReadableShort(block.endDate)}"
                                        Text(
                                            text = "$titleText • ${String.format(java.util.Locale.US, "%.1f%%", block.percentage)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "${DateUtils.formatReadableShort(block.startDate)} → ${DateUtils.formatReadableShort(block.endDate)} • ${block.presentDays}P / ${block.absentDays}A (${block.totalSchoolDays}d)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                    if (onDeletePeriodBlock != null) {
                                        IconButton(
                                            onClick = { onDeletePeriodBlock(block.id) },
                                            modifier = Modifier.testTag("calendar_unlock_period_${block.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.LockOpen,
                                                contentDescription = "Unlock period",
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Grid Card
            item {
                StaggeredReveal(index = 1) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
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
                            AnimatedContent(
                                targetState = displayYear * 12 + displayMonth,
                                transitionSpec = {
                                    val forward = targetState >= initialState
                                    (slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(220)))
                                        .togetherWith(slideOutHorizontally(tween(180)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(150)))
                                },
                                label = "calendar_month_title"
                            ) { ymIndex ->
                                val yr = (ymIndex - 1) / 12
                                val mo = ((ymIndex - 1) % 12) + 1
                                Text(
                                    text = DateUtils.formatMonthYear(yr, mo),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
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

                        Spacer(modifier = Modifier.height(6.dp))

                        // Multi-Day Bulk Select toggle & action toolbar
                        if (onBulkLogAttendance != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = bulkSelectMode,
                                    onClick = {
                                        bulkSelectMode = !bulkSelectMode
                                        if (!bulkSelectMode) selectedBulkDates = emptySet()
                                    },
                                    label = {
                                        Text(
                                            text = if (bulkSelectMode) "Selecting Multiple (${selectedBulkDates.size})" else "Select Multiple",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (bulkSelectMode) Icons.Rounded.Close else Icons.Rounded.CheckBox,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("calendar_bulk_select_toggle")
                                )
                                if (bulkSelectMode && selectedBulkDates.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.clickable { selectedBulkDates = emptySet() }
                                    ) {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = bulkSelectMode,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp, bottom = 4.dp)
                                        .testTag("calendar_bulk_action_bar")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (selectedBulkDates.isEmpty()) {
                                                "Tap multiple school days below (up to today) to mark them all at once."
                                            } else {
                                                "Mark ${selectedBulkDates.size} selected day${if (selectedBulkDates.size == 1) "" else "s"} as:"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        if (selectedBulkDates.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(
                                                    Triple("Present", AttendanceStatus.PRESENT, semantic.presentContainer to semantic.onPresentContainer),
                                                    Triple("Medical", AttendanceStatus.MEDICAL, semantic.medicalContainer to semantic.onMedicalContainer),
                                                    Triple("Excused", AttendanceStatus.SCHOOL_EVENT, semantic.schoolEventContainer to semantic.onSchoolEventContainer),
                                                    Triple("Absent", AttendanceStatus.ABSENT, semantic.absentContainer to semantic.onAbsentContainer)
                                                ).forEach { (label, status, colors) ->
                                                    Surface(
                                                        color = colors.first,
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable(enabled = !isSaving) {
                                                                onBulkLogAttendance(
                                                                    selectedBulkDates.sorted(),
                                                                    status,
                                                                    null
                                                                )
                                                                selectedBulkDates = emptySet()
                                                                bulkSelectMode = false
                                                            }
                                                            .testTag("calendar_bulk_apply_${status.raw}")
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.second,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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

                        AnimatedContent(
                            targetState = displayYear * 12 + displayMonth,
                            transitionSpec = {
                                val forward = targetState >= initialState
                                (slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(240)))
                                    .togetherWith(slideOutHorizontally(tween(190)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(160)))
                            },
                            label = "calendar_grid_month_slide"
                        ) { ymIndex ->
                        val gridYear = (ymIndex - 1) / 12
                        val gridMonth = ((ymIndex - 1) % 12) + 1
                        val firstDow = DateUtils.firstDayOfWeekMonZero(gridYear, gridMonth)
                        val daysCount = DateUtils.daysInMonth(gridYear, gridMonth)
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
                                            val dateKey = DateUtils.formatDateKey(gridYear, gridMonth, dayNum)
                                            val isWeekend = col >= 5 || DateUtils.isWeekend(dateKey)
                                            val isFuture = dateKey > todayKey
                                            val isBeforeSchoolStart = !isWeekend && state.isBeforeSchoolStart(dateKey)
                                            val isLocked = !isWeekend && !isBeforeSchoolStart && state.isDateLocked(dateKey)
                                            val rec = if (isWeekend || isFuture || isBeforeSchoolStart || isLocked) null else recordMap[dateKey]
                                            val dayEvents = eventMap[dateKey].orEmpty()
                                            val dayPersonalMarkers = personalMarkerMap[dateKey].orEmpty()
                                            val isSlHoliday = dayEvents.any { it.isSriLankanHoliday }
                                            val isSchoolOff = dayEvents.any { !it.isSriLankanHoliday && it.nonSchoolDay }
                                            val isToday = dateKey == todayKey
                                            val isBulkEligible = !isWeekend && !isFuture && !isBeforeSchoolStart && !isLocked && !isSlHoliday && !isSchoolOff
                                            val isBulkSelected = bulkSelectMode && dateKey in selectedBulkDates
                                            val isInteractive = if (bulkSelectMode) {
                                                isBulkEligible
                                            } else {
                                                isVerifiedAdmin || (!isWeekend && ((!isFuture && !isBeforeSchoolStart) || dayEvents.isNotEmpty() || dayPersonalMarkers.isNotEmpty() || onSavePersonalMarker != null || isBeforeSchoolStart)) || (isWeekend && (dayEvents.isNotEmpty() || dayPersonalMarkers.isNotEmpty()))
                                            }

                                            val targetCellBg = when {
                                                isBulkSelected -> MaterialTheme.colorScheme.primaryContainer
                                                isWeekend -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                                isBeforeSchoolStart -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
                                                isSlHoliday -> semantic.poyaContainer.copy(alpha = if (isFuture) 0.52f else 0.78f)
                                                isSchoolOff -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isFuture) 0.52f else 0.78f)
                                                isLocked -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
                                                rec?.statusEnum == AttendanceStatus.PRESENT -> semantic.presentContainer
                                                rec?.statusEnum == AttendanceStatus.ABSENT -> semantic.absentContainer
                                                rec?.statusEnum == AttendanceStatus.MEDICAL -> semantic.medicalContainer
                                                rec?.statusEnum == AttendanceStatus.SCHOOL_EVENT -> semantic.schoolEventContainer
                                                else -> Color.Transparent
                                            }
                                            val cellBg by animateColorAsState(
                                                targetValue = targetCellBg,
                                                animationSpec = tween(240),
                                                label = "cal_bg_$dateKey"
                                            )
                                            val bulkScale by animateFloatAsState(
                                                targetValue = if (isBulkSelected) 1.05f else 1f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                ),
                                                label = "cal_bulk_scale_$dateKey"
                                            )
                                            val textColor = when {
                                                isBulkSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                isWeekend -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
                                                isBeforeSchoolStart -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f)
                                                isSlHoliday -> semantic.onPoyaContainer.copy(alpha = if (isFuture) 0.72f else 1f)
                                                isSchoolOff -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = if (isFuture) 0.72f else 1f)
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
                                                    .graphicsLayer {
                                                        scaleX = bulkScale
                                                        scaleY = bulkScale
                                                    }
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(cellBg)
                                                    .then(
                                                        when {
                                                            isBulkSelected -> Modifier.border(
                                                                width = 2.dp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            isToday -> Modifier.border(
                                                                width = 2.dp,
                                                                color = if (isWeekend) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                                                shape = RoundedCornerShape(12.dp)
                                                            )
                                                            else -> Modifier
                                                        }
                                                    )
                                                    .then(
                                                        if (isInteractive) {
                                                            Modifier.bouncyClickable(pressedScale = 0.88f) {
                                                                if (bulkSelectMode) {
                                                                    selectedBulkDates = if (dateKey in selectedBulkDates) {
                                                                        selectedBulkDates - dateKey
                                                                    } else {
                                                                        selectedBulkDates + dateKey
                                                                    }
                                                                } else {
                                                                    selectedDateKey = dateKey
                                                                }
                                                            }
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
                                                        fontWeight = if (isBulkSelected || (!isWeekend && !isFuture && (isToday || rec != null || isLocked))) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                    when {
                                                        isBulkSelected -> {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = "Selected for bulk update",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                        }
                                                        dayPersonalMarkers.isNotEmpty() -> {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(RoyalGold)
                                                                )
                                                                if (dayEvents.isNotEmpty()) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(5.dp)
                                                                            .clip(CircleShape)
                                                                            .background(
                                                                                when {
                                                                                    isSlHoliday -> semantic.poya
                                                                                    isSchoolOff -> MaterialTheme.colorScheme.tertiary
                                                                                    else -> MaterialTheme.colorScheme.primary
                                                                                }
                                                                            )
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        dayEvents.size > 1 -> {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(
                                                                            when {
                                                                                isSlHoliday -> semantic.poya
                                                                                isSchoolOff -> MaterialTheme.colorScheme.tertiary
                                                                                else -> MaterialTheme.colorScheme.primary
                                                                            }
                                                                        )
                                                                )
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(5.dp)
                                                                        .clip(CircleShape)
                                                                        .background(MaterialTheme.colorScheme.secondary)
                                                                )
                                                            }
                                                        }
                                                        isSlHoliday -> {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(semantic.poya)
                                                            )
                                                        }
                                                        isSchoolOff -> {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.tertiary)
                                                            )
                                                        }
                                                        !isWeekend && isLocked -> {
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
                            if (profile?.hasLockedBaseline == true || state.periodBlocks.isNotEmpty()) {
                                LegendItem("Locked", MaterialTheme.colorScheme.secondary)
                            } else {
                                LegendItem("No School", semantic.poya)
                            }
                        }
                    }
                }
                }
            }

            // Section 0: Personal Academic Markers (Pinned Deadlines, Practicals & Term Tests)
            if (onSavePersonalMarker != null || monthPersonalMarkers.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_personal_markers_section"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Academic Markers",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (onSavePersonalMarker != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier
                                    .clickable {
                                        editingPersonalMarker = null
                                        markerDialogDate = todayKey
                                    }
                                    .testTag("calendar_add_personal_marker_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.BookmarkAdd,
                                        contentDescription = "Pin personal academic date",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Pin Date",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                if (monthPersonalMarkers.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Pin Physics Practicals, Term Test dates, or SBA deadlines on any calendar day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                } else {
                    items(monthPersonalMarkers, key = { "pm_${it.id}" }) { marker ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDateKey = marker.dateKey }
                                .testTag("calendar_personal_marker_${marker.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = RoyalGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Bookmark,
                                            contentDescription = marker.title,
                                            tint = RoyalGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = marker.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${DateUtils.formatReadableShort(marker.dateKey)} • ${marker.categoryEnum.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!marker.note.isNullOrBlank()) {
                                        Text(
                                            text = marker.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                if (onDeletePersonalMarker != null) {
                                    IconButton(
                                        onClick = { onDeletePersonalMarker(marker.id) },
                                        modifier = Modifier.testTag("calendar_delete_marker_${marker.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete personal marker",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: School Events & No School Days (separate from Sri Lankan Holidays!)
            val schoolMonthEvents = monthEvents.filterNot { it.isSriLankanHoliday }
            val slHolidayMonthEvents = monthEvents.filter { it.isSriLankanHoliday }

            item {
                Text(
                    text = "School Events & No School Days",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("calendar_school_updates_section")
                )
            }

            if (schoolMonthEvents.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No school events, exam breaks, or special no-school days listed for this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(schoolMonthEvents, key = { "school_${it.id}" }) { ev ->
                    val isSchoolOffDay = ev.nonSchoolDay
                    val iconBg = if (isSchoolOffDay) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                    val iconFg = if (isSchoolOffDay) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
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
                                color = iconBg,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = calendarEventIcon(ev.eventTypeEnum),
                                        contentDescription = ev.title,
                                        tint = iconFg,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = ev.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isSchoolOffDay) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            shape = RoundedCornerShape(999.dp)
                                        ) {
                                            Text(
                                                text = "No School",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                val audienceSuffix = ev.targetDisplayLabel?.let { " • $it" }.orEmpty()
                                Text(
                                    text = "${DateUtils.formatReadableShort(ev.dateKey)} • ${ev.eventTypeEnum.label}$audienceSuffix",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!ev.description.isNullOrBlank()) {
                                    Text(
                                        text = ev.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Official Sri Lankan Holidays (Poya Days & Public Holidays only)
            item {
                Text(
                    text = "Sri Lankan Holidays",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.testTag("calendar_sl_holidays_section")
                )
            }

            if (slHolidayMonthEvents.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No Sri Lankan Poya or public holidays listed for this month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(slHolidayMonthEvents, key = { "sl_${it.id}" }) { ev ->
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
                                        imageVector = calendarEventIcon(ev.eventTypeEnum),
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
                                Text(
                                    text = "${DateUtils.formatReadableShort(ev.dateKey)} • ${ev.eventTypeEnum.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!ev.description.isNullOrBlank()) {
                                    Text(
                                        text = ev.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
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
                    val isSlHolidayEv = ev.isSriLankanHoliday
                    val isSchoolOffEv = !isSlHolidayEv && ev.nonSchoolDay
                    val containerColor = when {
                        isSlHolidayEv -> semantic.poyaContainer
                        isSchoolOffEv -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
                    }
                    val contentColor = when {
                        isSlHolidayEv -> semantic.onPoyaContainer
                        isSchoolOffEv -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                    val categoryPrefix = when {
                        isSlHolidayEv -> "Sri Lankan Holiday"
                        isSchoolOffEv -> "School Update • No School"
                        else -> "School Event"
                    }
                    Surface(
                        color = containerColor,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = calendarEventIcon(ev.eventTypeEnum),
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = ev.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentColor
                                )
                                val audienceLabel = ev.targetDisplayLabel ?: if (isVerifiedAdmin) "Everyone" else null
                                val audienceSuffix = audienceLabel?.let { " • $it" }.orEmpty()
                                Text(
                                    text = "$categoryPrefix • ${ev.eventTypeEnum.label}$audienceSuffix",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor.copy(alpha = 0.85f)
                                )
                                if (!ev.description.isNullOrBlank()) {
                                    Text(
                                        text = ev.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = contentColor
                                    )
                                }
                            }
                            if (isVerifiedAdmin && onAdminSaveCalendarEvent != null) {
                                Row {
                                    IconButton(
                                        onClick = {
                                            editingCalendarEvent = ev
                                            adminEventDialogDate = ev.dateKey
                                            selectedDateKey = null
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edit calendar event",
                                            tint = contentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (onAdminDeleteCalendarEvent != null) {
                                        IconButton(
                                            onClick = { onAdminDeleteCalendarEvent(ev.id) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete calendar event",
                                                tint = contentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                val dayPersonalMarkers = personalMarkerMap[dateKey].orEmpty()
                dayPersonalMarkers.forEach { marker ->
                    Surface(
                        color = RoyalGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = RoyalGold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = marker.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Personal Marker • ${marker.categoryEnum.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!marker.note.isNullOrBlank()) {
                                    Text(
                                        text = marker.note,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (onDeletePersonalMarker != null) {
                                IconButton(onClick = { onDeletePersonalMarker(marker.id) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Remove personal marker",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (onSavePersonalMarker != null) {
                    OutlinedButton(
                        onClick = {
                            editingPersonalMarker = null
                            markerDialogDate = dateKey
                            selectedDateKey = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_sheet_pin_marker_button")
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pin Personal Academic Date")
                    }
                }

                if (isVerifiedAdmin && onAdminSaveCalendarEvent != null) {
                    OutlinedButton(
                        onClick = {
                            editingCalendarEvent = null
                            adminEventDialogDate = dateKey
                            selectedDateKey = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_admin_add_event_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Event / Update for This Day")
                    }
                }

                val isSelectedWeekend = DateUtils.isWeekend(dateKey)
                val isSelectedFuture = dateKey > todayKey
                val isSelectedBeforeStart = !isSelectedWeekend && state.isBeforeSchoolStart(dateKey)
                val lockingPeriod = state.lockingPeriodFor(dateKey)
                val isSelectedLocked = !isSelectedWeekend && !isSelectedBeforeStart && state.isDateLocked(dateKey)

                when {
                    isSelectedWeekend -> {
                        Text(
                            text = "Weekend — no attendance is tracked on Saturdays or Sundays.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    }
                    isSelectedBeforeStart -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                .testTag("calendar_before_start_notice")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Before school start date",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Before school start (${DateUtils.formatReadableShort(state.effectiveSchoolStartDateKey)}) — attendance can only be marked from this date onward.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                        val headerText = if (lockingPeriod != null) {
                            val labelPrefix = lockingPeriod.label?.takeIf { it.isNotBlank() }?.let { "$it • " }.orEmpty()
                            "Locked • ${labelPrefix}${String.format(java.util.Locale.US, "%.1f%%", lockingPeriod.percentage)}"
                        } else {
                            val pctLabel = profile?.baselinePercent?.let {
                                String.format(java.util.Locale.US, "%.1f%%", it)
                            } ?: "imported attendance"
                            "Locked • Included in Imported $pctLabel"
                        }
                        val detailText = if (lockingPeriod != null) {
                            "Covered by ${DateUtils.formatReadableShort(lockingPeriod.startDate)} → ${DateUtils.formatReadableShort(lockingPeriod.endDate)} (${lockingPeriod.presentDays} Present, ${lockingPeriod.absentDays} Missed out of ${lockingPeriod.totalSchoolDays} school days). Individual days in this period cannot be edited."
                        } else {
                            val startLbl = profile?.baselineStartDate?.let { DateUtils.formatReadableShort(it) } ?: "start"
                            val endLbl = profile?.baselineEndDate?.let { DateUtils.formatReadableShort(it) } ?: "end"
                            "Covered by $startLbl → $endLbl (${profile?.priorPresentCount ?: 0} Present, ${profile?.priorAbsentCount ?: 0} Absent). Individual days in this period cannot be edited."
                        }
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
                                        text = headerText,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Text(
                                    text = detailText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.88f)
                                )
                            }
                        }
                        if (lockingPeriod != null && onDeletePeriodBlock != null) {
                            OutlinedButton(
                                onClick = {
                                    onDeletePeriodBlock(lockingPeriod.id)
                                    selectedDateKey = null
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp)
                            ) {
                                Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unlock This Period")
                            }
                        } else if (lockingPeriod == null && onOpenBaselineImport != null) {
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

    val activeAdminEventDate = adminEventDialogDate
    if (isVerifiedAdmin && activeAdminEventDate != null && onAdminSaveCalendarEvent != null) {
        AdminCalendarEventEditorDialog(
            initialDateKey = activeAdminEventDate,
            editingEvent = editingCalendarEvent,
            students = state.adminAllStudents,
            catalog = state.academicCatalog,
            isBusy = isSaving,
            onDismiss = {
                adminEventDialogDate = null
                editingCalendarEvent = null
            },
            onSave = { eventId, title, dk, type, desc, targetAll, grades, streams, classes, userIds, summary, notify ->
                onAdminSaveCalendarEvent(
                    eventId, title, dk, type, desc, targetAll, grades, streams, classes, userIds, summary, notify
                )
                adminEventDialogDate = null
                editingCalendarEvent = null
            }
        )
    }

    val activeMarkerDate = markerDialogDate
    if (activeMarkerDate != null && onSavePersonalMarker != null) {
        PersonalMarkerDialog(
            dateKey = activeMarkerDate,
            existingMarker = editingPersonalMarker,
            onDismiss = {
                markerDialogDate = null
                editingPersonalMarker = null
            },
            onSave = { dk, title, cat, note, existingId ->
                onSavePersonalMarker(dk, title, cat, note, existingId)
                markerDialogDate = null
                editingPersonalMarker = null
            }
        )
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

private fun calendarEventIcon(type: CalendarEventType) = when (type) {
    CalendarEventType.POYA -> Icons.Rounded.NightsStay
    CalendarEventType.STUDY_LEAVE -> Icons.AutoMirrored.Rounded.MenuBook
    CalendarEventType.SCHOOL_HOLIDAY, CalendarEventType.TERM_BREAK -> Icons.Rounded.EventBusy
    CalendarEventType.EXAM -> Icons.Rounded.School
    CalendarEventType.PUBLIC_HOLIDAY, CalendarEventType.SCHOOL_EVENT -> Icons.Rounded.Event
}
