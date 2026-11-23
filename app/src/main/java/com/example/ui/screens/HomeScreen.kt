package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistType
import com.example.ui.components.DiviBrandLogo
import com.example.ui.components.LiveClockText
import com.example.ui.components.rememberIsTvDevice
import com.example.ui.components.tvFocusRing
import com.example.ui.components.tvFocusable
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.DiviCyan
import com.example.ui.theme.DiviMagenta
import com.example.ui.theme.DiviViolet
import com.example.ui.theme.LiveRed
import com.example.ui.theme.LiveTileEnd
import com.example.ui.theme.LiveTileStart
import com.example.ui.theme.MovieTileEnd
import com.example.ui.theme.MovieTileStart
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.SeriesTileEnd
import com.example.ui.theme.SeriesTileStart
import com.example.ui.theme.StarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    activePlaylist: PlaylistEntity?,
    favoritesCount: Int,
    recentlyWatched: List<MediaItemEntity>,
    onOpenLiveTv: () -> Unit,
    onOpenMovies: () -> Unit,
    onOpenSeries: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit,
    onOpenPlaylists: (initialTab: Int) -> Unit,
    onOpenRemotePanel: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onSyncActivePlaylist: () -> Unit,
    onRecentItemClick: (MediaItemEntity) -> Unit
) {
    val isTv = rememberIsTvDevice()
    val primaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(activePlaylist?.id, isTv) {
        if (isTv) {
            runCatching { primaryFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
    ) {
        // Atmospheric Cinema Backdrop
        Image(
            painter = painterResource(id = R.drawable.img_divi_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.45f,
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .align(Alignment.TopCenter)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to ObsidianDeep.copy(alpha = 0.25f),
                        0.32f to ObsidianDeep.copy(alpha = 0.85f),
                        0.55f to ObsidianDeep
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // IBO-style Top Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DiviBrandLogo()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activePlaylist != null) {
                        HeaderIconButton(
                            icon = Icons.Filled.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            testTag = "home_search_button",
                            onClick = onOpenSearch
                        )
                        if (activePlaylist.type != PlaylistType.M3U_FILE.name) {
                            HeaderIconButton(
                                icon = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                                testTag = "home_refresh_button",
                                onClick = onSyncActivePlaylist
                            )
                        }
                    }
                    HeaderIconButton(
                        icon = Icons.Filled.CloudSync,
                        contentDescription = "Remote Cloud Panel",
                        testTag = "home_remote_panel_button",
                        onClick = onOpenRemotePanel
                    )
                    HeaderIconButton(
                        icon = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        testTag = "home_settings_button",
                        onClick = onOpenSettings
                    )
                    Spacer(Modifier.width(4.dp))
                    LiveClockText()
                }
            }

            if (activePlaylist == null) {
                WelcomeNoPlaylistSection(
                    primaryFocusRequester = primaryFocusRequester,
                    onAddXtream = { onOpenPlaylists(0) },
                    onAddM3uUrl = { onOpenPlaylists(1) },
                    onImportM3uFile = { onOpenPlaylists(2) },
                    onOpenRemotePanel = onOpenRemotePanel
                )
            } else {
                // Active Playlist Pill & Quick Switch
                ActivePlaylistBanner(
                    playlist = activePlaylist,
                    onManagePlaylists = { onOpenPlaylists(0) }
                )

                // Signature IBO Player 3 Main Hero Tiles (Adaptive for Phone vs Tablet/Landscape)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 560.dp
                    if (isWide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            IboHeroTile(
                                title = stringResource(R.string.nav_live_tv),
                                subtitle = stringResource(R.string.tile_live_subtitle),
                                count = activePlaylist.liveCount,
                                icon = Icons.Filled.LiveTv,
                                gradientColors = listOf(LiveTileStart, LiveTileEnd),
                                badgeText = "LIVE",
                                testTag = "tile_live_tv",
                                onClick = onOpenLiveTv,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                                    .focusRequester(primaryFocusRequester)
                            )
                            IboHeroTile(
                                title = stringResource(R.string.nav_movies),
                                subtitle = stringResource(R.string.tile_movies_subtitle),
                                count = activePlaylist.movieCount,
                                icon = Icons.Filled.Movie,
                                gradientColors = listOf(MovieTileStart, MovieTileEnd),
                                badgeText = "VOD",
                                testTag = "tile_movies",
                                onClick = onOpenMovies,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            )
                            IboHeroTile(
                                title = stringResource(R.string.nav_series),
                                subtitle = stringResource(R.string.tile_series_subtitle),
                                count = activePlaylist.seriesCount,
                                icon = Icons.Filled.Tv,
                                gradientColors = listOf(SeriesTileStart, SeriesTileEnd),
                                badgeText = "SHOWS",
                                testTag = "tile_series",
                                onClick = onOpenSeries,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(180.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            IboHeroTile(
                                title = stringResource(R.string.nav_live_tv),
                                subtitle = stringResource(R.string.tile_live_subtitle),
                                count = activePlaylist.liveCount,
                                icon = Icons.Filled.LiveTv,
                                gradientColors = listOf(LiveTileStart, LiveTileEnd),
                                badgeText = "LIVE",
                                testTag = "tile_live_tv",
                                onClick = onOpenLiveTv,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(148.dp)
                                    .focusRequester(primaryFocusRequester)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                IboHeroTile(
                                    title = stringResource(R.string.nav_movies),
                                    subtitle = stringResource(R.string.tile_movies_subtitle),
                                    count = activePlaylist.movieCount,
                                    icon = Icons.Filled.Movie,
                                    gradientColors = listOf(MovieTileStart, MovieTileEnd),
                                    badgeText = "VOD",
                                    testTag = "tile_movies",
                                    onClick = onOpenMovies,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(152.dp)
                                )
                                IboHeroTile(
                                    title = stringResource(R.string.nav_series),
                                    subtitle = stringResource(R.string.tile_series_subtitle),
                                    count = activePlaylist.seriesCount,
                                    icon = Icons.Filled.Tv,
                                    gradientColors = listOf(SeriesTileStart, SeriesTileEnd),
                                    badgeText = "SHOWS",
                                    testTag = "tile_series",
                                    onClick = onOpenSeries,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(152.dp)
                                )
                            }
                        }
                    }
                }

                // Secondary IBO Action Tiles Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UtilityTile(
                        title = stringResource(R.string.nav_favorites),
                        badge = favoritesCount.toString(),
                        icon = Icons.Filled.Star,
                        accentColor = StarAmber,
                        testTag = "tile_favorites",
                        onClick = onOpenFavorites,
                        modifier = Modifier.weight(1f)
                    )
                    UtilityTile(
                        title = stringResource(R.string.nav_recent),
                        badge = recentlyWatched.size.toString(),
                        icon = Icons.Filled.History,
                        accentColor = ActiveEmerald,
                        testTag = "tile_recent",
                        onClick = onOpenRecent,
                        modifier = Modifier.weight(1f)
                    )
                    UtilityTile(
                        title = stringResource(R.string.nav_playlists),
                        badge = "M3U/XC",
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        accentColor = DiviCyan,
                        testTag = "tile_playlists",
                        onClick = { onOpenPlaylists(0) },
                        modifier = Modifier.weight(1f)
                    )
                    UtilityTile(
                        title = stringResource(R.string.nav_settings),
                        badge = "PRO",
                        icon = Icons.Filled.Settings,
                        accentColor = DiviViolet,
                        testTag = "tile_settings",
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Continue Watching / Recently Watched Strip
                if (recentlyWatched.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Continue Watching",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "See all",
                                style = MaterialTheme.typography.labelLarge,
                                color = DiviCyan,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenRecent)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(end = 8.dp)
                        ) {
                            items(recentlyWatched.take(10), key = { it.id }) { item ->
                                RecentQuickCard(
                                    item = item,
                                    onClick = { onRecentItemClick(item) }
                                )
                            }
                        }
                    }
                }

                // Account & Sync Footer Strip (just like IBO Player bottom bar)
                AccountFooterStrip(playlist = activePlaylist)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .tvFocusRing(CircleShape)
            .clip(CircleShape)
            .background(ObsidianSurfaceElevated.copy(alpha = 0.85f))
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun ActivePlaylistBanner(
    playlist: PlaylistEntity,
    onManagePlaylists: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.02f,
                onClick = onManagePlaylists
            )
            .testTag("active_playlist_banner"),
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ActiveEmerald)
                )
                Column {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val meta = buildString {
                        append(
                            when (playlist.type) {
                                PlaylistType.XTREAM.name -> "Xtream Codes"
                                PlaylistType.M3U_URL.name -> "M3U URL"
                                else -> "Local M3U"
                            }
                        )
                        if (playlist.expiryDate.isNotBlank()) {
                            append(" • Exp: ${playlist.expiryDate}")
                        }
                    }
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DiviCyan.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "Switch",
                    style = MaterialTheme.typography.labelLarge,
                    color = DiviCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun IboHeroTile(
    title: String,
    subtitle: String,
    count: Int,
    icon: ImageVector,
    gradientColors: List<Color>,
    badgeText: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .tvFocusable(
                shape = RoundedCornerShape(22.dp),
                focusedScale = 1.045f,
                borderWidth = 3.dp,
                focusColor = gradientColors.last(),
                onClick = onClick
            )
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, gradientColors.last().copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            gradientColors.first().copy(alpha = 0.92f),
                            gradientColors.last().copy(alpha = 0.78f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            // Subtle large watermark icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.14f),
                modifier = Modifier
                    .size(92.dp)
                    .align(Alignment.BottomEnd)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black.copy(alpha = 0.24f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.28f)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (count > 0) "$count items • $subtitle" else subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun UtilityTile(
    title: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(92.dp)
            .tvFocusable(
                shape = RoundedCornerShape(18.dp),
                focusedScale = 1.05f,
                focusColor = accentColor,
                onClick = onClick
            )
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentQuickCard(
    item: MediaItemEntity,
    onClick: () -> Unit
) {
    val isLive = item.mediaType == MediaType.LIVE.name
    Surface(
        modifier = Modifier
            .width(172.dp)
            .tvFocusable(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.05f,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(ObsidianSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (item.logoUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = item.logoUrl,
                        contentDescription = item.name,
                        contentScale = if (isLive) ContentScale.Fit else ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (isLive) 12.dp else 0.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (isLive) Icons.Filled.LiveTv else Icons.Filled.Movie,
                        contentDescription = null,
                        tint = DiviCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ObsidianDeep.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play),
                        tint = DiviCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isLive) {
                    Surface(
                        color = LiveRed,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
private fun AccountFooterStrip(playlist: PlaylistEntity) {
    val syncedText = if (playlist.lastSyncedAt > 0L) {
        SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault()).format(Date(playlist.lastSyncedAt))
    } else "Just now"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FooterMetric(
                label = "STATUS",
                value = playlist.accountStatus.ifBlank { "Active" },
                valueColor = ActiveEmerald
            )
            FooterMetric(
                label = "CONNECTIONS",
                value = if (playlist.maxConnections.isNotBlank()) {
                    "${playlist.activeConnections.ifBlank { "0" }} / ${playlist.maxConnections}"
                } else "M3U",
                valueColor = DiviCyan
            )
            FooterMetric(
                label = "LAST SYNC",
                value = syncedText,
                valueColor = TextSecondary
            )
        }
    }
}

@Composable
private fun FooterMetric(label: String, value: String, valueColor: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor
        )
    }
}

@Composable
private fun WelcomeNoPlaylistSection(
    primaryFocusRequester: FocusRequester,
    onAddXtream: () -> Unit,
    onAddM3uUrl: () -> Unit,
    onImportM3uFile: () -> Unit,
    onOpenRemotePanel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ObsidianSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_playlist),
                contentDescription = stringResource(R.string.welcome_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary
            )

            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Button(
                onClick = onAddXtream,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .focusRequester(primaryFocusRequester)
                    .tvFocusRing(RoundedCornerShape(16.dp), focusedScale = 1.03f)
                    .testTag("welcome_add_xtream_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DiviCyan,
                    contentColor = ObsidianDeep
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_add_xtream),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onAddM3uUrl,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .tvFocusRing(RoundedCornerShape(14.dp), focusedScale = 1.03f, focusColor = DiviViolet)
                        .testTag("welcome_add_m3u_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DiviViolet),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(Icons.Filled.Link, contentDescription = null, tint = DiviViolet, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_add_m3u), maxLines = 1)
                }

                OutlinedButton(
                    onClick = onImportM3uFile,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .tvFocusRing(RoundedCornerShape(14.dp), focusedScale = 1.03f, focusColor = DiviMagenta)
                        .testTag("welcome_import_file_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ObsidianBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = DiviMagenta, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_import_file), maxLines = 1)
                }
            }

            OutlinedButton(
                onClick = onOpenRemotePanel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .tvFocusRing(RoundedCornerShape(14.dp), focusedScale = 1.03f, focusColor = DiviCyan)
                    .testTag("welcome_remote_panel_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DiviCyan.copy(alpha = 0.65f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DiviCyan)
            ) {
                Icon(Icons.Filled.CloudSync, contentDescription = null, tint = DiviCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Remote Cloud Panel (Push from Web / Phone)", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
