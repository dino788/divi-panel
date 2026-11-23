package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.data.local.AspectRatioMode
import com.example.data.local.BufferProfile
import com.example.data.local.PlayerSettings
import okhttp3.OkHttpClient
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TrackOption(
    val id: String,
    val label: String,
    val language: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val isSelected: Boolean
)

data class StreamTechInfo(
    val resolution: String = "",
    val videoCodec: String = "",
    val audioCodec: String = "",
    val audioChannels: String = "",
    val frameRate: String = "",
    val bitrateKbps: Int = 0
) {
    val shortBadge: String
        get() = listOfNotNull(
            resolution.takeIf { it.isNotBlank() },
            videoCodec.takeIf { it.isNotBlank() },
            audioCodec.takeIf { it.isNotBlank() }
        ).joinToString(" • ")
}

object PlayerEngineFactory {

    @OptIn(UnstableApi::class)
    fun createExoPlayer(
        context: Context,
        settings: PlayerSettings
    ): ExoPlayer {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(settings.userAgent)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        val buffer = settings.bufferProfile
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                buffer.minBufferMs,
                buffer.maxBufferMs,
                buffer.bufferForPlaybackMs,
                buffer.bufferForPlaybackAfterRebufferMs
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
            )
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    fun buildMediaItem(url: String): MediaItem {
        val cleanUrl = url.trim()
        val lower = cleanUrl.lowercase(Locale.US)
        val builder = MediaItem.Builder().setUri(cleanUrl)
        when {
            lower.contains(".m3u8") -> builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            lower.contains(".mpd") -> builder.setMimeType(MimeTypes.APPLICATION_MPD)
            lower.endsWith(".ts") || lower.contains(".ts?") -> builder.setMimeType(MimeTypes.VIDEO_MP2T)
        }
        return builder.build()
    }

    @OptIn(UnstableApi::class)
    fun mapAspectRatioToResizeMode(mode: AspectRatioMode): Int = when (mode) {
        AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
        AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
        AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
    }

    @OptIn(UnstableApi::class)
    fun extractTracks(tracks: Tracks, trackType: Int): List<TrackOption> {
        val options = mutableListOf<TrackOption>()
        tracks.groups.forEachIndexed { groupIdx, group ->
            if (group.type != trackType) return@forEachIndexed
            for (trackIdx in 0 until group.length) {
                if (!group.isTrackSupported(trackIdx)) continue
                val format = group.getTrackFormat(trackIdx)
                val langCode = format.language?.takeIf { it != "und" } ?: ""
                val displayLang = if (langCode.isNotBlank()) {
                    Locale.forLanguageTag(langCode).displayLanguage.replaceFirstChar { it.uppercase() }
                } else ""
                val customLabel = format.label.orEmpty()
                val channels = if (trackType == C.TRACK_TYPE_AUDIO && format.channelCount > 0) {
                    when (format.channelCount) {
                        1 -> "Mono"
                        2 -> "Stereo"
                        6 -> "5.1"
                        8 -> "7.1"
                        else -> "${format.channelCount}ch"
                    }
                } else ""
                val label = buildString {
                    if (customLabel.isNotBlank()) {
                        append(customLabel)
                    } else if (displayLang.isNotBlank()) {
                        append(displayLang)
                    } else {
                        append(if (trackType == C.TRACK_TYPE_AUDIO) "Audio Track ${options.size + 1}" else "Subtitle ${options.size + 1}")
                    }
                    if (channels.isNotBlank()) append(" ($channels)")
                }
                options.add(
                    TrackOption(
                        id = "${groupIdx}_$trackIdx",
                        label = label,
                        language = displayLang,
                        groupIndex = groupIdx,
                        trackIndex = trackIdx,
                        isSelected = group.isTrackSelected(trackIdx)
                    )
                )
            }
        }
        return options
    }

    @OptIn(UnstableApi::class)
    fun selectTrack(player: Player, tracks: Tracks, trackType: Int, option: TrackOption?) {
        val builder = player.trackSelectionParameters.buildUpon()
        if (option == null) {
            // Disable track type (used for Subtitles -> Off)
            builder.setTrackTypeDisabled(trackType, true)
            builder.clearOverridesOfType(trackType)
        } else {
            val group = tracks.groups.getOrNull(option.groupIndex) ?: return
            builder.setTrackTypeDisabled(trackType, false)
            builder.clearOverridesOfType(trackType)
            builder.addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(option.trackIndex)))
        }
        player.trackSelectionParameters = builder.build()
    }

    @OptIn(UnstableApi::class)
    fun extractStreamTechInfo(player: ExoPlayer): StreamTechInfo {
        val vFmt = player.videoFormat
        val aFmt = player.audioFormat
        val resolution = if (vFmt != null && vFmt.width > 0 && vFmt.height > 0) {
            when {
                vFmt.height >= 2160 -> "4K UHD (${vFmt.width}×${vFmt.height})"
                vFmt.height >= 1080 -> "1080p FHD"
                vFmt.height >= 720 -> "720p HD"
                else -> "${vFmt.height}p SD"
            }
        } else ""

        val vCodec = vFmt?.sampleMimeType?.substringAfter('/')?.uppercase(Locale.US)?.replace("AVC", "H.264")
            ?.replace("HEVC", "H.265")
            .orEmpty()
        val aCodec = aFmt?.sampleMimeType?.substringAfter('/')?.uppercase(Locale.US)?.replace("MP4A-LATM", "AAC")
            ?.replace("EAC3", "Dolby E-AC3")
            ?.replace("AC3", "Dolby AC3")
            .orEmpty()
        val aChannels = when (aFmt?.channelCount) {
            1 -> "Mono"
            2 -> "Stereo 2.0"
            6 -> "Surround 5.1"
            8 -> "Surround 7.1"
            else -> ""
        }
        val fps = if (vFmt != null && vFmt.frameRate > 0f) {
            "${vFmt.frameRate.toInt()} fps"
        } else ""
        val bitrate = ((vFmt?.bitrate?.takeIf { it > 0 } ?: 0) + (aFmt?.bitrate?.takeIf { it > 0 } ?: 0)) / 1000

        return StreamTechInfo(
            resolution = resolution,
            videoCodec = vCodec,
            audioCodec = aCodec,
            audioChannels = aChannels,
            frameRate = fps,
            bitrateKbps = bitrate
        )
    }
}
