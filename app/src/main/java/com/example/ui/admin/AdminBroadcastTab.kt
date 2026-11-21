package com.example.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.AppConstants
import com.example.data.BroadcastNotification
import com.example.data.DateUtils
import com.example.data.UserProfile
import com.example.ui.components.ProfileAvatar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminBroadcastTab(
    students: List<UserProfile>,
    broadcasts: List<BroadcastNotification>,
    isBusy: Boolean,
    onSendBroadcast: (
        title: String,
        message: String,
        targetAll: Boolean,
        targetGrades: List<String>,
        targetStreams: List<String>,
        targetClasses: List<String>,
        targetUserIds: List<String>,
        audienceSummary: String,
        onDone: () -> Unit
    ) -> Unit,
    onDeleteBroadcast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var header by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var targetAll by remember { mutableStateOf(true) }
    var selectedGrades by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedStreams by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedClasses by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customClassInput by remember { mutableStateOf("") }
    var studentSearch by remember { mutableStateOf("") }
    var showConfirmPopup by remember { mutableStateOf(false) }

    val availableClasses = remember(students, selectedClasses) {
        (students.map { it.className.trim() }.filter { it.isNotEmpty() } + selectedClasses)
            .distinctBy { it.uppercase() }
            .sorted()
    }

    val audienceSummary = remember(targetAll, selectedGrades, selectedStreams, selectedClasses, selectedUserIds) {
        if (targetAll) {
            "Everyone (All Students)"
        } else {
            val parts = mutableListOf<String>()
            if (selectedGrades.isNotEmpty()) parts.add(selectedGrades.sorted().joinToString(", "))
            if (selectedStreams.isNotEmpty()) parts.add(selectedStreams.sorted().joinToString(", "))
            if (selectedClasses.isNotEmpty()) parts.add(selectedClasses.sorted().joinToString(", "))
            if (selectedUserIds.isNotEmpty()) {
                val n = selectedUserIds.size
                parts.add(if (n == 1) "1 selected student" else "$n selected students")
            }
            parts.joinToString(" • ").ifEmpty { "No audience selected" }
        }
    }

    val matchedRecipientsCount = remember(
        students, targetAll, selectedGrades, selectedStreams, selectedClasses, selectedUserIds
    ) {
        if (targetAll) {
            students.size
        } else {
            val previewNotification = BroadcastNotification(
                targetAll = false,
                targetGrades = selectedGrades.toList(),
                targetStreams = selectedStreams.toList(),
                targetClasses = selectedClasses.toList(),
                targetUserIds = selectedUserIds.toList()
            )
            students.count { st -> previewNotification.matchesStudent(st, st.userId) }
        }
    }

    val hasValidAudience = targetAll ||
        selectedGrades.isNotEmpty() ||
        selectedStreams.isNotEmpty() ||
        selectedClasses.isNotEmpty() ||
        selectedUserIds.isNotEmpty()

    val canOpenConfirm = header.trim().isNotEmpty() &&
        content.trim().isNotEmpty() &&
        hasValidAudience &&
        !isBusy

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_broadcast_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_broadcast_composer_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = "Release notification",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Release Custom Notification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Target everyone or combine grades, streams, classes & specific students",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = header,
                        onValueChange = { header = it.take(120) },
                        label = { Text("Notification Header *") },
                        placeholder = { Text("e.g., Special Assembly Tomorrow at 7:30 AM") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_broadcast_title_input")
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it.take(1000) },
                        label = { Text("Notification Content *") },
                        placeholder = { Text("Write the full announcement message to deliver…") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_broadcast_message_input")
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Audience(s)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = if (hasValidAudience) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Group,
                                    contentDescription = null,
                                    tint = if (hasValidAudience) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (students.isEmpty()) {
                                        if (targetAll) "All students" else "Custom"
                                    } else {
                                        "Reaches $matchedRecipientsCount / ${students.size}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasValidAudience) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Everyone vs Custom Multi-Audience Toggle
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = targetAll,
                            onClick = { targetAll = true },
                            leadingIcon = if (targetAll) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text("Everyone") },
                            modifier = Modifier.testTag("admin_broadcast_all_chip")
                        )
                        FilterChip(
                            selected = !targetAll,
                            onClick = { targetAll = false },
                            leadingIcon = if (!targetAll) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text("Select Audiences") },
                            modifier = Modifier.testTag("admin_broadcast_custom_chip")
                        )
                    }

                    if (!targetAll) {
                        // Multi-select Grades
                        Text(
                            text = "Grades",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppConstants.GRADES.forEach { grade ->
                                val picked = grade in selectedGrades
                                FilterChip(
                                    selected = picked,
                                    onClick = {
                                        selectedGrades = if (picked) selectedGrades - grade else selectedGrades + grade
                                    },
                                    label = { Text(grade) },
                                    modifier = Modifier.testTag("admin_audience_grade_${grade.replace(" ", "_")}")
                                )
                            }
                        }

                        // Multi-select Streams
                        Text(
                            text = "A/L Streams",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppConstants.AL_STREAMS.forEach { stream ->
                                val picked = stream in selectedStreams
                                FilterChip(
                                    selected = picked,
                                    onClick = {
                                        selectedStreams = if (picked) selectedStreams - stream else selectedStreams + stream
                                    },
                                    label = { Text(stream) }
                                )
                            }
                        }

                        // Multi-select Classes + quick add
                        Text(
                            text = "Classes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (availableClasses.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                availableClasses.forEach { cls ->
                                    val picked = selectedClasses.any { it.equals(cls, ignoreCase = true) }
                                    FilterChip(
                                        selected = picked,
                                        onClick = {
                                            selectedClasses = if (picked) {
                                                selectedClasses.filterNot { it.equals(cls, ignoreCase = true) }.toSet()
                                            } else {
                                                selectedClasses + cls
                                            }
                                        },
                                        label = { Text(cls) }
                                    )
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = customClassInput,
                                onValueChange = { customClassInput = it.take(20) },
                                label = { Text("Add class code (e.g., 13M2)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val trimmed = customClassInput.trim()
                                    if (trimmed.isNotEmpty()) {
                                        selectedClasses = selectedClasses + trimmed
                                        customClassInput = ""
                                    }
                                },
                                enabled = customClassInput.trim().isNotEmpty()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add class audience")
                            }
                        }

                        // Multi-select Individual Students
                        if (students.isNotEmpty()) {
                            Text(
                                text = "Specific Students (${selectedUserIds.size} selected)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = studentSearch,
                                onValueChange = { studentSearch = it },
                                placeholder = { Text("Filter students by name or class…") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            val matchingStudents = remember(students, studentSearch) {
                                val q = studentSearch.trim()
                                if (q.isEmpty()) students.take(8)
                                else students.filter {
                                    it.fullName.contains(q, ignoreCase = true) ||
                                        it.className.contains(q, ignoreCase = true)
                                }.take(12)
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    matchingStudents.forEach { st ->
                                        val checked = st.userId in selectedUserIds
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedUserIds = if (checked) {
                                                        selectedUserIds - st.userId
                                                    } else {
                                                        selectedUserIds + st.userId
                                                    }
                                                }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = { isChecked ->
                                                    selectedUserIds = if (isChecked) {
                                                        selectedUserIds + st.userId
                                                    } else {
                                                        selectedUserIds - st.userId
                                                    }
                                                }
                                            )
                                            ProfileAvatar(
                                                customAvatarBase64 = st.customAvatarBase64,
                                                googlePhotoUrl = st.photoUrl,
                                                displayName = st.fullName,
                                                size = 30.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(st.fullName, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    text = "${st.className} • ${st.alStream}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    if (studentSearch.isBlank() && students.size > 8) {
                                        Text(
                                            text = "Search above to find more of ${students.size} students",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showConfirmPopup = true },
                        enabled = canOpenConfirm,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_broadcast_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Notification")
                    }
                }
            }
        }

        // Sent Notification History
        item {
            Text(
                text = "Released Notifications (${broadcasts.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (broadcasts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No custom notifications released yet. Compose one above to alert students.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(broadcasts, key = { it.id }) { item ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = item.audienceSummary,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteBroadcast(item.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete notification")
                            }
                        }
                        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val sentAtMs = item.createdAt?.toDate()?.time
                        if (sentAtMs != null) {
                            Text(
                                text = "Sent ${DateUtils.formatLastSeen(sentAtMs, isOnline = false).removePrefix("Last seen ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    // Confirmation Popup before releasing notification
    if (showConfirmPopup) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) showConfirmPopup = false },
            modifier = Modifier.testTag("admin_broadcast_confirm_dialog"),
            icon = {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Confirm Notification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Review the message and audience before sending:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = header.trim(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = content.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "TARGET AUDIENCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = audienceSummary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (students.isNotEmpty()) {
                                Text(
                                    text = "Estimated reach: $matchedRecipientsCount student(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSendBroadcast(
                            header.trim(),
                            content.trim(),
                            targetAll,
                            selectedGrades.toList(),
                            selectedStreams.toList(),
                            selectedClasses.toList(),
                            selectedUserIds.toList(),
                            audienceSummary
                        ) {
                            header = ""
                            content = ""
                            selectedGrades = emptySet()
                            selectedStreams = emptySet()
                            selectedClasses = emptySet()
                            selectedUserIds = emptySet()
                            targetAll = true
                            showConfirmPopup = false
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.testTag("admin_broadcast_confirm_send_button")
                ) {
                    Text(if (isBusy) "Sending…" else "Confirm & Send")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmPopup = false },
                    enabled = !isBusy
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
