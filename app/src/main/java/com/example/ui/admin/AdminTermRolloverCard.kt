package com.example.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.AcademicTerm
import com.example.data.AttendanceCalculator
import com.example.data.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminTermRolloverCard(
    terms: List<AcademicTerm>,
    isBusy: Boolean,
    onRolloverTerm: (targetTerm: AcademicTerm, alsoPostNotice: Boolean) -> Unit,
    schoolStartDate: String = "2026-01-15",
    onSaveSchoolStartDate: (String) -> Unit = {},
    onDeleteTerm: (AcademicTerm) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeTerm = remember(terms) { AttendanceCalculator.resolveActiveTerm(terms) }
    val orderedTerms = remember(terms) { terms.sortedBy { it.startDate } }
    val defaultNextTerm = remember(orderedTerms, activeTerm) {
        orderedTerms.firstOrNull { it.startDate > activeTerm.startDate }
            ?: orderedTerms.firstOrNull { it.id != activeTerm.id }
            ?: activeTerm
    }

    var useCustomTerm by remember { mutableStateOf(false) }
    var selectedTermId by remember(defaultNextTerm.id) { mutableStateOf(defaultNextTerm.id) }
    var customName by remember { mutableStateOf("") }
    var customStart by remember { mutableStateOf(DateUtils.todayKey()) }
    var customEnd by remember { mutableStateOf(DateUtils.addDays(DateUtils.todayKey(), 90)) }
    var eligibilityText by remember { mutableStateOf("80") }
    var alsoPostNotice by remember { mutableStateOf(true) }
    var schoolStartInput by remember(schoolStartDate) { mutableStateOf(schoolStartDate) }
    var termPendingDelete by remember { mutableStateOf<AcademicTerm?>(null) }

    val dateRegex = remember { Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$") }
    val selectedPreset = orderedTerms.firstOrNull { it.id == selectedTermId } ?: defaultNextTerm

    val isCustomValid = customName.trim().isNotEmpty() &&
        customStart.matches(dateRegex) &&
        customEnd.matches(dateRegex) &&
        customStart <= customEnd

    val isSchoolStartValid = schoolStartInput.trim().matches(dateRegex)
    val schoolStartChanged = schoolStartInput.trim() != schoolStartDate.trim()

    val canSubmit = !isBusy && if (useCustomTerm) {
        isCustomValid
    } else {
        selectedPreset.id != activeTerm.id
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .testTag("admin_term_rollover_card")
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
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = "Term Rollover Wizard",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Column {
                    Text(
                        text = "Term Rollover Wizard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Switch everyone to the next term, add or remove terms, and set the official school start date.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            // School Start Date (Grade 12 / A/L Journey Start & Calendar Lock)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "School Start Date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "SCHOOL START DATE (A/L JOURNEY & CALENDAR LOCK)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Students can only mark attendance on or after this date inside the Calendar. Also sets Grade 12 Day 1 on the A/L Journey timeline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = schoolStartInput,
                            onValueChange = { schoolStartInput = it.take(10) },
                            label = { Text("Start Date (YYYY-MM-DD)") },
                            supportingText = if (isSchoolStartValid) {
                                { Text(DateUtils.formatReadableShort(schoolStartInput.trim())) }
                            } else {
                                { Text("Format: YYYY-MM-DD") }
                            },
                            isError = schoolStartInput.isNotBlank() && !isSchoolStartValid,
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_school_start_input")
                        )
                        Button(
                            onClick = { onSaveSchoolStartDate(schoolStartInput.trim()) },
                            enabled = !isBusy && isSchoolStartValid && schoolStartChanged,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("admin_school_start_save_button")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "CURRENT ACTIVE TERM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = activeTerm.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${DateUtils.formatReadableShort(activeTerm.startDate)} → ${DateUtils.formatReadableShort(activeTerm.endDate)} • Min ${activeTerm.minEligibilityPercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Select, Add, or Remove Terms",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                orderedTerms.forEach { term ->
                    val isCurrent = term.id == activeTerm.id
                    val canRemove = orderedTerms.size > 1 && !isBusy
                    FilterChip(
                        selected = !useCustomTerm && selectedTermId == term.id,
                        onClick = {
                            useCustomTerm = false
                            selectedTermId = term.id
                        },
                        label = { Text(if (isCurrent) "${term.name} (Active)" else term.name) },
                        trailingIcon = if (canRemove) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove ${term.name}",
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { termPendingDelete = term }
                                        .testTag("admin_remove_term_${term.id}")
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
                FilterChip(
                    selected = useCustomTerm,
                    onClick = { useCustomTerm = true },
                    label = { Text("+ Custom Term") },
                    modifier = Modifier.testTag("admin_add_custom_term_chip")
                )
            }

            // Explicit remove button for currently highlighted preset term for easy touch access
            AnimatedVisibility(
                visible = !useCustomTerm && orderedTerms.size > 1,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                OutlinedButton(
                    onClick = { termPendingDelete = selectedPreset },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_remove_selected_term_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove \"${selectedPreset.name}\"")
                }
            }

            AnimatedVisibility(
                visible = useCustomTerm,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it.take(60) },
                        label = { Text("New Term Name *") },
                        placeholder = { Text("e.g., Term 1 — 2027") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = customStart,
                            onValueChange = { customStart = it.take(10) },
                            label = { Text("Start (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customEnd,
                            onValueChange = { customEnd = it.take(10) },
                            label = { Text("End (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = eligibilityText,
                        onValueChange = { eligibilityText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Min Eligibility % (default 80)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pin a welcome notice for students",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = alsoPostNotice,
                    onCheckedChange = { alsoPostNotice = it }
                )
            }

            Button(
                onClick = {
                    val target = if (useCustomTerm) {
                        val yr = customStart.take(4).toIntOrNull() ?: 2027
                        AcademicTerm(
                            id = "",
                            name = customName.trim(),
                            startDate = customStart.trim(),
                            endDate = customEnd.trim(),
                            academicYear = yr,
                            minEligibilityPercent = (eligibilityText.toIntOrNull() ?: 80).coerceIn(50, 100),
                            active = true,
                            published = true
                        )
                    } else {
                        selectedPreset.copy(active = true, published = true)
                    }
                    onRolloverTerm(target, alsoPostNotice)
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_rollover_confirm_button")
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (useCustomTerm) {
                        "Activate Custom Term"
                    } else if (selectedPreset.id == activeTerm.id) {
                        "Pick a Different Term to Roll Over"
                    } else {
                        "Roll Over to ${selectedPreset.name}"
                    }
                )
            }
        }
    }

    val deletingTerm = termPendingDelete
    if (deletingTerm != null) {
        AlertDialog(
            onDismissRequest = { termPendingDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remove ${deletingTerm.name}?") },
            text = {
                Text(
                    text = "This removes \"${deletingTerm.name}\" (${DateUtils.formatReadableShort(deletingTerm.startDate)} → ${DateUtils.formatReadableShort(deletingTerm.endDate)}) from the school terms list. Students' logged attendance records remain untouched.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTerm(deletingTerm)
                        termPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("admin_confirm_remove_term_button")
                ) {
                    Text("Remove Term")
                }
            },
            dismissButton = {
                TextButton(onClick = { termPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
