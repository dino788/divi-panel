package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.CategoryEntity
import com.example.data.local.DiviDao
import com.example.data.local.DiviPreferences
import com.example.data.local.EpgProgram
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.data.local.MovieDetailInfo
import com.example.data.local.PlayerSettings
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistType
import com.example.data.local.SeriesDetailInfo
import com.example.data.local.StreamFormat
import com.example.data.network.M3uParser
import com.example.data.network.XtreamApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class DiviRepository(
    private val context: Context,
    private val dao: DiviDao,
    private val preferences: DiviPreferences,
    private val httpClient: OkHttpClient = XtreamApiClient.defaultClient(),
    private val xtreamClient: XtreamApiClient = XtreamApiClient(httpClient)
) {

    val playlists: Flow<List<PlaylistEntity>> = dao.observePlaylists()
    val activePlaylist: Flow<PlaylistEntity?> = dao.observeActivePlaylist()
    val settings: Flow<PlayerSettings> = preferences.settingsFlow

    suspend fun initializeDeviceIdentity() = preferences.ensureDeviceIdentity()

    fun observeCategories(playlistId: Long, mediaType: MediaType): Flow<List<CategoryEntity>> =
        dao.observeCategories(playlistId, mediaType.name)

    fun observeAllCategories(playlistId: Long): Flow<List<CategoryEntity>> =
        dao.observeAllCategories(playlistId)

    fun observeMediaByType(playlistId: Long, mediaType: MediaType): Flow<List<MediaItemEntity>> =
        dao.observeMediaByType(playlistId, mediaType.name)

    fun observeMediaByCategory(
        playlistId: Long,
        mediaType: MediaType,
        categoryId: String
    ): Flow<List<MediaItemEntity>> = dao.observeMediaByCategory(playlistId, mediaType.name, categoryId)

    fun observeFavorites(playlistId: Long): Flow<List<MediaItemEntity>> =
        dao.observeFavorites(playlistId)

    fun observeFavoritesByType(playlistId: Long, mediaType: MediaType): Flow<List<MediaItemEntity>> =
        dao.observeFavoritesByType(playlistId, mediaType.name)

    fun observeRecentlyWatched(playlistId: Long): Flow<List<MediaItemEntity>> =
        dao.observeRecentlyWatched(playlistId)

    fun observeRecentlyWatchedByType(playlistId: Long, mediaType: MediaType): Flow<List<MediaItemEntity>> =
        dao.observeRecentlyWatchedByType(playlistId, mediaType.name)

    fun searchMedia(playlistId: Long, query: String): Flow<List<MediaItemEntity>> =
        dao.searchMedia(playlistId, query)

    fun searchMediaByType(playlistId: Long, mediaType: MediaType, query: String): Flow<List<MediaItemEntity>> =
        dao.searchMediaByType(playlistId, mediaType.name, query)

    fun observeMediaItem(itemId: Long): Flow<MediaItemEntity?> =
        dao.observeMediaItemById(itemId)

    suspend fun getMediaItem(itemId: Long): MediaItemEntity? = dao.getMediaItemById(itemId)

    suspend fun toggleFavorite(itemId: Long, currentFavorite: Boolean) {
        dao.setFavorite(itemId, !currentFavorite)
    }

    suspend fun updateWatchProgress(itemId: Long, positionMs: Long, durationMs: Long) {
        dao.updateWatchProgress(
            itemId = itemId,
            watchedAt = System.currentTimeMillis(),
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = durationMs.coerceAtLeast(0L)
        )
    }

    suspend fun clearWatchHistory(playlistId: Long) {
        dao.clearWatchHistory(playlistId)
    }

    suspend fun setCategoryLocked(categoryRowId: Long, locked: Boolean) {
        dao.setCategoryLocked(categoryRowId, locked)
    }

    suspend fun activatePlaylist(playlistId: Long) {
        dao.activatePlaylist(playlistId)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        val wasActive = dao.getPlaylistById(playlistId)?.isActive == true
        dao.deletePlaylistCompletely(playlistId)
        if (wasActive) {
            val remaining = dao.observePlaylists().first()
            remaining.firstOrNull()?.let { dao.activatePlaylist(it.id) }
        }
    }

    // --- Add & Sync Xtream Codes Playlist ---

    suspend fun addAndSyncXtreamPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String,
        streamFormat: StreamFormat,
        existingPlaylistId: Long? = null,
        onProgress: (String) -> Unit
    ): Result<PlaylistEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val currentSettings = preferences.settingsFlow.first()
            val userAgent = currentSettings.userAgent
            onProgress("Authenticating with Xtream server...")

            val account = xtreamClient.authenticate(serverUrl, username.trim(), password.trim(), userAgent)
            val cleanName = name.trim().ifBlank { "Xtream • ${username.trim()}" }

            val baseEntity = if (existingPlaylistId != null) {
                dao.getPlaylistById(existingPlaylistId)?.copy(
                    name = cleanName,
                    serverUrl = account.normalizedServerUrl,
                    username = username.trim(),
                    password = password.trim(),
                    epgUrl = account.epgUrl,
                    accountStatus = account.status,
                    expiryDate = account.expiryDateFormatted,
                    activeConnections = account.activeConnections,
                    maxConnections = account.maxConnections
                ) ?: PlaylistEntity(
                    name = cleanName,
                    type = PlaylistType.XTREAM.name,
                    serverUrl = account.normalizedServerUrl,
                    username = username.trim(),
                    password = password.trim(),
                    epgUrl = account.epgUrl,
                    accountStatus = account.status,
                    expiryDate = account.expiryDateFormatted,
                    activeConnections = account.activeConnections,
                    maxConnections = account.maxConnections
                )
            } else {
                PlaylistEntity(
                    name = cleanName,
                    type = PlaylistType.XTREAM.name,
                    serverUrl = account.normalizedServerUrl,
                    username = username.trim(),
                    password = password.trim(),
                    epgUrl = account.epgUrl,
                    accountStatus = account.status,
                    expiryDate = account.expiryDateFormatted,
                    activeConnections = account.activeConnections,
                    maxConnections = account.maxConnections
                )
            }

            val playlistId = if (baseEntity.id == 0L) {
                dao.insertPlaylist(baseEntity)
            } else {
                dao.updatePlaylist(baseEntity)
                baseEntity.id
            }

            val favIds = dao.getFavoriteStreamIds(playlistId).toSet()

            val cleanUser = username.trim()
            val cleanPass = password.trim()

            onProgress("Loading Live TV channels...")
            val liveCats = xtreamClient.fetchCategories(
                account.normalizedServerUrl, cleanUser, cleanPass,
                playlistId, MediaType.LIVE, userAgent
            )
            val liveCatMap = liveCats.associate { it.categoryId to it.categoryName }
            val (liveStreams, liveErr) = xtreamClient.fetchLiveStreams(
                account.normalizedServerUrl, cleanUser, cleanPass,
                playlistId, liveCatMap, streamFormat, favIds, userAgent
            )

            onProgress("Live TV: ${liveStreams.size} • Loading Movies...")
            val vodCats = xtreamClient.fetchCategories(
                account.normalizedServerUrl, cleanUser, cleanPass,
                playlistId, MediaType.MOVIE, userAgent
            )
            val vodCatMap = vodCats.associate { it.categoryId to it.categoryName }
            val (vodStreams, vodErr) = xtreamClient.fetchVodStreams(
                account.normalizedServerUrl, cleanUser, cleanPass,
                playlistId, vodCatMap, favIds, userAgent
            )

            onProgress("Movies: ${vodStreams.size} • Loading Series...")
            val seriesCats = xtreamClient.fetchCategories(
                account.normalizedServerUrl, cleanUser, cleanPass,
                playlistId, MediaType.SERIES, userAgent
            )
            val seriesCatMap = seriesCats.associate { it.categoryId to it.categoryName }
            val (seriesItems, seriesErr) = xtreamClient.fetchSeries(
                account.normalizedServerUrl, cleanUser, cleanPass,
                playlistId, seriesCatMap, favIds, userAgent
            )

            var finalCategories: List<CategoryEntity>
            var finalItems: List<MediaItemEntity>
            var finalLiveCount = liveStreams.size
            var finalMovieCount = vodStreams.size
            var finalSeriesCount = seriesItems.size

            if (liveStreams.isEmpty() && vodStreams.isEmpty() && seriesItems.isEmpty()) {
                // Automatic M3U Plus fallback for servers that restrict player_api JSON lists
                onProgress("Trying M3U Plus fallback on server...")
                val fallbackUrl =
                    "${account.normalizedServerUrl}/get.php?username=$cleanUser&password=$cleanPass&type=m3u_plus&output=${streamFormat.ext}"
                val fallbackParsed = runCatching {
                    val req = Request.Builder()
                        .url(fallbackUrl)
                        .header("User-Agent", userAgent)
                        .build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                        val bodyStream = resp.body?.byteStream() ?: throw IOException("Empty body")
                        M3uParser.parse(bodyStream, playlistId, favIds)
                    }
                }.getOrNull()

                if (fallbackParsed != null && fallbackParsed.items.isNotEmpty()) {
                    finalCategories = fallbackParsed.categories
                    finalItems = fallbackParsed.items
                    finalLiveCount = fallbackParsed.liveCount
                    finalMovieCount = fallbackParsed.movieCount
                    finalSeriesCount = fallbackParsed.seriesCount
                } else {
                    if (existingPlaylistId == null) dao.deletePlaylistById(playlistId)
                    val details = listOfNotNull(
                        liveErr?.let { "Live: $it" },
                        vodErr?.let { "Movies: $it" },
                        seriesErr?.let { "Series: $it" }
                    ).joinToString(" | ").ifBlank { "Server returned empty lists" }
                    throw IOException("0 streams loaded ($details)")
                }
            } else {
                val allItems = ArrayList<MediaItemEntity>(liveStreams.size + vodStreams.size + seriesItems.size).apply {
                    addAll(liveStreams)
                    addAll(vodStreams)
                    addAll(seriesItems)
                }
                val countsByCat = allItems.groupingBy { it.mediaType to it.categoryId }.eachCount()
                finalCategories = buildList {
                    addAll(liveCats.map { it.copy(itemCount = countsByCat[MediaType.LIVE.name to it.categoryId] ?: 0) })
                    addAll(vodCats.map { it.copy(itemCount = countsByCat[MediaType.MOVIE.name to it.categoryId] ?: 0) })
                    addAll(seriesCats.map { it.copy(itemCount = countsByCat[MediaType.SERIES.name to it.categoryId] ?: 0) })
                }
                finalItems = allItems
            }

            onProgress("Saving ${finalItems.size} items to divi...")
            dao.replacePlaylistContent(playlistId, finalCategories, finalItems)

            val updated = baseEntity.copy(
                id = playlistId,
                isActive = true,
                lastSyncedAt = System.currentTimeMillis(),
                liveCount = finalLiveCount,
                movieCount = finalMovieCount,
                seriesCount = finalSeriesCount
            )
            dao.updatePlaylist(updated)
            dao.activatePlaylist(playlistId)
            preferences.setStreamFormat(streamFormat)
            updated
        }
    }

    // --- Add & Sync M3U URL Playlist ---

    suspend fun addAndSyncM3uUrlPlaylist(
        name: String,
        m3uUrl: String,
        epgUrl: String = "",
        existingPlaylistId: Long? = null,
        onProgress: (String) -> Unit
    ): Result<PlaylistEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUrl = m3uUrl.trim()
            if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
                !cleanUrl.startsWith("https://", ignoreCase = true)
            ) {
                throw IOException("Playlist URL must start with http:// or https://")
            }

            val userAgent = preferences.settingsFlow.first().userAgent
            onProgress("Downloading M3U playlist...")

            val request = Request.Builder()
                .url(cleanUrl)
                .header("User-Agent", userAgent)
                .build()

            val playlistId: Long
            val baseEntity: PlaylistEntity

            if (existingPlaylistId != null) {
                val existing = dao.getPlaylistById(existingPlaylistId)
                baseEntity = (existing ?: PlaylistEntity(name = name, type = PlaylistType.M3U_URL.name)).copy(
                    name = name.trim().ifBlank { "M3U Playlist" },
                    m3uUrl = cleanUrl,
                    epgUrl = epgUrl.trim()
                )
                playlistId = if (baseEntity.id == 0L) dao.insertPlaylist(baseEntity) else {
                    dao.updatePlaylist(baseEntity)
                    baseEntity.id
                }
            } else {
                baseEntity = PlaylistEntity(
                    name = name.trim().ifBlank { "M3U Playlist" },
                    type = PlaylistType.M3U_URL.name,
                    m3uUrl = cleanUrl,
                    epgUrl = epgUrl.trim()
                )
                playlistId = dao.insertPlaylist(baseEntity)
            }

            val favIds = dao.getFavoriteStreamIds(playlistId).toSet()

            val parsed = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Failed to fetch M3U (HTTP ${response.code})")
                }
                val stream = response.body?.byteStream()
                    ?: throw IOException("Empty playlist response")
                onProgress("Parsing channels, movies & series...")
                M3uParser.parse(stream, playlistId, favIds)
            }

            if (parsed.items.isEmpty()) {
                if (existingPlaylistId == null) dao.deletePlaylistById(playlistId)
                throw IOException("No valid streams found in this M3U playlist")
            }

            onProgress("Saving ${parsed.items.size} items to divi...")
            dao.replacePlaylistContent(playlistId, parsed.categories, parsed.items)

            val updated = baseEntity.copy(
                id = playlistId,
                epgUrl = epgUrl.trim().ifBlank { parsed.detectedEpgUrl },
                isActive = true,
                lastSyncedAt = System.currentTimeMillis(),
                accountStatus = "M3U Ready",
                liveCount = parsed.liveCount,
                movieCount = parsed.movieCount,
                seriesCount = parsed.seriesCount
            )
            dao.updatePlaylist(updated)
            dao.activatePlaylist(playlistId)
            updated
        }
    }

    // --- Import Local M3U File via Storage Access Framework ---

    suspend fun importLocalM3uFile(
        name: String,
        fileUri: Uri,
        onProgress: (String) -> Unit
    ): Result<PlaylistEntity> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress("Reading local M3U file...")
            val baseEntity = PlaylistEntity(
                name = name.trim().ifBlank { "Local M3U Playlist" },
                type = PlaylistType.M3U_FILE.name,
                m3uUrl = fileUri.toString(),
                accountStatus = "Local File"
            )
            val playlistId = dao.insertPlaylist(baseEntity)

            val parsed = context.contentResolver.openInputStream(fileUri)?.use { stream ->
                onProgress("Parsing M3U entries...")
                M3uParser.parse(stream, playlistId)
            } ?: run {
                dao.deletePlaylistById(playlistId)
                throw IOException("Unable to open selected M3U file")
            }

            if (parsed.items.isEmpty()) {
                dao.deletePlaylistById(playlistId)
                throw IOException("No valid streams found in file")
            }

            onProgress("Saving ${parsed.items.size} items...")
            dao.replacePlaylistContent(playlistId, parsed.categories, parsed.items)

            val updated = baseEntity.copy(
                id = playlistId,
                epgUrl = parsed.detectedEpgUrl,
                isActive = true,
                lastSyncedAt = System.currentTimeMillis(),
                liveCount = parsed.liveCount,
                movieCount = parsed.movieCount,
                seriesCount = parsed.seriesCount
            )
            dao.updatePlaylist(updated)
            dao.activatePlaylist(playlistId)
            updated
        }
    }

    suspend fun syncExistingPlaylist(
        playlist: PlaylistEntity,
        onProgress: (String) -> Unit
    ): Result<PlaylistEntity> {
        val currentFormat = preferences.settingsFlow.first().streamFormat
        return when (playlist.type) {
            PlaylistType.XTREAM.name -> addAndSyncXtreamPlaylist(
                name = playlist.name,
                serverUrl = playlist.serverUrl,
                username = playlist.username,
                password = playlist.password,
                streamFormat = currentFormat,
                existingPlaylistId = playlist.id,
                onProgress = onProgress
            )

            PlaylistType.M3U_URL.name -> addAndSyncM3uUrlPlaylist(
                name = playlist.name,
                m3uUrl = playlist.m3uUrl,
                epgUrl = playlist.epgUrl,
                existingPlaylistId = playlist.id,
                onProgress = onProgress
            )

            else -> Result.failure(IOException("Local M3U files can be re-imported from Playlist Manager"))
        }
    }

    // --- Live EPG, VOD Info, Series Episodes ---

    suspend fun fetchShortEpg(playlist: PlaylistEntity, channel: MediaItemEntity): List<EpgProgram> {
        if (playlist.type != PlaylistType.XTREAM.name) return emptyList()
        val ua = preferences.settingsFlow.first().userAgent
        return xtreamClient.fetchShortEpg(
            baseUrl = playlist.serverUrl,
            username = playlist.username,
            password = playlist.password,
            rawStreamId = channel.streamId,
            userAgent = ua
        )
    }

    suspend fun fetchMovieDetail(playlist: PlaylistEntity, movie: MediaItemEntity): MovieDetailInfo? {
        if (playlist.type != PlaylistType.XTREAM.name) return null
        val ua = preferences.settingsFlow.first().userAgent
        return xtreamClient.fetchVodInfo(
            baseUrl = playlist.serverUrl,
            username = playlist.username,
            password = playlist.password,
            rawStreamId = movie.streamId,
            userAgent = ua
        )
    }

    suspend fun fetchSeriesDetail(playlist: PlaylistEntity, series: MediaItemEntity): SeriesDetailInfo? {
        if (playlist.type != PlaylistType.XTREAM.name) return null
        val ua = preferences.settingsFlow.first().userAgent
        return xtreamClient.fetchSeriesInfo(
            baseUrl = playlist.serverUrl,
            username = playlist.username,
            password = playlist.password,
            rawSeriesId = series.streamId,
            userAgent = ua
        )
    }

    // --- Settings Delegates ---

    suspend fun setStreamFormat(format: StreamFormat) = preferences.setStreamFormat(format)
    suspend fun setBufferProfile(profile: com.example.data.local.BufferProfile) = preferences.setBufferProfile(profile)
    suspend fun setDefaultAspectRatio(mode: com.example.data.local.AspectRatioMode) = preferences.setDefaultAspectRatio(mode)
    suspend fun setUserAgent(userAgent: String) = preferences.setUserAgent(userAgent)
    suspend fun setSubtitleScale(scale: Float) = preferences.setSubtitleScale(scale)
    suspend fun setAutoPlayPreview(enabled: Boolean) = preferences.setAutoPlayPreview(enabled)
    suspend fun setParentalPin(pin: String) = preferences.setParentalPin(pin)
}
