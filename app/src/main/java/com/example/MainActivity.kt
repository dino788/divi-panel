package com.example

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.DiviDatabase
import com.example.data.local.DiviPreferences
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.data.local.PlayerSettings
import com.example.data.local.PlaylistType
import com.example.data.local.StreamFormat
import com.example.data.remote.CloudRemotePlaylist
import com.example.data.remote.CloudRemoteRepository
import com.example.data.repository.DiviRepository
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutGoogle
import com.example.ui.components.SyncProgressOverlay
import com.example.ui.components.rememberIsTvDevice
import com.example.ui.player.PlayerScreen
import com.example.ui.screens.CatalogGridScreen
import com.example.ui.screens.DiviAuthScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveTvScreen
import com.example.ui.screens.MovieDetailScreen
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.RecentScreen
import com.example.ui.screens.RemotePanelScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SeriesDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DiviTheme
import com.example.ui.viewmodel.CloudPanelViewModel
import com.example.ui.viewmodel.CloudUiState
import com.example.ui.viewmodel.DiviViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object DiviRoutes {
    const val HOME = "home"
    const val PLAYLISTS = "playlists/{tab}"
    const val REMOTE_PANEL = "remote_panel"
    const val LIVE_TV = "live_tv"
    const val MOVIES = "movies"
    const val SERIES = "series"
    const val MOVIE_DETAIL = "movie_detail"
    const val SERIES_DETAIL = "series_detail"
    const val FAVORITES = "favorites"
    const val RECENT = "recent"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PLAYER = "player"

    fun playlists(tab: Int = 0) = "playlists/$tab"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            DiviTheme {
                DiviAuthGate()
            }
        }
    }
}

@Composable
private fun DiviAuthGate() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val appContext = LocalContext.current.applicationContext
    val prefs = remember(appContext) { DiviPreferences(appContext) }
    val settings by prefs.settingsFlow.collectAsStateWithLifecycle(initialValue = PlayerSettings())

    LaunchedEffect(prefs) {
        prefs.ensureDeviceIdentity()
    }

    val user = currentUser
    if (user == null) {
        DiviAuthScreen(
            deviceMac = settings.deviceMac,
            deviceKey = settings.deviceKey,
            onAuthSuccess = { }
        )
    } else {
        key(user.uid) {
            AuthenticatedDiviApp(currentUser = user)
        }
    }
}

