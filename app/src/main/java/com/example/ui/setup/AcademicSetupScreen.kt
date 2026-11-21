package com.example.ui.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AcademicCatalog
import com.example.data.AcademicTerm
import com.example.data.AppConstants
import com.example.data.AttendanceCalculator
import com.example.data.CalendarEvent
import com.example.data.DateUtils
import com.example.data.SriLankaCalendarDefaults
import com.example.data.UserProfile
import com.example.ui.components.LiquidGlassButton
import com.example.ui.theme.LocalAttendanceColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SetupAttendanceBaseline(
    val startDateKey: String,
    val endDateKey: String,
    val percentage: Double,
    val presentDays: Int,
    val absentDays: Int
)

private enum class SetupImportMode {
    PERCENTAGE,
    CALCULATOR
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AcademicSetupScreen(
    initialDisplayName: String?,
    existingProfile: UserProfile?,
    isSaving: Boolean,
    catalog: AcademicCatalog = AcademicCatalog.defaultCatalog(),
    activeTerm: AcademicTerm = SriLankaCalendarDefaults.defaultTerms.last(),
    terms: List<AcademicTerm> = SriLankaCalendarDefaults.defaultTerms,
    calendarEvents: List<CalendarEvent> = SriLankaCalendarDefaults.defaultEvents,
    onSave: (
        firstName: String,
        lastName: String,
        nickname: String?,
        schoolName: String,
        grade: String,
        alStream: String,
        className: String,
        alExamYear: Int,
        subjects: List<String>,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
        countExcusedAsPresent: Boolean,
        baseline: SetupAttendanceBaseline?,
        updateBaselineFields: Boolean,
        missingSubjectAlerts: List<Pair<String, String>>,
        missingClassAlert: String?
    ) -> Unit
) {
    val semantic = LocalAttendanceColors.current
    val nameParts = remember(initialDisplayName) {
        initialDisplayName?.trim()?.split(Regex("\\s+")).orEmpty()
    }

    var firstName by remember {
        mutableStateOf(
            existingProfile?.firstName?.ifEmpty { nameParts.firstOrNull().orEmpty() }
                ?: nameParts.firstOrNull().orEmpty()
        )
    }
    var lastName by remember {
        mutableStateOf(
            existingProfile?.lastName?.ifEmpty { nameParts.drop(1).joinToString(" ") }
                ?: nameParts.drop(1).joinToString(" ")
        )
    }
    var nickname by remember { mutableStateOf(existingProfile?.nickname.orEmpty()) }
    var grade by remember { mutableStateOf(existingProfile?.grade ?: "Grade 13") }
    var alStream by remember { mutableStateOf(existingProfile?.alStream ?: "Physical Science") }
    var examYear by remember {
        mutableIntStateOf(
            existingProfile?.alExamYear ?: AppConstants.defaultExamYearForGrade(grade)
        )
    }

    val initialClasses = remember(grade, alStream, catalog) { catalog.classesFor(grade, alStream) }
    var className by remember { mutableStateOf(existingProfile?.className.orEmpty()) }
    var useCustomClass by remember {
        mutableStateOf(
            existingProfile?.className?.isNotBlank() == true &&
                initialClasses.none { it.equals(existingProfile.className, ignoreCase = true) }
        )
    }

    val defaultSubs = remember(alStream, catalog) { catalog.defaultSelectionForStream(alStream) }
    var subject1 by remember { mutableStateOf(existingProfile?.subjects?.getOrNull(0) ?: defaultSubs[0]) }
    var subject2 by remember { mutableStateOf(existingProfile?.subjects?.getOrNull(1) ?: defaultSubs[1]) }
    var subject3 by remember { mutableStateOf(existingProfile?.subjects?.getOrNull(2) ?: defaultSubs[2]) }

    var useCustomSubject1 by remember {
        val list = catalog.subjectsFor(alStream, AcademicCatalog.BUCKET_1)
        mutableStateOf(subject1.isNotBlank() && list.none { it.equals(subject1, ignoreCase = true) })
    }
    var useCustomSubject2 by remember {
        val list = catalog.subjectsFor(alStream, AcademicCatalog.BUCKET_2)
        mutableStateOf(subject2.isNotBlank() && list.none { it.equals(subject2, ignoreCase = true) })
    }
    var useCustomSubject3 by remember {
        val list = catalog.subjectsFor(alStream, AcademicCatalog.BUCKET_3)
        mutableStateOf(subject3.isNotBlank() && list.none { it.equals(subject3, ignoreCase = true) })
    }

    var reminderEnabled by remember { mutableStateOf(existingProfile?.reminderEnabled ?: true) }
    var reminderHour by remember { mutableIntStateOf(existingProfile?.reminderHour ?: AppConstants.DEFAULT_REMINDER_HOUR) }
    var reminderMinute by remember { mutableIntStateOf(existingProfile?.reminderMinute ?: AppConstants.DEFAULT_REMINDER_MINUTE) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Import Attendance % + Built-in % Calculator + "Up Until This Day" Locking
    val todayKey = remember { DateUtils.todayKey() }
    val yesterdayKey = remember(todayKey) { DateUtils.addDays(todayKey, -1) }
    val lastFridayKey = remember(todayKey) { DateUtils.lastFridayKey(todayKey) }
    val endOfLastMonthKey = remember(todayKey) { DateUtils.endOfPreviousMonthKey(todayKey) }
    val yearStartTerm = remember(terms, activeTerm) {
        terms.filter { it.academicYear == activeTerm.academicYear }.minByOrNull { it.startDate }
    }
    val defaultBaselineStart = remember(existingProfile, activeTerm, todayKey) {
        val saved = existingProfile?.baselineStartDate?.takeIf { it.length == 10 }
        val candidate = saved ?: activeTerm.startDate
        if (candidate > todayKey) DateUtils.addDays(todayKey, -30) else candidate
    }
    val defaultBaselineEnd = remember(existingProfile, lastFridayKey, yesterdayKey, defaultBaselineStart, todayKey) {
        val saved = existingProfile?.baselineEndDate?.takeIf { it.length == 10 && it <= todayKey }
        val candidate = saved ?: if (lastFridayKey >= defaultBaselineStart) lastFridayKey else yesterdayKey
        if (candidate < defaultBaselineStart) defaultBaselineStart else candidate
    }

    var importAttendanceEnabled by remember {
        mutableStateOf(existingProfile?.hasLockedBaseline == true)
    }
    var importMode by remember { mutableStateOf(SetupImportMode.PERCENTAGE) }
    var baselineStartDate by remember { mutableStateOf(defaultBaselineStart) }
    var baselineEndDate by remember { mutableStateOf(defaultBaselineEnd) }
    var baselinePercentText by remember {
        mutableStateOf(
            existingProfile?.baselinePercent?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.1f", it)
            } ?: ""
        )
    }
    var showOverrideSchoolDays by remember { mutableStateOf(false) }
    var customSchoolDaysText by remember { mutableStateOf("") }

    val existingPriorTotal = (existingProfile?.priorPresentCount ?: 0) + (existingProfile?.priorAbsentCount ?: 0)
    var calcDaysHeldText by remember {
        mutableStateOf(existingPriorTotal.takeIf { it > 0 && existingProfile?.hasLockedBaseline == true }?.toString().orEmpty())
    }
    var calcDaysAttendedText by remember {
        mutableStateOf((existingProfile?.priorPresentCount ?: 0).takeIf { it > 0 && existingProfile?.hasLockedBaseline == true }?.toString().orEmpty())
    }
    var pickingBaselineEndDate by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val effectiveStartDate = if (baselineStartDate <= baselineEndDate) baselineStartDate else baselineEndDate
    val detectedSchoolDays = remember(effectiveStartDate, baselineEndDate, calendarEvents) {
        AttendanceCalculator.countSchoolDaysInRange(effectiveStartDate, baselineEndDate, calendarEvents)
    }
    val parsedPercent = baselinePercentText.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }
    val customOverrideDays = if (showOverrideSchoolDays) {
        customSchoolDaysText.toIntOrNull()?.takeIf { it in 1..600 }
    } else {
        null
    }
    val parsedDaysHeld = calcDaysHeldText.toIntOrNull()
    val parsedDaysAttended = calcDaysAttendedText.toIntOrNull()
    val calcInputMismatch = parsedDaysHeld != null && parsedDaysAttended != null && parsedDaysAttended > parsedDaysHeld

