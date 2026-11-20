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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.data.AppConstants
import com.example.data.DateUtils
import com.example.data.UserProfile
import com.example.ui.components.LiquidGlassButton

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AcademicSetupScreen(
    initialDisplayName: String?,
    existingProfile: UserProfile?,
    isSaving: Boolean,
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
        priorPresent: Int,
        priorAbsent: Int
    ) -> Unit
) {
    val nameParts = remember(initialDisplayName) {
        initialDisplayName?.trim()?.split(Regex("\\s+")).orEmpty()
    }

    var firstName by remember {
        mutableStateOf(existingProfile?.firstName?.ifEmpty { nameParts.firstOrNull().orEmpty() } ?: nameParts.firstOrNull().orEmpty())
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
    var className by remember { mutableStateOf(existingProfile?.className.orEmpty()) }
    var examYear by remember { mutableIntStateOf(existingProfile?.alExamYear ?: 2026) }

    val defaultSubs = remember(alStream) { AppConstants.defaultSubjectsForStream(alStream) }
    var subject1 by remember { mutableStateOf(existingProfile?.subjects?.getOrNull(0) ?: defaultSubs[0]) }
    var subject2 by remember { mutableStateOf(existingProfile?.subjects?.getOrNull(1) ?: defaultSubs[1]) }
    var subject3 by remember { mutableStateOf(existingProfile?.subjects?.getOrNull(2) ?: defaultSubs[2]) }

    var reminderEnabled by remember { mutableStateOf(existingProfile?.reminderEnabled ?: true) }
    var reminderHour by remember { mutableIntStateOf(existingProfile?.reminderHour ?: AppConstants.DEFAULT_REMINDER_HOUR) }
    var reminderMinute by remember { mutableIntStateOf(existingProfile?.reminderMinute ?: AppConstants.DEFAULT_REMINDER_MINUTE) }
    var showTimePicker by remember { mutableStateOf(false) }

    var showPriorBalance by remember { mutableStateOf((existingProfile?.priorPresentCount ?: 0) > 0 || (existingProfile?.priorAbsentCount ?: 0) > 0) }
    var priorPresentText by remember { mutableStateOf((existingProfile?.priorPresentCount ?: 0).takeIf { it > 0 }?.toString().orEmpty()) }
    var priorAbsentText by remember { mutableStateOf((existingProfile?.priorAbsentCount ?: 0).takeIf { it > 0 }?.toString().orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission outcome handled gracefully; daily popup still works in-app even if denied
    }

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

                // 2. A/L Stream, Grade, Class & Exam Year
                SetupSectionCard(title = "A/L Stream & Class") {
                    Text(
                        text = "Grade & Target A/L Year",
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
                                onClick = { grade = g },
                                label = { Text(g) }
                            )
                        }
                        AppConstants.EXAM_YEARS.forEach { yr ->
                            FilterChip(
                                selected = examYear == yr,
                                onClick = { examYear = yr },
                                label = { Text("$yr A/L") }
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
                                    val preset = AppConstants.defaultSubjectsForStream(stream)
                                    subject1 = preset[0]
                                    subject2 = preset[1]
                                    subject3 = preset[2]
                                },
                                label = { Text(stream) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it.uppercase().take(20); validationError = null },
                        label = { Text("Class *") },
                        placeholder = { Text("e.g., 13M2, 12B1, 13C3") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_class_input")
                    )
                }

                // 3. Three A/L Subjects
                SetupSectionCard(title = "Your Three A/L Subjects") {
                    Text(
                        text = "Pre-filled from your stream — edit any subject if needed (e.g., ICT).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = subject1,
                        onValueChange = { subject1 = it.take(60); validationError = null },
                        label = { Text("Subject 1 *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_subject1_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subject2,
                        onValueChange = { subject2 = it.take(60); validationError = null },
                        label = { Text("Subject 2 *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_subject2_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = subject3,
                        onValueChange = { subject3 = it.take(60); validationError = null },
                        label = { Text("Subject 3 *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_subject3_input")
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

                // 5. Optional Mid-Term Starting Balance
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Joining Mid-Term? (Optional)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Add prior days attended/missed for accurate 80% math from Day 1.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { showPriorBalance = !showPriorBalance }) {
                                Text(if (showPriorBalance) "Hide" else "Add")
                                Icon(
                                    imageVector = if (showPriorBalance) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(visible = showPriorBalance) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                OutlinedTextField(
                                    value = priorPresentText,
                                    onValueChange = { priorPresentText = it.filter(Char::isDigit).take(3) },
                                    label = { Text("Prior Present Days") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = priorAbsentText,
                                    onValueChange = { priorAbsentText = it.filter(Char::isDigit).take(3) },
                                    label = { Text("Prior Absent Days") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
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
                        when {
                            firstName.trim().isEmpty() -> validationError = "Please enter your first name."
                            lastName.trim().isEmpty() -> validationError = "Please enter your last name."
                            className.trim().isEmpty() -> validationError = "Please enter your class (e.g., 13M2)."
                            s1.isEmpty() || s2.isEmpty() || s3.isEmpty() ->
                                validationError = "Please enter all three A/L subjects."
                            else -> {
                                if (reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                onSave(
                                    firstName.trim(),
                                    lastName.trim(),
                                    nickname.trim().ifEmpty { null },
                                    AppConstants.DEFAULT_SCHOOL_NAME,
                                    grade,
                                    alStream,
                                    className.trim(),
                                    examYear,
                                    listOf(s1, s2, s3),
                                    reminderEnabled,
                                    reminderHour,
                                    reminderMinute,
                                    existingProfile?.countExcusedAsPresent ?: true,
                                    priorPresentText.toIntOrNull() ?: 0,
                                    priorAbsentText.toIntOrNull() ?: 0
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
