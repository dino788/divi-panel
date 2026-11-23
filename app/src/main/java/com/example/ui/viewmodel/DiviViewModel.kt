package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AspectRatioMode
import com.example.data.local.BufferProfile
import com.example.data.local.CategoryEntity
import com.example.data.local.EpgProgram
import com.example.data.local.EpisodeItem
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.data.local.MovieDetailInfo
import com.example.data.local.PlayerSettings
import com.example.data.local.PlaylistEntity
import com.example.data.local.SeriesDetailInfo
import com.example.data.local.StreamFormat
import com.example.data.repository.DiviRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val VIRTUAL_CAT_ALL = "__ALL__"
const val VIRTUAL_CAT_FAVORITES = "__FAVORITES__"
const val VIRTUAL_CAT_RECENT = "__RECENT__"

enum class SortOption(val label: String) {
    DEFAULT("Default"),
    NAME_ASC("A → Z"),
    NAME_DESC("Z → A"),
    RATING_DESC("Top Rated")
}

data class SyncUiState(
    val isSyncing: Boolean = false,
    val progressMessage: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class PlaybackSession(
    val mediaItemId: Long? = null,
    val title: String,
    val subtitle: String = "",
    val streamUrl: String,
    val logoUrl: String = "",
    val isLive: Boolean = false,
    val resumePositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val channelList: List<MediaItemEntity> = emptyList(),
    val currentChannelIndex: Int = -1,
    val episodeList: List<EpisodeItem> = emptyList(),
    val currentEpisodeIndex: Int = -1
)

sealed interface DetailUiState<out T> {
    data object Idle : DetailUiState<Nothing>
    data object Loading : DetailUiState<Nothing>
    data class Success<T>(val data: T) : DetailUiState<T>
    data class Error(val message: String) : DetailUiState<Nothing>
}

@OptIn(ExperimentalCoroutinesApi::class)
class DiviViewModel(
    private val repository: DiviRepository
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePlaylist: StateFlow<PlaylistEntity?> = repository.activePlaylist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val settings: StateFlow<PlayerSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerSettings())

    private val _syncState = MutableStateFlow(SyncUiState())
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    // Parental unlocked category IDs for the current app session
    private val _unlockedCategories = MutableStateFlow<Set<String>>(emptySet())
    val unlockedCategories: StateFlow<Set<String>> = _unlockedCategories.asStateFlow()

    // Favorites & Recently Watched for active playlist
    val allFavorites: StateFlow<List<MediaItemEntity>> = activePlaylist
        .flatMapLatest { playlist ->
            if (playlist == null) flowOf(emptyList()) else repository.observeFavorites(playlist.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyWatched: StateFlow<List<MediaItemEntity>> = activePlaylist
        .flatMapLatest { playlist ->
            if (playlist == null) flowOf(emptyList()) else repository.observeRecentlyWatched(playlist.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategoriesForActivePlaylist: StateFlow<List<CategoryEntity>> = activePlaylist
        .flatMapLatest { playlist ->
            if (playlist == null) flowOf(emptyList()) else repository.observeAllCategories(playlist.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Live TV state ---
    val liveCategories: StateFlow<List<CategoryEntity>> = activePlaylist
        .flatMapLatest { p ->
            if (p == null) flowOf(emptyList()) else repository.observeCategories(p.id, MediaType.LIVE)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedLiveCategory = MutableStateFlow(VIRTUAL_CAT_ALL)
    val selectedLiveCategory: StateFlow<String> = _selectedLiveCategory.asStateFlow()

    private val _liveSearchQuery = MutableStateFlow("")
    val liveSearchQuery: StateFlow<String> = _liveSearchQuery.asStateFlow()

    val liveChannels: StateFlow<List<MediaItemEntity>> = combine(
        activePlaylist,
        _selectedLiveCategory,
        _liveSearchQuery
    ) { playlist, catId, query -> Triple(playlist, catId, query) }
        .flatMapLatest { (playlist, catId, query) ->
            if (playlist == null) return@flatMapLatest flowOf(emptyList())
            val trimmed = query.trim()
            if (trimmed.isNotEmpty() && catId == VIRTUAL_CAT_ALL) {
                return@flatMapLatest repository.searchMediaByType(playlist.id, MediaType.LIVE, trimmed)
            }
            val source: Flow<List<MediaItemEntity>> = when (catId) {
                VIRTUAL_CAT_ALL -> repository.observeMediaByType(playlist.id, MediaType.LIVE)
                VIRTUAL_CAT_FAVORITES -> repository.observeFavoritesByType(playlist.id, MediaType.LIVE)
                VIRTUAL_CAT_RECENT -> repository.observeRecentlyWatchedByType(playlist.id, MediaType.LIVE)
                else -> repository.observeMediaByCategory(playlist.id, MediaType.LIVE, catId)
            }
            combine(source, flowOf(trimmed)) { items, q ->
                if (q.isEmpty()) items
                else items.filter { it.name.contains(q, ignoreCase = true) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Live EPG state for preview / fullscreen channel ---
    private val _activeEpgChannelId = MutableStateFlow<Long?>(null)
    private val _epgPrograms = MutableStateFlow<List<EpgProgram>>(emptyList())
    val epgPrograms: StateFlow<List<EpgProgram>> = _epgPrograms.asStateFlow()

    // --- Movies (VOD) state ---
    val movieCategories: StateFlow<List<CategoryEntity>> = activePlaylist
        .flatMapLatest { p ->
            if (p == null) flowOf(emptyList()) else repository.observeCategories(p.id, MediaType.MOVIE)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMovieCategory = MutableStateFlow(VIRTUAL_CAT_ALL)
    val selectedMovieCategory: StateFlow<String> = _selectedMovieCategory.asStateFlow()

    private val _movieSearchQuery = MutableStateFlow("")
    val movieSearchQuery: StateFlow<String> = _movieSearchQuery.asStateFlow()

    private val _movieSort = MutableStateFlow(SortOption.DEFAULT)
    val movieSort: StateFlow<SortOption> = _movieSort.asStateFlow()

    val movies: StateFlow<List<MediaItemEntity>> = combine(
        activePlaylist,
        _selectedMovieCategory,
        _movieSearchQuery,
        _movieSort
    ) { playlist, catId, query, sort -> Quad(playlist, catId, query, sort) }
        .flatMapLatest { (playlist, catId, query, sort) ->
            if (playlist == null) return@flatMapLatest flowOf(emptyList())
            val trimmed = query.trim()
            val source = when {
                trimmed.isNotEmpty() && catId == VIRTUAL_CAT_ALL ->
                    repository.searchMediaByType(playlist.id, MediaType.MOVIE, trimmed)
                catId == VIRTUAL_CAT_ALL -> repository.observeMediaByType(playlist.id, MediaType.MOVIE)
                catId == VIRTUAL_CAT_FAVORITES -> repository.observeFavoritesByType(playlist.id, MediaType.MOVIE)
                catId == VIRTUAL_CAT_RECENT -> repository.observeRecentlyWatchedByType(playlist.id, MediaType.MOVIE)
                else -> repository.observeMediaByCategory(playlist.id, MediaType.MOVIE, catId)
            }
            combine(source, flowOf(trimmed to sort)) { items, (q, s) ->
                val filtered = if (q.isEmpty() || catId == VIRTUAL_CAT_ALL) items else items.filter {
                    it.name.contains(q, ignoreCase = true)
                }
                applySort(filtered, s)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Series state ---
    val seriesCategories: StateFlow<List<CategoryEntity>> = activePlaylist
        .flatMapLatest { p ->
            if (p == null) flowOf(emptyList()) else repository.observeCategories(p.id, MediaType.SERIES)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSeriesCategory = MutableStateFlow(VIRTUAL_CAT_ALL)
    val selectedSeriesCategory: StateFlow<String> = _selectedSeriesCategory.asStateFlow()

    private val _seriesSearchQuery = MutableStateFlow("")
    val seriesSearchQuery: StateFlow<String> = _seriesSearchQuery.asStateFlow()

    private val _seriesSort = MutableStateFlow(SortOption.DEFAULT)
    val seriesSort: StateFlow<SortOption> = _seriesSort.asStateFlow()

    val seriesList: StateFlow<List<MediaItemEntity>> = combine(
        activePlaylist,
        _selectedSeriesCategory,
        _seriesSearchQuery,
        _seriesSort
    ) { playlist, catId, query, sort -> Quad(playlist, catId, query, sort) }
        .flatMapLatest { (playlist, catId, query, sort) ->
            if (playlist == null) return@flatMapLatest flowOf(emptyList())
            val trimmed = query.trim()
            val source = when {
                trimmed.isNotEmpty() && catId == VIRTUAL_CAT_ALL ->
                    repository.searchMediaByType(playlist.id, MediaType.SERIES, trimmed)
                catId == VIRTUAL_CAT_ALL -> repository.observeMediaByType(playlist.id, MediaType.SERIES)
                catId == VIRTUAL_CAT_FAVORITES -> repository.observeFavoritesByType(playlist.id, MediaType.SERIES)
                catId == VIRTUAL_CAT_RECENT -> repository.observeRecentlyWatchedByType(playlist.id, MediaType.SERIES)
                else -> repository.observeMediaByCategory(playlist.id, MediaType.SERIES, catId)
            }
            combine(source, flowOf(trimmed to sort)) { items, (q, s) ->
                val filtered = if (q.isEmpty() || catId == VIRTUAL_CAT_ALL) items else items.filter {
                    it.name.contains(q, ignoreCase = true)
                }
                applySort(filtered, s)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Global Search ---
    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    val globalSearchResults: StateFlow<List<MediaItemEntity>> = combine(
        activePlaylist,
        _globalSearchQuery
    ) { playlist, query -> playlist to query }
        .flatMapLatest { (playlist, query) ->
            if (playlist == null || query.trim().length < 2) flowOf(emptyList())
            else repository.searchMedia(playlist.id, query.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Movie & Series Detail State ---
    private val _selectedDetailItem = MutableStateFlow<MediaItemEntity?>(null)
    val selectedDetailItem: StateFlow<MediaItemEntity?> = _selectedDetailItem.asStateFlow()

    private val _movieDetailState = MutableStateFlow<DetailUiState<MovieDetailInfo>>(DetailUiState.Idle)
    val movieDetailState: StateFlow<DetailUiState<MovieDetailInfo>> = _movieDetailState.asStateFlow()

    private val _seriesDetailState = MutableStateFlow<DetailUiState<SeriesDetailInfo>>(DetailUiState.Idle)
    val seriesDetailState: StateFlow<DetailUiState<SeriesDetailInfo>> = _seriesDetailState.asStateFlow()

    // --- Active Playback Session ---
    private val _playbackSession = MutableStateFlow<PlaybackSession?>(null)
    val playbackSession: StateFlow<PlaybackSession?> = _playbackSession.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDeviceIdentity()
        }
    }

    // --- Selection & Filters ---

    fun selectLiveCategory(categoryId: String) {
        _selectedLiveCategory.value = categoryId
    }

    fun updateLiveSearch(query: String) {
        _liveSearchQuery.value = query
    }

    fun selectMovieCategory(categoryId: String) {
        _selectedMovieCategory.value = categoryId
    }

    fun updateMovieSearch(query: String) {
        _movieSearchQuery.value = query
    }

    fun updateMovieSort(sort: SortOption) {
        _movieSort.value = sort
    }

    fun selectSeriesCategory(categoryId: String) {
        _selectedSeriesCategory.value = categoryId
    }

    fun updateSeriesSearch(query: String) {
        _seriesSearchQuery.value = query
    }

    fun updateSeriesSort(sort: SortOption) {
        _seriesSort.value = sort
    }

    fun updateGlobalSearch(query: String) {
        _globalSearchQuery.value = query
    }

    fun unlockCategoryForSession(categoryId: String) {
        _unlockedCategories.update { it + categoryId }
    }

    // --- Playlist Operations ---

    fun clearSyncFeedback() {
        _syncState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun addXtreamPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String,
        streamFormat: StreamFormat,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _syncState.value = SyncUiState(isSyncing = true, progressMessage = "Connecting to server...")
            val result = repository.addAndSyncXtreamPlaylist(
                name = name,
                serverUrl = serverUrl,
                username = username,
                password = password,
                streamFormat = streamFormat,
                onProgress = { msg -> _syncState.update { it.copy(progressMessage = msg) } }
            )
            result.fold(
                onSuccess = { playlist ->
                    resetCategoryFilters()
                    _syncState.value = SyncUiState(
                        isSyncing = false,
                        successMessage = "Loaded ${playlist.liveCount} Live • ${playlist.movieCount} Movies • ${playlist.seriesCount} Series"
                    )
                    onComplete()
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState(
                        isSyncing = false,
                        errorMessage = error.message ?: "Failed to connect to Xtream server"
                    )
                }
            )
        }
    }

    fun addM3uUrlPlaylist(
        name: String,
        m3uUrl: String,
        epgUrl: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _syncState.value = SyncUiState(isSyncing = true, progressMessage = "Downloading M3U...")
            val result = repository.addAndSyncM3uUrlPlaylist(
                name = name,
                m3uUrl = m3uUrl,
                epgUrl = epgUrl,
                onProgress = { msg -> _syncState.update { it.copy(progressMessage = msg) } }
            )
            result.fold(
                onSuccess = { playlist ->
                    resetCategoryFilters()
                    _syncState.value = SyncUiState(
                        isSyncing = false,
                        successMessage = "Loaded ${playlist.liveCount} Live • ${playlist.movieCount} Movies • ${playlist.seriesCount} Series"
                    )
                    onComplete()
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState(
                        isSyncing = false,
                        errorMessage = error.message ?: "Failed to load M3U playlist"
                    )
                }
            )
        }
    }

    fun importLocalM3u(
        name: String,
        uri: Uri,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _syncState.value = SyncUiState(isSyncing = true, progressMessage = "Reading M3U file...")
            val result = repository.importLocalM3uFile(
                name = name,
                fileUri = uri,
                onProgress = { msg -> _syncState.update { it.copy(progressMessage = msg) } }
            )
            result.fold(
                onSuccess = { playlist ->
                    resetCategoryFilters()
                    _syncState.value = SyncUiState(
                        isSyncing = false,
                        successMessage = "Imported ${playlist.liveCount} Live • ${playlist.movieCount} Movies • ${playlist.seriesCount} Series"
                    )
                    onComplete()
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState(
                        isSyncing = false,
                        errorMessage = error.message ?: "Could not parse M3U file"
                    )
                }
            )
        }
    }

    fun syncPlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            _syncState.value = SyncUiState(isSyncing = true, progressMessage = "Refreshing ${playlist.name}...")
            val result = repository.syncExistingPlaylist(
                playlist = playlist,
                onProgress = { msg -> _syncState.update { it.copy(progressMessage = msg) } }
            )
            result.fold(
                onSuccess = { updated ->
                    _syncState.value = SyncUiState(
                        isSyncing = false,
                        successMessage = "Synced ${updated.liveCount} Live • ${updated.movieCount} Movies • ${updated.seriesCount} Series"
                    )
                },
                onFailure = { error ->
                    _syncState.value = SyncUiState(
                        isSyncing = false,
                        errorMessage = error.message ?: "Failed to refresh playlist"
                    )
                }
            )
        }
    }

    fun activatePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.activatePlaylist(playlistId)
            resetCategoryFilters()
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            resetCategoryFilters()
        }
    }

    private fun resetCategoryFilters() {
        _selectedLiveCategory.value = VIRTUAL_CAT_ALL
        _selectedMovieCategory.value = VIRTUAL_CAT_ALL
        _selectedSeriesCategory.value = VIRTUAL_CAT_ALL
        _liveSearchQuery.value = ""
        _movieSearchQuery.value = ""
        _seriesSearchQuery.value = ""
    }

    // --- Details & EPG ---

    fun loadEpgForChannel(channel: MediaItemEntity) {
        if (_activeEpgChannelId.value == channel.id) return
        _activeEpgChannelId.value = channel.id
        _epgPrograms.value = emptyList()
        val playlist = activePlaylist.value ?: return
        viewModelScope.launch {
            val programs = repository.fetchShortEpg(playlist, channel)
            if (_activeEpgChannelId.value == channel.id) {
                _epgPrograms.value = programs
            }
        }
    }

    fun openMovieDetail(movie: MediaItemEntity) {
        _selectedDetailItem.value = movie
        _movieDetailState.value = DetailUiState.Loading
        val playlist = activePlaylist.value
        viewModelScope.launch {
            val fresh = repository.getMediaItem(movie.id) ?: movie
            _selectedDetailItem.value = fresh
            val detail = if (playlist != null) {
                repository.fetchMovieDetail(playlist, fresh)
            } else null

            _movieDetailState.value = DetailUiState.Success(
                detail ?: MovieDetailInfo(
                    plot = fresh.plot,
                    cast = fresh.cast,
                    director = fresh.director,
                    genre = fresh.genre,
                    releaseDate = fresh.year,
                    rating = fresh.rating,
                    backdropUrl = fresh.logoUrl
                )
            )
        }
    }

    fun openSeriesDetail(series: MediaItemEntity) {
        _selectedDetailItem.value = series
        _seriesDetailState.value = DetailUiState.Loading
        val playlist = activePlaylist.value
        viewModelScope.launch {
            val fresh = repository.getMediaItem(series.id) ?: series
            _selectedDetailItem.value = fresh

            // If this is an M3U series entry (direct playable URL instead of numeric Xtream ID)
            if (fresh.streamUrl.startsWith("http://", ignoreCase = true) ||
                fresh.streamUrl.startsWith("https://", ignoreCase = true)
            ) {
                val singleEp = EpisodeItem(
                    id = fresh.streamId,
                    episodeNum = 1,
                    seasonNum = 1,
                    title = fresh.name,
                    streamUrl = fresh.streamUrl,
                    containerExtension = fresh.containerExtension.ifBlank { "mp4" },
                    thumbnailUrl = fresh.logoUrl,
                    plot = fresh.plot
                )
                _seriesDetailState.value = DetailUiState.Success(
                    SeriesDetailInfo(
                        plot = fresh.plot,
                        cast = fresh.cast,
                        director = fresh.director,
                        genre = fresh.genre,
                        releaseDate = fresh.year,
                        rating = fresh.rating,
                        backdropUrl = fresh.logoUrl,
                        seasons = mapOf(1 to listOf(singleEp))
                    )
                )
                return@launch
            }

            val detail = if (playlist != null) {
                repository.fetchSeriesDetail(playlist, fresh)
            } else null

            if (detail != null) {
                _seriesDetailState.value = DetailUiState.Success(detail)
            } else {
                _seriesDetailState.value = DetailUiState.Error("Could not load episodes from server.")
            }
        }
    }

    // --- Playback Launchers ---

    fun playLiveChannel(channel: MediaItemEntity, channelList: List<MediaItemEntity>) {
        val index = channelList.indexOfFirst { it.id == channel.id }
        _playbackSession.value = PlaybackSession(
            mediaItemId = channel.id,
            title = channel.name,
            subtitle = channel.categoryName,
            streamUrl = channel.streamUrl,
            logoUrl = channel.logoUrl,
            isLive = true,
            resumePositionMs = 0L,
            isFavorite = channel.isFavorite,
            channelList = channelList,
            currentChannelIndex = index
        )
        loadEpgForChannel(channel)
        viewModelScope.launch {
            repository.updateWatchProgress(channel.id, 0L, 0L)
        }
    }

    fun playMovie(movie: MediaItemEntity, startFromBeginning: Boolean = false) {
        _playbackSession.value = PlaybackSession(
            mediaItemId = movie.id,
            title = movie.name,
            subtitle = listOfNotNull(
                movie.categoryName.takeIf { it.isNotBlank() },
                movie.year.takeIf { it.isNotBlank() }
            ).joinToString(" • "),
            streamUrl = movie.streamUrl,
            logoUrl = movie.logoUrl,
            isLive = false,
            resumePositionMs = if (startFromBeginning) 0L else movie.resumePositionMs,
            isFavorite = movie.isFavorite
        )
        viewModelScope.launch {
            repository.updateWatchProgress(movie.id, if (startFromBeginning) 0L else movie.resumePositionMs, movie.durationMs)
        }
    }

    fun playEpisode(
        series: MediaItemEntity,
        episode: EpisodeItem,
        seasonEpisodes: List<EpisodeItem>
    ) {
        val idx = seasonEpisodes.indexOfFirst { it.id == episode.id }
        _playbackSession.value = PlaybackSession(
            mediaItemId = series.id,
            title = "${series.name} — S${episode.seasonNum.toString().padStart(2, '0')}E${episode.episodeNum.toString().padStart(2, '0')}",
            subtitle = episode.title,
            streamUrl = episode.streamUrl,
            logoUrl = episode.thumbnailUrl.ifBlank { series.logoUrl },
            isLive = false,
            resumePositionMs = 0L,
            isFavorite = series.isFavorite,
            episodeList = seasonEpisodes,
            currentEpisodeIndex = idx
        )
        viewModelScope.launch {
            repository.updateWatchProgress(series.id, 0L, 0L)
        }
    }

    fun switchToChannelInPlayer(channel: MediaItemEntity) {
        val current = _playbackSession.value ?: return
        val idx = current.channelList.indexOfFirst { it.id == channel.id }
        _playbackSession.value = current.copy(
            mediaItemId = channel.id,
            title = channel.name,
            subtitle = channel.categoryName,
            streamUrl = channel.streamUrl,
            logoUrl = channel.logoUrl,
            isFavorite = channel.isFavorite,
            currentChannelIndex = idx
        )
        loadEpgForChannel(channel)
        viewModelScope.launch {
            repository.updateWatchProgress(channel.id, 0L, 0L)
        }
    }

    fun zapChannelOffset(offset: Int) {
        val current = _playbackSession.value ?: return
        if (!current.isLive || current.channelList.isEmpty()) return
        val base = if (current.currentChannelIndex >= 0) current.currentChannelIndex else 0
        val nextIdx = (base + offset + current.channelList.size) % current.channelList.size
        switchToChannelInPlayer(current.channelList[nextIdx])
    }

    fun playNextEpisodeInPlayer() {
        val current = _playbackSession.value ?: return
        if (current.episodeList.isEmpty() || current.currentEpisodeIndex < 0) return
        val nextIdx = current.currentEpisodeIndex + 1
        if (nextIdx >= current.episodeList.size) return
        val ep = current.episodeList[nextIdx]
        val seriesPrefix = current.title.substringBefore(" — ")
        _playbackSession.value = current.copy(
            title = "$seriesPrefix — S${ep.seasonNum.toString().padStart(2, '0')}E${ep.episodeNum.toString().padStart(2, '0')}",
            subtitle = ep.title,
            streamUrl = ep.streamUrl,
            logoUrl = ep.thumbnailUrl.ifBlank { current.logoUrl },
            resumePositionMs = 0L,
            currentEpisodeIndex = nextIdx
        )
    }

    fun savePlaybackProgress(positionMs: Long, durationMs: Long) {
        val itemId = _playbackSession.value?.mediaItemId ?: return
        viewModelScope.launch {
            repository.updateWatchProgress(itemId, positionMs, durationMs)
        }
    }

    fun toggleFavorite(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, item.isFavorite)
            if (_selectedDetailItem.value?.id == item.id) {
                _selectedDetailItem.update { it?.copy(isFavorite = !item.isFavorite) }
            }
            if (_playbackSession.value?.mediaItemId == item.id) {
                _playbackSession.update { it?.copy(isFavorite = !item.isFavorite) }
            }
        }
    }

    fun toggleCurrentPlaybackFavorite() {
        val session = _playbackSession.value ?: return
        val id = session.mediaItemId ?: return
        val newFav = !session.isFavorite
        _playbackSession.update { it?.copy(isFavorite = newFav) }
        viewModelScope.launch {
            repository.toggleFavorite(id, !newFav)
        }
    }

    // --- Settings Actions ---

    fun setStreamFormat(format: StreamFormat) = viewModelScope.launch { repository.setStreamFormat(format) }
    fun setBufferProfile(profile: BufferProfile) = viewModelScope.launch { repository.setBufferProfile(profile) }
    fun setDefaultAspectRatio(mode: AspectRatioMode) = viewModelScope.launch { repository.setDefaultAspectRatio(mode) }
    fun setUserAgent(ua: String) = viewModelScope.launch { repository.setUserAgent(ua) }
    fun setSubtitleScale(scale: Float) = viewModelScope.launch { repository.setSubtitleScale(scale) }
    fun setAutoPlayPreview(enabled: Boolean) = viewModelScope.launch { repository.setAutoPlayPreview(enabled) }
    fun setParentalPin(pin: String) = viewModelScope.launch { repository.setParentalPin(pin) }
    fun toggleCategoryLock(category: CategoryEntity) = viewModelScope.launch {
        repository.setCategoryLocked(category.id, !category.isLocked)
    }
    fun clearWatchHistory() = viewModelScope.launch {
        activePlaylist.value?.let { repository.clearWatchHistory(it.id) }
    }

    private fun applySort(items: List<MediaItemEntity>, sort: SortOption): List<MediaItemEntity> =
        when (sort) {
            SortOption.DEFAULT -> items
            SortOption.NAME_ASC -> items.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> items.sortedByDescending { it.name.lowercase() }
            SortOption.RATING_DESC -> items.sortedByDescending { it.rating.toDoubleOrNull() ?: 0.0 }
        }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    class Factory(private val repository: DiviRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiviViewModel(repository) as T
        }
    }
}
