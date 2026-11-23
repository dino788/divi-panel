package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PlaylistType {
    XTREAM,
    M3U_URL,
    M3U_FILE
}

enum class MediaType {
    LIVE,
    MOVIE,
    SERIES
}

enum class StreamFormat(val ext: String, val label: String) {
    HLS("m3u8", "HLS (.m3u8)"),
    MPEG_TS("ts", "MPEG-TS (.ts)")
}

enum class BufferProfile(
    val label: String,
    val description: String,
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int
) {
    FAST("Fast Zapping", "Low latency channel switching (1.5s–12s)", 1500, 12000, 1000, 2000),
    BALANCED("Balanced", "Recommended for most streams (5s–30s)", 5000, 30000, 2000, 4000),
    ANTI_FREEZE("Anti-Freeze", "Large buffer for unstable networks (12s–60s)", 12000, 60000, 4000, 8000)
}

enum class AspectRatioMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ZOOM("Zoom"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // PlaylistType.name
    val m3uUrl: String = "",
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val epgUrl: String = "",
    val isActive: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val expiryDate: String = "",
    val accountStatus: String = "",
    val activeConnections: String = "",
    val maxConnections: String = "",
    val liveCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0
)

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["playlistId", "mediaType"]),
        Index(value = ["playlistId", "categoryId", "mediaType"])
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val categoryId: String,
    val categoryName: String,
    val mediaType: String, // MediaType.name
    val itemCount: Int = 0,
    val isLocked: Boolean = false
)

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["playlistId", "mediaType"]),
        Index(value = ["playlistId", "mediaType", "categoryId"]),
        Index(value = ["playlistId", "isFavorite"]),
        Index(value = ["playlistId", "lastWatchedAt"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val streamId: String,
    val mediaType: String, // MediaType.name
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val epgChannelId: String = "",
    val rating: String = "",
    val year: String = "",
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val containerExtension: String = "",
    val channelNumber: Int = 0,
    val isFavorite: Boolean = false,
    val lastWatchedAt: Long = 0L,
    val resumePositionMs: Long = 0L,
    val durationMs: Long = 0L
)

data class EpgProgram(
    val id: String,
    val title: String,
    val description: String,
    val startTimeMs: Long,
    val endTimeMs: Long
) {
    fun progressFraction(nowMs: Long = System.currentTimeMillis()): Float {
        if (endTimeMs <= startTimeMs) return 0f
        return ((nowMs - startTimeMs).toFloat() / (endTimeMs - startTimeMs).toFloat()).coerceIn(0f, 1f)
    }
}

data class MovieDetailInfo(
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val duration: String = "",
    val rating: String = "",
    val backdropUrl: String = "",
    val videoCodec: String = "",
    val audioCodec: String = ""
)

data class EpisodeItem(
    val id: String,
    val episodeNum: Int,
    val seasonNum: Int,
    val title: String,
    val streamUrl: String,
    val containerExtension: String = "mp4",
    val thumbnailUrl: String = "",
    val plot: String = "",
    val duration: String = "",
    val rating: String = ""
)

data class SeriesDetailInfo(
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val rating: String = "",
    val backdropUrl: String = "",
    val seasons: Map<Int, List<EpisodeItem>> = emptyMap()
)
