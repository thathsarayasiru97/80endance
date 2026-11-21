package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppSettings
import com.example.data.DateUtils
import com.example.data.Notice
import com.example.data.NoticeTargetType
import com.example.data.UserProfile
import com.example.ui.theme.LocalAttendanceColors
import com.example.viewmodel.AdminViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminPanelScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbar.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val semantic = LocalAttendanceColors.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var showNoticeDialog by remember { mutableStateOf(false) }
    var editingNotice by remember { mutableStateOf<Notice?>(null) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    // Student filter state
    var searchQuery by remember { mutableStateOf("") }
    var studentFilter by remember { mutableStateOf("all") } // all, risk, eligible
    var overrideStudent by remember { mutableStateOf<UserProfile?>(null) }
    var overrideDateKey by remember { mutableStateOf(DateUtils.todayKey()) }
    var editStudentProfile by remember { mutableStateOf<UserProfile?>(null) }

    // Settings editor state
    var thresholdText by remember(state.appSettings) { mutableStateOf(state.appSettings.defaultEligibilityPercent.toString()) }
    var exam2026 by remember(state.appSettings) { mutableStateOf(state.appSettings.al2026ExamDate) }
    var exam2027 by remember(state.appSettings) { mutableStateOf(state.appSettings.al2027ExamDate) }
    var exam2028 by remember(state.appSettings) { mutableStateOf(state.appSettings.al2028ExamDate) }

    // Quick "No School for Everyone" state
    val todayKey = remember { DateUtils.todayKey() }
    val tomorrowKey = remember(todayKey) { DateUtils.addDays(todayKey, 1) }
    val inTwoDaysKey = remember(todayKey) { DateUtils.addDays(todayKey, 2) }
    var noSchoolDateKey by remember { mutableStateOf(tomorrowKey) }
    var noSchoolReason by remember { mutableStateOf("Exam Study Leave") }
    var noSchoolAlsoNotice by remember { mutableStateOf(true) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { editingNotice = null; showNoticeDialog = true },
                    modifier = Modifier.testTag("admin_add_notice_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add notice")
                }
            } else if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = { showAddEventDialog = true },
                    modifier = Modifier.testTag("admin_add_event_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add calendar event")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Notices") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Students (${state.students.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Calendar") })
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Pinned notices rotate automatically every 5 seconds on student dashboards.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (state.notices.isEmpty()) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No notices yet. Tap + to publish a school-wide, stream, or class update.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(18.dp)
                                        )
                                    }
                                }
                            } else {
                                items(state.notices, key = { it.id }) { notice ->
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = buildString {
                                                        append(notice.categoryEnum.label)
                                                        append(" • ")
                                                        append(
                                                            when (notice.targetTypeEnum) {
                                                                NoticeTargetType.ALL -> "All"
                                                                else -> notice.targetValue ?: notice.targetTypeEnum.label
                                                            }
                                                        )
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Row {
                                                    IconButton(onClick = { viewModel.togglePin(notice) }) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.PushPin,
                                                            contentDescription = "Toggle pin",
                                                            tint = if (notice.pinned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                                                        )
                                                    }
                                                    IconButton(onClick = { editingNotice = notice; showNoticeDialog = true }) {
                                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit notice")
                                                    }
                                                    IconButton(onClick = { viewModel.deleteNotice(notice.id) }) {
                                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete notice")
                                                    }
                                                }
                                            }
                                            Text(notice.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                notice.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }

                    1 -> {
                        val filteredStudents = remember(state.students, searchQuery, studentFilter) {
                            state.students.filter { st ->
                                val matchesQuery = searchQuery.isBlank() ||
                                    st.fullName.contains(searchQuery, ignoreCase = true) ||
                                    st.className.contains(searchQuery, ignoreCase = true) ||
                                    st.alStream.contains(searchQuery, ignoreCase = true) ||
                                    st.email.contains(searchQuery, ignoreCase = true)
                                val matchesFilter = when (studentFilter) {
                                    "risk" -> st.quickOverallPercent < state.appSettings.defaultEligibilityPercent
                                    "eligible" -> st.quickOverallPercent >= state.appSettings.defaultEligibilityPercent
                                    else -> true
                                }
                                matchesQuery && matchesFilter
                            }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search name, class (13M2), or stream") },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = studentFilter == "all", onClick = { studentFilter = "all" }, label = { Text("All") })
                                    FilterChip(selected = studentFilter == "risk", onClick = { studentFilter = "risk" }, label = { Text("Below 80%") })
                                    FilterChip(selected = studentFilter == "eligible", onClick = { studentFilter = "eligible" }, label = { Text("Eligible") })
                                }
                            }
                            items(filteredStudents, key = { it.userId }) { student ->
                                val pct = student.quickOverallPercent
                                val isOk = pct >= state.appSettings.defaultEligibilityPercent
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectStudent(student.userId) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(student.fullName, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "${student.className} • ${student.alStream} • ${student.alExamYear} A/L",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "P:${student.presentCount}  A:${student.absentCount}  M:${student.medicalCount}  E:${student.schoolEventCount}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            color = if (isOk) semantic.presentContainer else semantic.absentContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "%.1f%%", pct),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isOk) semantic.onPresentContainer else semantic.onAbsentContainer,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        val sortedEvents = remember(state.calendarEvents, todayKey) {
                            val upcoming = state.calendarEvents
                                .filter { it.dateKey >= todayKey }
                                .sortedBy { it.dateKey }
                            val past = state.calendarEvents
                                .filter { it.dateKey < todayKey }
                                .sortedByDescending { it.dateKey }
                            upcoming + past
                        }
                        val quickReasons = listOf(
                            "Exam Study Leave",
                            "Interval Day Between Exams",
                            "Special School Holiday",
                            "Weather Closure"
                        )
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Quick Mark No School Day for Everyone
                            item {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = semantic.poyaContainer.copy(alpha = 0.65f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_no_school_card")
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
                                                imageVector = Icons.Rounded.EventBusy,
                                                contentDescription = null,
                                                tint = semantic.onPoyaContainer
                                            )
                                            Text(
                                                text = "Mark No School Day for Everyone",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = semantic.onPoyaContainer
                                            )
                                        }
                                        Text(
                                            text = "Instantly marks a day off (e.g., exam interval / study leave) on every student's calendar and suppresses daily check-in reminders.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = semantic.onPoyaContainer.copy(alpha = 0.85f)
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            FilterChip(
                                                selected = noSchoolDateKey == todayKey,
                                                onClick = { noSchoolDateKey = todayKey },
                                                label = { Text("Today") }
                                            )
                                            FilterChip(
                                                selected = noSchoolDateKey == tomorrowKey,
                                                onClick = { noSchoolDateKey = tomorrowKey },
                                                label = { Text("Tomorrow") }
                                            )
                                            FilterChip(
                                                selected = noSchoolDateKey == inTwoDaysKey,
                                                onClick = { noSchoolDateKey = inTwoDaysKey },
                                                label = { Text("+2 Days") }
                                            )
                                        }
                                        OutlinedTextField(
                                            value = noSchoolDateKey,
                                            onValueChange = { noSchoolDateKey = it.take(10) },
                                            label = { Text("Date (YYYY-MM-DD)") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_no_school_date_input")
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            quickReasons.forEach { preset ->
                                                FilterChip(
                                                    selected = noSchoolReason == preset,
                                                    onClick = { noSchoolReason = preset },
                                                    label = { Text(preset) }
                                                )
                                            }
                                        }
                                        OutlinedTextField(
                                            value = noSchoolReason,
                                            onValueChange = { noSchoolReason = it.take(100) },
                                            label = { Text("Reason shown to students") },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_no_school_reason_input")
                                        )
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
                                                    text = "Posts an urgent banner so everyone sees the day off",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = semantic.onPoyaContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                            Switch(
                                                checked = noSchoolAlsoNotice,
                                                onCheckedChange = { noSchoolAlsoNotice = it }
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.markNoSchoolDayForEveryone(
                                                    dateKey = noSchoolDateKey.trim(),
                                                    reason = noSchoolReason,
                                                    alsoPostNotice = noSchoolAlsoNotice
                                                )
                                            },
                                            enabled = !state.isBusy && noSchoolDateKey.trim().matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("admin_mark_no_school_button")
                                        ) {
                                            Icon(Icons.Rounded.EventBusy, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Mark No School for Everyone")
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Sri Lankan Poya & School Terms", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "Sync standard 2026–2027 Full Moon Poya days, public holidays, and Term 1–3 dates to Firestore.",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Button(
                                            onClick = { viewModel.seedSriLankaCalendar() },
                                            enabled = !state.isBusy,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Seed 2026–2027 SL Calendar")
                                        }
                                    }
                                }
                            }

                            // Global Defaults & A/L Exam Dates
                            item {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Global Policy & A/L Exam Countdowns", style = MaterialTheme.typography.titleSmall)
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
                                                    viewModel.saveGlobalSettings(
                                                        AppSettings(
                                                            defaultEligibilityPercent = (thresholdText.toIntOrNull() ?: 80).coerceIn(50, 100),
                                                            al2026ExamDate = exam2026,
                                                            al2027ExamDate = exam2027,
                                                            al2028ExamDate = exam2028
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("Save Global Defaults") }
                                    }
                                }
                            }

                            item {
                                Text("Calendar Events (${sortedEvents.size})", style = MaterialTheme.typography.titleSmall)
                            }
                            items(sortedEvents, key = { it.id }) { ev ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ev.title, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                "${ev.dateKey} • ${ev.eventTypeEnum.label}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteCalendarEvent(ev.id) }) {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete event")
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }
    }

    // Selected Student Detail Sheet
    state.selectedStudent?.let { student ->
        ModalBottomSheet(onDismissRequest = { viewModel.selectStudent(null) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(student.fullName, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${student.className} • ${student.alStream} • ${student.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            overrideDateKey = DateUtils.todayKey()
                            overrideStudent = student
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Log / Fix Day") }
                    OutlinedButton(
                        onClick = { editStudentProfile = student },
                        modifier = Modifier.weight(1f)
                    ) { Text("Edit Balance/Class") }
                }

                Text("Recent Attendance Logs (${state.selectedStudentRecords.size})", style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    items(state.selectedStudentRecords, key = { it.dateKey }) { rec ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${rec.dateKey} — ${rec.statusEnum.label}", style = MaterialTheme.typography.titleSmall)
                                    if (!rec.note.isNullOrBlank()) {
                                        Text(rec.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row {
                                    IconButton(onClick = { overrideDateKey = rec.dateKey; overrideStudent = student }) {
                                        Icon(Icons.Rounded.Edit, contentDescription = "Edit day")
                                    }
                                    IconButton(onClick = { viewModel.deleteStudentAttendance(student, rec.dateKey) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete day")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showNoticeDialog) {
        NoticeEditorDialog(
            editingNotice = editingNotice,
            onDismiss = { showNoticeDialog = false },
            onSave = { id, title, msg, cat, pinned, targetType, targetVal, evDate, urgent ->
                viewModel.saveNotice(id, title, msg, cat, pinned, targetType, targetVal, evDate, urgent) {
                    showNoticeDialog = false
                }
            }
        )
    }

    overrideStudent?.let { st ->
        AdminOverrideAttendanceDialog(
            student = st,
            initialDateKey = overrideDateKey,
            onDismiss = { overrideStudent = null },
            onSave = { dk, status, note ->
                viewModel.overrideStudentAttendance(st, dk, status, note) {
                    overrideStudent = null
                }
            }
        )
    }

    editStudentProfile?.let { st ->
        AdminEditStudentDialog(
            student = st,
            onDismiss = { editStudentProfile = null },
            onSave = { cls, stream, pPres, pAbs ->
                viewModel.updateStudentBalanceAndClass(st, cls, stream, pPres, pAbs) {
                    editStudentProfile = null
                }
            }
        )
    }

    if (showAddEventDialog) {
        AddCalendarEventDialog(
            onDismiss = { showAddEventDialog = false },
            onSave = { title, dk, type, desc ->
                viewModel.saveCalendarEvent(title, dk, type, desc) {
                    showAddEventDialog = false
                }
            }
        )
    }
}
