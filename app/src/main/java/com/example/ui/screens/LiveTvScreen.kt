package com.example.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.local.CategoryEntity
import com.example.data.local.EpgProgram
import com.example.data.local.MediaItemEntity
import com.example.data.local.PlayerSettings
import com.example.player.PlayerEngineFactory
import com.example.ui.components.DiviSearchField
import com.example.ui.components.DiviTopScreenBar
import com.example.ui.components.ParentalPinDialog
import com.example.ui.components.formatTimeHHmm
import com.example.ui.components.tvFocusRing
import com.example.ui.components.tvFocusable
import com.example.ui.theme.DiviCyan
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
import com.example.ui.viewmodel.VIRTUAL_CAT_ALL
import com.example.ui.viewmodel.VIRTUAL_CAT_FAVORITES
import com.example.ui.viewmodel.VIRTUAL_CAT_RECENT

@Composable
fun LiveTvScreen(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    channels: List<MediaItemEntity>,
    searchQuery: String,
    epgPrograms: List<EpgProgram>,
    settings: PlayerSettings,
    unlockedCategories: Set<String>,
    onBack: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onUnlockCategory: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onLoadEpg: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onOpenFullscreen: (MediaItemEntity, List<MediaItemEntity>) -> Unit
) {
    var previewChannel by remember { mutableStateOf<MediaItemEntity?>(null) }
    var pendingLockedCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    // Keep preview channel synced with first item if none selected yet
    LaunchedEffect(channels) {
        if (previewChannel == null && channels.isNotEmpty()) {
            previewChannel = channels.first()
        } else if (previewChannel != null) {
            val updated = channels.firstOrNull { it.id == previewChannel?.id }
            if (updated != null) previewChannel = updated
        }
    }

    LaunchedEffect(previewChannel?.id) {
        previewChannel?.let { onLoadEpg(it) }
    }

    fun handleCategoryTap(catId: String, categoryObj: CategoryEntity?) {
        if (categoryObj != null &&
            categoryObj.isLocked &&
            settings.parentalPin.isNotBlank() &&
            !unlockedCategories.contains(categoryObj.categoryId)
        ) {
            pendingLockedCategory = categoryObj
        } else {
            onSelectCategory(catId)
        }
    }

    if (pendingLockedCategory != null) {
        val cat = pendingLockedCategory!!
        ParentalPinDialog(
            expectedPin = settings.parentalPin,
            categoryName = cat.categoryName,
            onDismiss = { pendingLockedCategory = null },
            onUnlocked = {
                onUnlockCategory(cat.categoryId)
                onSelectCategory(cat.categoryId)
                pendingLockedCategory = null
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
            title = stringResource(R.string.nav_live_tv),
            subtitle = "${channels.size} channels",
            onBack = onBack
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isThreePane = maxWidth >= 680.dp

            if (isThreePane) {
                // Iconic IBO Player 3-Column TV Layout (Categories | Channels | Preview + EPG)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Column 1: Categories Sidebar
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = ObsidianSurface,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .width(210.dp)
                            .fillMaxHeight()
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                CategoryRailItem(
                                    label = "All Channels",
                                    count = null,
                                    isSelected = selectedCategoryId == VIRTUAL_CAT_ALL,
                                    isLocked = false,
                                    onClick = { handleCategoryTap(VIRTUAL_CAT_ALL, null) }
                                )
                            }
                            item {
                                CategoryRailItem(
                                    label = "Favorites",
                                    count = null,
                                    isSelected = selectedCategoryId == VIRTUAL_CAT_FAVORITES,
                                    isLocked = false,
                                    onClick = { handleCategoryTap(VIRTUAL_CAT_FAVORITES, null) }
                                )
                            }
                            item {
                                CategoryRailItem(
                                    label = "Recently Watched",
                                    count = null,
                                    isSelected = selectedCategoryId == VIRTUAL_CAT_RECENT,
                                    isLocked = false,
                                    onClick = { handleCategoryTap(VIRTUAL_CAT_RECENT, null) }
                                )
                            }
                            items(categories, key = { it.id }) { cat ->
                                CategoryRailItem(
                                    label = cat.categoryName,
                                    count = cat.itemCount,
                                    isSelected = selectedCategoryId == cat.categoryId,
                                    isLocked = cat.isLocked && settings.parentalPin.isNotBlank() && !unlockedCategories.contains(cat.categoryId),
                                    onClick = { handleCategoryTap(cat.categoryId, cat) }
                                )
                            }
                        }
                    }

                    // Column 2: Search + Channels List
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiviSearchField(
                            query = searchQuery,
                            onQueryChange = onSearchChange,
                            placeholder = "Search channels...",
                            testTag = "live_search_input"
                        )
                        ChannelListContent(
                            channels = channels,
                            selectedChannelId = previewChannel?.id,
                            onChannelTap = { ch ->
                                if (previewChannel?.id == ch.id) {
                                    onOpenFullscreen(ch, channels)
                                } else {
                                    previewChannel = ch
                                }
                            },
                            onChannelPlayFullscreen = { ch -> onOpenFullscreen(ch, channels) },
                            onToggleFavorite = onToggleFavorite
                        )
                    }

                    // Column 3: Live Mini-Player Preview + EPG Guide
                    Column(
                        modifier = Modifier
                            .width(310.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LivePreviewPane(
                            channel = previewChannel,
                            epgPrograms = epgPrograms,
                            settings = settings,
                            onFullscreen = {
                                previewChannel?.let { onOpenFullscreen(it, channels) }
                            }
                        )
                    }
                }
            } else {
                // Compact Portrait Phone Layout: Search + Category Chips + Compact Preview + Channel List
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DiviSearchField(
                        query = searchQuery,
                        onQueryChange = onSearchChange,
                        placeholder = "Search channels...",
                        testTag = "live_search_input"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            CategoryChipItem(
                                label = "All",
                                selected = selectedCategoryId == VIRTUAL_CAT_ALL,
                                locked = false,
                                onClick = { handleCategoryTap(VIRTUAL_CAT_ALL, null) }
                            )
                        }
                        item {
                            CategoryChipItem(
                                label = "★ Favorites",
                                selected = selectedCategoryId == VIRTUAL_CAT_FAVORITES,
                                locked = false,
                                onClick = { handleCategoryTap(VIRTUAL_CAT_FAVORITES, null) }
                            )
                        }
                        item {
                            CategoryChipItem(
                                label = "Recent",
                                selected = selectedCategoryId == VIRTUAL_CAT_RECENT,
                                locked = false,
                                onClick = { handleCategoryTap(VIRTUAL_CAT_RECENT, null) }
                            )
                        }
                        items(categories, key = { it.id }) { cat ->
                            val showLock = cat.isLocked && settings.parentalPin.isNotBlank() && !unlockedCategories.contains(cat.categoryId)
                            CategoryChipItem(
                                label = if (cat.itemCount > 0) "${cat.categoryName} (${cat.itemCount})" else cat.categoryName,
                                selected = selectedCategoryId == cat.categoryId,
                                locked = showLock,
                                onClick = { handleCategoryTap(cat.categoryId, cat) }
                            )
                        }
                    }

                    // Embedded IBO Live Preview Card (compact 16:9)
                    if (previewChannel != null) {
                        LivePreviewPane(
                            channel = previewChannel,
                            epgPrograms = epgPrograms,
                            settings = settings,
                            compact = true,
                            onFullscreen = {
                                previewChannel?.let { onOpenFullscreen(it, channels) }
                            }
                        )
                    }

                    // Scrollable Channel List
                    Box(modifier = Modifier.weight(1f)) {
                        ChannelListContent(
                            channels = channels,
                            selectedChannelId = previewChannel?.id,
                            onChannelTap = { ch ->
                                if (previewChannel?.id == ch.id) {
                                    onOpenFullscreen(ch, channels)
                                } else {
                                    previewChannel = ch
                                }
                            },
                            onChannelPlayFullscreen = { ch -> onOpenFullscreen(ch, channels) },
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRailItem(
    label: String,
    count: Int?,
    isSelected: Boolean,
    isLocked: Boolean,
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
        color = if (isSelected) DiviCyan.copy(alpha = 0.18f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, DiviCyan.copy(alpha = 0.7f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isLocked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = StarAmber,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) DiviCyan else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (count != null && count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) DiviCyan else TextMuted
                )
            }
        }
    }
}

