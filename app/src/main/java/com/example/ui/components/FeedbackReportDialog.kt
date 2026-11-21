package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FeedbackType
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun FeedbackReportDialog(
    initialType: FeedbackType = FeedbackType.SUGGESTION,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (type: FeedbackType, title: String, message: String) -> Unit
) {
    var selectedType by remember(initialType) { mutableStateOf(initialType) }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    val canSubmit = title.trim().isNotEmpty() && message.trim().isNotEmpty() && !isSubmitting

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dialogBackdrop = rememberLayerBackdrop()
        val surfaceBase = MaterialTheme.colorScheme.surfaceContainerLowest
        val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        val secondaryGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = surfaceBase,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(horizontal = 20.dp)
                .testTag("feedback_dialog")
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .layerBackdrop(dialogBackdrop)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(surfaceBase, primaryGlow, secondaryGlow, surfaceBase)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (selectedType == FeedbackType.BUG) Icons.Rounded.BugReport else Icons.Rounded.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Help Shape 80endance",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { if (!isSubmitting) onDismiss() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close feedback dialog")
                        }
                    }

                    Text(
                        text = "Spotted a glitch or thought of something awesome? Every report and suggestion goes straight to the developer and helps build the next update!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedType == FeedbackType.BUG,
                            onClick = { selectedType = FeedbackType.BUG },
                            leadingIcon = {
                                Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Bug Report") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("feedback_type_bug")
                        )
                        FilterChip(
                            selected = selectedType == FeedbackType.SUGGESTION,
                            onClick = { selectedType = FeedbackType.SUGGESTION },
                            leadingIcon = {
                                Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Feature Idea") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("feedback_type_suggestion")
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(120) },
                        label = { Text("Short Summary *") },
                        placeholder = {
                            Text(
                                if (selectedType == FeedbackType.BUG) "e.g., Calendar date didn't update after tap"
                                else "e.g., Add timetable view for A/L subjects"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_title_input")
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it.take(1500) },
                        label = { Text("Details *") },
                        placeholder = {
                            Text(
                                if (selectedType == FeedbackType.BUG) "What happened and what did you expect to see?"
                                else "Tell us how this idea would help your A/L routine!"
                            )
                        },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_message_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LiquidGlassButton(
                        onClick = {
                            if (canSubmit) {
                                onSubmit(selectedType, title.trim(), message.trim())
                            }
                        },
                        backdrop = dialogBackdrop,
                        enabled = canSubmit,
                        tint = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.primary,
                        minHeight = 50.dp,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("feedback_submit_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending…")
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedType == FeedbackType.BUG) "Send Bug Report" else "Send Suggestion")
                        }
                    }
                }
            }
        }
    }
}
