package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.ui.theme.DiviCyan
import com.example.ui.theme.DiviViolet
import com.example.ui.theme.LiveRed
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.StarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiviBrandLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 34.dp else 40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(DiviCyan, DiviViolet)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.cd_divi_logo),
                tint = ObsidianDeep,
                modifier = Modifier.size(if (compact) 22.dp else 26.dp)
            )
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "divi",
                    style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(DiviCyan)
                )
            }
            if (!compact) {
                Text(
                    text = "PRO PLAYER",
                    style = MaterialTheme.typography.labelSmall,
                    color = DiviCyan,
                    letterSpacing = 1.6.sp
                )
            }
        }
    }
}

@Composable
fun LiveClockText(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(15_000L)
        }
    }
    val timeStr = remember(now) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    }
    val dateStr = remember(now) {
        SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(now))
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = timeStr,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Text(
            text = dateStr,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
fun DiviTopScreenBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .tvFocusRing(CircleShape)
                .clip(CircleShape)
                .background(ObsidianSurfaceElevated.copy(alpha = 0.85f))
                .testTag("top_bar_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = TextPrimary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = DiviCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        actions()
    }
}

@Composable
fun DiviSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    testTag: String = "search_input"
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.cd_search),
                tint = DiviCyan
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = TextSecondary
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ObsidianSurfaceElevated,
            unfocusedContainerColor = ObsidianSurface,
            focusedBorderColor = DiviCyan,
            unfocusedBorderColor = ObsidianBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = DiviCyan
        )
    )
}

@Composable
fun MediaPosterCard(
    item: MediaItemEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSeries = item.mediaType == MediaType.SERIES.name
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.05f,
                onClick = onClick
            )
            .testTag("poster_card_${item.id}"),
        color = ObsidianSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.7f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(ObsidianSurfaceElevated)
            ) {
                if (item.logoUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = item.logoUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = { PosterPlaceholder(isSeries = isSeries, title = item.name) },
                        loading = { PosterPlaceholder(isSeries = isSeries, title = item.name) }
                    )
                } else {
                    PosterPlaceholder(isSeries = isSeries, title = item.name)
                }

                // Bottom gradient for badge legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, ObsidianDeep.copy(alpha = 0.92f))
                            )
                        )
                )

                // Rating badge top-left
                if (item.rating.isNotBlank() && item.rating != "0" && item.rating != "0.0") {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianDeep.copy(alpha = 0.82f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = StarAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = item.rating.take(4),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )
                    }
                }

                // Favorite button top-right
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ObsidianDeep.copy(alpha = 0.72f))
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = stringResource(R.string.cd_favorite),
                        tint = if (item.isFavorite) StarAmber else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Year or progress bar at bottom of poster
                if (item.year.isNotBlank()) {
                    Text(
                        text = item.year,
                        style = MaterialTheme.typography.labelSmall,
                        color = DiviCyan,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    )
                }
                if (item.durationMs > 0L && item.resumePositionMs > 0L) {
                    val progress = (item.resumePositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0.05f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter)
                            .background(ObsidianSurfaceHigh)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(3.dp)
                                .background(DiviCyan)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PosterPlaceholder(isSeries: Boolean, title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        ObsidianSurfaceElevated,
                        if (isSeries) Color(0xFF28174A) else Color(0xFF10243C)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = if (isSeries) Icons.Filled.Tv else Icons.Filled.Movie,
                contentDescription = null,
                tint = if (isSeries) DiviViolet.copy(alpha = 0.7f) else DiviCyan.copy(alpha = 0.7f),
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title.take(18),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ParentalPinDialog(
    expectedPin: String,
    categoryName: String,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        icon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = DiviCyan,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Locked Category",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your Parental PIN to access \"$categoryName\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 6) {
                            enteredPin = it
                            isError = false
                        }
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    isError = isError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = "Incorrect PIN. Please try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LiveRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredPin == expectedPin) {
                        onUnlocked()
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DiviCyan, contentColor = ObsidianDeep)
            ) {
                Text(stringResource(R.string.action_unlock))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = TextSecondary)
            }
        }
    )
}

@Composable
fun SyncProgressOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDeep.copy(alpha = 0.84f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = ObsidianSurfaceElevated,
            border = BorderStroke(1.dp, DiviCyan.copy(alpha = 0.5f)),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = DiviCyan,
                    trackColor = ObsidianSurfaceHigh,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = message.ifBlank { "Syncing playlist..." },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Please keep divi open while loading streams",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun formatTimeHHmm(ms: Long): String {
    if (ms <= 0L) return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))
}
