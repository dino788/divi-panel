package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.ui.components.DiviSearchField
import com.example.ui.components.DiviTopScreenBar
import com.example.ui.components.formatDurationMs
import com.example.ui.components.tvFocusable
import com.example.ui.theme.DiviCyan
import com.example.ui.theme.DiviMagenta
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
fun FavoritesScreen(
    favorites: List<MediaItemEntity>,
    onBack: () -> Unit,
    onItemClick: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit
) {
    var filterType by remember { mutableStateOf<MediaType?>(null) }
    val filtered = remember(favorites, filterType) {
        if (filterType == null) favorites else favorites.filter { it.mediaType == filterType!!.name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        DiviTopScreenBar(
            title = stringResource(R.string.nav_favorites),
            subtitle = "${favorites.size} pinned items",
            onBack = onBack
        )

        TypeFilterRow(
            selected = filterType,
            onSelect = { filterType = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(28.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = StarAmber.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.empty_favorites_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(stringResource(R.string.empty_favorites_subtitle), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { item ->
                    MediaListItemCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onToggleFavorite = { onToggleFavorite(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecentScreen(
    recentItems: List<MediaItemEntity>,
    onBack: () -> Unit,
    onItemClick: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        DiviTopScreenBar(
            title = stringResource(R.string.nav_recent),
            subtitle = "${recentItems.size} recently watched",
            onBack = onBack,
            actions = {
                if (recentItems.isNotEmpty()) {
                    IconButton(
                        onClick = onClearHistory,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = stringResource(R.string.settings_clear_history),
                            tint = LiveRed
                        )
                    }
                }
            }
        )

        if (recentItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(28.dp)
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = DiviCyan.copy(alpha = 0.6f), modifier = Modifier.size(48.dp))
                    Text(stringResource(R.string.empty_recent_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Text(stringResource(R.string.empty_recent_subtitle), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(recentItems, key = { it.id }) { item ->
                    MediaListItemCard(
                        item = item,
                        showResumeTag = true,
                        onClick = { onItemClick(item) },
                        onToggleFavorite = { onToggleFavorite(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    query: String,
    results: List<MediaItemEntity>,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onItemClick: (MediaItemEntity) -> Unit,
    onToggleFavorite: (MediaItemEntity) -> Unit
) {
    var filterType by remember { mutableStateOf<MediaType?>(null) }
    val filtered = remember(results, filterType) {
        if (filterType == null) results else results.filter { it.mediaType == filterType!!.name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        DiviTopScreenBar(
            title = stringResource(R.string.nav_search),
            subtitle = if (query.length >= 2) "${filtered.size} matches" else "Live TV • Movies • Series",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DiviSearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = "Search channels, movies, or series...",
                testTag = "global_search_input"
            )

            TypeFilterRow(
                selected = filterType,
                onSelect = { filterType = it }
            )

            if (query.trim().length < 2) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(46.dp))
                        Text("Type at least 2 characters", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text("Search across your entire active playlist", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            } else if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results matching \"$query\"", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 22.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { item ->
                        MediaListItemCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onToggleFavorite = { onToggleFavorite(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeFilterRow(
    selected: MediaType?,
    onSelect: (MediaType?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChipItem(
                label = "All",
                selected = selected == null,
                locked = false,
                onClick = { onSelect(null) }
            )
        }
        item {
            CategoryChipItem(
                label = "Live TV",
                selected = selected == MediaType.LIVE,
                locked = false,
                onClick = { onSelect(MediaType.LIVE) }
            )
        }
        item {
            CategoryChipItem(
                label = "Movies",
                selected = selected == MediaType.MOVIE,
                locked = false,
                onClick = { onSelect(MediaType.MOVIE) }
            )
        }
        item {
            CategoryChipItem(
                label = "Series",
                selected = selected == MediaType.SERIES,
                locked = false,
                onClick = { onSelect(MediaType.SERIES) }
            )
        }
    }
}

@Composable
private fun MediaListItemCard(
    item: MediaItemEntity,
    showResumeTag: Boolean = false,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val (typeLabel, typeAccent, typeIcon) = when (item.mediaType) {
        MediaType.LIVE.name -> Triple("LIVE", DiviCyan, Icons.Filled.LiveTv)
        MediaType.MOVIE.name -> Triple("MOVIE", DiviViolet, Icons.Filled.Movie)
        else -> Triple("SERIES", DiviMagenta, Icons.Filled.Tv)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(
                shape = RoundedCornerShape(16.dp),
                focusedScale = 1.02f,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (item.logoUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = item.logoUrl,
                        contentDescription = item.name,
                        contentScale = if (item.mediaType == MediaType.LIVE.name) ContentScale.Fit else ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(if (item.mediaType == MediaType.LIVE.name) 4.dp else 0.dp)
                    )
                } else {
                    Icon(typeIcon, contentDescription = null, tint = typeAccent, modifier = Modifier.size(26.dp))
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeAccent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (showResumeTag && item.resumePositionMs > 10_000L) {
                        Text(
                            text = "Resume ${formatDurationMs(item.resumePositionMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DiviCyan
                        )
                    }
                }
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
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

            IconButton(onClick = onClick) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.cd_play), tint = DiviCyan)
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (item.isFavorite) StarAmber else TextMuted
                )
            }
        }
    }
}