    val calculatedEstimate = remember(
        importMode,
        effectiveStartDate,
        baselineEndDate,
        parsedPercent,
        calendarEvents,
        customOverrideDays,
        parsedDaysHeld,
        parsedDaysAttended,
        todayKey
    ) {
        if (baselineEndDate > todayKey) {
            null
        } else {
            when (importMode) {
                SetupImportMode.PERCENTAGE -> {
                    if (parsedPercent != null) {
                        AttendanceCalculator.estimateBaselineFromPercentage(
                            startDateKey = effectiveStartDate,
                            endDateKey = baselineEndDate,
                            percentage = parsedPercent,
                            calendarEvents = calendarEvents,
                            customSchoolDaysOverride = customOverrideDays
                        )
                    } else {
                        null
                    }
                }
                SetupImportMode.CALCULATOR -> {
                    if (parsedDaysHeld != null && parsedDaysHeld in 1..600 &&
                        parsedDaysAttended != null && parsedDaysAttended in 0..parsedDaysHeld
                    ) {
                        AttendanceCalculator.estimateBaselineFromDays(
                            startDateKey = effectiveStartDate,
                            endDateKey = baselineEndDate,
                            daysSchoolHeld = parsedDaysHeld,
                            daysAttended = parsedDaysAttended
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }

    val availableClasses = remember(grade, alStream, catalog) { catalog.classesFor(grade, alStream) }
    val bucket1Subjects = remember(alStream, catalog) { catalog.subjectsFor(alStream, AcademicCatalog.BUCKET_1) }
    val bucket2Subjects = remember(alStream, catalog) { catalog.subjectsFor(alStream, AcademicCatalog.BUCKET_2) }
    val bucket3Subjects = remember(alStream, catalog) { catalog.subjectsFor(alStream, AcademicCatalog.BUCKET_3) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = "Academic profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Set Up Your A/L Profile",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = AppConstants.DEFAULT_SCHOOL_NAME,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 1. Name Card (Two Parts + Optional Nickname)
                SetupSectionCard(title = "About You") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it.take(50); validationError = null },
                            label = { Text("First Name *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("setup_first_name_input")
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it.take(50); validationError = null },
                            label = { Text("Last Name *") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("setup_last_name_input")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it.take(40) },
                        label = { Text("Nickname (Optional)") },
                        placeholder = { Text("Used for dashboard greetings") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_nickname_input")
                    )
                }

                // 2. Grade, Target A/L Year (auto-selected by grade, manually changeable), Stream & Class Picker
                SetupSectionCard(title = "Grade, Stream & Class") {
                    Text(
                        text = "Grade",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppConstants.GRADES.forEach { g ->
                            FilterChip(
                                selected = grade == g,
                                onClick = {
                                    grade = g
                                    examYear = AppConstants.defaultExamYearForGrade(g)
                                    val newClasses = catalog.classesFor(g, alStream)
                                    if (!useCustomClass && newClasses.none { it.equals(className, ignoreCase = true) }) {
                                        className = ""
                                    }
                                    validationError = null
                                },
                                label = { Text(g) },
                                modifier = Modifier.testTag("setup_grade_${g.replace(" ", "_").lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Target A/L Exam Year (auto-matched, tap to change)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppConstants.EXAM_YEARS.forEach { yr ->
                            FilterChip(
                                selected = examYear == yr,
                                onClick = { examYear = yr },
                                label = { Text("${yr % 100} A/L ($yr)") },
                                modifier = Modifier.testTag("setup_exam_year_$yr")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "A/L Stream",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppConstants.AL_STREAMS.forEach { stream ->
                            FilterChip(
                                selected = alStream == stream,
                                onClick = {
                                    alStream = stream
                                    val preset = catalog.defaultSelectionForStream(stream)
                                    subject1 = preset.getOrElse(0) { "" }
                                    subject2 = preset.getOrElse(1) { "" }
                                    subject3 = preset.getOrElse(2) { "" }
                                    useCustomSubject1 = false
                                    useCustomSubject2 = false
                                    useCustomSubject3 = false
                                    val newClasses = catalog.classesFor(grade, stream)
                                    if (!useCustomClass && newClasses.none { it.equals(className, ignoreCase = true) }) {
                                        className = ""
                                    }
                                    validationError = null
                                },
                                label = { Text(stream) },
                                modifier = Modifier.testTag("setup_stream_${stream.replace(" ", "_").lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select Your Class ($grade • $alStream) *",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableClasses.forEach { cls ->
                            FilterChip(
                                selected = !useCustomClass && className.equals(cls, ignoreCase = true),
                                onClick = {
                                    useCustomClass = false
                                    className = cls
                                    validationError = null
                                },
                                label = { Text(cls) },
                                modifier = Modifier.testTag("setup_class_chip_$cls")
                            )
                        }
                        FilterChip(
                            selected = useCustomClass,
                            onClick = {
                                useCustomClass = true
                                if (availableClasses.any { it.equals(className, ignoreCase = true) }) {
                                    className = ""
                                }
                                validationError = null
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Other / Not listed") },
                            modifier = Modifier.testTag("setup_class_other_chip")
                        )
                    }

                    AnimatedVisibility(visible = useCustomClass || availableClasses.isEmpty()) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            OutlinedTextField(
                                value = className,
                                onValueChange = { className = it.uppercase().take(20); validationError = null },
                                label = { Text("Enter Your Class *") },
                                placeholder = { Text("e.g., 12S7, 13M4") },
                                supportingText = {
                                    Text("Not in the list? Type your class and the admin will be notified to add it.")
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("setup_class_input")
                            )
                        }
                    }
                }

                // 3. Structured Three A/L Subject Buckets
                SetupSectionCard(title = "Your Three A/L Subjects ($alStream)") {
                    Text(
                        text = "Pick one subject from each bucket. If a subject is missing, tap Other to type it manually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SubjectBucketPicker(
                        bucketLabel = AcademicCatalog.BUCKET_1,
                        options = bucket1Subjects,
                        selectedValue = subject1,
                        useCustom = useCustomSubject1,
                        inputTag = "setup_subject1_input",
                        otherChipTag = "setup_bucket1_other_chip",
                        streamName = alStream,
                        onPickOption = { chosen ->
                            useCustomSubject1 = false
                            subject1 = chosen
                            validationError = null
                        },
                        onSelectCustom = {
                            useCustomSubject1 = true
                            if (bucket1Subjects.any { it.equals(subject1, ignoreCase = true) }) {
                                subject1 = ""
                            }
                            validationError = null
                        },
                        onCustomTextChange = {
                            subject1 = it.take(60)
                            validationError = null
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SubjectBucketPicker(
                        bucketLabel = AcademicCatalog.BUCKET_2,
                        options = bucket2Subjects,
                        selectedValue = subject2,
                        useCustom = useCustomSubject2,
                        inputTag = "setup_subject2_input",
                        otherChipTag = "setup_bucket2_other_chip",
                        streamName = alStream,
                        onPickOption = { chosen ->
                            useCustomSubject2 = false
                            subject2 = chosen
                            validationError = null
                        },
                        onSelectCustom = {
                            useCustomSubject2 = true
                            if (bucket2Subjects.any { it.equals(subject2, ignoreCase = true) }) {
                                subject2 = ""
                            }
                            validationError = null
                        },
                        onCustomTextChange = {
                            subject2 = it.take(60)
                            validationError = null
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SubjectBucketPicker(
                        bucketLabel = AcademicCatalog.BUCKET_3,
                        options = bucket3Subjects,
                        selectedValue = subject3,
                        useCustom = useCustomSubject3,
                        inputTag = "setup_subject3_input",
                        otherChipTag = "setup_bucket3_other_chip",
                        streamName = alStream,
                        onPickOption = { chosen ->
                            useCustomSubject3 = false
                            subject3 = chosen
                            validationError = null
                        },
                        onSelectCustom = {
                            useCustomSubject3 = true
                            if (bucket3Subjects.any { it.equals(subject3, ignoreCase = true) }) {
                                subject3 = ""
                            }
                            validationError = null
                        },
                        onCustomTextChange = {
                            subject3 = it.take(60)
                            validationError = null
                        }
                    )
                }

                // 4. Daily Check-In Reminder Time
                SetupSectionCard(title = "Daily Check-In Reminder") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ask after school every weekday",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Skips weekends and Sri Lankan Poya days automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                reminderEnabled = enabled
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }
                    AnimatedVisibility(visible = reminderEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.AccessTime, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reminder Time: ${DateUtils.formatTime12Hour(reminderHour, reminderMinute)}")
                            }
                        }
                    }
                }

                // 5. Import Attendance Percentage & Calculator (Replaces "Joining Mid-Term?" to prevent duplicate counting)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_import_attendance_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Percent,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Import Attendance % (Optional)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Import your attendance % or calculate it from school days held & attended. All days on or before your chosen date will be locked in the Calendar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = importAttendanceEnabled,
                                onCheckedChange = {
                                    importAttendanceEnabled = it
                                    validationError = null
                                },
                                modifier = Modifier.testTag("setup_import_attendance_toggle")
                            )
                        }

                        AnimatedVisibility(visible = importAttendanceEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Mode Selector: Enter % vs Attendance % Calculator
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = importMode == SetupImportMode.PERCENTAGE,
                                        onClick = {
                                            importMode = SetupImportMode.PERCENTAGE
                                            validationError = null
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Percent,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = { Text("Enter %") },
                                        modifier = Modifier.testTag("setup_import_mode_percent")
                                    )
                                    FilterChip(
                                        selected = importMode == SetupImportMode.CALCULATOR,
                                        onClick = {
                                            importMode = SetupImportMode.CALCULATOR
                                            validationError = null
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Calculate,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = { Text("% Calculator") },
                                        modifier = Modifier.testTag("setup_import_mode_calculator")
                                    )
                                }

                                // Up Until This Day Selector (Mandatory for locking prior days)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Up Until This Day (Included & Locked) *",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    OutlinedButton(
                                        onClick = { pickingBaselineEndDate = true },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("setup_baseline_end_date_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CalendarMonth,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(DateUtils.formatReadableFull(baselineEndDate))
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FilterChip(
                                            selected = baselineEndDate == yesterdayKey,
                                            onClick = { baselineEndDate = yesterdayKey; validationError = null },
                                            label = { Text("Yesterday") }
                                        )
                                        FilterChip(
                                            selected = baselineEndDate == lastFridayKey,
                                            onClick = { baselineEndDate = lastFridayKey; validationError = null },
                                            label = { Text("Last Friday") }
                                        )
                                        FilterChip(
                                            selected = baselineEndDate == todayKey,
                                            onClick = { baselineEndDate = todayKey; validationError = null },
                                            label = { Text("Today") }
                                        )
                                        FilterChip(
                                            selected = baselineEndDate == endOfLastMonthKey,
                                            onClick = { baselineEndDate = endOfLastMonthKey; validationError = null },
                                            label = { Text("End of Last Month") }
                                        )
                                    }
                                }

                                if (importMode == SetupImportMode.PERCENTAGE) {
                                    // Direct Percentage Mode
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = baselinePercentText,
                                            onValueChange = { raw ->
                                                val filtered = raw.filter { it.isDigit() || it == '.' }
                                                if (filtered.count { it == '.' } <= 1 && filtered.length <= 6) {
                                                    baselinePercentText = filtered
                                                    validationError = null
                                                }
                                            },
                                            label = { Text("Attendance Percentage (%) *") },
                                            placeholder = { Text("e.g., 84.5") },
                                            suffix = { Text("%") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            isError = baselinePercentText.isNotBlank() && parsedPercent == null,
                                            supportingText = if (baselinePercentText.isNotBlank() && parsedPercent == null) {
                                                { Text("Enter a percentage between 0 and 100") }
                                            } else {
                                                null
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("setup_import_percent_input")
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf("75", "80", "85", "90", "95").forEach { preset ->
                                                FilterChip(
                                                    selected = baselinePercentText == preset,
                                                    onClick = { baselinePercentText = preset; validationError = null },
                                                    label = { Text("$preset%") }
                                                )
                                            }
                                        }

                                        // Period start + detected school days
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
                                                            text = "From ${DateUtils.formatReadableShort(effectiveStartDate)} → ${DateUtils.formatReadableShort(baselineEndDate)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            showOverrideSchoolDays = !showOverrideSchoolDays
                                                            if (showOverrideSchoolDays && customSchoolDaysText.isEmpty() && detectedSchoolDays > 0) {
                                                                customSchoolDaysText = detectedSchoolDays.toString()
                                                            }
                                                        }
                                                    ) {
                                                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(if (showOverrideSchoolDays) "Auto" else "Adjust")
                                                    }
                                                }

                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    if (activeTerm.startDate <= baselineEndDate) {
                                                        FilterChip(
                                                            selected = baselineStartDate == activeTerm.startDate,
                                                            onClick = { baselineStartDate = activeTerm.startDate },
                                                            label = { Text("${activeTerm.name} Start") }
                                                        )
                                                    }
                                                    if (yearStartTerm != null &&
                                                        yearStartTerm.startDate != activeTerm.startDate &&
                                                        yearStartTerm.startDate <= baselineEndDate
                                                    ) {
                                                        FilterChip(
                                                            selected = baselineStartDate == yearStartTerm.startDate,
                                                            onClick = { baselineStartDate = yearStartTerm.startDate },
                                                            label = { Text("${yearStartTerm.academicYear} Day 1") }
                                                        )
                                                    }
                                                }

                                                AnimatedVisibility(visible = showOverrideSchoolDays) {
                                                    OutlinedTextField(
                                                        value = customSchoolDaysText,
                                                        onValueChange = {
                                                            customSchoolDaysText = it.filter(Char::isDigit).take(3)
                                                            validationError = null
                                                        },
                                                        label = { Text("Exact Total School Days (Register)") },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .testTag("setup_import_override_days_input")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Attendance % Calculator Mode (Days School Held & Days You Attended)
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Enter how many days school was held and how many days you attended to calculate your exact percentage.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = calcDaysHeldText,
                                                onValueChange = {
                                                    calcDaysHeldText = it.filter(Char::isDigit).take(3)
                                                    validationError = null
                                                },
                                                label = { Text("Days School Held *") },
                                                placeholder = { Text("e.g., 50") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                isError = calcDaysHeldText.isNotBlank() && (parsedDaysHeld == null || parsedDaysHeld <= 0),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("setup_calc_days_held_input")
                                            )
                                            OutlinedTextField(
                                                value = calcDaysAttendedText,
                                                onValueChange = {
                                                    calcDaysAttendedText = it.filter(Char::isDigit).take(3)
                                                    validationError = null
                                                },
                                                label = { Text("Days You Attended *") },
                                                placeholder = { Text("e.g., 42") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                isError = calcInputMismatch,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("setup_calc_days_attended_input")
                                            )
                                        }
                                        if (calcInputMismatch) {
                                            Text(
                                                text = "Days attended cannot be greater than days school held.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                // Live Result Card
                                if (calculatedEstimate != null && calculatedEstimate.totalSchoolDays > 0) {
                                    val isEligible = calculatedEstimate.effectivePercent >= activeTerm.minEligibilityPercent
                                    Surface(
                                        color = if (isEligible) {
                                            semantic.presentContainer.copy(alpha = 0.65f)
                                        } else {
                                            semantic.absentContainer.copy(alpha = 0.72f)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("setup_calc_result_card")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Calculated Attendance: ${String.format(Locale.US, "%.1f", calculatedEstimate.effectivePercent)}%",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isEligible) semantic.onPresentContainer else semantic.onAbsentContainer
                                                )
                                                Text(
                                                    text = "${calculatedEstimate.presentDays} Present • ${calculatedEstimate.absentDays} Absent (${calculatedEstimate.totalSchoolDays} school days)",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isEligible) semantic.onPresentContainer else semantic.onAbsentContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                // Explicit Calendar Lock Warning
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("setup_baseline_lock_notice")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = "Calendar lock notice",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "All days on or before ${DateUtils.formatReadableFull(baselineEndDate)} will be locked and cannot be edited in the Calendar to prevent duplicate counting.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                LiquidGlassButton(
                    onClick = {
                        val s1 = subject1.trim()
                        val s2 = subject2.trim()
                        val s3 = subject3.trim()
                        val cls = className.trim().uppercase()
                        val baselineError = if (importAttendanceEnabled) {
                            when {
                                baselineEndDate > todayKey ->
                                    "The 'Up Until This Day' date cannot be in the future."
                                importMode == SetupImportMode.CALCULATOR && (parsedDaysHeld == null || parsedDaysHeld <= 0) ->
                                    "Please enter how many days school was held."
                                importMode == SetupImportMode.CALCULATOR && (parsedDaysAttended == null || parsedDaysAttended < 0) ->
                                    "Please enter how many days you attended."
                                importMode == SetupImportMode.CALCULATOR && calcInputMismatch ->
                                    "Days attended cannot be greater than days school held."
                                importMode == SetupImportMode.PERCENTAGE && parsedPercent == null ->
                                    "Please enter a valid attendance percentage between 0 and 100."
                                calculatedEstimate == null || calculatedEstimate.totalSchoolDays <= 0 ->
                                    "No school days found for the selected date range. Adjust the date or school days."
                                else -> null
                            }
                        } else {
                            null
                        }

                        when {
                            firstName.trim().isEmpty() -> validationError = "Please enter your first name."
                            lastName.trim().isEmpty() -> validationError = "Please enter your last name."
                            cls.isEmpty() -> validationError = "Please select or enter your class."
                            s1.isEmpty() || s2.isEmpty() || s3.isEmpty() ->
                                validationError = "Please select or enter all three A/L subjects."
                            baselineError != null -> validationError = baselineError
                            else -> {
                                if (reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                val missingSubjects = buildList {
                                    if (bucket1Subjects.none { it.equals(s1, ignoreCase = true) }) {
                                        add(AcademicCatalog.BUCKET_1 to s1)
                                    }
                                    if (bucket2Subjects.none { it.equals(s2, ignoreCase = true) }) {
                                        add(AcademicCatalog.BUCKET_2 to s2)
                                    }
                                    if (bucket3Subjects.none { it.equals(s3, ignoreCase = true) }) {
                                        add(AcademicCatalog.BUCKET_3 to s3)
                                    }
                                }
                                val missingClass = cls.takeIf {
                                    availableClasses.none { it.equals(cls, ignoreCase = true) }
                                }
                                val setupBaseline = if (importAttendanceEnabled && calculatedEstimate != null) {
                                    SetupAttendanceBaseline(
                                        startDateKey = calculatedEstimate.startDateKey,
                                        endDateKey = calculatedEstimate.endDateKey,
                                        percentage = calculatedEstimate.enteredPercent,
                                        presentDays = calculatedEstimate.presentDays,
                                        absentDays = calculatedEstimate.absentDays
                                    )
                                } else {
                                    null
                                }
                                val shouldUpdateBaseline = importAttendanceEnabled || (existingProfile?.hasLockedBaseline == true)
                                onSave(
                                    firstName.trim(),
                                    lastName.trim(),
                                    nickname.trim().ifEmpty { null },
                                    AppConstants.DEFAULT_SCHOOL_NAME,
                                    grade,
                                    alStream,
                                    cls,
                                    examYear,
                                    listOf(s1, s2, s3),
                                    reminderEnabled,
                                    reminderHour,
                                    reminderMinute,
                                    existingProfile?.countExcusedAsPresent ?: true,
                                    setupBaseline,
                                    shouldUpdateBaseline,
                                    missingSubjects,
                                    missingClass
                                )
                            }
                        }
                    },
                    enabled = !isSaving,
                    tint = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.primary,
                    minHeight = 56.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("complete_setup_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Start Tracking Attendance",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Choose Reminder Time") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        reminderHour = pickerState.hour
                        reminderMinute = pickerState.minute
                        showTimePicker = false
                    }
                ) { Text("Set Time") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }

    if (pickingBaselineEndDate) {
        val maxMillis = remember(todayKey) { setupIsoDateToUtcMillis(todayKey) }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = setupIsoDateToUtcMillis(baselineEndDate),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return maxMillis == null || utcTimeMillis <= maxMillis
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { pickingBaselineEndDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            baselineEndDate = setupUtcMillisToIsoDate(millis)
                            validationError = null
                        }
                        pickingBaselineEndDate = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { pickingBaselineEndDate = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun setupIsoDateToUtcMillis(dateKey: String): Long? {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        sdf.parse(dateKey)?.time
    } catch (_: Exception) {
        null
    }
}

private fun setupUtcMillisToIsoDate(millis: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return sdf.format(Date(millis))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectBucketPicker(
    bucketLabel: String,
    options: List<String>,
    selectedValue: String,
    useCustom: Boolean,
    inputTag: String,
    otherChipTag: String,
    streamName: String,
    onPickOption: (String) -> Unit,
    onSelectCustom: () -> Unit,
    onCustomTextChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$bucketLabel *",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { subject ->
                FilterChip(
                    selected = !useCustom && selectedValue.equals(subject, ignoreCase = true),
                    onClick = { onPickOption(subject) },
                    label = { Text(subject) }
                )
            }
            FilterChip(
                selected = useCustom,
                onClick = onSelectCustom,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text("Other") },
                modifier = Modifier.testTag(otherChipTag)
            )
        }
        AnimatedVisibility(visible = useCustom || options.isEmpty()) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = onCustomTextChange,
                label = { Text("Enter $bucketLabel Subject *") },
                placeholder = { Text("Type subject name") },
                supportingText = {
                    Text("Not in the list? We'll let the admin know to add it under $streamName → $bucketLabel.")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(inputTag)
            )
        }
    }
}

@Composable
private fun SetupSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
