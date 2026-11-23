package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.PlayerSettings
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistType
import com.example.data.local.StreamFormat
import com.example.ui.components.DiviTopScreenBar
import com.example.ui.components.tvFocusRing
import com.example.ui.components.tvFocusable
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.DiviCyan
import com.example.ui.theme.DiviMagenta
import com.example.ui.theme.DiviViolet
import com.example.ui.theme.LiveRed
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlaylistScreen(
    playlists: List<PlaylistEntity>,
    settings: PlayerSettings,
    initialTab: Int = 0,
    errorMessage: String?,
    successMessage: String?,
    onBack: () -> Unit,
    onAddXtream: (name: String, serverUrl: String, username: String, password: String, format: StreamFormat) -> Unit,
    onAddM3uUrl: (name: String, m3uUrl: String, epgUrl: String) -> Unit,
    onImportLocalM3u: (name: String, uri: Uri) -> Unit,
    onActivatePlaylist: (Long) -> Unit,
    onSyncPlaylist: (PlaylistEntity) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onClearMessages: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val clipboard = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf("") }

    // Xtream form state
    var xtName by remember { mutableStateOf("") }
    var xtServer by remember { mutableStateOf("") }
    var xtUser by remember { mutableStateOf("") }
    var xtPass by remember { mutableStateOf("") }
    var xtShowPassword by remember { mutableStateOf(false) }
    var xtFormat by remember { mutableStateOf(settings.streamFormat) }

    // M3U URL form state
    var m3uName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var m3uEpgUrl by remember { mutableStateOf("") }

    // Local File form state
    var localName by remember { mutableStateOf("") }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportLocalM3u(localName.ifBlank { "Local M3U Playlist" }, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        DiviTopScreenBar(
            title = stringResource(R.string.playlists_title),
            subtitle = "Xtream Codes API • M3U / M3U8",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Feedback banners
            if (!errorMessage.isNullOrBlank()) {
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
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelLarge,
                            color = LiveRed,
                            modifier = Modifier
                                .clickable(onClick = onClearMessages)
                                .padding(start = 12.dp)
                        )
                    }
                }
            }

            if (!successMessage.isNullOrBlank()) {
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
                            text = successMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "OK",
                            style = MaterialTheme.typography.labelLarge,
                            color = ActiveEmerald,
                            modifier = Modifier
                                .clickable(onClick = onClearMessages)
                                .padding(start = 12.dp)
                        )
                    }
                }
            }

            // Add Playlist Card (3 tabs: Xtream Codes | M3U URL | Local File)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.playlist_add_new),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlaylistModeTab(
                            label = stringResource(R.string.playlist_tab_xtream),
                            selected = selectedTab == 0,
                            testTag = "tab_xtream",
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        PlaylistModeTab(
                            label = stringResource(R.string.playlist_tab_m3u_url),
                            selected = selectedTab == 1,
                            testTag = "tab_m3u_url",
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        PlaylistModeTab(
                            label = stringResource(R.string.playlist_tab_m3u_file),
                            selected = selectedTab == 2,
                            testTag = "tab_m3u_file",
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    when (selectedTab) {
                        0 -> {
                            DiviTextField(
                                value = xtName,
                                onValueChange = { xtName = it },
                                label = stringResource(R.string.label_playlist_name),
                                placeholder = "My IPTV Provider",
                                testTag = "xtream_name_input"
                            )
                            DiviTextField(
                                value = xtServer,
                                onValueChange = { xtServer = it },
                                label = stringResource(R.string.label_server_url),
                                placeholder = "http://server.domain:8080",
                                testTag = "xtream_server_input"
                            )
                            DiviTextField(
                                value = xtUser,
                                onValueChange = { xtUser = it },
                                label = stringResource(R.string.label_username),
                                placeholder = "username",
                                testTag = "xtream_user_input"
                            )
                            OutlinedTextField(
                                value = xtPass,
                                onValueChange = { xtPass = it },
                                label = { Text(stringResource(R.string.label_password)) },
                                singleLine = true,
                                visualTransformation = if (xtShowPassword) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { xtShowPassword = !xtShowPassword }) {
                                        Icon(
                                            imageVector = if (xtShowPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "Toggle password",
                                            tint = TextSecondary
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = diviFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("xtream_password_input")
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Live Format:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary
                                )
                                StreamFormat.entries.forEach { fmt ->
                                    FilterChip(
                                        selected = xtFormat == fmt,
                                        onClick = { xtFormat = fmt },
                                        label = { Text(fmt.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DiviCyan.copy(alpha = 0.2f),
                                            selectedLabelColor = DiviCyan,
                                            containerColor = ObsidianSurfaceElevated,
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onAddXtream(xtName, xtServer, xtUser, xtPass, xtFormat)
                                },
                                enabled = xtServer.isNotBlank() && xtUser.isNotBlank() && xtPass.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("xtream_connect_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DiviCyan,
                                    contentColor = ObsidianDeep
                                )
                            ) {
                                Icon(Icons.Filled.Router, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_connect_playlist),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        1 -> {
                            DiviTextField(
                                value = m3uName,
                                onValueChange = { m3uName = it },
                                label = stringResource(R.string.label_playlist_name),
                                placeholder = "My M3U Playlist",
                                testTag = "m3u_name_input"
                            )
                            DiviTextField(
                                value = m3uUrl,
                                onValueChange = { m3uUrl = it },
                                label = stringResource(R.string.label_m3u_url),
                                placeholder = "http://provider.com/get.php?type=m3u_plus",
                                testTag = "m3u_url_input"
                            )
                            DiviTextField(
                                value = m3uEpgUrl,
                                onValueChange = { m3uEpgUrl = it },
                                label = stringResource(R.string.label_epg_url),
                                placeholder = "http://provider.com/xmltv.php",
                                testTag = "m3u_epg_input"
                            )

                            Button(
                                onClick = {
                                    onAddM3uUrl(m3uName, m3uUrl, m3uEpgUrl)
                                },
                                enabled = m3uUrl.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("m3u_connect_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DiviViolet,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(Icons.Filled.Link, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_connect_playlist),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        2 -> {
                            DiviTextField(
                                value = localName,
                                onValueChange = { localName = it },
                                label = stringResource(R.string.label_playlist_name),
                                placeholder = "Imported Channels",
                                testTag = "local_m3u_name_input"
                            )
                            Text(
                                text = "Select any .m3u or .m3u8 file from your device storage. divi automatically sorts Live TV, Movies, and Series.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Button(
                                onClick = {
                                    filePicker.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("pick_m3u_file_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DiviMagenta,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_pick_m3u_file),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Saved Playlists List
            if (playlists.isNotEmpty()) {
                Text(
                    text = "Saved Playlists (${playlists.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                playlists.forEach { playlist ->
                    SavedPlaylistCard(
                        playlist = playlist,
                        onActivate = { onActivatePlaylist(playlist.id) },
                        onSync = { onSyncPlaylist(playlist) },
                        onDelete = { onDeletePlaylist(playlist.id) }
                    )
                }
            }

            // Authentic IBO Player Device Identity Card (MAC + Device Key)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = DiviCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.device_info_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.device_mac_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = settings.deviceMac.ifBlank { "Generating..." },
                                style = MaterialTheme.typography.titleMedium,
                                color = DiviCyan
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(settings.deviceMac))
                                copiedNotice = "MAC copied"
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_copy))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.device_key_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = settings.deviceKey.ifBlank { "------" },
                                style = MaterialTheme.typography.titleMedium,
                                color = DiviViolet
                            )
                        }
                        if (copiedNotice.isNotEmpty()) {
                            Text(
                                text = copiedNotice,
                                style = MaterialTheme.typography.labelSmall,
                                color = ActiveEmerald
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlaylistModeTab(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.05f,
                onClick = onClick
            )
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) DiviCyan.copy(alpha = 0.18f) else ObsidianSurfaceElevated,
        border = BorderStroke(1.dp, if (selected) DiviCyan else ObsidianBorder)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) DiviCyan else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SavedPlaylistCard(
    playlist: PlaylistEntity,
    onActivate: () -> Unit,
    onSync: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(18.dp),
                focusedScale = 1.02f,
                onClick = onActivate
            ),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(
            width = if (playlist.isActive) 1.5.dp else 1.dp,
            color = if (playlist.isActive) DiviCyan else ObsidianBorder
        )
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (playlist.isActive) DiviCyan.copy(alpha = 0.18f)
                                else ObsidianSurfaceElevated
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (playlist.isActive) DiviCyan else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (playlist.isActive) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ActiveEmerald.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = stringResource(R.string.playlist_active_badge),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ActiveEmerald,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = when (playlist.type) {
                                PlaylistType.XTREAM.name -> "${playlist.serverUrl} • ${playlist.username}"
                                PlaylistType.M3U_URL.name -> playlist.m3uUrl
                                else -> "Imported Local M3U File"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${playlist.liveCount} Live  •  ${playlist.movieCount} Movies  •  ${playlist.seriesCount} Series",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (playlist.type != PlaylistType.M3U_FILE.name) {
                        IconButton(onClick = onSync, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.action_refresh),
                                tint = DiviCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = LiveRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiviTextField(
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
        shape = RoundedCornerShape(14.dp),
        colors = diviFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
private fun diviFieldColors() = OutlinedTextFieldDefaults.colors(
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
