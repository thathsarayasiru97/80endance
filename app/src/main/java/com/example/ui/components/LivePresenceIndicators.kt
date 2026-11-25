package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PeopleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.BroadcastNotification
import com.example.data.DateUtils
import com.example.data.UserProfile
import com.example.ui.theme.LocalAttendanceColors
import java.util.Locale

/**
 * Shared height across Home header pills (A/L Journey, Community Account Count, Admin Online Now).
 */
val HOME_HEADER_PILL_HEIGHT: Dp = 34.dp

/**
 * Animated radar-ring green dot representing live community count or active online users.
 */
@Composable
fun PulsingLiveDot(
    color: Color = LocalAttendanceColors.current.present,
    dotSize: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_dot_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "live_ring_scale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.68f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "live_ring_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(dotSize * 2.4f)
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                    alpha = ringAlpha
                }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Public community counter shown on Home: pulsing green dot + rolling account count number only.
 * Matches the exact height & sleek proportions of the A/L Journey pill.
 */
@Composable
fun CommunityAccountCountPill(
    totalAccounts: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAttendanceColors.current
    val count = totalAccounts.coerceAtLeast(1)
    val pillShape = RoundedCornerShape(999.dp)

    Surface(
        color = semantic.presentContainer.copy(alpha = 0.85f),
        shape = pillShape,
        modifier = modifier
            .height(HOME_HEADER_PILL_HEIGHT)
            .clip(pillShape)
            .border(
                width = 1.2.dp,
                color = semantic.present.copy(alpha = 0.36f),
                shape = pillShape
            )
            .then(
                if (onClick != null) {
                    Modifier.bouncyClickable(pressedScale = 0.94f, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = if (onClick != null) {
                    "$count registered accounts. Tap to view current members list."
                } else {
                    "$count registered accounts"
                }
            }
            .testTag("home_community_count_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingLiveDot(color = semantic.present, dotSize = 7.dp)
            Spacer(modifier = Modifier.width(6.dp))
            RollingCounterText(
                count = count,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = semantic.onPresentContainer
            )
        }
    }
}

/**
 * Live online user count pill visible exclusively to the verified admin.
 * Tappable to inspect who is currently online, sized identically to the A/L Journey pill.
 */
@Composable
fun AdminOnlineNowPill(
    onlineCount: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val semantic = LocalAttendanceColors.current
    val pillShape = RoundedCornerShape(999.dp)
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = pillShape,
        modifier = modifier
            .height(HOME_HEADER_PILL_HEIGHT)
            .clip(pillShape)
            .border(
                width = 1.2.dp,
                color = semantic.present.copy(alpha = 0.48f),
                shape = pillShape
            )
            .then(
                if (onClick != null) {
                    Modifier.bouncyClickable(pressedScale = 0.94f, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "$onlineCount online now. Tap to view online users."
            }
            .testTag("admin_online_now_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsingLiveDot(color = semantic.present, dotSize = 7.dp)
            Spacer(modifier = Modifier.width(6.dp))
            RollingCounterText(
                count = onlineCount,
                suffix = " online now",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Admin-only bottom sheet opened by tapping the pulsing "<x> online now" pill on Home.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOnlineUsersSheet(
    onlineUsers: List<UserProfile>,
    onDismiss: () -> Unit
) {
    val semantic = LocalAttendanceColors.current
    val nowMs = remember(onlineUsers) { System.currentTimeMillis() }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("admin_online_users_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PulsingLiveDot(color = semantic.present, dotSize = 8.dp)
                    Column {
                        Text(
                            text = "Online Now (${onlineUsers.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Students currently active on 80endance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (onlineUsers.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PeopleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "No one else is online right now",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active users appear here in real time while the app is open.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    itemsIndexed(
                        items = onlineUsers,
                        key = { _, user -> user.userId.ifBlank { user.email } }
                    ) { idx, user ->
                        StaggeredReveal(index = idx.coerceAtMost(8)) {
                            val seenMs = user.lastSeenAt?.toDate()?.time
                            val activeLabel = when {
                                seenMs == null -> "Active now"
                                nowMs - seenMs < 45_000L -> "Active now"
                                else -> "${((nowMs - seenMs) / 60_000L).coerceAtLeast(1L)}m ago"
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_online_user_${user.userId}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box {
                                        ProfileAvatar(
                                            customAvatarBase64 = user.customAvatarBase64,
                                            googlePhotoUrl = user.photoUrl,
                                            displayName = user.fullName,
                                            size = 42.dp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(semantic.present)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        val nick = user.nickname?.trim()?.takeIf { it.isNotEmpty() }
                                        Text(
                                            text = if (nick != null) "${user.fullName} ($nick)" else user.fullName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = listOf(user.grade, user.className, user.alStream)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" • "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        color = semantic.presentContainer.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = activeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = semantic.onPresentContainer,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bottom sheet opened by tapping the pulsing app downloads / registered accounts pill on Home,
 * showing the full current members list with search, online status, class/stream, and attendance %.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRegisteredMembersSheet(
    members: List<UserProfile>,
    onDismiss: () -> Unit
) {
    val semantic = LocalAttendanceColors.current
    val nowMs = remember(members) { System.currentTimeMillis() }
    var searchQuery by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sortedMembers = remember(members, searchQuery, nowMs) {
        val q = searchQuery.trim().lowercase(Locale.US)
        members
            .filter { user ->
                q.isEmpty() ||
                    user.fullName.lowercase(Locale.US).contains(q) ||
                    user.nickname.orEmpty().lowercase(Locale.US).contains(q) ||
                    user.className.lowercase(Locale.US).contains(q) ||
                    user.alStream.lowercase(Locale.US).contains(q) ||
                    user.grade.lowercase(Locale.US).contains(q)
            }
            .sortedWith(
                compareByDescending<UserProfile> { it.isCurrentlyOnline(nowMs) }
                    .thenByDescending { it.lastSeenAt?.toDate()?.time ?: 0L }
                    .thenBy { it.fullName }
            )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("admin_registered_members_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PulsingLiveDot(color = semantic.present, dotSize = 8.dp)
                    Column {
                        Text(
                            text = "Current Members (${members.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Registered student accounts on 80endance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (members.size > 4) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it.take(60) },
                    placeholder = { Text("Search name, class, or stream...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_members_search_input")
                )
            }

            if (sortedMembers.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PeopleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching members" else "No members found",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    itemsIndexed(
                        items = sortedMembers,
                        key = { _, user -> user.userId.ifBlank { user.email } }
                    ) { idx, user ->
                        StaggeredReveal(index = idx.coerceAtMost(8)) {
                            val isOnline = user.isCurrentlyOnline(nowMs)
                            val seenLabel = DateUtils.formatLastSeen(
                                timestampMillis = user.lastSeenAt?.toDate()?.time,
                                isOnline = isOnline,
                                nowMillis = nowMs
                            )
                            val pct = user.quickOverallPercent
                            val isEligible = pct >= 80.0

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_member_row_${user.userId}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box {
                                        ProfileAvatar(
                                            customAvatarBase64 = user.customAvatarBase64,
                                            googlePhotoUrl = user.photoUrl,
                                            displayName = user.fullName,
                                            size = 42.dp
                                        )
                                        if (isOnline) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                                    .padding(2.dp)
                                                    .clip(CircleShape)
                                                    .background(semantic.present)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        val nick = user.nickname?.trim()?.takeIf { it.isNotEmpty() }
                                        Text(
                                            text = if (nick != null) "${user.fullName} ($nick)" else user.fullName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = listOf(user.grade, user.className, user.alStream)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" • ")
                                                .ifEmpty { "Setup pending" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = seenLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isOnline) semantic.present else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        color = if (isEligible) {
                                            semantic.presentContainer.copy(alpha = 0.85f)
                                        } else {
                                            semantic.absentContainer.copy(alpha = 0.85f)
                                        },
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.0f%%", pct),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isEligible) semantic.onPresentContainer else semantic.onAbsentContainer,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * In-app banner displaying an admin-released custom notification targeted at the student,
 * with a bouncy spring entrance and gentle notification bell chime micro-animation.
 */
@Composable
fun BroadcastAlertBanner(
    broadcast: BroadcastNotification,
    remainingCount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember(broadcast.id) { mutableStateOf(false) }
    LaunchedEffect(broadcast.id) { visible = true }
    val bellRotation = rememberChimeRotation(broadcast.id)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) +
            slideInVertically(
                initialOffsetY = { -32 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) +
            scaleIn(
                initialScale = 0.94f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("home_broadcast_alert_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "New announcement",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = bellRotation }
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NEW ANNOUNCEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        if (remainingCount > 1) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "+${remainingCount - 1} more",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = broadcast.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = broadcast.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.92f)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("home_broadcast_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss notification",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
