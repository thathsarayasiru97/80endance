package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

object AvatarImageProcessor {
    private const val TARGET_SIZE_PX = 256
    private const val MAX_BASE64_LENGTH = 95_000

    suspend fun encodeUriToBase64(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            // Pass 1: Read dimensions only
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            var sampleSize = 1
            while (
                (bounds.outWidth / sampleSize) > TARGET_SIZE_PX * 2 &&
                (bounds.outHeight / sampleSize) > TARGET_SIZE_PX * 2
            ) {
                sampleSize *= 2
            }

            // Pass 2: Decode downsampled bitmap
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext null

            // Center-crop to square and scale cleanly to 256x256
            val side = min(decoded.width, decoded.height)
            val cropX = (decoded.width - side) / 2
            val cropY = (decoded.height - side) / 2
            val cropped = Bitmap.createBitmap(decoded, cropX, cropY, side, side)
            if (cropped !== decoded) decoded.recycle()

            val scaled = if (cropped.width != TARGET_SIZE_PX) {
                Bitmap.createScaledBitmap(cropped, TARGET_SIZE_PX, TARGET_SIZE_PX, true).also {
                    if (it !== cropped) cropped.recycle()
                }
            } else {
                cropped
            }

            // Compress progressively until well under Firestore field limit
            var quality = 80
            var encoded: String? = null
            while (quality >= 40) {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val candidate = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                if (candidate.length <= MAX_BASE64_LENGTH) {
                    encoded = candidate
                    break
                }
                quality -= 10
            }
            scaled.recycle()
            encoded
        }.getOrNull()
    }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
}

@Composable
fun ProfileAvatar(
    customAvatarBase64: String?,
    googlePhotoUrl: String?,
    displayName: String,
    size: Dp = 46.dp,
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customBitmap = remember(customAvatarBase64) {
        AvatarImageProcessor.decodeBase64ToImageBitmap(customAvatarBase64)
    }
    val initials = remember(displayName) {
        displayName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { "S" }
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Fallback monogram always visible underneath while remote image loads
            Text(
                text = initials,
                style = if (size >= 64.dp) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            when {
                customBitmap != null -> {
                    Image(
                        bitmap = customBitmap,
                        contentDescription = "$displayName profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                !googlePhotoUrl.isNullOrBlank() -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(googlePhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "$displayName Google profile photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showEditBadge) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Change profile photo",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfilePhotoDialog(
    displayName: String,
    customAvatarBase64: String?,
    googlePhotoUrl: String?,
    onSaveCustomAvatar: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                errorMessage = null
                val encoded = AvatarImageProcessor.encodeUriToBase64(context, uri)
                isProcessing = false
                if (encoded != null) {
                    onSaveCustomAvatar(encoded)
                    onDismiss()
                } else {
                    errorMessage = "Could not process that image. Please try another photo."
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                .widthIn(max = 420.dp)
                .padding(horizontal = 24.dp)
                .testTag("profile_photo_dialog")
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
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Profile Picture",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close profile photo dialog")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(contentAlignment = Alignment.Center) {
                        ProfileAvatar(
                            customAvatarBase64 = customAvatarBase64,
                            googlePhotoUrl = googlePhotoUrl,
                            displayName = displayName,
                            size = 112.dp
                        )
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (!customAvatarBase64.isNullOrBlank()) {
                            "Custom photo synced across your devices"
                        } else if (!googlePhotoUrl.isNullOrBlank()) {
                            "Using your Google account photo by default"
                        } else {
                            "Choose a photo to personalize your profile"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    LiquidGlassButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        backdrop = dialogBackdrop,
                        enabled = !isProcessing,
                        tint = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.primary,
                        minHeight = 50.dp,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("choose_profile_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose Custom Photo")
                    }

                    if (!customAvatarBase64.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                onSaveCustomAvatar(null)
                                onDismiss()
                            },
                            enabled = !isProcessing,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("revert_google_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Google Account Photo")
                        }
                    }
                }
            }
        }
    }
}
