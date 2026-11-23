package com.example.data.network

import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import com.example.data.local.CategoryEntity
import com.example.data.local.EpgProgram
import com.example.data.local.EpisodeItem
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import com.example.data.local.MovieDetailInfo
import com.example.data.local.SeriesDetailInfo
import com.example.data.local.StreamFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class XtreamAccountInfo(
    val normalizedServerUrl: String,
    val status: String,
    val expiryDateFormatted: String,
    val activeConnections: String,
    val maxConnections: String,
    val epgUrl: String
)

class XtreamApiClient(
    private val httpClient: OkHttpClient = defaultClient()
) {

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()

        fun normalizeServerUrl(rawUrl: String): String {
            var cleaned = rawUrl.trim()
            if (cleaned.isEmpty()) return ""
            if (!cleaned.startsWith("http://", ignoreCase = true) &&
                !cleaned.startsWith("https://", ignoreCase = true)
            ) {
                cleaned = "http://$cleaned"
            }
            // Strip accidental /player_api.php or /get.php suffix if user pasted full URL
            cleaned = cleaned.substringBefore("/player_api.php")
                .substringBefore("/get.php")
                .substringBefore("/c/")
            return cleaned.trimEnd('/')
        }
    }

    private fun buildApiUrl(
        baseUrl: String,
        username: String,
        password: String,
        action: String? = null,
        extraParams: Map<String, String> = emptyMap()
    ): String {
        val parsed = "$baseUrl/player_api.php".toHttpUrlOrNull()
            ?: throw IOException("Invalid server URL: $baseUrl")
        val builder = parsed.newBuilder()
            .addQueryParameter("username", username)
            .addQueryParameter("password", password)
        if (!action.isNullOrBlank()) {
            builder.addQueryParameter("action", action)
        }
        extraParams.forEach { (k, v) -> builder.addQueryParameter(k, v) }
        return builder.build().toString()
    }

    private fun executeRequest(url: String, userAgent: String): Response {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json, text/plain, */*")
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            throw IOException("HTTP $code")
        }
        return response
    }

    private suspend fun getRaw(url: String, userAgent: String): String = withContext(Dispatchers.IO) {
        executeRequest(url, userAgent).use { response ->
            response.body?.string().orEmpty().removePrefix("\uFEFF")
        }
    }

    /**
     * Opens a streaming JsonReader that automatically strips UTF-8 BOM (EF BB BF)
     * and handles both JSON arrays `[...]` and JSON object maps `{"0": ...}` using <2MB RAM
     * even on 100MB+ IPTV catalogs.
     */
    private fun openStreamingJsonReader(rawStream: InputStream): JsonReader {
        val pb = PushbackInputStream(BufferedInputStream(rawStream, 64 * 1024), 3)
        val bom = ByteArray(3)
        val read = pb.read(bom, 0, 3)
        if (read == 3) {
            val isUtf8Bom = (bom[0].toInt() and 0xFF) == 0xEF &&
                (bom[1].toInt() and 0xFF) == 0xBB &&
                (bom[2].toInt() and 0xFF) == 0xBF
            if (!isUtf8Bom) {
                pb.unread(bom, 0, 3)
            }
        } else if (read > 0) {
            pb.unread(bom, 0, read)
        }
        return JsonReader(BufferedReader(InputStreamReader(pb, Charsets.UTF_8), 64 * 1024)).apply {
            isLenient = true
        }
    }

    /**
     * Iterates over objects in either a top-level JSON Array `[ {...}, {...} ]`
     * OR a top-level JSON Object map `{ "0": {...}, "1": {...} }` (common on PHP/XUI.ONE servers).
     */
    private inline fun forEachJsonObjectEntry(
        reader: JsonReader,
        onObject: (JsonReader) -> Unit
    ) {
        val firstToken = try {
            reader.peek()
        } catch (_: Exception) {
            return
        }
        when (firstToken) {
            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        onObject(reader)
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endArray()
            }

            JsonToken.BEGIN_OBJECT -> {
                reader.beginObject()
                while (reader.hasNext()) {
                    reader.nextName()
                    if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                        onObject(reader)
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }

            else -> Unit
        }
    }

    private fun JsonReader.nextStringSafe(): String {
        return when (peek()) {
            JsonToken.NULL -> {
                nextNull()
                ""
            }
            JsonToken.STRING -> nextString().cleanNull()
            JsonToken.NUMBER -> nextString().cleanNull()
            JsonToken.BOOLEAN -> nextBoolean().toString()
            else -> {
                skipValue()
                ""
            }
        }
    }

    private fun JsonReader.nextIntSafe(default: Int = 0): Int {
        return when (peek()) {
            JsonToken.NULL -> {
                nextNull()
                default
            }
            JsonToken.NUMBER -> try {
                nextInt()
            } catch (_: Exception) {
                nextDouble().toInt()
            }
            JsonToken.STRING -> nextString().trim().toIntOrNull() ?: default
            else -> {
                skipValue()
                default
            }
        }
    }

    private fun JsonReader.nextFirstArrayString(): String {
        return when (peek()) {
            JsonToken.BEGIN_ARRAY -> {
                var first = ""
                beginArray()
                while (hasNext()) {
                    val value = nextStringSafe()
                    if (first.isEmpty() && value.isNotEmpty()) first = value
                }
                endArray()
                first
            }
            else -> nextStringSafe()
        }
    }

    suspend fun authenticate(
        rawServerUrl: String,
        username: String,
        password: String,
        userAgent: String
    ): XtreamAccountInfo = withContext(Dispatchers.IO) {
        val baseUrl = normalizeServerUrl(rawServerUrl)
        val url = buildApiUrl(baseUrl, username, password)
        val body = executeRequest(url, userAgent).use { response ->
            response.body?.string().orEmpty().removePrefix("\uFEFF")
        }
        if (body.isBlank()) throw IOException("Empty response from server")

        val root = JSONObject(body)
        val userInfo = root.optJSONObject("user_info")
            ?: throw IOException("Invalid Xtream response — check server URL and credentials")

        val authFlag = userInfo.optInt("auth", 0)
        if (authFlag == 0) {
            val message = userInfo.optString("message", "Authentication failed. Check username or password.")
            throw IOException(message.ifBlank { "Authentication failed" })
        }

        // Preserve user's working URL, but also respect server_info protocol/port if valid
        val status = userInfo.optString("status", "Active").cleanNull().ifBlank { "Active" }
        val expRaw = userInfo.optString("exp_date", "")
        val expiryFormatted = formatUnixExpiry(expRaw)
        val activeCons = userInfo.optString("active_cons", "0").cleanNull().ifBlank { "0" }
        val maxCons = userInfo.optString("max_connections", "1").cleanNull().ifBlank { "1" }
        val epgUrl = "$baseUrl/xmltv.php?username=$username&password=$password"

        XtreamAccountInfo(
            normalizedServerUrl = baseUrl,
            status = status,
            expiryDateFormatted = expiryFormatted,
            activeConnections = activeCons,
            maxConnections = maxCons,
            epgUrl = epgUrl
        )
    }

    suspend fun fetchCategories(
        baseUrl: String,
        username: String,
        password: String,
        playlistId: Long,
        mediaType: MediaType,
        userAgent: String
    ): List<CategoryEntity> = withContext(Dispatchers.IO) {
        val action = when (mediaType) {
            MediaType.LIVE -> "get_live_categories"
            MediaType.MOVIE -> "get_vod_categories"
            MediaType.SERIES -> "get_series_categories"
        }
        val result = ArrayList<CategoryEntity>(128)
        runCatching {
            executeRequest(buildApiUrl(baseUrl, username, password, action), userAgent).use { response ->
                val stream = response.body?.byteStream() ?: return@use
                openStreamingJsonReader(stream).use { reader ->
                    forEachJsonObjectEntry(reader) { objReader ->
                        var catId = ""
                        var catName = ""
                        objReader.beginObject()
                        while (objReader.hasNext()) {
                            when (objReader.nextName()) {
                                "category_id" -> catId = objReader.nextStringSafe()
                                "category_name" -> catName = objReader.nextStringSafe()
                                else -> objReader.skipValue()
                            }
                        }
                        objReader.endObject()
                        if (catId.isNotEmpty()) {
                            result.add(
                                CategoryEntity(
                                    playlistId = playlistId,
                                    categoryId = catId,
                                    categoryName = catName.ifBlank { "General" },
                                    mediaType = mediaType.name
                                )
                            )
                        }
                    }
                }
            }
        }
        result
    }

    suspend fun fetchLiveStreams(
        baseUrl: String,
        username: String,
        password: String,
        playlistId: Long,
        categoryMap: Map<String, String>,
        streamFormat: StreamFormat,
        existingFavorites: Set<String>,
        userAgent: String
    ): Pair<List<MediaItemEntity>, String?> = withContext(Dispatchers.IO) {
        val result = ArrayList<MediaItemEntity>(2048)
        val ext = streamFormat.ext
        var index = 0
        val error = runCatching {
            executeRequest(buildApiUrl(baseUrl, username, password, "get_live_streams"), userAgent).use { response ->
                val stream = response.body?.byteStream() ?: throw IOException("Empty live stream response")
                openStreamingJsonReader(stream).use { reader ->
                    forEachJsonObjectEntry(reader) { objReader ->
                        var streamId = ""
                        var name = ""
                        var catId = ""
                        var catIdsFirst = ""
                        var logo = ""
                        var epgId = ""
                        var num = 0
                        var directSource = ""

                        objReader.beginObject()
                        while (objReader.hasNext()) {
                            when (objReader.nextName()) {
                                "stream_id" -> streamId = objReader.nextStringSafe()
                                "name" -> name = objReader.nextStringSafe()
                                "category_id" -> catId = objReader.nextStringSafe()
                                "category_ids" -> catIdsFirst = objReader.nextFirstArrayString()
                                "stream_icon" -> logo = objReader.nextStringSafe()
                                "epg_channel_id" -> epgId = objReader.nextStringSafe()
                                "num" -> num = objReader.nextIntSafe(0)
                                "direct_source" -> directSource = objReader.nextStringSafe()
                                else -> objReader.skipValue()
                            }
                        }
                        objReader.endObject()

                        if (streamId.isNotEmpty()) {
                            index++
                            val resolvedCatId = catId.ifBlank { catIdsFirst.ifBlank { "0" } }
                            val catName = categoryMap[resolvedCatId] ?: "Live Channels"
                            val finalUrl = if (directSource.startsWith("http://", true) ||
                                directSource.startsWith("https://", true)
                            ) {
                                directSource
                            } else {
                                "$baseUrl/live/$username/$password/$streamId.$ext"
                            }
                            val uniqueStreamId = "LIVE_$streamId"

                            result.add(
                                MediaItemEntity(
                                    playlistId = playlistId,
                                    streamId = uniqueStreamId,
                                    mediaType = MediaType.LIVE.name,
                                    name = name.ifBlank { "Channel $index" },
                                    categoryId = resolvedCatId,
                                    categoryName = catName,
                                    streamUrl = finalUrl,
                                    logoUrl = logo,
                                    epgChannelId = epgId,
                                    containerExtension = ext,
                                    channelNumber = if (num > 0) num else index,
                                    isFavorite = existingFavorites.contains(uniqueStreamId)
                                )
                            )
                        }
                    }
                }
            }
        }.exceptionOrNull()?.message

        result to error
    }

    suspend fun fetchVodStreams(
        baseUrl: String,
        username: String,
        password: String,
        playlistId: Long,
        categoryMap: Map<String, String>,
        existingFavorites: Set<String>,
        userAgent: String
    ): Pair<List<MediaItemEntity>, String?> = withContext(Dispatchers.IO) {
        val result = ArrayList<MediaItemEntity>(2048)
        var index = 0
        val error = runCatching {
            executeRequest(buildApiUrl(baseUrl, username, password, "get_vod_streams"), userAgent).use { response ->
                val stream = response.body?.byteStream() ?: throw IOException("Empty movies response")
                openStreamingJsonReader(stream).use { reader ->
                    forEachJsonObjectEntry(reader) { objReader ->
                        var streamId = ""
                        var name = ""
                        var catId = ""
                        var catIdsFirst = ""
                        var logo = ""
                        var ext = "mp4"
                        var rating = ""
                        var year = ""
                        var directSource = ""

                        objReader.beginObject()
                        while (objReader.hasNext()) {
                            when (objReader.nextName()) {
                                "stream_id" -> streamId = objReader.nextStringSafe()
                                "name" -> name = objReader.nextStringSafe()
                                "category_id" -> catId = objReader.nextStringSafe()
                                "category_ids" -> catIdsFirst = objReader.nextFirstArrayString()
                                "stream_icon" -> logo = objReader.nextStringSafe()
                                "container_extension" -> ext = objReader.nextStringSafe().ifBlank { "mp4" }
                                "rating" -> rating = objReader.nextStringSafe()
                                "year" -> year = objReader.nextStringSafe()
                                "direct_source" -> directSource = objReader.nextStringSafe()
                                else -> objReader.skipValue()
                            }
                        }
                        objReader.endObject()

                        if (streamId.isNotEmpty()) {
                            index++
                            val resolvedCatId = catId.ifBlank { catIdsFirst.ifBlank { "0" } }
                            val catName = categoryMap[resolvedCatId] ?: "Movies"
                            val finalUrl = if (directSource.startsWith("http://", true) ||
                                directSource.startsWith("https://", true)
                            ) {
                                directSource
                            } else {
                                "$baseUrl/movie/$username/$password/$streamId.$ext"
                            }
                            val uniqueStreamId = "MOVIE_$streamId"

                            result.add(
                                MediaItemEntity(
                                    playlistId = playlistId,
                                    streamId = uniqueStreamId,
                                    mediaType = MediaType.MOVIE.name,
                                    name = name.ifBlank { "Movie $index" },
                                    categoryId = resolvedCatId,
                                    categoryName = catName,
                                    streamUrl = finalUrl,
                                    logoUrl = logo,
                                    rating = rating.takeIf { it != "0" && it != "0.0" }.orEmpty(),
                                    year = year.take(4),
                                    containerExtension = ext,
                                    channelNumber = index,
                                    isFavorite = existingFavorites.contains(uniqueStreamId)
                                )
                            )
                        }
                    }
                }
            }
        }.exceptionOrNull()?.message

        result to error
    }

    suspend fun fetchSeries(
        baseUrl: String,
        username: String,
        password: String,
        playlistId: Long,
        categoryMap: Map<String, String>,
        existingFavorites: Set<String>,
        userAgent: String
    ): Pair<List<MediaItemEntity>, String?> = withContext(Dispatchers.IO) {
        val result = ArrayList<MediaItemEntity>(1024)
        var index = 0
        val error = runCatching {
            executeRequest(buildApiUrl(baseUrl, username, password, "get_series"), userAgent).use { response ->
                val stream = response.body?.byteStream() ?: throw IOException("Empty series response")
                openStreamingJsonReader(stream).use { reader ->
                    forEachJsonObjectEntry(reader) { objReader ->
                        var seriesId = ""
                        var name = ""
                        var catId = ""
                        var catIdsFirst = ""
                        var cover = ""
                        var plot = ""
                        var cast = ""
                        var director = ""
                        var genre = ""
                        var rating = ""
                        var releaseDate = ""

                        objReader.beginObject()
                        while (objReader.hasNext()) {
                            when (objReader.nextName()) {
                                "series_id" -> seriesId = objReader.nextStringSafe()
                                "name" -> name = objReader.nextStringSafe()
                                "category_id" -> catId = objReader.nextStringSafe()
                                "category_ids" -> catIdsFirst = objReader.nextFirstArrayString()
                                "cover" -> cover = objReader.nextStringSafe()
                                "plot" -> plot = objReader.nextStringSafe()
                                "cast" -> cast = objReader.nextStringSafe()
                                "director" -> director = objReader.nextStringSafe()
                                "genre" -> genre = objReader.nextStringSafe()
                                "rating" -> rating = objReader.nextStringSafe()
                                "releaseDate", "release_date", "year" -> {
                                    val v = objReader.nextStringSafe()
                                    if (releaseDate.isEmpty()) releaseDate = v
                                }
                                else -> objReader.skipValue()
                            }
                        }
                        objReader.endObject()

                        if (seriesId.isNotEmpty()) {
                            index++
                            val resolvedCatId = catId.ifBlank { catIdsFirst.ifBlank { "0" } }
                            val catName = categoryMap[resolvedCatId] ?: "Series"
                            val uniqueStreamId = "SERIES_$seriesId"

                            result.add(
                                MediaItemEntity(
                                    playlistId = playlistId,
                                    streamId = uniqueStreamId,
                                    mediaType = MediaType.SERIES.name,
                                    name = name.ifBlank { "Series $index" },
                                    categoryId = resolvedCatId,
                                    categoryName = catName,
                                    streamUrl = seriesId,
                                    logoUrl = cover,
                                    rating = rating.takeIf { it != "0" && it != "0.0" }.orEmpty(),
                                    year = releaseDate.take(4),
                                    plot = plot,
                                    cast = cast,
                                    director = director,
                                    genre = genre,
                                    channelNumber = index,
                                    isFavorite = existingFavorites.contains(uniqueStreamId)
                                )
                            )
                        }
                    }
                }
            }
        }.exceptionOrNull()?.message

        result to error
    }

    suspend fun fetchVodInfo(
        baseUrl: String,
        username: String,
        password: String,
        rawStreamId: String,
        userAgent: String
    ): MovieDetailInfo? {
        val numericId = rawStreamId.removePrefix("MOVIE_")
        val body = runCatching {
            getRaw(
                buildApiUrl(
                    baseUrl, username, password, "get_vod_info",
                    mapOf("vod_id" to numericId)
                ),
                userAgent
            )
        }.getOrNull().orEmpty()
        if (body.isBlank() || !body.trimStart().startsWith("{")) return null

        return runCatching {
            val root = JSONObject(body)
            val info = root.optJSONObject("info") ?: return null
            val backdrops = info.optJSONArray("backdrop_path")
            val backdrop = if (backdrops != null && backdrops.length() > 0) {
                backdrops.optString(0, "")
            } else {
                info.optString("movie_image", "")
            }
            val videoObj = info.optJSONObject("video")
            val audioObj = info.optJSONObject("audio")

            MovieDetailInfo(
                plot = info.optString("plot", info.optString("description", "")).cleanNull(),
                cast = info.optString("cast", info.optString("actors", "")).cleanNull(),
                director = info.optString("director", "").cleanNull(),
                genre = info.optString("genre", "").cleanNull(),
                releaseDate = info.optString("releasedate", info.optString("release_date", "")).cleanNull(),
                duration = info.optString("duration", "").cleanNull(),
                rating = info.optString("rating", "").cleanNull(),
                backdropUrl = backdrop.cleanNull(),
                videoCodec = videoObj?.optString("codec_name", "")?.uppercase(Locale.US).orEmpty(),
                audioCodec = audioObj?.optString("codec_name", "")?.uppercase(Locale.US).orEmpty()
            )
        }.getOrNull()
    }

    suspend fun fetchSeriesInfo(
        baseUrl: String,
        username: String,
        password: String,
        rawSeriesId: String,
        userAgent: String
    ): SeriesDetailInfo? {
        val numericId = rawSeriesId.removePrefix("SERIES_")
        val body = runCatching {
            getRaw(
                buildApiUrl(
                    baseUrl, username, password, "get_series_info",
                    mapOf("series_id" to numericId)
                ),
                userAgent
            )
        }.getOrNull().orEmpty()
        if (body.isBlank() || !body.trimStart().startsWith("{")) return null

        return runCatching {
            val root = JSONObject(body)
            val info = root.optJSONObject("info")
            val backdrops = info?.optJSONArray("backdrop_path")
            val backdrop = if (backdrops != null && backdrops.length() > 0) {
                backdrops.optString(0, "")
            } else {
                info?.optString("cover", "").orEmpty()
            }

            val episodesObj = root.optJSONObject("episodes")
            val seasonsMap = sortedMapOf<Int, List<EpisodeItem>>()

            if (episodesObj != null) {
                val keys = episodesObj.keys()
                while (keys.hasNext()) {
                    val seasonKey = keys.next()
                    val seasonNum = seasonKey.toIntOrNull() ?: 1
                    val epArray = episodesObj.optJSONArray(seasonKey) ?: continue
                    val episodes = ArrayList<EpisodeItem>(epArray.length())

                    for (i in 0 until epArray.length()) {
                        val ep = epArray.optJSONObject(i) ?: continue
                        val epId = ep.optString("id", "").trim()
                        if (epId.isEmpty()) continue
                        val epNum = ep.optInt("episode_num", i + 1)
                        val title = ep.optString("title", "Episode $epNum").cleanNull().ifBlank { "Episode $epNum" }
                        val ext = ep.optString("container_extension", "mp4").cleanNull().ifBlank { "mp4" }
                        val epInfo = ep.optJSONObject("info")
                        val thumb = epInfo?.optString("movie_image", "")?.cleanNull().orEmpty()
                        val plot = epInfo?.optString("plot", "")?.cleanNull().orEmpty()
                        val duration = epInfo?.optString("duration", "")?.cleanNull().orEmpty()
                        val rating = epInfo?.optString("rating", "")?.cleanNull().orEmpty()
                        val streamUrl = "$baseUrl/series/$username/$password/$epId.$ext"

                        episodes.add(
                            EpisodeItem(
                                id = epId,
                                episodeNum = epNum,
                                seasonNum = seasonNum,
                                title = title,
                                streamUrl = streamUrl,
                                containerExtension = ext,
                                thumbnailUrl = thumb,
                                plot = plot,
                                duration = duration,
                                rating = rating
                            )
                        )
                    }
                    if (episodes.isNotEmpty()) {
                        seasonsMap[seasonNum] = episodes.sortedBy { it.episodeNum }
                    }
                }
            }

            SeriesDetailInfo(
                plot = info?.optString("plot", "")?.cleanNull().orEmpty(),
                cast = info?.optString("cast", "")?.cleanNull().orEmpty(),
                director = info?.optString("director", "")?.cleanNull().orEmpty(),
                genre = info?.optString("genre", "")?.cleanNull().orEmpty(),
                releaseDate = info?.optString("releaseDate", "")?.cleanNull().orEmpty(),
                rating = info?.optString("rating", "")?.cleanNull().orEmpty(),
                backdropUrl = backdrop.cleanNull(),
                seasons = seasonsMap
            )
        }.getOrNull()
    }

    suspend fun fetchShortEpg(
        baseUrl: String,
        username: String,
        password: String,
        rawStreamId: String,
        userAgent: String
    ): List<EpgProgram> {
        val numericId = rawStreamId.removePrefix("LIVE_")
        if (numericId.toLongOrNull() == null) return emptyList()

        val body = runCatching {
            getRaw(
                buildApiUrl(
                    baseUrl, username, password, "get_short_epg",
                    mapOf("stream_id" to numericId, "limit" to "6")
                ),
                userAgent
            )
        }.getOrNull().orEmpty()

        if (body.isBlank() || !body.trimStart().startsWith("{")) return emptyList()

        return runCatching {
            val root = JSONObject(body)
            val listings = root.optJSONArray("epg_listings") ?: return emptyList()
            val result = ArrayList<EpgProgram>(listings.length())

            for (i in 0 until listings.length()) {
                val item = listings.optJSONObject(i) ?: continue
                val id = item.optString("id", "$i")
                val title = decodeBase64Safe(item.optString("title", ""))
                val desc = decodeBase64Safe(item.optString("description", ""))
                val startSec = item.optString("start_timestamp", "").toLongOrNull() ?: 0L
                val stopSec = item.optString("stop_timestamp", "").toLongOrNull() ?: 0L

                if (title.isNotBlank()) {
                    result.add(
                        EpgProgram(
                            id = id,
                            title = title,
                            description = desc,
                            startTimeMs = startSec * 1000L,
                            endTimeMs = stopSec * 1000L
                        )
                    )
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    private fun decodeBase64Safe(raw: String): String {
        if (raw.isBlank() || raw == "null") return ""
        return try {
            val decoded = Base64.decode(raw, Base64.DEFAULT)
            String(decoded, Charsets.UTF_8).trim()
        } catch (_: IllegalArgumentException) {
            raw.trim()
        }
    }

    private fun formatUnixExpiry(raw: String): String {
        if (raw.isBlank() || raw == "null") return "Unlimited"
        val epochSeconds = raw.toLongOrNull() ?: return raw
        if (epochSeconds <= 0L) return "Unlimited"
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(epochSeconds * 1000L))
    }

    private fun String.cleanNull(): String = if (this.equals("null", ignoreCase = true)) "" else this.trim()
}