@Composable
private fun AuthenticatedDiviApp(currentUser: FirebaseUser) {
    val uid = currentUser.uid
    val uiContext = LocalContext.current
    val appContext = uiContext.applicationContext
    val scope = rememberCoroutineScope()
    val credentialManager = remember(uiContext) { CredentialManager.create(uiContext) }
    val isTv = rememberIsTvDevice()

    val repository = remember(appContext) {
        val db = DiviDatabase.getInstance(appContext)
        val prefs = DiviPreferences(appContext)
        DiviRepository(context = appContext, dao = db.diviDao(), preferences = prefs)
    }
    val vm: DiviViewModel = viewModel(
        key = "divi_local_$uid",
        factory = DiviViewModel.Factory(repository)
    )

    val cloudVm: CloudPanelViewModel = viewModel(
        key = "divi_cloud_$uid",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                CloudPanelViewModel(
                    cloudRepository = CloudRemoteRepository(firestore),
                    currentUserId = uid
                )
            }
        }
    )

    val navController = rememberNavController()

    val playlists by vm.playlists.collectAsStateWithLifecycle()
    val activePlaylist by vm.activePlaylist.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val syncState by vm.syncState.collectAsStateWithLifecycle()
    val unlockedCategories by vm.unlockedCategories.collectAsStateWithLifecycle()

    val cloudDevicesState by cloudVm.devicesState.collectAsStateWithLifecycle()
    val remotePlaylistsState by cloudVm.remotePlaylistsState.collectAsStateWithLifecycle()
    val cloudFeedback by cloudVm.feedback.collectAsStateWithLifecycle()

    // Register / heartbeat this TV or Phone in the user's Cloud Remote Panel
    LaunchedEffect(uid, settings.deviceMac, settings.deviceKey, isTv) {
        if (settings.deviceMac.isNotBlank() && settings.deviceKey.isNotBlank()) {
            val friendlyName = listOfNotNull(
                Build.MANUFACTURER?.takeIf { it.isNotBlank() },
                Build.MODEL?.takeIf { it.isNotBlank() }
            ).distinct().joinToString(" ").ifBlank { if (isTv) "Android TV" else "Android Phone" }
            cloudVm.registerThisDevice(
                deviceMac = settings.deviceMac,
                deviceKey = settings.deviceKey,
                deviceName = friendlyName,
                isTv = isTv
            )
        }
    }

    // Auto-import newly pushed Remote Playlists (from HTML Web Panel or Phone) in real-time!
    val autoProcessedRemoteIds = remember(uid) { mutableSetOf<String>() }
    LaunchedEffect(remotePlaylistsState, playlists, settings.deviceMac, syncState.isSyncing) {
        val remoteList = (remotePlaylistsState as? CloudUiState.Success)?.data ?: return@LaunchedEffect
        if (syncState.isSyncing || settings.deviceMac.isBlank()) return@LaunchedEffect

        val nextToImport = remoteList.firstOrNull { remote ->
            remote.id !in autoProcessedRemoteIds &&
                remote.matchesDevice(settings.deviceMac) &&
                !isAlreadyImportedLocally(remote, playlists)
        }

        if (nextToImport != null) {
            autoProcessedRemoteIds.add(nextToImport.id)
            importRemotePlaylistToLocal(vm, nextToImport, settings.streamFormat)
        }
    }

    val allFavorites by vm.allFavorites.collectAsStateWithLifecycle()
    val recentlyWatched by vm.recentlyWatched.collectAsStateWithLifecycle()
    val allCategories by vm.allCategoriesForActivePlaylist.collectAsStateWithLifecycle()

    val liveCategories by vm.liveCategories.collectAsStateWithLifecycle()
    val selectedLiveCat by vm.selectedLiveCategory.collectAsStateWithLifecycle()
    val liveChannels by vm.liveChannels.collectAsStateWithLifecycle()
    val liveSearch by vm.liveSearchQuery.collectAsStateWithLifecycle()
    val epgPrograms by vm.epgPrograms.collectAsStateWithLifecycle()

    val movieCategories by vm.movieCategories.collectAsStateWithLifecycle()
    val selectedMovieCat by vm.selectedMovieCategory.collectAsStateWithLifecycle()
    val movies by vm.movies.collectAsStateWithLifecycle()
    val movieSearch by vm.movieSearchQuery.collectAsStateWithLifecycle()
    val movieSort by vm.movieSort.collectAsStateWithLifecycle()

    val seriesCategories by vm.seriesCategories.collectAsStateWithLifecycle()
    val selectedSeriesCat by vm.selectedSeriesCategory.collectAsStateWithLifecycle()
    val seriesList by vm.seriesList.collectAsStateWithLifecycle()
    val seriesSearch by vm.seriesSearchQuery.collectAsStateWithLifecycle()
    val seriesSort by vm.seriesSort.collectAsStateWithLifecycle()

    val globalSearchQuery by vm.globalSearchQuery.collectAsStateWithLifecycle()
    val globalSearchResults by vm.globalSearchResults.collectAsStateWithLifecycle()

    val selectedDetailItem by vm.selectedDetailItem.collectAsStateWithLifecycle()
    val movieDetailState by vm.movieDetailState.collectAsStateWithLifecycle()
    val seriesDetailState by vm.seriesDetailState.collectAsStateWithLifecycle()
    val playbackSession by vm.playbackSession.collectAsStateWithLifecycle()

    fun openMediaItem(item: MediaItemEntity) {
        when (item.mediaType) {
            MediaType.LIVE.name -> {
                vm.playLiveChannel(item, liveChannels.ifEmpty { listOf(item) })
                navController.navigate(DiviRoutes.PLAYER)
            }
            MediaType.MOVIE.name -> {
                vm.openMovieDetail(item)
                navController.navigate(DiviRoutes.MOVIE_DETAIL)
            }
            MediaType.SERIES.name -> {
                vm.openSeriesDetail(item)
                navController.navigate(DiviRoutes.SERIES_DETAIL)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = DiviRoutes.HOME
        ) {
            composable(DiviRoutes.HOME) {
                HomeScreen(
                    activePlaylist = activePlaylist,
                    favoritesCount = allFavorites.size,
                    recentlyWatched = recentlyWatched,
                    onOpenLiveTv = { navController.navigate(DiviRoutes.LIVE_TV) },
                    onOpenMovies = { navController.navigate(DiviRoutes.MOVIES) },
                    onOpenSeries = { navController.navigate(DiviRoutes.SERIES) },
                    onOpenFavorites = { navController.navigate(DiviRoutes.FAVORITES) },
                    onOpenRecent = { navController.navigate(DiviRoutes.RECENT) },
                    onOpenPlaylists = { tab -> navController.navigate(DiviRoutes.playlists(tab)) },
                    onOpenRemotePanel = { navController.navigate(DiviRoutes.REMOTE_PANEL) },
                    onOpenSearch = { navController.navigate(DiviRoutes.SEARCH) },
                    onOpenSettings = { navController.navigate(DiviRoutes.SETTINGS) },
                    onSyncActivePlaylist = {
                        activePlaylist?.let { vm.syncPlaylist(it) }
                    },
                    onRecentItemClick = { openMediaItem(it) }
                )
            }

            composable(DiviRoutes.REMOTE_PANEL) {
                RemotePanelScreen(
                    settings = settings,
                    userEmail = currentUser.email.orEmpty(),
                    devicesState = cloudDevicesState,
                    remotePlaylistsState = remotePlaylistsState,
                    feedback = cloudFeedback,
                    onBack = {
                        cloudVm.clearFeedback()
                        navController.popBackStack()
                    },
                    onPushXtream = cloudVm::pushXtreamToCloud,
                    onPushM3u = cloudVm::pushM3uToCloud,
                    onLoadRemotePlaylistNow = { remote ->
                        autoProcessedRemoteIds.add(remote.id)
                        importRemotePlaylistToLocal(vm, remote, settings.streamFormat) {
                            navController.popBackStack(DiviRoutes.HOME, inclusive = false)
                        }
                    },
                    onDeleteRemotePlaylist = cloudVm::deleteRemotePlaylist,
                    onRemoveDevice = cloudVm::removeDevice,
                    onClearFeedback = cloudVm::clearFeedback
                )
            }

            composable(
                route = DiviRoutes.PLAYLISTS,
                arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })
            ) { backStackEntry ->
                val tab = backStackEntry.arguments?.getInt("tab") ?: 0
                PlaylistScreen(
                    playlists = playlists,
                    settings = settings,
                    initialTab = tab,
                    errorMessage = syncState.errorMessage,
                    successMessage = syncState.successMessage,
                    onBack = {
                        vm.clearSyncFeedback()
                        navController.popBackStack()
                    },
                    onAddXtream = { name, server, user, pass, format ->
                        vm.addXtreamPlaylist(name, server, user, pass, format) {
                            navController.popBackStack(DiviRoutes.HOME, inclusive = false)
                        }
                    },
                    onAddM3uUrl = { name, url, epg ->
                        vm.addM3uUrlPlaylist(name, url, epg) {
                            navController.popBackStack(DiviRoutes.HOME, inclusive = false)
                        }
                    },
                    onImportLocalM3u = { name, uri ->
                        vm.importLocalM3u(name, uri) {
                            navController.popBackStack(DiviRoutes.HOME, inclusive = false)
                        }
                    },
                    onActivatePlaylist = { id -> vm.activatePlaylist(id) },
                    onSyncPlaylist = { pl -> vm.syncPlaylist(pl) },
                    onDeletePlaylist = { id -> vm.deletePlaylist(id) },
                    onClearMessages = { vm.clearSyncFeedback() }
                )
            }

            composable(DiviRoutes.LIVE_TV) {
                LiveTvScreen(
                    categories = liveCategories,
                    selectedCategoryId = selectedLiveCat,
                    channels = liveChannels,
                    searchQuery = liveSearch,
                    epgPrograms = epgPrograms,
                    settings = settings,
                    unlockedCategories = unlockedCategories,
                    onBack = { navController.popBackStack() },
                    onSelectCategory = vm::selectLiveCategory,
                    onUnlockCategory = vm::unlockCategoryForSession,
                    onSearchChange = vm::updateLiveSearch,
                    onLoadEpg = vm::loadEpgForChannel,
                    onToggleFavorite = vm::toggleFavorite,
                    onOpenFullscreen = { channel, list ->
                        vm.playLiveChannel(channel, list)
                        navController.navigate(DiviRoutes.PLAYER)
                    }
                )
            }

            composable(DiviRoutes.MOVIES) {
                CatalogGridScreen(
                    mediaType = MediaType.MOVIE,
                    categories = movieCategories,
                    selectedCategoryId = selectedMovieCat,
                    items = movies,
                    searchQuery = movieSearch,
                    sortOption = movieSort,
                    settings = settings,
                    unlockedCategories = unlockedCategories,
                    onBack = { navController.popBackStack() },
                    onSelectCategory = vm::selectMovieCategory,
                    onUnlockCategory = vm::unlockCategoryForSession,
                    onSearchChange = vm::updateMovieSearch,
                    onSortChange = vm::updateMovieSort,
                    onItemClick = { movie ->
                        vm.openMovieDetail(movie)
                        navController.navigate(DiviRoutes.MOVIE_DETAIL)
                    },
                    onToggleFavorite = vm::toggleFavorite
                )
            }

            composable(DiviRoutes.SERIES) {
                CatalogGridScreen(
                    mediaType = MediaType.SERIES,
                    categories = seriesCategories,
                    selectedCategoryId = selectedSeriesCat,
                    items = seriesList,
                    searchQuery = seriesSearch,
                    sortOption = seriesSort,
                    settings = settings,
                    unlockedCategories = unlockedCategories,
                    onBack = { navController.popBackStack() },
                    onSelectCategory = vm::selectSeriesCategory,
                    onUnlockCategory = vm::unlockCategoryForSession,
                    onSearchChange = vm::updateSeriesSearch,
                    onSortChange = vm::updateSeriesSort,
                    onItemClick = { series ->
                        vm.openSeriesDetail(series)
                        navController.navigate(DiviRoutes.SERIES_DETAIL)
                    },
                    onToggleFavorite = vm::toggleFavorite
                )
            }

            composable(DiviRoutes.MOVIE_DETAIL) {
                val movie = selectedDetailItem
                if (movie != null) {
                    MovieDetailScreen(
                        movie = movie,
                        detailState = movieDetailState,
                        onBack = { navController.popBackStack() },
                        onPlay = { fromStart ->
                            vm.playMovie(movie, startFromBeginning = fromStart)
                            navController.navigate(DiviRoutes.PLAYER)
                        },
                        onToggleFavorite = { vm.toggleFavorite(movie) }
                    )
                }
            }

            composable(DiviRoutes.SERIES_DETAIL) {
                val series = selectedDetailItem
                if (series != null) {
                    SeriesDetailScreen(
                        series = series,
                        detailState = seriesDetailState,
                        onBack = { navController.popBackStack() },
                        onPlayEpisode = { episode, seasonEpisodes ->
                            vm.playEpisode(series, episode, seasonEpisodes)
                            navController.navigate(DiviRoutes.PLAYER)
                        },
                        onToggleFavorite = { vm.toggleFavorite(series) }
                    )
                }
            }

            composable(DiviRoutes.FAVORITES) {
                FavoritesScreen(
                    favorites = allFavorites,
                    onBack = { navController.popBackStack() },
                    onItemClick = { openMediaItem(it) },
                    onToggleFavorite = vm::toggleFavorite
                )
            }

            composable(DiviRoutes.RECENT) {
                RecentScreen(
                    recentItems = recentlyWatched,
                    onBack = { navController.popBackStack() },
                    onItemClick = { openMediaItem(it) },
                    onToggleFavorite = vm::toggleFavorite,
                    onClearHistory = vm::clearWatchHistory
                )
            }

            composable(DiviRoutes.SEARCH) {
                SearchScreen(
                    query = globalSearchQuery,
                    results = globalSearchResults,
                    onBack = { navController.popBackStack() },
                    onQueryChange = vm::updateGlobalSearch,
                    onItemClick = { openMediaItem(it) },
                    onToggleFavorite = vm::toggleFavorite
                )
            }

            composable(DiviRoutes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    activePlaylist = activePlaylist,
                    categories = allCategories,
                    onBack = { navController.popBackStack() },
                    onSetStreamFormat = vm::setStreamFormat,
                    onSetBufferProfile = vm::setBufferProfile,
                    onSetAspectRatio = vm::setDefaultAspectRatio,
                    onSetUserAgent = vm::setUserAgent,
                    onSetAutoPlayPreview = vm::setAutoPlayPreview,
                    onSetParentalPin = vm::setParentalPin,
                    onToggleCategoryLock = vm::toggleCategoryLock,
                    onClearHistory = vm::clearWatchHistory,
                    userEmail = currentUser.email.orEmpty(),
                    onOpenRemotePanel = { navController.navigate(DiviRoutes.REMOTE_PANEL) },
                    onSignOut = {
                        signOutGoogle(credentialManager = credentialManager, scope = scope)
                    }
                )
            }

            composable(DiviRoutes.PLAYER) {
                val session = playbackSession
                if (session != null) {
                    PlayerScreen(
                        session = session,
                        settings = settings,
                        epgPrograms = epgPrograms,
                        onExit = { navController.popBackStack() },
                        onSaveProgress = vm::savePlaybackProgress,
                        onToggleFavorite = vm::toggleCurrentPlaybackFavorite,
                        onSwitchChannel = vm::switchToChannelInPlayer,
                        onZapOffset = vm::zapChannelOffset,
                        onNextEpisode = vm::playNextEpisodeInPlayer
                    )
                }
            }
        }

        // Global Playlist Loading / Syncing Overlay
        if (syncState.isSyncing) {
            SyncProgressOverlay(message = syncState.progressMessage)
        }
    }
}

