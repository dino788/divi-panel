package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AspectRatioMode
import com.example.data.local.BufferProfile
import com.example.data.local.CategoryEntity
import com.example.data.local.PlayerSettings
import com.example.data.local.PlaylistEntity
import com.example.data.local.StreamFormat
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
import com.example.ui.theme.StarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: PlayerSettings,
    activePlaylist: PlaylistEntity?,
    categories: List<CategoryEntity>,
    onBack: () -> Unit,
    onSetStreamFormat: (StreamFormat) -> Unit,
    onSetBufferProfile: (BufferProfile) -> Unit,
    onSetAspectRatio: (AspectRatioMode) -> Unit,
    onSetUserAgent: (String) -> Unit,
    onSetAutoPlayPreview: (Boolean) -> Unit,
    onSetParentalPin: (String) -> Unit,
    onToggleCategoryLock: (CategoryEntity) -> Unit,
    onClearHistory: () -> Unit,
    userEmail: String = "",
    onOpenRemotePanel: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showCategoryLocksDialog by remember { mutableStateOf(false) }
    var userAgentDraft by remember(settings.userAgent) { mutableStateOf(settings.userAgent) }
    var feedbackBanner by remember { mutableStateOf("") }

    if (showPinDialog) {
        var newPin by remember { mutableStateOf(settings.parentalPin) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            containerColor = ObsidianSurfaceElevated,
            title = { Text("Set Parental Control PIN", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a 4-digit PIN to protect locked categories. Leave empty to disable parental lock.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) newPin = it },
                        label = { Text("4-Digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetParentalPin(newPin)
                        feedbackBanner = if (newPin.isBlank()) "Parental PIN cleared" else "Parental PIN saved"
                        showPinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiviCyan, contentColor = ObsidianDeep)
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        )
    }

    if (showCategoryLocksDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryLocksDialog = false },
            containerColor = ObsidianSurfaceElevated,
            title = { Text("Lock Categories", color = TextPrimary) },
            text = {
                if (categories.isEmpty()) {
                    Text("No categories loaded in active playlist.", color = TextSecondary)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(340.dp)
                    ) {
                        items(categories, key = { it.id }) { cat ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleCategoryLock(cat) },
                                shape = RoundedCornerShape(10.dp),
                                color = ObsidianSurface,
                                border = BorderStroke(1.dp, if (cat.isLocked) StarAmber else ObsidianBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cat.categoryName,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = cat.mediaType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                    }
                                    Icon(
                                        imageVector = if (cat.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                        contentDescription = null,
                                        tint = if (cat.isLocked) StarAmber else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryLocksDialog = false }) {
                    Text("Done", color = DiviCyan)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        DiviTopScreenBar(
            title = stringResource(R.string.nav_settings),
            subtitle = "divi Pro Player Configuration",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (feedbackBanner.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ActiveEmerald.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, ActiveEmerald.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackBanner,
                        style = MaterialTheme.typography.labelLarge,
                        color = ActiveEmerald,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. Player Engine Section
            SettingsSectionCard(
                title = stringResource(R.string.settings_player_section),
                icon = Icons.Filled.Tune
            ) {
                Text(
                    text = stringResource(R.string.settings_stream_format),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StreamFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = settings.streamFormat == fmt,
                            onClick = { onSetStreamFormat(fmt) },
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

                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_buffer_profile),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BufferProfile.entries.forEach { profile ->
                        val selected = settings.bufferProfile == profile
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusable(
                                    shape = RoundedCornerShape(12.dp),
                                    focusedScale = 1.02f,
                                    onClick = { onSetBufferProfile(profile) }
                                )
                                .testTag("buffer_profile_${profile.name}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) DiviCyan.copy(alpha = 0.15f) else ObsidianSurfaceElevated,
                            border = BorderStroke(1.dp, if (selected) DiviCyan else ObsidianBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = profile.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) DiviCyan else TextPrimary
                                )
                                Text(
                                    text = profile.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_aspect_ratio),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AspectRatioMode.entries.forEach { mode ->
                        val selected = settings.defaultAspectRatio == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .tvFocusable(
                                    shape = RoundedCornerShape(10.dp),
                                    focusedScale = 1.05f,
                                    focusColor = DiviViolet,
                                    onClick = { onSetAspectRatio(mode) }
                                ),
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) DiviViolet else ObsidianSurfaceElevated,
                            border = BorderStroke(1.dp, if (selected) DiviViolet else ObsidianBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Play Live TV Preview",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Play highlighted channel inline inside Live TV browser",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Switch(
                        checked = settings.autoPlayLivePreview,
                        onCheckedChange = onSetAutoPlayPreview,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianDeep,
                            checkedTrackColor = DiviCyan
                        )
                    )
                }

                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = userAgentDraft,
                    onValueChange = { userAgentDraft = it },
                    label = { Text(stringResource(R.string.settings_user_agent)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        onSetUserAgent(userAgentDraft)
                        feedbackBanner = "User-Agent updated"
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DiviCyan),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Apply User-Agent", color = DiviCyan)
                }
            }

            // 2. Parental Control Section
            SettingsSectionCard(
                title = stringResource(R.string.settings_parental_section),
                icon = Icons.Filled.Security
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_parental_pin),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = if (settings.parentalPin.isBlank()) "Not configured" else "PIN active (••••)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.parentalPin.isBlank()) TextMuted else ActiveEmerald
                        )
                    }
                    OutlinedButton(
                        onClick = { showPinDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DiviCyan)
                    ) {
                        Text(if (settings.parentalPin.isBlank()) "Set PIN" else "Change PIN", color = DiviCyan)
                    }
                }

                if (categories.isNotEmpty()) {
                    val lockedCount = categories.count { it.isLocked }
                    OutlinedButton(
                        onClick = { showCategoryLocksDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = StarAmber, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Manage Locked Categories ($lockedCount locked)", color = TextPrimary)
                    }
                }
            }

            // 3. Storage & Account Section
            SettingsSectionCard(
                title = stringResource(R.string.settings_account_section),
                icon = Icons.Filled.Memory
            ) {
                SettingsInfoRow("App", "divi Pro IPTV Player v1.0")
                if (userEmail.isNotBlank()) {
                    SettingsInfoRow("Cloud Account", userEmail)
                }
                SettingsInfoRow("Device MAC", settings.deviceMac.ifBlank { "—" })
                SettingsInfoRow("Device Key", settings.deviceKey.ifBlank { "—" })
                if (activePlaylist != null) {
                    SettingsInfoRow("Active Playlist", activePlaylist.name)
                    if (activePlaylist.expiryDate.isNotBlank()) {
                        SettingsInfoRow("Subscription Expiry", activePlaylist.expiryDate)
                    }
                    if (activePlaylist.maxConnections.isNotBlank()) {
                        SettingsInfoRow(
                            "Connections",
                            "${activePlaylist.activeConnections.ifBlank { "0" }} / ${activePlaylist.maxConnections}"
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onOpenRemotePanel,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DiviCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusRing(RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.CloudSync, contentDescription = null, tint = DiviCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Remote Cloud Panel", color = DiviCyan)
                }

                OutlinedButton(
                    onClick = {
                        onClearHistory()
                        feedbackBanner = "Watch history cleared"
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusRing(RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = LiveRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_clear_history), color = LiveRed)
                }

                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .tvFocusRing(RoundedCornerShape(12.dp))
                        .testTag("settings_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out of Google Account", color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
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
                Icon(icon, contentDescription = null, tint = DiviCyan, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
            content()
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.labelLarge, color = TextPrimary)
    }
}
