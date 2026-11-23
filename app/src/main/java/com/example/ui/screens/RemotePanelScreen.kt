package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlayerSettings
import com.example.data.remote.CloudDevice
import com.example.data.remote.CloudRemotePlaylist
import com.example.ui.components.DiviTopScreenBar
import com.example.ui.components.tvFocusRing
import com.example.ui.components.tvFocusable
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.DiviCyan
import com.example.ui.theme.DiviViolet
import com.example.ui.theme.LiveRed
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.CloudActionFeedback
import com.example.ui.viewmodel.CloudUiState

@Composable
fun RemotePanelScreen(
    settings: PlayerSettings,
    userEmail: String,
    devicesState: CloudUiState<List<CloudDevice>>,
    remotePlaylistsState: CloudUiState<List<CloudRemotePlaylist>>,
    feedback: CloudActionFeedback,
    onBack: () -> Unit,
    onPushXtream: (targetMac: String, name: String, serverUrl: String, username: String, password: String) -> Unit,
    onPushM3u: (targetMac: String, name: String, m3uUrl: String, epgUrl: String) -> Unit,
    onLoadRemotePlaylistNow: (CloudRemotePlaylist) -> Unit,
    onDeleteRemotePlaylist: (String) -> Unit,
    onRemoveDevice: (String) -> Unit,
    onClearFeedback: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf("") }
    var pushTab by remember { mutableIntStateOf(0) } // 0 = Xtream, 1 = M3U
    var selectedTargetMac by remember { mutableStateOf("ALL") }

    var xtName by remember { mutableStateOf("") }
    var xtServer by remember { mutableStateOf("") }
    var xtUser by remember { mutableStateOf("") }
    var xtPass by remember { mutableStateOf("") }

    var m3uName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var m3uEpg by remember { mutableStateOf("") }

    val devices = (devicesState as? CloudUiState.Success)?.data.orEmpty()
    val remotePlaylists = (remotePlaylistsState as? CloudUiState.Success)?.data.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        DiviTopScreenBar(
            title = "Remote Cloud Panel",
            subtitle = userEmail.ifBlank { "IBO-Style Remote Playlist Manager" },
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Feedback Banners
            if (!feedback.errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = LiveRed.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = feedback.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelLarge,
                            color = LiveRed,
                            modifier = Modifier
                                .clickable(onClick = onClearFeedback)
                                .padding(start = 12.dp)
                        )
                    }
                }
            }

            if (!feedback.successMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ActiveEmerald.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, ActiveEmerald.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = feedback.successMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelLarge,
                            color = ActiveEmerald,
                            modifier = Modifier
                                .clickable(onClick = onClearFeedback)
                                .padding(start = 12.dp)
                        )
                    }
                }
            }

            // 1. This Device Identity Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, DiviCyan.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Icon(Icons.Filled.Devices, contentDescription = null, tint = DiviCyan)
                            Text(
                                text = "This Device (Auto-Sync ON)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ActiveEmerald.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "ONLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = ActiveEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("MAC ADDRESS", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = settings.deviceMac.ifBlank { "Generating..." },
                                style = MaterialTheme.typography.titleMedium,
                                color = DiviCyan
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("DEVICE KEY", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            Text(
                                text = settings.deviceKey.ifBlank { "------" },
                                style = MaterialTheme.typography.titleMedium,
                                color = DiviViolet
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = copiedNotice.ifBlank { "Playlists pushed from Web Panel or Phone auto-load here." },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (copiedNotice.isNotBlank()) ActiveEmerald else TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(settings.deviceMac))
                                copiedNotice = "MAC copied!"
                            },
                            modifier = Modifier.tvFocusRing(RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy MAC")
                        }
                    }
                }
            }

            // 2. Cloud Playlists Received / Pushed
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = DiviCyan)
                        Text(
                            text = "Cloud Playlists (${remotePlaylists.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }

                    when (remotePlaylistsState) {
                        is CloudUiState.Loading -> {
                            CircularProgressIndicator(color = DiviCyan, modifier = Modifier.size(28.dp))
                        }
                        is CloudUiState.Error -> {
                            Text(
                                text = remotePlaylistsState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = LiveRed
                            )
                        }
                        is CloudUiState.Success -> {
                            if (remotePlaylists.isEmpty()) {
                                Text(
                                    text = "No remote playlists yet. Send one below or from your Web HTML Control Panel!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            } else {
                                remotePlaylists.forEach { remote ->
                                    RemotePlaylistRowCard(
                                        remote = remote,
                                        isForThisDevice = remote.matchesDevice(settings.deviceMac),
                                        onLoadNow = { onLoadRemotePlaylistNow(remote) },
                                        onDelete = { onDeleteRemotePlaylist(remote.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Push Subscription Remotely to TV (Right from Phone or Web)
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = DiviViolet)
                        Text(
                            text = "Send Subscription to TV",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Target Device:",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTargetMac == "ALL",
                            onClick = { selectedTargetMac = "ALL" },
                            label = { Text("All Devices") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DiviCyan.copy(alpha = 0.2f),
                                selectedLabelColor = DiviCyan,
                                containerColor = ObsidianSurfaceElevated,
                                labelColor = TextSecondary
                            )
                        )
                        devices.take(3).forEach { dev ->
                            FilterChip(
                                selected = selectedTargetMac.equals(dev.deviceMac, ignoreCase = true),
                                onClick = { selectedTargetMac = dev.deviceMac },
                                label = {
                                    Text(
                                        text = "${dev.platform}: ${dev.deviceName.take(12)}",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DiviViolet.copy(alpha = 0.25f),
                                    selectedLabelColor = DiviViolet,
                                    containerColor = ObsidianSurfaceElevated,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RemoteTabPill(
                            label = "Xtream Codes",
                            selected = pushTab == 0,
                            onClick = { pushTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        RemoteTabPill(
                            label = "M3U / M3U8 URL",
                            selected = pushTab == 1,
                            onClick = { pushTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (pushTab == 0) {
                        RemoteField(xtName, { xtName = it }, "Playlist Name", "VIP IPTV", "remote_xt_name")
                        RemoteField(xtServer, { xtServer = it }, "Server URL", "http://server.com:8080", "remote_xt_server")
                        RemoteField(xtUser, { xtUser = it }, "Username", "username", "remote_xt_user")
                        OutlinedTextField(
                            value = xtPass,
                            onValueChange = { xtPass = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            colors = remoteFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("remote_xt_pass")
                        )
                        Button(
                            onClick = {
                                onPushXtream(selectedTargetMac, xtName, xtServer, xtUser, xtPass)
                                xtName = ""; xtServer = ""; xtUser = ""; xtPass = ""
                            },
                            enabled = !feedback.isSubmitting && xtServer.isNotBlank() && xtUser.isNotBlank() && xtPass.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .tvFocusRing(RoundedCornerShape(14.dp))
                                .testTag("remote_push_xtream_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DiviCyan, contentColor = ObsidianDeep)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (feedback.isSubmitting) "Sending..." else "Push Xtream to Device",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        RemoteField(m3uName, { m3uName = it }, "Playlist Name", "My M3U", "remote_m3u_name")
                        RemoteField(m3uUrl, { m3uUrl = it }, "M3U / M3U8 URL", "http://provider.com/list.m3u", "remote_m3u_url")
                        RemoteField(m3uEpg, { m3uEpg = it }, "EPG URL (Optional)", "http://provider.com/epg.xml", "remote_m3u_epg")
                        Button(
                            onClick = {
                                onPushM3u(selectedTargetMac, m3uName, m3uUrl, m3uEpg)
                                m3uName = ""; m3uUrl = ""; m3uEpg = ""
                            },
                            enabled = !feedback.isSubmitting && m3uUrl.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .tvFocusRing(RoundedCornerShape(14.dp))
                                .testTag("remote_push_m3u_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DiviViolet, contentColor = TextPrimary)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (feedback.isSubmitting) "Sending..." else "Push M3U to Device",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            // 4. Paired Devices List
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Linked TVs & Phones (${devices.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    when (devicesState) {
                        is CloudUiState.Loading -> CircularProgressIndicator(color = DiviCyan, modifier = Modifier.size(24.dp))
                        is CloudUiState.Error -> Text(devicesState.message, color = LiveRed, style = MaterialTheme.typography.bodySmall)
                        is CloudUiState.Success -> {
                            if (devices.isEmpty()) {
                                Text("Registering device...", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            } else {
                                devices.forEach { dev ->
                                    val isCurrent = dev.deviceMac.equals(settings.deviceMac, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = ObsidianSurfaceElevated,
                                        border = BorderStroke(1.dp, if (isCurrent) DiviCyan else ObsidianBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(ObsidianDeep),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (dev.platform == "TV") Icons.Filled.Tv else Icons.Filled.PhoneAndroid,
                                                        contentDescription = null,
                                                        tint = if (dev.platform == "TV") DiviCyan else DiviViolet
                                                    )
                                                }
                                                Column {
                                                    Text(
                                                        text = dev.deviceName + if (isCurrent) " (This Device)" else "",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "MAC: ${dev.deviceMac}  •  Key: ${dev.deviceKey}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                            if (!isCurrent) {
                                                IconButton(
                                                    onClick = { onRemoveDevice(dev.id) },
                                                    modifier = Modifier.tvFocusRing(CircleShape)
                                                ) {
                                                    Icon(Icons.Filled.Delete, contentDescription = "Remove device", tint = LiveRed)
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

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RemotePlaylistRowCard(
    remote: CloudRemotePlaylist,
    isForThisDevice: Boolean,
    onLoadNow: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(1.dp, if (isForThisDevice) DiviCyan.copy(alpha = 0.6f) else ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = remote.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${remote.playlistType} • Target: ${remote.targetMac}",
                        style = MaterialTheme.typography.labelSmall,
                        color = DiviCyan
                    )
                    Text(
                        text = if (remote.playlistType == "XTREAM") {
                            "${remote.serverUrl} (${remote.username})"
                        } else {
                            remote.m3uUrl
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.tvFocusRing(CircleShape)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete cloud playlist", tint = LiveRed)
                }
            }

            Button(
                onClick = onLoadNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .tvFocusRing(RoundedCornerShape(10.dp)),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DiviCyan, contentColor = ObsidianDeep)
            ) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Load on This Device", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RemoteTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .tvFocusable(shape = RoundedCornerShape(10.dp), focusedScale = 1.04f, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) DiviCyan.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
        border = BorderStroke(1.dp, if (selected) DiviCyan else ObsidianBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) DiviCyan else TextSecondary
            )
        }
    }
}

@Composable
private fun RemoteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = TextMuted) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = remoteFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
private fun remoteFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = ObsidianSurfaceElevated,
    unfocusedContainerColor = ObsidianSurfaceElevated.copy(alpha = 0.7f),
    focusedBorderColor = DiviCyan,
    unfocusedBorderColor = ObsidianBorder,
    focusedLabelColor = DiviCyan,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = DiviCyan
)
