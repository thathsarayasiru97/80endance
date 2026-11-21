package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.AcademicCatalog
import com.example.data.AppConstants
import com.example.data.CalendarEvent
import com.example.data.CalendarEventTargetType
import com.example.data.CalendarEventType
import com.example.data.DateUtils
import com.example.data.UserProfile

/**
 * Rich admin dialog to add or edit a calendar event/update on ANY calendar date,
 * supporting custom notes (e.g., "there might be no teaching"), multi-audience visibility
 * targeting (Everyone or any combination of Grades, Streams, Classes, and Specific Students),
 * and optional push/in-app notification dispatch to that exact audience.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCalendarEventEditorDialog(
    initialDateKey: String,
    editingEvent: CalendarEvent? = null,
    students: List<UserProfile> = emptyList(),
    catalog: AcademicCatalog = AcademicCatalog.defaultCatalog(),
    isBusy: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var title by remember(editingEvent) { mutableStateOf(editingEvent?.title.orEmpty()) }
    var dateKey by remember(editingEvent, initialDateKey) { mutableStateOf(editingEvent?.dateKey ?: initialDateKey) }
    var eventType by remember(editingEvent) {
        mutableStateOf(editingEvent?.eventTypeEnum ?: CalendarEventType.SCHOOL_EVENT)
    }
    var note by remember(editingEvent) { mutableStateOf(editingEvent?.description.orEmpty()) }

    val initTargetAll = remember(editingEvent) {
        when {
            editingEvent == null -> true
            editingEvent.hasMultiAudienceTargeting -> editingEvent.targetAll == true
            else -> editingEvent.targetTypeEnum == CalendarEventTargetType.ALL
        }
    }
    var targetAll by remember(editingEvent) { mutableStateOf(initTargetAll) }
    var selectedGrades by remember(editingEvent) {
        mutableStateOf(
            when {
                editingEvent == null -> emptySet()
                editingEvent.targetGrades.isNotEmpty() -> editingEvent.targetGrades.toSet()
                editingEvent.targetTypeEnum == CalendarEventTargetType.GRADE && !editingEvent.targetValue.isNullOrBlank() ->
                    setOf(editingEvent.targetValue.trim())
                else -> emptySet()
            }
        )
    }
    var selectedStreams by remember(editingEvent) {
        mutableStateOf(
            when {
                editingEvent == null -> emptySet()
                editingEvent.targetStreams.isNotEmpty() -> editingEvent.targetStreams.toSet()
                editingEvent.targetTypeEnum == CalendarEventTargetType.STREAM && !editingEvent.targetValue.isNullOrBlank() ->
                    setOf(editingEvent.targetValue.trim())
                else -> emptySet()
            }
        )
    }
    var selectedClasses by remember(editingEvent) {
        mutableStateOf(
            when {
                editingEvent == null -> emptySet()
                editingEvent.targetClasses.isNotEmpty() -> editingEvent.targetClasses.toSet()
                editingEvent.targetTypeEnum == CalendarEventTargetType.CLASS && !editingEvent.targetValue.isNullOrBlank() ->
                    setOf(editingEvent.targetValue.trim())
                else -> emptySet()
            }
        )
    }
    var selectedUserIds by remember(editingEvent) {
        mutableStateOf(editingEvent?.targetUserIds?.toSet() ?: emptySet())
    }
    var customClassInput by remember { mutableStateOf("") }
    var studentSearch by remember { mutableStateOf("") }
    var alsoSendNotification by remember(editingEvent) { mutableStateOf(editingEvent == null) }

    val availableClasses = remember(students, catalog) {
        val fromStudents = students.map { it.className.trim().uppercase() }.filter { it.isNotEmpty() }
        val fromCatalog = catalog.gradeStreamClasses.values.flatten()
        (fromStudents + fromCatalog).distinct().sorted()
    }

    val matchingStudents = remember(students, studentSearch) {
        if (studentSearch.isBlank()) {
            students.take(8)
        } else {
            students.filter {
                it.fullName.contains(studentSearch, ignoreCase = true) ||
                    it.className.contains(studentSearch, ignoreCase = true) ||
                    it.alStream.contains(studentSearch, ignoreCase = true)
            }.take(10)
        }
    }

    val hasSelectedAudience = targetAll ||
        selectedGrades.isNotEmpty() ||
        selectedStreams.isNotEmpty() ||
        selectedClasses.isNotEmpty() ||
        selectedUserIds.isNotEmpty()

    val audienceSummary = remember(targetAll, selectedGrades, selectedStreams, selectedClasses, selectedUserIds) {
        if (targetAll) {
            "All Students"
        } else {
            buildList {
                if (selectedGrades.isNotEmpty()) add(selectedGrades.sorted().joinToString(", "))
                if (selectedStreams.isNotEmpty()) add(selectedStreams.sorted().joinToString(", "))
                if (selectedClasses.isNotEmpty()) add(selectedClasses.sorted().joinToString(", "))
                if (selectedUserIds.isNotEmpty()) add("${selectedUserIds.size} student${if (selectedUserIds.size == 1) "" else "s"}")
            }.joinToString(" • ").ifEmpty { "Selected Group" }
        }
    }

    val validDate = dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (editingEvent == null) "Add Calendar Update" else "Edit Calendar Update",
                    style = MaterialTheme.typography.titleLarge
                )
                if (validDate) {
                    Text(
                        text = DateUtils.formatReadableFull(dateKey),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("admin_calendar_event_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dateKey,
                    onValueChange = { dateKey = it.take(10) },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "School Updates & No School Days",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CalendarEventType.schoolUpdateTypes.forEach { t ->
                        FilterChip(
                            selected = eventType == t,
                            onClick = { eventType = t },
                            label = { Text(t.label) },
                            modifier = Modifier.testTag("admin_event_type_${t.raw}")
                        )
                    }
                }

                Text(
                    text = "Sri Lankan Holidays",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CalendarEventType.sriLankanHolidayTypes.forEach { t ->
                        FilterChip(
                            selected = eventType == t,
                            onClick = { eventType = t },
                            label = { Text(t.label) },
                            modifier = Modifier.testTag("admin_event_type_${t.raw}")
                        )
                    }
                }

                Surface(
                    color = if (eventType.isSriLankanHoliday) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (eventType) {
                            CalendarEventType.STUDY_LEAVE ->
                                "No school day (e.g., break between Term Test / Exam days). Excluded from attendance and listed under School Updates — NOT Sri Lankan Holidays."
                            CalendarEventType.SCHOOL_HOLIDAY ->
                                "School-specific No School day with a reason. Excluded from attendance and listed under School Updates — NOT Sri Lankan Holidays."
                            CalendarEventType.TERM_BREAK ->
                                "School term vacation day. Excluded from attendance and listed under School Updates."
                            CalendarEventType.SCHOOL_EVENT ->
                                "Normal school day with a special event or teaching advisory (attendance still tracked)."
                            CalendarEventType.EXAM ->
                                "Exam / Term Test day listed under School Updates (attendance still tracked)."
                            CalendarEventType.POYA, CalendarEventType.PUBLIC_HOLIDAY ->
                                "Official Sri Lankan Holiday — excluded from school days and shown under Sri Lankan Holidays."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Title *") },
                    placeholder = {
                        Text(
                            when (eventType) {
                                CalendarEventType.STUDY_LEAVE -> "e.g., Study Leave (Between Term Tests)"
                                CalendarEventType.SCHOOL_HOLIDAY -> "e.g., Sports Meet Prep — No School"
                                CalendarEventType.EXAM -> "e.g., Term 2 Physics Paper I"
                                CalendarEventType.POYA -> "e.g., Unduvap Full Moon Poya Day"
                                else -> "e.g., Sports Day Rehearsal, No School"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_calendar_event_title_input")
                )

                val isReasonField = eventType == CalendarEventType.STUDY_LEAVE ||
                    eventType == CalendarEventType.SCHOOL_HOLIDAY
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    label = {
                        Text(if (isReasonField) "Reason / Note (Optional)" else "Note / Advisory (Optional)")
                    },
                    placeholder = {
                        Text(
                            when (eventType) {
                                CalendarEventType.STUDY_LEAVE ->
                                    "e.g., No school between Term Test Day 1 & Day 2 — study at home"
                                CalendarEventType.SCHOOL_HOLIDAY ->
                                    "e.g., School closed for A/L hall preparation"
                                else ->
                                    "e.g., There might be no teaching for Grade 12 Bio Science periods 3–5"
                            }
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_calendar_event_note_input")
                )

                HorizontalDivider()

                Text(
                    text = "Who can see this on their calendar?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "If targeted to specific groups, only those students will see it. You can always view & edit all events.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetAll,
                        onClick = { targetAll = true },
                        label = { Text("Everyone") }
                    )
                    FilterChip(
                        selected = !targetAll,
                        onClick = { targetAll = false },
                        label = { Text("Specific Audience") },
                        modifier = Modifier.testTag("admin_calendar_event_specific_audience_chip")
                    )
                }

                if (!targetAll) {
                    Text("Grades", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppConstants.GRADES.forEach { g ->
                            val picked = g in selectedGrades
                            FilterChip(
                                selected = picked,
                                onClick = { selectedGrades = if (picked) selectedGrades - g else selectedGrades + g },
                                label = { Text(g) }
                            )
                        }
                    }

                    Text("A/L Streams", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppConstants.AL_STREAMS.forEach { s ->
                            val picked = s in selectedStreams
                            FilterChip(
                                selected = picked,
                                onClick = { selectedStreams = if (picked) selectedStreams - s else selectedStreams + s },
                                label = { Text(s) }
                            )
                        }
                    }

                    Text("Classes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableClasses.take(16).forEach { cls ->
                            val picked = cls in selectedClasses
                            FilterChip(
                                selected = picked,
                                onClick = { selectedClasses = if (picked) selectedClasses - cls else selectedClasses + cls },
                                label = { Text(cls) }
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = customClassInput,
                            onValueChange = { customClassInput = it.uppercase().take(20) },
                            label = { Text("Add class code") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                val clean = customClassInput.trim().uppercase()
                                if (clean.isNotEmpty()) {
                                    selectedClasses = selectedClasses + clean
                                    customClassInput = ""
                                }
                            },
                            enabled = customClassInput.trim().isNotEmpty()
                        ) { Text("Add") }
                    }

                    if (students.isNotEmpty()) {
                        Text("Specific Students", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = studentSearch,
                            onValueChange = { studentSearch = it.take(40) },
                            label = { Text("Search student by name or class") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            matchingStudents.forEach { st ->
                                val picked = st.userId in selectedUserIds
                                FilterChip(
                                    selected = picked,
                                    onClick = {
                                        selectedUserIds = if (picked) selectedUserIds - st.userId else selectedUserIds + st.userId
                                    },
                                    label = { Text("${st.greetingName} (${st.className})") }
                                )
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Visible to: $audienceSummary",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notify selected audience",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Sends an announcement & device notification to $audienceSummary",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = alsoSendNotification,
                        onCheckedChange = { alsoSendNotification = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isNotEmpty() && validDate && hasSelectedAudience) {
                        onSave(
                            editingEvent?.id,
                            title.trim(),
                            dateKey.trim(),
                            eventType,
                            note.trim().ifEmpty { null },
                            targetAll,
                            selectedGrades.toList(),
                            selectedStreams.toList(),
                            selectedClasses.toList(),
                            selectedUserIds.toList(),
                            audienceSummary,
                            alsoSendNotification
                        )
                    }
                },
                enabled = !isBusy && title.trim().isNotEmpty() && validDate && hasSelectedAudience,
                modifier = Modifier.testTag("admin_calendar_event_save_button")
            ) {
                Text(if (editingEvent == null) "Save Update" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
