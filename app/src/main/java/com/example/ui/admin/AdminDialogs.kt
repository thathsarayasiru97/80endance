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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AppConstants
import com.example.data.AttendanceStatus
import com.example.data.CalendarEventType
import com.example.data.DateUtils
import com.example.data.Notice
import com.example.data.NoticeCategory
import com.example.data.NoticeTargetType
import com.example.data.UserProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoticeEditorDialog(
    editingNotice: Notice?,
    onDismiss: () -> Unit,
    onSave: (
        noticeId: String?,
        title: String,
        message: String,
        category: NoticeCategory,
        pinned: Boolean,
        targetType: NoticeTargetType,
        targetValue: String?,
        eventDate: String?,
        urgent: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(editingNotice?.title.orEmpty()) }
    var message by remember { mutableStateOf(editingNotice?.message.orEmpty()) }
    var category by remember { mutableStateOf(editingNotice?.categoryEnum ?: NoticeCategory.TEACHER_ABSENT) }
    var pinned by remember { mutableStateOf(editingNotice?.pinned ?: true) }
    var targetType by remember { mutableStateOf(editingNotice?.targetTypeEnum ?: NoticeTargetType.ALL) }
    var targetValue by remember { mutableStateOf(editingNotice?.targetValue.orEmpty()) }
    var eventDate by remember { mutableStateOf(editingNotice?.eventDate.orEmpty()) }
    var urgent by remember { mutableStateOf(editingNotice?.urgentFlag ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingNotice == null) "Publish Notice" else "Edit Notice") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Headline *") },
                    placeholder = { Text("e.g., Physics Sir Absent Tomorrow") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it.take(1000) },
                    label = { Text("Message *") },
                    placeholder = { Text("Details about schedule change or event") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    NoticeCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }

                Text("Target Audience", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NoticeTargetType.entries.forEach { tt ->
                        FilterChip(
                            selected = targetType == tt,
                            onClick = {
                                targetType = tt
                                if (tt == NoticeTargetType.GRADE && targetValue !in AppConstants.GRADES) {
                                    targetValue = AppConstants.GRADES.last()
                                } else if (tt == NoticeTargetType.STREAM && targetValue !in AppConstants.AL_STREAMS) {
                                    targetValue = AppConstants.AL_STREAMS.first()
                                } else if (tt == NoticeTargetType.ALL) {
                                    targetValue = ""
                                }
                            },
                            label = { Text(tt.label) }
                        )
                    }
                }

                if (targetType == NoticeTargetType.GRADE) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppConstants.GRADES.forEach { grade ->
                            FilterChip(
                                selected = targetValue == grade,
                                onClick = { targetValue = grade },
                                label = { Text(grade) }
                            )
                        }
                    }
                } else if (targetType == NoticeTargetType.STREAM) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppConstants.AL_STREAMS.forEach { stream ->
                            FilterChip(
                                selected = targetValue == stream,
                                onClick = { targetValue = stream },
                                label = { Text(stream) }
                            )
                        }
                    }
                } else if (targetType == NoticeTargetType.CLASS) {
                    OutlinedTextField(
                        value = targetValue,
                        onValueChange = { targetValue = it.uppercase().take(20) },
                        label = { Text("Class Code (e.g., 13M2)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = eventDate,
                    onValueChange = { eventDate = it.take(10) },
                    label = { Text("Event Date (Optional, YYYY-MM-DD)") },
                    placeholder = { Text(DateUtils.todayKey()) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pin to 5s Dashboard Slideshow", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark Urgent Highlight", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = urgent, onCheckedChange = { urgent = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isNotEmpty() && message.trim().isNotEmpty()) {
                        onSave(
                            editingNotice?.id,
                            title.trim(),
                            message.trim(),
                            category,
                            pinned,
                            targetType,
                            targetValue.trim().ifEmpty { null },
                            eventDate.trim().ifEmpty { null },
                            urgent
                        )
                    }
                },
                enabled = title.trim().isNotEmpty() && message.trim().isNotEmpty()
            ) {
                Text(if (editingNotice == null) "Publish" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminOverrideAttendanceDialog(
    student: UserProfile,
    initialDateKey: String = DateUtils.todayKey(),
    onDismiss: () -> Unit,
    onSave: (dateKey: String, status: AttendanceStatus, note: String?) -> Unit
) {
    var dateKey by remember { mutableStateOf(initialDateKey) }
    var status by remember { mutableStateOf(AttendanceStatus.PRESENT) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log / Fix Attendance for ${student.greetingName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = dateKey,
                    onValueChange = { dateKey = it.take(10) },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AttendanceStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.shortLabel) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g., Fixed accidental absent tap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
                        onSave(dateKey, status, note.trim().ifEmpty { null })
                    }
                },
                enabled = dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))
            ) { Text("Save Fix") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminEditStudentDialog(
    student: UserProfile,
    onDismiss: () -> Unit,
    onSave: (className: String, stream: String, priorPresent: Int, priorAbsent: Int) -> Unit
) {
    var className by remember { mutableStateOf(student.className) }
    var stream by remember { mutableStateOf(student.alStream) }
    var priorPresent by remember { mutableStateOf(student.priorPresentCount.toString()) }
    var priorAbsent by remember { mutableStateOf(student.priorAbsentCount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${student.fullName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it.uppercase().take(20) },
                    label = { Text("Class") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppConstants.AL_STREAMS.forEach { s ->
                        FilterChip(selected = stream == s, onClick = { stream = s }, label = { Text(s) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priorPresent,
                        onValueChange = { priorPresent = it.filter(Char::isDigit).take(3) },
                        label = { Text("Prior Present") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priorAbsent,
                        onValueChange = { priorAbsent = it.filter(Char::isDigit).take(3) },
                        label = { Text("Prior Absent") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (className.isNotBlank()) {
                        onSave(
                            className.trim(),
                            stream,
                            priorPresent.toIntOrNull() ?: 0,
                            priorAbsent.toIntOrNull() ?: 0
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCalendarEventDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, dateKey: String, type: CalendarEventType, description: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dateKey by remember { mutableStateOf(DateUtils.todayKey()) }
    var eventType by remember { mutableStateOf(CalendarEventType.POYA) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Calendar Date") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Event Title *") },
                    placeholder = { Text("e.g., Sports Meet Holiday") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dateKey,
                    onValueChange = { dateKey = it.take(10) },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CalendarEventType.entries.forEach { t ->
                        FilterChip(selected = eventType == t, onClick = { eventType = t }, label = { Text(t.label) })
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(200) },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
                        onSave(title.trim(), dateKey, eventType, description.trim().ifEmpty { null })
                    }
                },
                enabled = title.isNotBlank() && dateKey.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))
            ) { Text("Add Event") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