@Composable
fun CategoryChipItem(
    label: String,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(38.dp)
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.06f,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) DiviCyan else ObsidianSurface,
        border = BorderStroke(1.dp, if (selected) DiviCyan else ObsidianBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (locked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (selected) ObsidianDeep else StarAmber,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) ObsidianDeep else TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChannelListContent(
    channels: List<MediaItemEntity>,
    selectedChannelId: Long?,
    onChannelTap: (MediaItemEntity) -> Unit,
    onChannelPlayFullscreen: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit
) {
    if (channels.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LiveTv,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = stringResource(R.string.empty_channels_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = stringResource(R.string.empty_channels_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 18.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(channels, key = { it.id }) { channel ->
            val isSelected = channel.id == selectedChannelId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .tvFocusable(
                        shape = RoundedCornerShape(14.dp),
                        focusedScale = 1.02f,
                        onClick = { onChannelTap(channel) }
                    )
                    .testTag("channel_row_${channel.id}"),
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) ObsidianSurfaceElevated else ObsidianSurface,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) DiviCyan else ObsidianBorder.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Channel Number Badge
                    Text(
                        text = channel.channelNumber.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) DiviCyan else TextMuted,
                        modifier = Modifier.width(34.dp)
                    )

                    // Channel Logo Box
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianSurfaceHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        if (channel.logoUrl.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = channel.logoUrl,
                                contentDescription = channel.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                error = {
                                    Icon(
                                        imageVector = Icons.Filled.LiveTv,
                                        contentDescription = null,
                                        tint = DiviCyan.copy(alpha = 0.6f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.LiveTv,
                                contentDescription = null,
                                tint = DiviCyan.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Channel Name + Category
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) DiviCyan else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isSelected) "Tap again for fullscreen • ${channel.categoryName}" else channel.categoryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Quick Fullscreen Play button
                    IconButton(
                        onClick = { onChannelPlayFullscreen(channel) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DiviCyan.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.player_watch_fullscreen),
                            tint = DiviCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Favorite Star Toggle
                    IconButton(
                        onClick = { onToggleFavorite(channel) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (channel.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(R.string.cd_favorite),
                            tint = if (channel.isFavorite) StarAmber else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LivePreviewPane(
    channel: MediaItemEntity?,
    epgPrograms: List<EpgProgram>,
    settings: PlayerSettings,
    compact: Boolean = false,
    onFullscreen: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isBuffering by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    val previewPlayer = remember(settings.userAgent, settings.bufferProfile) {
        if (settings.autoPlayLivePreview) {
            PlayerEngineFactory.createExoPlayer(context, settings)
        } else null
    }

    DisposableEffect(previewPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) hasError = false
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                isBuffering = false
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> previewPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> if (channel != null) previewPlayer?.play()
                else -> Unit
            }
        }
        previewPlayer?.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            previewPlayer?.removeListener(listener)
            previewPlayer?.release()
        }
    }

    LaunchedEffect(channel?.streamUrl, previewPlayer) {
        if (previewPlayer != null && channel != null && channel.streamUrl.isNotBlank()) {
            hasError = false
            isBuffering = true
            previewPlayer.setMediaItem(PlayerEngineFactory.buildMediaItem(channel.streamUrl))
            previewPlayer.prepare()
            previewPlayer.playWhenReady = true
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Video Preview Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (compact) Modifier.height(175.dp) else Modifier.aspectRatio(16f / 9f)
                    )
                    .background(Color.Black)
                    .clickable(enabled = channel != null, onClick = onFullscreen),
                contentAlignment = Alignment.Center
            ) {
                if (previewPlayer != null && channel != null && !hasError) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = previewPlayer
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        update = { view ->
                            if (view.player !== previewPlayer) view.player = previewPlayer
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LiveTv,
                            contentDescription = null,
                            tint = DiviCyan.copy(alpha = 0.6f),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = if (hasError) "Tap to open in Pro Player" else channel?.name ?: "Select a channel",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                if (isBuffering && previewPlayer != null) {
                    CircularProgressIndicator(
                        color = DiviCyan,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // LIVE badge top-left
                if (channel != null) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LiveRed)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Fullscreen button bottom-right
                    Button(
                        onClick = onFullscreen,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianDeep.copy(alpha = 0.82f),
                            contentColor = DiviCyan
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .tvFocusRing(RoundedCornerShape(10.dp), focusedScale = 1.06f)
                            .testTag("live_preview_fullscreen_button")
                    ) {
                        Icon(Icons.Filled.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.player_watch_fullscreen),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // EPG Info Panel below preview
            if (channel != null) {
                val nowProgram = epgPrograms.firstOrNull()
                val nextProgram = epgPrograms.getOrNull(1)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (nowProgram != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = nowProgram.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = DiviCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${formatTimeHHmm(nowProgram.startTimeMs)} - ${formatTimeHHmm(nowProgram.endTimeMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                        LinearProgressIndicator(
                            progress = { nowProgram.progressFraction() },
                            color = DiviCyan,
                            trackColor = ObsidianSurfaceHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        if (nextProgram != null && !compact) {
                            Text(
                                text = "Next: ${formatTimeHHmm(nextProgram.startTimeMs)} ${nextProgram.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.player_epg_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
