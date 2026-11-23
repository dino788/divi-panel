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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.local.EpisodeItem
import com.example.data.local.MediaItemEntity
import com.example.data.local.MovieDetailInfo
import com.example.data.local.SeriesDetailInfo
import com.example.ui.components.formatDurationMs
import com.example.ui.components.tvFocusRing
import com.example.ui.components.tvFocusable
import com.example.ui.theme.DiviCyan
import com.example.ui.theme.DiviViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.StarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DetailUiState

@Composable
fun MovieDetailScreen(
    movie: MediaItemEntity,
    detailState: DetailUiState<MovieDetailInfo>,
    onBack: () -> Unit,
    onPlay: (startFromBeginning: Boolean) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val info = (detailState as? DetailUiState.Success)?.data
    val backdropUrl = info?.backdropUrl?.takeIf { it.isNotBlank() } ?: movie.logoUrl
    val hasResume = movie.resumePositionMs > 10_000L

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Cinematic Backdrop Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
            ) {
                if (backdropUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = backdropUrl,
                        contentDescription = movie.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF102840), ObsidianDeep))
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to ObsidianDeep.copy(alpha = 0.4f),
                                0.5f to ObsidianDeep.copy(alpha = 0.65f),
                                1f to ObsidianDeep
                            )
                        )
                )

                // Top floating back + favorite actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ObsidianDeep.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ObsidianDeep.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = if (movie.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(R.string.cd_favorite),
                            tint = if (movie.isFavorite) StarAmber else TextPrimary
                        )
                    }
                }

                // Poster + Title overlay at bottom of hero
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianSurfaceElevated,
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier
                            .width(96.dp)
                            .aspectRatio(2f / 3f)
                    ) {
                        if (movie.logoUrl.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = movie.logoUrl,
                                contentDescription = movie.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Movie, contentDescription = null, tint = DiviCyan, modifier = Modifier.size(34.dp))
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = movie.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rating = info?.rating?.takeIf { it.isNotBlank() && it != "0" } ?: movie.rating
                            if (rating.isNotBlank() && rating != "0") {
                                MetadataPill(text = "★ ${rating.take(4)}", accent = StarAmber)
                            }
                            val year = info?.releaseDate?.take(4)?.takeIf { it.isNotBlank() } ?: movie.year
                            if (year.isNotBlank()) {
                                MetadataPill(text = year, accent = DiviCyan)
                            }
                            if (!info?.duration.isNullOrBlank()) {
                                MetadataPill(text = info!!.duration, accent = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Primary Play / Resume Actions + Metadata Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onPlay(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .tvFocusRing(RoundedCornerShape(16.dp), focusedScale = 1.03f)
                            .testTag("movie_play_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DiviCyan,
                            contentColor = ObsidianDeep
                        )
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (hasResume) {
                                "${stringResource(R.string.action_resume)} (${formatDurationMs(movie.resumePositionMs)})"
                            } else {
                                stringResource(R.string.action_play_now)
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (hasResume) {
                        OutlinedButton(
                            onClick = { onPlay(true) },
                            modifier = Modifier.height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, ObsidianBorder)
                        ) {
                            Icon(Icons.Filled.Replay, contentDescription = "Start over", tint = TextPrimary)
                        }
                    }
                }

                // Technical codecs + Genre row
                val tags = buildList {
                    if (movie.categoryName.isNotBlank()) add(movie.categoryName)
                    if (!info?.genre.isNullOrBlank()) add(info!!.genre)
                    if (!info?.videoCodec.isNullOrBlank()) add(info!!.videoCodec)
                    if (!info?.audioCodec.isNullOrBlank()) add(info!!.audioCodec)
                    if (movie.containerExtension.isNotBlank()) add(movie.containerExtension.uppercase())
                }
                if (tags.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags) { tag ->
                            MetadataPill(text = tag, accent = TextSecondary)
                        }
                    }
                }

                if (detailState is DetailUiState.Loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = DiviCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Loading movie details...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                val plot = info?.plot?.takeIf { it.isNotBlank() } ?: movie.plot
                if (plot.isNotBlank()) {
                    DetailInfoBlock(title = stringResource(R.string.label_synopsis), body = plot)
                }

                val director = info?.director?.takeIf { it.isNotBlank() } ?: movie.director
                if (director.isNotBlank()) {
                    DetailInfoBlock(title = stringResource(R.string.label_director), body = director)
                }

                val cast = info?.cast?.takeIf { it.isNotBlank() } ?: movie.cast
                if (cast.isNotBlank()) {
                    DetailInfoBlock(title = stringResource(R.string.label_cast), body = cast)
                }

                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
fun SeriesDetailScreen(
    series: MediaItemEntity,
    detailState: DetailUiState<SeriesDetailInfo>,
    onBack: () -> Unit,
    onPlayEpisode: (EpisodeItem, List<EpisodeItem>) -> Unit,
    onToggleFavorite: () -> Unit
) {
    val info = (detailState as? DetailUiState.Success)?.data
    val seasons = info?.seasons.orEmpty()
    val seasonNumbers = remember(seasons) { seasons.keys.toList().sorted() }
    var selectedSeason by remember(seasonNumbers) {
        mutableIntStateOf(seasonNumbers.firstOrNull() ?: 1)
    }

    LaunchedEffect(seasonNumbers) {
        if (seasonNumbers.isNotEmpty() && selectedSeason !in seasonNumbers) {
            selectedSeason = seasonNumbers.first()
        }
    }

    val episodes = seasons[selectedSeason].orEmpty()
    val backdropUrl = info?.backdropUrl?.takeIf { it.isNotBlank() } ?: series.logoUrl

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDeep)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 26.dp)
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                if (backdropUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = backdropUrl,
                        contentDescription = series.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF28164A), ObsidianDeep)))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to ObsidianDeep.copy(alpha = 0.4f),
                                0.55f to ObsidianDeep.copy(alpha = 0.72f),
                                1f to ObsidianDeep
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ObsidianDeep.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ObsidianDeep.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = if (series.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(R.string.cd_favorite),
                            tint = if (series.isFavorite) StarAmber else TextPrimary
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = series.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val rating = info?.rating?.takeIf { it.isNotBlank() && it != "0" } ?: series.rating
                        if (rating.isNotBlank() && rating != "0") {
                            MetadataPill("★ ${rating.take(4)}", StarAmber)
                        }
                        if (seasonNumbers.isNotEmpty()) {
                            MetadataPill("${seasonNumbers.size} Seasons", DiviViolet)
                        }
                        if (series.categoryName.isNotBlank()) {
                            MetadataPill(series.categoryName, DiviCyan)
                        }
                    }
                }
            }
        }

        // Synopsis
        item {
            val plot = info?.plot?.takeIf { it.isNotBlank() } ?: series.plot
            if (plot.isNotBlank()) {
                Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                    DetailInfoBlock(title = stringResource(R.string.label_synopsis), body = plot)
                }
            }
        }

        // Loading / Error states
        if (detailState is DetailUiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = DiviViolet)
                        Text("Loading seasons & episodes...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else if (detailState is DetailUiState.Error) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(detailState.message, color = TextSecondary)
                }
            }
        }

        // Season Selector Pills
        if (seasonNumbers.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_seasons),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(seasonNumbers) { sNum ->
                            val isSelected = sNum == selectedSeason
                            Surface(
                                modifier = Modifier
                                    .height(40.dp)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(12.dp),
                                        focusedScale = 1.06f,
                                        focusColor = DiviViolet,
                                        onClick = { selectedSeason = sNum }
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) DiviViolet else ObsidianSurface,
                                border = BorderStroke(1.dp, if (isSelected) DiviViolet else ObsidianBorder)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        text = "Season $sNum",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Episodes List
        items(episodes, key = { it.id }) { ep ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
                    .tvFocusable(
                        shape = RoundedCornerShape(16.dp),
                        focusedScale = 1.025f,
                        onClick = { onPlayEpisode(ep, episodes) }
                    )
                    .testTag("episode_row_${ep.id}"),
                shape = RoundedCornerShape(16.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder)
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
                            .width(104.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (ep.thumbnailUrl.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = ep.thumbnailUrl,
                                contentDescription = ep.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Tv,
                                contentDescription = null,
                                tint = DiviViolet.copy(alpha = 0.6f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(ObsidianDeep.copy(alpha = 0.72f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play),
                                tint = DiviCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "E${ep.episodeNum.toString().padStart(2, '0')} • ${ep.title}",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (ep.duration.isNotBlank()) {
                            Text(
                                text = ep.duration,
                                style = MaterialTheme.typography.labelSmall,
                                color = DiviCyan
                            )
                        }
                        if (ep.plot.isNotBlank()) {
                            Text(
                                text = ep.plot,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataPill(text: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ObsidianSurfaceElevated,
        border = BorderStroke(1.dp, ObsidianBorder)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DetailInfoBlock(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = DiviCyan
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
