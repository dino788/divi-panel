package com.example.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.local.MediaItemEntity
import com.example.player.StreamTechInfo
import com.example.player.TrackOption
import com.example.ui.components.DiviSearchField
import com.example.ui.components.tvFocusable
import com.example.ui.theme.DiviCyan
import com.example.ui.theme.DiviViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ChannelZapDrawer(
    channels: List<MediaItemEntity>,
    currentChannelId: Long?,
    onSelectChannel: (MediaItemEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(channels, filter) {
        if (filter.isBlank()) channels
        else channels.filter { it.name.contains(filter.trim(), ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .clickable(enabled = false) {},
            color = ObsidianDeep.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.player_channels),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "${filtered.size} channels",
                            style = MaterialTheme.typography.labelSmall,
                            color = DiviCyan
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = TextSecondary
                        )
                    }
                }

                DiviSearchField(
                    query = filter,
                    onQueryChange = { filter = it },
                    placeholder = "Zap channel..."
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filtered, key = { it.id }) { ch ->
                        val isPlaying = ch.id == currentChannelId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(
                                    shape = RoundedCornerShape(12.dp),
                                    focusedScale = 1.03f,
                                    onClick = { onSelectChannel(ch) }
                                ),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isPlaying) DiviCyan.copy(alpha = 0.2f) else ObsidianSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isPlaying) DiviCyan else ObsidianBorder.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = ch.channelNumber.toString().padStart(2, '0'),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPlaying) DiviCyan else TextMuted,
                                    modifier = Modifier.width(28.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ObsidianSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ch.logoUrl.isNotBlank()) {
                                        SubcomposeAsyncImage(
                                            model = ch.logoUrl,
                                            contentDescription = ch.name,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.LiveTv,
                                            contentDescription = null,
                                            tint = DiviCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = ch.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isPlaying) DiviCyan else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackSelectionDialog(
    title: String,
    tracks: List<TrackOption>,
    includeOffOption: Boolean,
    isCurrentlyOff: Boolean,
    onSelect: (TrackOption?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            if (tracks.isEmpty()) {
                Text(
                    text = "No additional tracks available in this stream.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (includeOffOption) {
                        item {
                            TrackOptionRow(
                                label = "Off (Disabled)",
                                selected = isCurrentlyOff,
                                onClick = {
                                    onSelect(null)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    items(tracks, key = { it.id }) { track ->
                        TrackOptionRow(
                            label = track.label,
                            selected = track.isSelected && !isCurrentlyOff,
                            onClick = {
                                onSelect(track)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = DiviCyan)
            }
        }
    )
}

@Composable
private fun TrackOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.03f,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) DiviCyan.copy(alpha = 0.18f) else ObsidianSurface,
        border = BorderStroke(1.dp, if (selected) DiviCyan else ObsidianBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) DiviCyan else TextPrimary
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = DiviCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StreamInfoDialog(
    title: String,
    streamUrl: String,
    techInfo: StreamTechInfo,
    bufferPercent: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        title = {
            Text(
                text = "Stream Diagnostics",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoMetricRow("Title", title)
                InfoMetricRow("Video", techInfo.resolution.ifBlank { "Detecting..." })
                InfoMetricRow("Video Codec", techInfo.videoCodec.ifBlank { "Auto" })
                InfoMetricRow(
                    "Audio",
                    listOf(techInfo.audioCodec, techInfo.audioChannels)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                        .ifBlank { "Auto" }
                )
                if (techInfo.frameRate.isNotBlank()) {
                    InfoMetricRow("Frame Rate", techInfo.frameRate)
                }
                if (techInfo.bitrateKbps > 0) {
                    InfoMetricRow("Bitrate", "${techInfo.bitrateKbps} kbps")
                }
                InfoMetricRow("Buffer", "$bufferPercent%")
                InfoMetricRow(
                    "Protocol",
                    when {
                        streamUrl.contains(".m3u8", ignoreCase = true) -> "HLS (M3U8)"
                        streamUrl.contains(".mpd", ignoreCase = true) -> "MPEG-DASH"
                        streamUrl.endsWith(".ts", ignoreCase = true) -> "MPEG-TS"
                        streamUrl.endsWith(".mkv", ignoreCase = true) -> "Matroska (MKV)"
                        else -> "HTTP / Media3"
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = DiviCyan)
            }
        }
    )
}

@Composable
private fun InfoMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GestureHudIndicator(
    isBrightness: Boolean,
    level: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = ObsidianDeep.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, if (isBrightness) DiviCyan.copy(alpha = 0.6f) else DiviViolet.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isBrightness) DiviCyan.copy(alpha = 0.2f) else DiviViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isBrightness) Icons.Filled.Brightness6 else Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = if (isBrightness) DiviCyan else DiviViolet,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${(level.coerceIn(0f, 1f) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                LinearProgressIndicator(
                    progress = { level.coerceIn(0f, 1f) },
                    color = if (isBrightness) DiviCyan else DiviViolet,
                    trackColor = ObsidianSurfaceHigh,
                    modifier = Modifier
                        .width(110.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
