package com.example.data.network

import com.example.data.local.CategoryEntity
import com.example.data.local.MediaItemEntity
import com.example.data.local.MediaType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

data class M3uParseResult(
    val detectedEpgUrl: String,
    val categories: List<CategoryEntity>,
    val items: List<MediaItemEntity>,
    val liveCount: Int,
    val movieCount: Int,
    val seriesCount: Int
)

object M3uParser {

    private val attrRegex = Regex("""([a-zA-Z0-9_-]+)="([^"]*)"""")
    private val seriesPattern = Regex("""(?i)\bS\d{1,2}\s*E\d{1,3}\b|\bSeason\s*\d+\b""")
    private val yearPattern = Regex("""\((\d{4})\)|\b(19\d{2}|20\d{2})\b""")

    fun parse(
        inputStream: InputStream,
        playlistId: Long,
        existingFavoriteStreamIds: Set<String> = emptySet()
    ): M3uParseResult {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        var detectedEpgUrl = ""

        val items = ArrayList<MediaItemEntity>(1024)
        val categoryCounts = LinkedHashMap<Pair<String, String>, Int>() // (mediaType, categoryName) -> count

        var pendingName = ""
        var pendingLogo = ""
        var pendingGroup = ""
        var pendingEpgId = ""
        var pendingChannelNo = 0
        var hasPendingExtInf = false

        var liveCounter = 0
        var movieCounter = 0
        var seriesCounter = 0

        reader.useLines { sequence ->
            for (rawLine in sequence) {
                val line = rawLine.removePrefix("\uFEFF").trim()
                if (line.isEmpty()) continue

                when {
                    line.startsWith("#EXTM3U", ignoreCase = true) -> {
                        val attrs = extractAttributes(line)
                        detectedEpgUrl = attrs["x-tvg-url"]
                            ?: attrs["url-tvg"]
                            ?: attrs["tvg-url"]
                            ?: ""
                    }

                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        val attrs = extractAttributes(line)
                        val commaIdx = line.lastIndexOf(',')
                        val displayTitle = if (commaIdx != -1 && commaIdx + 1 < line.length) {
                            line.substring(commaIdx + 1).trim()
                        } else {
                            attrs["tvg-name"].orEmpty().trim()
                        }

                        pendingName = displayTitle.ifBlank { attrs["tvg-name"].orEmpty().ifBlank { "Untitled Stream" } }
                        pendingLogo = attrs["tvg-logo"].orEmpty().trim()
                        pendingGroup = attrs["group-title"].orEmpty().trim()
                        pendingEpgId = attrs["tvg-id"].orEmpty().trim()
                        pendingChannelNo = attrs["tvg-chno"]?.toIntOrNull() ?: 0
                        hasPendingExtInf = true
                    }

                    line.startsWith("#EXTGRP:", ignoreCase = true) -> {
                        val grp = line.substringAfter(":").trim()
                        if (grp.isNotEmpty() && pendingGroup.isEmpty()) {
                            pendingGroup = grp
                        }
                    }

                    line.startsWith("#") -> {
                        // Ignore other M3U directives (#EXTVLCOPT, #KODIPROP, etc.)
                    }

                    line.startsWith("http://", ignoreCase = true) ||
                        line.startsWith("https://", ignoreCase = true) ||
                        line.startsWith("rtmp://", ignoreCase = true) ||
                        line.startsWith("rtsp://", ignoreCase = true) -> {
                        if (!hasPendingExtInf) {
                            pendingName = line.substringAfterLast('/').substringBefore('?').ifBlank { "Stream" }
                        }

                        val mediaType = classifyMediaType(line, pendingGroup, pendingName)
                        val cleanGroup = pendingGroup.ifBlank {
                            when (mediaType) {
                                MediaType.LIVE -> "Live Channels"
                                MediaType.MOVIE -> "Movies"
                                MediaType.SERIES -> "Series"
                            }
                        }

                        val chNum = when (mediaType) {
                            MediaType.LIVE -> {
                                liveCounter++
                                if (pendingChannelNo > 0) pendingChannelNo else liveCounter
                            }
                            MediaType.MOVIE -> {
                                movieCounter++
                                movieCounter
                            }
                            MediaType.SERIES -> {
                                seriesCounter++
                                seriesCounter
                            }
                        }

                        val streamKey = "${mediaType.name}_${line.hashCode()}"
                        val extractedYear = if (mediaType != MediaType.LIVE) {
                            yearPattern.find(pendingName)?.groups?.let { g ->
                                g[1]?.value ?: g[2]?.value
                            }.orEmpty()
                        } else ""

                        val ext = line.substringBefore('?').substringAfterLast('.', "").lowercase(Locale.US)
                            .takeIf { it.length in 2..5 } ?: ""

                        items.add(
                            MediaItemEntity(
                                playlistId = playlistId,
                                streamId = streamKey,
                                mediaType = mediaType.name,
                                name = pendingName,
                                categoryId = cleanGroup.lowercase(Locale.US),
                                categoryName = cleanGroup,
                                streamUrl = line,
                                logoUrl = pendingLogo,
                                epgChannelId = pendingEpgId,
                                year = extractedYear,
                                containerExtension = ext,
                                channelNumber = chNum,
                                isFavorite = existingFavoriteStreamIds.contains(streamKey)
                            )
                        )

                        val catKey = mediaType.name to cleanGroup
                        categoryCounts[catKey] = (categoryCounts[catKey] ?: 0) + 1

                        // Reset pending metadata
                        pendingName = ""
                        pendingLogo = ""
                        pendingGroup = ""
                        pendingEpgId = ""
                        pendingChannelNo = 0
                        hasPendingExtInf = false
                    }
                }
            }
        }

        val categories = categoryCounts.entries.map { (key, count) ->
            CategoryEntity(
                playlistId = playlistId,
                categoryId = key.second.lowercase(Locale.US),
                categoryName = key.second,
                mediaType = key.first,
                itemCount = count
            )
        }

        return M3uParseResult(
            detectedEpgUrl = detectedEpgUrl,
            categories = categories,
            items = items,
            liveCount = liveCounter,
            movieCount = movieCounter,
            seriesCount = seriesCounter
        )
    }

    private fun extractAttributes(line: String): Map<String, String> {
        val map = HashMap<String, String>()
        for (match in attrRegex.findAll(line)) {
            val key = match.groupValues[1].lowercase(Locale.US)
            val value = match.groupValues[2]
            map[key] = value
        }
        return map
    }

    private fun classifyMediaType(url: String, group: String, name: String): MediaType {
        val lowerUrl = url.lowercase(Locale.US)
        val lowerGroup = group.lowercase(Locale.US)

        if (lowerUrl.contains("/series/") ||
            seriesPattern.containsMatchIn(name) ||
            lowerGroup.contains("series") ||
            lowerGroup.contains("tv show") ||
            lowerGroup.contains("مسلسل") ||
            lowerGroup.contains("séries")
        ) {
            return MediaType.SERIES
        }

        if (lowerUrl.contains("/movie/") ||
            lowerUrl.endsWith(".mp4") ||
            lowerUrl.endsWith(".mkv") ||
            lowerUrl.endsWith(".avi") ||
            lowerUrl.endsWith(".mov") ||
            lowerGroup.contains("vod") ||
            lowerGroup.contains("movie") ||
            lowerGroup.contains("film") ||
            lowerGroup.contains("cinema") ||
            lowerGroup.contains("أفلام")
        ) {
            return MediaType.MOVIE
        }

        return MediaType.LIVE
    }
}
