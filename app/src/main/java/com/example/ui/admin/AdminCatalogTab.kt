package com.example.ui.admin

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.AcademicCatalog
import com.example.data.AppConstants

/**
 * Admin Panel tab for managing the entire structured A/L Subject Catalog (Stream -> Bucket -> Subjects)
 * and Class Lists (Grade + Stream -> Classes), synced in real time to all students via Firestore.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCatalogTab(
    catalog: AcademicCatalog,
    isBusy: Boolean,
    onSaveCatalog: (updated: AcademicCatalog, message: String) -> Unit
) {
    var selectedSubjectStream by remember { mutableStateOf(AppConstants.AL_STREAMS.first()) }
    var selectedBucket by remember { mutableStateOf(AcademicCatalog.BUCKET_1) }
    var newSubjectText by remember { mutableStateOf("") }

    var selectedClassGrade by remember { mutableStateOf(AppConstants.GRADES.first()) }
    var selectedClassStream by remember { mutableStateOf("Bio Science") }
    var newClassText by remember { mutableStateOf("") }

    var showResetConfirm by remember { mutableStateOf(false) }

    val currentBucketSubjects = catalog.subjectsFor(selectedSubjectStream, selectedBucket)
    val currentClasses = catalog.classesFor(selectedClassGrade, selectedClassStream)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_catalog_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Manage the A/L subject buckets and class lists shown to students during setup & profile editing. Changes sync instantly to all devices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Subjects by Stream -> Bucket
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.MenuBook,
                            contentDescription = "Subjects catalog",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subjects by Stream & Bucket",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text("Select A/L Stream", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppConstants.AL_STREAMS.forEach { stream ->
                            FilterChip(
                                selected = selectedSubjectStream == stream,
                                onClick = { selectedSubjectStream = stream },
                                label = { Text(stream) }
                            )
                        }
                    }

                    Text("Select Bucket", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AcademicCatalog.BUCKET_KEYS.forEach { bucket ->
                            val count = catalog.subjectsFor(selectedSubjectStream, bucket).size
                            FilterChip(
                                selected = selectedBucket == bucket,
                                onClick = { selectedBucket = bucket },
                                label = { Text("$bucket ($count)") }
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$selectedSubjectStream → $selectedBucket",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (currentBucketSubjects.isEmpty()) {
                                Text(
                                    text = "No subjects in this bucket yet. Add one below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    currentBucketSubjects.forEach { sub ->
                                        InputChip(
                                            selected = false,
                                            onClick = {
                                                if (!isBusy) {
                                                    val updated = catalog.withRemovedSubject(selectedSubjectStream, selectedBucket, sub)
                                                    onSaveCatalog(updated, "Removed '$sub' from $selectedSubjectStream → $selectedBucket")
                                                }
                                            },
                                            label = { Text(sub) },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Remove $sub",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubjectText,
                            onValueChange = { newSubjectText = it.take(60) },
                            label = { Text("Add subject to $selectedBucket") },
                            placeholder = { Text("e.g., ICT, Higher Maths") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_catalog_add_subject_input")
                        )
                        Button(
                            onClick = {
                                val trimmed = newSubjectText.trim()
                                if (trimmed.isNotEmpty()) {
                                    val updated = catalog.withAddedSubject(selectedSubjectStream, selectedBucket, trimmed)
                                    onSaveCatalog(updated, "Added '$trimmed' to $selectedSubjectStream → $selectedBucket")
                                    newSubjectText = ""
                                }
                            },
                            enabled = !isBusy && newSubjectText.trim().isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("admin_catalog_add_subject_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }

        // Section 2: Classes by Grade + Stream
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.MeetingRoom,
                            contentDescription = "Classes catalog",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Classes by Grade & Stream",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text("Select Grade", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppConstants.GRADES.forEach { g ->
                            FilterChip(
                                selected = selectedClassGrade == g,
                                onClick = { selectedClassGrade = g },
                                label = { Text(g) }
                            )
                        }
                    }

                    Text("Select A/L Stream", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AppConstants.AL_STREAMS.forEach { stream ->
                            FilterChip(
                                selected = selectedClassStream == stream,
                                onClick = { selectedClassStream = stream },
                                label = { Text(stream) }
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$selectedClassGrade • $selectedClassStream (${currentClasses.size} classes)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (currentClasses.isEmpty()) {
                                Text(
                                    text = "No classes listed yet for $selectedClassGrade $selectedClassStream.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    currentClasses.forEach { cls ->
                                        InputChip(
                                            selected = false,
                                            onClick = {
                                                if (!isBusy) {
                                                    val updated = catalog.withRemovedClass(selectedClassGrade, selectedClassStream, cls)
                                                    onSaveCatalog(updated, "Removed class $cls from $selectedClassGrade $selectedClassStream")
                                                }
                                            },
                                            label = { Text(cls) },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "Remove class $cls",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newClassText,
                            onValueChange = { newClassText = it.uppercase().take(20) },
                            label = { Text("Add class code") },
                            placeholder = { Text("e.g., 12S7, 13M7") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_catalog_add_class_input")
                        )
                        Button(
                            onClick = {
                                val trimmed = newClassText.trim().uppercase()
                                if (trimmed.isNotEmpty()) {
                                    val updated = catalog.withAddedClass(selectedClassGrade, selectedClassStream, trimmed)
                                    onSaveCatalog(updated, "Added class $trimmed to $selectedClassGrade $selectedClassStream")
                                    newClassText = ""
                                }
                            },
                            enabled = !isBusy && newClassText.trim().isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("admin_catalog_add_class_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }

        // Reset defaults option
        item {
            OutlinedButton(
                onClick = { showResetConfirm = true },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Catalog to Sri Lankan A/L Defaults")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Subjects & Classes?") },
            text = { Text("This will restore the default Sri Lankan GCE A/L subject buckets and class lists for all streams and grades.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onSaveCatalog(AcademicCatalog.defaultCatalog(), "Catalog restored to default subjects & classes")
                    }
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
