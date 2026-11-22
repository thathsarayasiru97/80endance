package com.example.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceCalculator
import com.example.data.AttendanceStats
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusOrange
import com.example.ui.theme.StatusRed
import java.util.Locale

/**
 * Class Register Verification Tool:
 * Enter the totals your class teacher reads out from the official register (e.g., 64 out of 78)
 * and immediately spot any ±day mismatch against your app records.
 */
@Composable
fun RegisterVerificationDialog(
    termName: String,
    stats: AttendanceStats,
    onDismiss: () -> Unit
) {
    var officialAttendedInput by remember { mutableStateOf("") }
    var officialTotalInput by remember { mutableStateOf("") }

    val parsedAtt = officialAttendedInput.trim().toIntOrNull()
    val parsedTot = officialTotalInput.trim().toIntOrNull()
    val result = remember(stats, parsedAtt, parsedTot) {
        if (parsedAtt != null && parsedTot != null && parsedTot > 0 && parsedAtt in 0..parsedTot) {
            AttendanceCalculator.verifyAgainstClassRegister(stats, parsedAtt, parsedTot)
        } else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.FactCheck, contentDescription = null) },
        title = {
            Text(
                text = "Class Register Verification",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("register_verification_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Enter the numbers read from your official class register for $termName to check for any ±day mismatch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = officialAttendedInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) officialAttendedInput = it },
                        label = { Text("Attended") },
                        placeholder = { Text("e.g. 64") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("register_attended_input")
                    )
                    OutlinedTextField(
                        value = officialTotalInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) officialTotalInput = it },
                        label = { Text("School Days") },
                        placeholder = { Text("e.g. 78") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("register_total_input")
                    )
                }

                // Current app record reference
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your 80endance Record",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "${stats.effectiveAttended} / ${stats.totalCountedDays} (${String.format(Locale.US, "%.1f%%", stats.percentage)})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (parsedAtt != null && parsedTot != null && parsedAtt > parsedTot) {
                    Text(
                        text = "Attended days cannot be greater than total school days.",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusRed
                    )
                }

                if (result != null) {
                    val accent = if (result.isExactMatch) StatusGreen else StatusOrange
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accent.copy(alpha = 0.11f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_verification_result")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (result.isExactMatch) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (result.isExactMatch) "100% Register Match" else "Mismatch Detected",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = accent
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Official Register", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${result.officialAttended}/${result.officialTotal} (${String.format(Locale.US, "%.1f%%", result.officialPercent)})",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Difference (App − Reg)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val attSign = if (result.attendedDiff >= 0) "+${result.attendedDiff}" else "${result.attendedDiff}"
                                    val totSign = if (result.totalDiff >= 0) "+${result.totalDiff}" else "${result.totalDiff}"
                                    Text(
                                        text = "${attSign}d att · ${totSign}d total",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = accent
                                    )
                                }
                            }

                            HorizontalDivider(color = accent.copy(alpha = 0.25f))
                            Text(
                                text = result.summaryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}