private fun isAlreadyImportedLocally(
    remote: CloudRemotePlaylist,
    localPlaylists: List<com.example.data.local.PlaylistEntity>
): Boolean {
    return if (remote.playlistType.equals("XTREAM", ignoreCase = true)) {
        val normServer = remote.serverUrl.trim().trimEnd('/').lowercase()
        val normUser = remote.username.trim().lowercase()
        localPlaylists.any { local ->
            local.type == PlaylistType.XTREAM.name &&
                local.serverUrl.trim().trimEnd('/').lowercase() == normServer &&
                local.username.trim().lowercase() == normUser
        }
    } else {
        val normUrl = remote.m3uUrl.trim().lowercase()
        localPlaylists.any { local ->
            local.type == PlaylistType.M3U_URL.name &&
                local.m3uUrl.trim().lowercase() == normUrl
        }
    }
}

private fun importRemotePlaylistToLocal(
    vm: DiviViewModel,
    remote: CloudRemotePlaylist,
    defaultFormat: StreamFormat,
    onComplete: () -> Unit = {}
) {
    if (remote.playlistType.equals("XTREAM", ignoreCase = true)) {
        if (remote.serverUrl.isNotBlank() && remote.username.isNotBlank() && remote.password.isNotBlank()) {
            vm.addXtreamPlaylist(
                name = remote.name.ifBlank { "Remote Xtream" },
                serverUrl = remote.serverUrl,
                username = remote.username,
                password = remote.password,
                streamFormat = defaultFormat,
                onComplete = onComplete
            )
        }
    } else if (remote.m3uUrl.isNotBlank()) {
        vm.addM3uUrlPlaylist(
            name = remote.name.ifBlank { "Remote M3U" },
            m3uUrl = remote.m3uUrl,
            epgUrl = remote.epgUrl,
            onComplete = onComplete
        )
    }
}
