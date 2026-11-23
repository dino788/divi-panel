package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.KeyEvent as AndroidKeyEvent
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.local.AspectRatioMode
import com.example.data.local.EpgProgram
import com.example.data.local.MediaItemEntity
import com.example.data.local.PlayerSettings
import com.example.player.PlayerEngineFactory
import com.example.player.StreamTechInfo
import com.example.player.TrackOption
import com.example.ui.components.LiveClockText
import com.example.ui.components.formatDurationMs
import com.example.ui.components.formatTimeHHmm
import com.example.ui.components.tvFocusRing
import com.example.ui.components.tvFocusable
import com.example.ui.theme.DiviCyan
import com.example.ui.theme.DiviViolet
import com.example.ui.theme.LiveRed
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceHigh
import com.example.ui.theme.StarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.PlaybackSession
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    session: PlaybackSession,
    settings: PlayerSettings,
    epgPrograms: List<EpgProgram>,
    onExit: () -> Unit,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onSwitchChannel: (MediaItemEntity) -> Unit,
    onZapOffset: (Int) -> Unit,
    onNextEpisode: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val exoPlayer = remember(settings.userAgent, settings.bufferProfile) {
        PlayerEngineFactory.createExoPlayer(context, settings)
    }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPosMs by remember { mutableLongStateOf(session.resumePositionMs) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedPercent by remember { mutableIntStateOf(0) }
    var aspectMode by remember { mutableStateOf(settings.defaultAspectRatio) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    var showControls by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var showChannelDrawer by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(true) }

    var currentTracks by remember { mutableStateOf(Tracks.EMPTY) }
    var audioTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackOption>>(emptyList()) }
    var subtitlesDisabled by remember { mutableStateOf(false) }
    var techInfo by remember { mutableStateOf(StreamTechInfo()) }

    var toastNotice by remember { mutableStateOf("") }
    var gestureHudVisible by remember { mutableStateOf(false) }
    var gestureIsBrightness by remember { mutableStateOf(true) }
    var gestureLevel by remember { mutableFloatStateOf(0.5f) }
    var lastInteractionTick by remember { mutableLongStateOf(0L) }

    val playerRootFocus = remember { FocusRequester() }
    val playPauseFocus = remember { FocusRequester() }

    LaunchedEffect(showControls, showChannelDrawer) {
        if (showControls && !isLocked && !showChannelDrawer) {
            runCatching { playPauseFocus.requestFocus() }
        } else if (!showControls && !showChannelDrawer) {
            runCatching { playerRootFocus.requestFocus() }
        }
    }

    // Fullscreen immersive bars & wake lock + landscape entry (authentic IBO Player)
    DisposableEffect(activity) {
        val window = activity?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            if (!session.isLive && pos > 3000L) {
                onSaveProgress(pos, dur)
            }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoPlayer.release()
        }
    }

    // Back handler inside player
    BackHandler {
        when {
            showChannelDrawer -> showChannelDrawer = false
            isLocked -> {
                toastNotice = "Screen is locked — tap unlock icon"
            }
            else -> onExit()
        }
    }

    // Lifecycle pause/resume & Player Listener
    DisposableEffect(exoPlayer, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    errorMessage = null
                    durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    techInfo = PlayerEngineFactory.extractStreamTechInfo(exoPlayer)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                currentTracks = tracks
                audioTracks = PlayerEngineFactory.extractTracks(tracks, C.TRACK_TYPE_AUDIO)
                subtitleTracks = PlayerEngineFactory.extractTracks(tracks, C.TRACK_TYPE_TEXT)
                techInfo = PlayerEngineFactory.extractStreamTechInfo(exoPlayer)
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                errorMessage = error.errorCodeName.replace("ERROR_CODE_", "").replace('_', ' ') +
                    (error.cause?.localizedMessage?.let { " — $it" } ?: "")
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (errorMessage == null) exoPlayer.play()
                else -> Unit
            }
        }
        exoPlayer.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
        }
    }

    // Load media whenever streamUrl changes (e.g. zapping channels or next episode)
    LaunchedEffect(session.streamUrl) {
        errorMessage = null
        isBuffering = true
        techInfo = StreamTechInfo()
        val mediaItem = PlayerEngineFactory.buildMediaItem(session.streamUrl)
        exoPlayer.setMediaItem(mediaItem)
        if (!session.isLive && session.resumePositionMs > 5000L) {
            exoPlayer.seekTo(session.resumePositionMs)
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Poll playback position & periodically persist VOD progress
    LaunchedEffect(exoPlayer, session.streamUrl) {
        var tickCount = 0
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            bufferedPercent = exoPlayer.bufferedPercentage
            if (!session.isLive && tickCount % 10 == 0 && currentPosMs > 5000L) {
                onSaveProgress(currentPosMs, durationMs)
            }
            tickCount++
            delay(500L)
        }
    }

    // Auto-hide controls after 4.5 seconds when playing (resets on any remote key press)
    LaunchedEffect(showControls, isPlaying, showChannelDrawer, isLocked, lastInteractionTick) {
        if (showControls && isPlaying && !showChannelDrawer && !isLocked) {
            delay(4500L)
            showControls = false
        }
    }

    // Auto-clear toast pill
    LaunchedEffect(toastNotice) {
        if (toastNotice.isNotEmpty()) {
            delay(1800L)
            toastNotice = ""
        }
    }

    // Auto-hide brightness/volume HUD
    LaunchedEffect(gestureHudVisible, gestureLevel) {
        if (gestureHudVisible) {
            delay(1100L)
            gestureHudVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerRootFocus)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                lastInteractionTick++
                val code = keyEvent.nativeKeyEvent.keyCode

                // Global media remote keys always work
                when (code) {
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        showControls = true
                        return@onPreviewKeyEvent true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> {
                        exoPlayer.play()
                        showControls = true
                        return@onPreviewKeyEvent true
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> {
                        exoPlayer.pause()
                        showControls = true
                        return@onPreviewKeyEvent true
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_UP -> {
                        if (session.isLive) {
                            onZapOffset(1)
                            toastNotice = "Next Channel"
                            return@onPreviewKeyEvent true
                        }
                    }
                    AndroidKeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        if (session.isLive) {
                            onZapOffset(-1)
                            toastNotice = "Previous Channel"
                            return@onPreviewKeyEvent true
                        }
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                        if (!session.isLive) {
                            val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs.coerceAtLeast(1L))
                            exoPlayer.seekTo(target)
                            toastNotice = "+10s"
                            return@onPreviewKeyEvent true
                        }
                    }
                    AndroidKeyEvent.KEYCODE_MEDIA_REWIND -> {
                        if (!session.isLive) {
                            val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(target)
                            toastNotice = "-10s"
                            return@onPreviewKeyEvent true
                        }
                    }
                }

                // When controls are HIDDEN and drawer is closed, D-Pad acts as instant IBO TV Remote shortcuts!
                if (!showControls && !showChannelDrawer && !isLocked) {
                    when (code) {
                        AndroidKeyEvent.KEYCODE_DPAD_CENTER,
                        AndroidKeyEvent.KEYCODE_ENTER,
                        AndroidKeyEvent.KEYCODE_MENU -> {
                            showControls = true
                            return@onPreviewKeyEvent true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_UP -> {
                            if (session.isLive) {
                                onZapOffset(1)
                                toastNotice = "Next Channel"
                            } else {
                                showControls = true
                            }
                            return@onPreviewKeyEvent true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_DOWN -> {
                            if (session.isLive) {
                                onZapOffset(-1)
                                toastNotice = "Previous Channel"
                            } else {
                                showControls = true
                            }
                            return@onPreviewKeyEvent true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (session.isLive && session.channelList.isNotEmpty()) {
                                showChannelDrawer = true
                            } else if (!session.isLive) {
                                val target = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(target)
                                toastNotice = "-10s"
                            }
                            return@onPreviewKeyEvent true
                        }
                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (!session.isLive) {
                                val target = (exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs.coerceAtLeast(1L))
                                exoPlayer.seekTo(target)
                                toastNotice = "+10s"
                            } else {
                                showControls = true
                            }
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            }
    ) {
        // Video Surface (Media3 PlayerView)
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    resizeMode = PlayerEngineFactory.mapAspectRatioToResizeMode(aspectMode)
                }
            },
            update = { view ->
                if (view.player !== exoPlayer) view.player = exoPlayer
                view.resizeMode = PlayerEngineFactory.mapAspectRatioToResizeMode(aspectMode)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture Layer (Tap, Double-Tap, Vertical Brightness / Volume Swipes)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked, session.isLive) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            if (isLocked) return@detectTapGestures
                            val isLeft = offset.x < size.width / 2f
                            if (session.isLive) {
                                onZapOffset(if (isLeft) -1 else 1)
                                toastNotice = if (isLeft) "Previous Channel" else "Next Channel"
                            } else {
                                val delta = if (isLeft) -10_000L else 10_000L
                                val target = (exoPlayer.currentPosition + delta).coerceIn(0L, durationMs.coerceAtLeast(1L))
                                exoPlayer.seekTo(target)
                                toastNotice = if (isLeft) "-10s" else "+10s"
                            }
                        }
                    )
                }
                .pointerInput(isLocked) {
                    detectVerticalDragGestures { change, dragAmount ->
                        if (isLocked) return@detectVerticalDragGestures
                        change.consume()
                        val isLeftSide = change.position.x < size.width / 2f
                        val deltaFraction = -dragAmount / (size.height.coerceAtLeast(1) * 0.7f)

                        if (isLeftSide && activity != null) {
                            val lp = activity.window.attributes
                            val currentBright = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                            val nextBright = (currentBright + deltaFraction).coerceIn(0.05f, 1.0f)
                            lp.screenBrightness = nextBright
                            activity.window.attributes = lp
                            gestureIsBrightness = true
                            gestureLevel = nextBright
                            gestureHudVisible = true
                        } else {
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val nextVolFloat = ((curVol.toFloat() / maxVol) + deltaFraction).coerceIn(0f, 1f)
                            val nextVolInt = (nextVolFloat * maxVol).toInt().coerceIn(0, maxVol)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVolInt, 0)
                            gestureIsBrightness = false
                            gestureLevel = nextVolFloat
                            gestureHudVisible = true
                        }
                    }
                }
        )

        // Buffering Indicator
        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(
                color = DiviCyan,
                strokeWidth = 3.5.dp,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }

        // Stream Error Recovery Card
        if (errorMessage != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                color = ObsidianDeep.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Playback Interrupted",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                errorMessage = null
                                isBuffering = true
                                exoPlayer.setMediaItem(PlayerEngineFactory.buildMediaItem(session.streamUrl))
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            },
                            modifier = Modifier.tvFocusRing(RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = DiviCyan, contentColor = ObsidianDeep),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.player_retry))
                        }
                        if (session.isLive && session.channelList.size > 1) {
                            Button(
                                onClick = { showChannelDrawer = true },
                                modifier = Modifier.tvFocusRing(RoundedCornerShape(12.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated, contentColor = TextPrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.player_channels))
                            }
                        }
                    }
                }
            }
        }

        // Gesture Brightness / Volume HUD
        AnimatedVisibility(
            visible = gestureHudVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            GestureHudIndicator(isBrightness = gestureIsBrightness, level = gestureLevel)
        }

        // Toast Pill (Aspect ratio, seek, zap)
        AnimatedVisibility(
            visible = toastNotice.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 74.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ObsidianDeep.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, DiviCyan.copy(alpha = 0.6f))
            ) {
                Text(
                    text = toastNotice,
                    style = MaterialTheme.typography.labelLarge,
                    color = DiviCyan,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // Screen Lock Floating Unlock Pill
        AnimatedVisibility(
            visible = isLocked && showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        isLocked = false
                        showControls = true
                    },
                shape = RoundedCornerShape(16.dp),
                color = ObsidianDeep.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, DiviCyan)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.LockOpen, contentDescription = "Unlock", tint = DiviCyan)
                    Text(stringResource(R.string.player_locked), color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // FULL IBO PLAYER OSD OVERLAY
        AnimatedVisibility(
            visible = showControls && !isLocked && !showChannelDrawer,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to ObsidianDeep.copy(alpha = 0.84f),
                            0.25f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1f to ObsidianDeep.copy(alpha = 0.9f)
                        )
                    )
            ) {
                // TOP OSD BAR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onExit,
                            modifier = Modifier
                                .size(44.dp)
                                .tvFocusRing(CircleShape)
                                .clip(CircleShape)
                                .background(ObsidianDeep.copy(alpha = 0.65f))
                                .testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = TextPrimary
                            )
                        }

                        if (session.logoUrl.isNotBlank()) {
                            SubcomposeAsyncImage(
                                model = session.logoUrl,
                                contentDescription = session.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ObsidianSurfaceElevated)
                                    .padding(3.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = session.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (session.isLive) {
                                    Surface(color = LiveRed, shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = "LIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            val subLine = listOfNotNull(
                                session.subtitle.takeIf { it.isNotBlank() },
                                techInfo.shortBadge.takeIf { it.isNotBlank() }
                            ).joinToString("  •  ")
                            if (subLine.isNotBlank()) {
                                Text(
                                    text = subLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DiviCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (session.mediaItemId != null) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(42.dp)
                                    .tvFocusRing(CircleShape)
                                    .clip(CircleShape)
                                    .background(ObsidianDeep.copy(alpha = 0.65f))
                            ) {
                                Icon(
                                    imageVector = if (session.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = stringResource(R.string.cd_favorite),
                                    tint = if (session.isFavorite) StarAmber else TextPrimary
                                )
                            }
                        }
                        IconButton(
                            onClick = { isLocked = true },
                            modifier = Modifier
                                .size(42.dp)
                                .tvFocusRing(CircleShape)
                                .clip(CircleShape)
                                .background(ObsidianDeep.copy(alpha = 0.65f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = stringResource(R.string.cd_lock),
                                tint = TextPrimary
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        LiveClockText()
                    }
                }

                // CENTER PLAYBACK CONTROLS
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(26.dp)
                ) {
                    if (session.isLive) {
                        CenterControlCircle(
                            icon = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous channel",
                            size = 52,
                            onClick = { onZapOffset(-1) }
                        )
                    } else {
                        CenterControlCircle(
                            icon = Icons.Filled.Replay10,
                            contentDescription = "Rewind 10 seconds",
                            size = 52,
                            onClick = {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L))
                            }
                        )
                    }

                    // Primary Play/Pause Circle
                    Surface(
                        modifier = Modifier
                            .size(68.dp)
                            .focusRequester(playPauseFocus)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedScale = 1.12f,
                                onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                }
                            )
                            .testTag("player_play_pause_button"),
                        shape = CircleShape,
                        color = DiviCyan,
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                                tint = ObsidianDeep,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    if (session.isLive) {
                        CenterControlCircle(
                            icon = Icons.Filled.SkipNext,
                            contentDescription = "Next channel",
                            size = 52,
                            onClick = { onZapOffset(1) }
                        )
                    } else {
                        CenterControlCircle(
                            icon = Icons.Filled.Forward10,
                            contentDescription = "Forward 10 seconds",
                            size = 52,
                            onClick = {
                                exoPlayer.seekTo((exoPlayer.currentPosition + 10_000L).coerceAtMost(durationMs.coerceAtLeast(1L)))
                            }
                        )
                    }
                }

                // BOTTOM IBO PRO TOOLBAR + SEEKBAR / EPG
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (session.isLive) {
                        val nowEpg = epgPrograms.firstOrNull()
                        if (nowEpg != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "NOW: ${nowEpg.title}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${formatTimeHHmm(nowEpg.startTimeMs)} – ${formatTimeHHmm(nowEpg.endTimeMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DiviCyan
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { nowEpg.progressFraction() },
                                    color = DiviCyan,
                                    trackColor = ObsidianSurfaceHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    } else if (durationMs > 0L) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = formatDurationMs(currentPosMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                            Slider(
                                value = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                                onValueChange = { fraction ->
                                    val seekMs = (fraction * durationMs).toLong()
                                    currentPosMs = seekMs
                                    exoPlayer.seekTo(seekMs)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = DiviCyan,
                                    activeTrackColor = DiviCyan,
                                    inactiveTrackColor = ObsidianSurfaceHigh
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatDurationMs(durationMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    // Bottom Pro Controls Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (session.isLive && session.channelList.isNotEmpty()) {
                                PlayerToolbarButton(
                                    icon = Icons.AutoMirrored.Filled.List,
                                    label = stringResource(R.string.player_channels),
                                    accent = DiviCyan,
                                    onClick = { showChannelDrawer = true }
                                )
                            }
                            if (!session.isLive && session.currentEpisodeIndex in 0 until (session.episodeList.size - 1)) {
                                PlayerToolbarButton(
                                    icon = Icons.Filled.SkipNext,
                                    label = "Next Ep",
                                    accent = DiviViolet,
                                    onClick = onNextEpisode
                                )
                            }
                            PlayerToolbarButton(
                                icon = Icons.Filled.Audiotrack,
                                label = stringResource(R.string.player_audio_tracks),
                                onClick = { showAudioDialog = true }
                            )
                            PlayerToolbarButton(
                                icon = Icons.Filled.Subtitles,
                                label = stringResource(R.string.player_subtitles),
                                onClick = { showSubtitleDialog = true }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PlayerToolbarButton(
                                icon = Icons.Filled.AspectRatio,
                                label = aspectMode.label,
                                onClick = {
                                    val entries = AspectRatioMode.entries
                                    aspectMode = entries[(aspectMode.ordinal + 1) % entries.size]
                                    toastNotice = "Aspect Ratio: ${aspectMode.label}"
                                }
                            )
                            if (!session.isLive) {
                                PlayerToolbarButton(
                                    icon = Icons.Filled.Speed,
                                    label = "${playbackSpeed}x",
                                    onClick = {
                                        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                        val idx = speeds.indexOfFirst { it == playbackSpeed }
                                        playbackSpeed = speeds[(idx + 1) % speeds.size]
                                        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
                                        toastNotice = "Speed: ${playbackSpeed}x"
                                    }
                                )
                            }
                            PlayerToolbarButton(
                                icon = Icons.Filled.Info,
                                label = stringResource(R.string.player_stream_info),
                                onClick = { showInfoDialog = true }
                            )
                            PlayerToolbarButton(
                                icon = Icons.Filled.ScreenRotation,
                                label = if (isLandscape) "Portrait" else "Land",
                                onClick = {
                                    isLandscape = !isLandscape
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Slide-over Channel Zap Drawer (Live TV)
        if (showChannelDrawer && session.isLive) {
            ChannelZapDrawer(
                channels = session.channelList,
                currentChannelId = session.mediaItemId,
                onSelectChannel = { ch ->
                    onSwitchChannel(ch)
                    showChannelDrawer = false
                },
                onDismiss = { showChannelDrawer = false }
            )
        }

        // Audio Track Selection Dialog
        if (showAudioDialog) {
            TrackSelectionDialog(
                title = "Audio Track",
                tracks = audioTracks,
                includeOffOption = false,
                isCurrentlyOff = false,
                onSelect = { opt ->
                    if (opt != null) {
                        PlayerEngineFactory.selectTrack(exoPlayer, currentTracks, C.TRACK_TYPE_AUDIO, opt)
                        toastNotice = "Audio: ${opt.label}"
                    }
                },
                onDismiss = { showAudioDialog = false }
            )
        }

        // Subtitle Track Selection Dialog
        if (showSubtitleDialog) {
            TrackSelectionDialog(
                title = "Subtitles",
                tracks = subtitleTracks,
                includeOffOption = true,
                isCurrentlyOff = subtitlesDisabled,
                onSelect = { opt ->
                    subtitlesDisabled = opt == null
                    PlayerEngineFactory.selectTrack(exoPlayer, currentTracks, C.TRACK_TYPE_TEXT, opt)
                    toastNotice = if (opt == null) "Subtitles: Off" else "Subtitles: ${opt.label}"
                },
                onDismiss = { showSubtitleDialog = false }
            )
        }

        // Stream Diagnostics Dialog
        if (showInfoDialog) {
            StreamInfoDialog(
                title = session.title,
                streamUrl = session.streamUrl,
                techInfo = techInfo,
                bufferPercent = bufferedPercent,
                onDismiss = { showInfoDialog = false }
            )
        }
    }
}

@Composable
private fun CenterControlCircle(
    icon: ImageVector,
    contentDescription: String,
    size: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(size.dp)
            .tvFocusable(
                shape = CircleShape,
                focusedScale = 1.12f,
                onClick = onClick
            ),
        shape = CircleShape,
        color = ObsidianDeep.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = TextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun PlayerToolbarButton(
    icon: ImageVector,
    label: String,
    accent: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(40.dp)
            .tvFocusable(
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1.08f,
                focusColor = if (accent == TextPrimary) DiviCyan else accent,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = ObsidianDeep.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1
            )
        }
    }
}
