package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.DateUtils
import com.example.data.PersonalAcademicMarker
import com.example.data.PersonalMarkerCategory

/**
 * Dialog for students to pin Personal Academic Markers directly onto their calendar
 * (e.g. Physics Practical, Term Test Start, SBA Deadline).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalMarkerDialog(
    dateKey: String,
    existingMarker: PersonalAcademicMarker? = null,
    onDismiss: () -> Unit,
    onSave: (dateKey: String, title: String, category: PersonalMarkerCategory, note: String?, existingId: String?) -> Unit
) {
    var title by remember(existingMarker) { mutableStateOf(existingMarker?.title.orEmpty()) }
    var category by remember(existingMarker) {
        mutableStateOf(existingMarker?.categoryEnum ?: PersonalMarkerCategory.EXAM)
    }
    var note by remember(existingMarker) { mutableStateOf(existingMarker?.note.orEmpty()) }

    val quickTemplates = listOf(
        "Physics Practical" to PersonalMarkerCategory.PRACTICAL,
        "Term Test Start" to PersonalMarkerCategory.EXAM,
        "SBA Deadline" to PersonalMarkerCategory.DEADLINE,
        "Chemistry Lab" to PersonalMarkerCategory.PRACTICAL
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.BookmarkAdd, contentDescription = null) },
        title = {
            Text(
                text = if (existingMarker == null) "Pin Personal Academic Date" else "Edit Personal Marker",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("personal_marker_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Date: ${DateUtils.formatReadableFull(dateKey)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )

                // Quick one-tap presets
                if (existingMarker == null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        quickTemplates.forEach { (presetTitle, presetCat) ->
                            FilterChip(
                                selected = title == presetTitle,
                                onClick = {
                                    title = presetTitle
                                    category = presetCat
                                },
                                label = { Text(presetTitle, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("Marker title") },
                    placeholder = { Text("e.g., Physics Practical, SBA Submission") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("personal_marker_title_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val categoryOptions = remember {
                        PersonalMarkerCategory.entries.map { cat ->
                            LiquidGlassSegmentOption(
                                label = cat.label,
                                testTag = "personal_marker_cat_${cat.name.lowercase()}"
                            )
                        }
                    }
                    SlidableLiquidGlassSegmentedBar(
                        options = categoryOptions,
                        selectedIndex = PersonalMarkerCategory.entries.indexOf(category).coerceAtLeast(0),
                        onOptionSelected = { idx ->
                            PersonalMarkerCategory.entries.getOrNull(idx)?.let { category = it }
                        },
                        barHeight = 42.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 300) note = it },
                    label = { Text("Note (optional)") },
                    placeholder = { Text("Room, chapters, or items to bring") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isNotEmpty()) {
                        onSave(
                            dateKey,
                            title.trim(),
                            category,
                            note.trim().ifEmpty { null },
                            existingMarker?.id?.ifBlank { null }
                        )
                    }
                },
                enabled = title.trim().isNotEmpty(),
                modifier = Modifier.testTag("personal_marker_save_btn")
            ) {
                Text(if (existingMarker == null) "Pin to Calendar" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
