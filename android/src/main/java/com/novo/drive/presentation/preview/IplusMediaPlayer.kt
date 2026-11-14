package com.novo.drive.presentation.preview

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Rational
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.novo.drive.core.media.PipController
import com.novo.drive.core.media.TelegramDataSourceFactory
import com.novo.drive.core.media.ThumbnailModel
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.resources.Res
import com.novo.drive.resources.common_retry
import com.novo.drive.resources.player_codec_unsupported
import com.novo.drive.resources.player_playback_failed
import com.novo.drive.resources.player_unlock_controls
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * PlayerView renders only the video; controls are Compose so they follow the theme and window
 * insets.
 */
@OptIn(UnstableApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IplusMediaPlayer(
    content: PreviewContent,
    title: String,
    fileId: String,
    dataSourceFactory: TelegramDataSourceFactory,
    isActivePage: Boolean,
    controlsVisible: Boolean = true,
    allowBackgroundPlayback: Boolean = false,
    preferredAudioLanguage: String = "",
    preferredSubtitleLanguage: String = "",
    onPreferredAudioLanguage: (String) -> Unit = {},
    onPreferredSubtitleLanguage: (String) -> Unit = {},
    onControlsVisibilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activePage by rememberUpdatedState(isActivePage)
    val notifyControls by rememberUpdatedState(onControlsVisibilityChanged)
    val audioLanguage by rememberUpdatedState(preferredAudioLanguage)
    val subtitleLanguage by rememberUpdatedState(preferredSubtitleLanguage)

    val holder = remember(content) { mutableStateOf<ExoPlayer?>(null) }
    val player = holder.value
    val failedDecoders = remember(content) { mutableSetOf<String>() }

    LaunchedEffect(holder, isActivePage) {
        if (!isActivePage || holder.value != null) return@LaunchedEffect
        val renderers = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)
            .setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                MediaCodecSelector.DEFAULT
                    .getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                    .filterNot { it.name in failedDecoders }
            }
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        holder.value = ExoPlayer.Builder(context, renderers)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setPreferredAudioLanguage(audioLanguage.ifEmpty { null })
                    .setPreferredTextLanguage(subtitleLanguage.ifEmpty { null })
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, subtitleLanguage.isEmpty())
                    .build()
                when (content) {
                    is PreviewContent.LocalMedia -> {
                        val mediaUri = if (content.path.startsWith("content://")) {
                            Uri.parse(content.path)
                        } else {
                            Uri.fromFile(File(content.path))
                        }
                        setMediaItem(MediaItem.fromUri(mediaUri))
                    }

                    is PreviewContent.StreamedMedia -> {
                        val factory = if (content.parts.isNotEmpty()) {
                            dataSourceFactory.createParted(
                                parts = content.parts,
                                encrypted = content.encrypted
                            )
                        } else {
                            dataSourceFactory.create(content.remoteFileId)
                        }
                        val source = ProgressiveMediaSource.Factory(factory)
                            .createMediaSource(MediaItem.fromUri("telegram://stream"))
                        setMediaSource(source)
                    }

                    else -> Unit
                }
                prepare()
                playWhenReady = true
            }
    }

    DisposableEffect(holder) {
        onDispose { holder.value?.release() }
    }

    DisposableEffect(player, isActivePage) {
        if (!isActivePage) player?.pause()
        onDispose { }
    }

    val audioOnly = when (content) {
        is PreviewContent.LocalMedia -> content.isAudio
        is PreviewContent.StreamedMedia -> content.isAudio
        else -> false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val backgroundAllowed by rememberUpdatedState(allowBackgroundPlayback)
    DisposableEffect(lifecycleOwner, player, audioOnly) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !backgroundAllowed && !audioOnly) {
                player?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var interactionTick by remember { mutableIntStateOf(0) }
    var locked by remember(content) { mutableStateOf(false) }
    var lockHintVisible by remember(content) { mutableStateOf(false) }
    val visibleControls by rememberUpdatedState(controlsVisible)
    var resizeMode by remember { mutableStateOf(IplusPlayerResizeMode.FIT) }
    var playing by remember(player) { mutableStateOf(player?.isPlaying == true) }
    var sized by remember(player) { mutableStateOf((player?.videoSize?.width ?: 0) > 0) }
    var videoRatio by remember(player) {
        mutableStateOf(player?.videoSize?.toRational())
    }
    var decoderFailed by remember(player) { mutableStateOf(false) }
    var playbackFailed by remember(player) { mutableStateOf(false) }
    var currentAudioSessionId by remember(player) { mutableIntStateOf(player?.audioSessionId ?: 0) }
    var currentBoostMb by remember { mutableIntStateOf(0) }
    var hasResumed by remember(fileId) { mutableStateOf(false) }
    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }
    var screenshotNotice by remember { mutableStateOf<String?>(null) }
    var preHoldSpeed by remember(player) { mutableFloatStateOf(1f) }
    var pendingSeekTargetMs by remember(player) { mutableStateOf<Long?>(null) }
    var lastSeekTapTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(screenshotNotice) {
        if (screenshotNotice != null) {
            delay(1500.milliseconds)
            screenshotNotice = null
        }
    }

    DisposableEffect(player) {
        if (player == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !hasResumed) {
                    hasResumed = true
                    if (fileId.isNotBlank()) {
                        val sp = context.getSharedPreferences("video_resume_prefs", Context.MODE_PRIVATE)
                        val savedPos = sp.getLong("pos_$fileId", 0L)
                        val dur = player.duration
                        if (dur > 0L && savedPos > 2000L) {
                            if (savedPos < (dur * 0.95f).toLong()) {
                                player.seekTo((savedPos - 2000L).coerceAtLeast(0L))
                            } else {
                                sp.edit().remove("pos_$fileId").apply()
                            }
                        }
                    }
                }
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0) {
                    currentAudioSessionId = audioSessionId
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                sized = videoSize.width > 0 && videoSize.height > 0
                videoRatio = videoSize.toRational()
            }

            override fun onPlayerError(error: PlaybackException) {
                when (error.errorCode) {
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> {
                        val decoder = error.failedDecoderName()
                        if (decoder != null && failedDecoders.add(decoder)) {
                            player.prepare()
                        } else {
                            decoderFailed = true
                        }
                    }

                    else -> playbackFailed = true
                }
            }
        }
        player.addListener(listener)
        if (player.audioSessionId != 0) {
            currentAudioSessionId = player.audioSessionId
        }
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(playing, player, fileId) {
        if (!playing || player == null || fileId.isBlank()) return@LaunchedEffect
        val sp = context.getSharedPreferences("video_resume_prefs", Context.MODE_PRIVATE)
        while (isActive) {
            delay(5000.milliseconds)
            val pos = player.currentPosition
            val dur = player.duration
            if (dur > 0L && pos > 0L) {
                sp.edit().putLong("pos_$fileId", pos).apply()
            }
        }
    }

    DisposableEffect(player, fileId) {
        onDispose {
            if (player != null && fileId.isNotBlank()) {
                val pos = player.currentPosition
                val dur = player.duration
                if (dur > 0L && pos > 0L) {
                    val sp = context.getSharedPreferences("video_resume_prefs", Context.MODE_PRIVATE)
                    sp.edit().putLong("pos_$fileId", pos).apply()
                }
            }
        }
    }

    LaunchedEffect(controlsVisible, playing, interactionTick, activePage, audioOnly) {
        if (!controlsVisible || !playing || !activePage || audioOnly) return@LaunchedEffect
        delay(CONTROLLER_TIMEOUT_MS.milliseconds)
        notifyControls(false)
    }

    LaunchedEffect(locked, lockHintVisible) {
        if (!locked) {
            lockHintVisible = false
            return@LaunchedEffect
        }
        if (!lockHintVisible) return@LaunchedEffect
        delay(CONTROLLER_TIMEOUT_MS.milliseconds)
        lockHintVisible = false
    }

    val pipController = koinInject<PipController>()
    val inPipMode by pipController.inPipMode.collectAsStateWithLifecycle()
    val pipAvailable by pipController.available.collectAsStateWithLifecycle()

    val pipOwner = remember { Any() }
    DisposableEffect(pipController, pipOwner, videoRatio, activePage, audioOnly) {
        pipController.offerVideo(pipOwner, videoRatio.takeIf { activePage && !audioOnly })
        onDispose { pipController.offerVideo(pipOwner, null) }
    }

    val activity = LocalActivity.current
    DisposableEffect(activity, playing, activePage, audioOnly) {
        val window = activity?.window
        val holding = playing && activePage && !audioOnly
        if (holding) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (holding) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var isFastForwarding by remember { mutableStateOf(false) }
    val loudnessEnhancer = remember(currentAudioSessionId) {
        if (currentAudioSessionId != 0) {
            runCatching {
                android.media.audiofx.LoudnessEnhancer(currentAudioSessionId).apply {
                    enabled = true
                    if (currentBoostMb > 0) {
                        setTargetGain(currentBoostMb)
                    }
                }
            }.getOrNull()
        } else null
    }
    DisposableEffect(loudnessEnhancer) {
        onDispose {
            runCatching {
                loudnessEnhancer?.enabled = false
                loudnessEnhancer?.release()
            }
        }
    }

    val darkTheme = MaterialTheme.colorScheme.surface.luminance() < LIGHT_LUMINANCE
    val stageColor = when {
        audioOnly -> ComposeColor.Transparent
        darkTheme -> ComposeColor.Black
        else -> MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(stageColor)
    ) {
        if (audioOnly && player != null) {
            AudioStage(
                player = player,
                fallbackTitle = title,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        if (!audioOnly && player != null) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = false
                        this.player = player
                        setBackgroundColor(Color.TRANSPARENT)
                        setShutterBackgroundColor(Color.TRANSPARENT)
                        playerViewRef = this
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = when (resizeMode) {
                        IplusPlayerResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        IplusPlayerResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        IplusPlayerResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    }
                },
                onReset = { view ->
                    view.player = null
                },
                onRelease = { view ->
                    view.player = null
                    if (playerViewRef === view) {
                        playerViewRef = null
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (!audioOnly && !sized) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(stageColor)
            ) {
                AsyncImage(
                    model = ThumbnailModel(fileId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (decoderFailed || playbackFailed) {
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(
                        if (decoderFailed) {
                            Res.string.player_codec_unsupported
                        } else {
                            Res.string.player_playback_failed
                        }
                    ),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (playbackFailed && player != null) {
                    Button(
                        onClick = {
                            playbackFailed = false
                            player.prepare()
                            player.play()
                        },
                        modifier = Modifier.padding(top = 20.dp),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(stringResource(Res.string.common_retry))
                    }
                }
            }
        }
        if (!inPipMode) {
            IplusPlayerGestureArea(
                onTap = {
                    if (locked) {
                        lockHintVisible = !lockHintVisible
                    } else {
                        notifyControls(!visibleControls)
                    }
                },
                onDoubleTapSeek = { isForward ->
                    val now = System.currentTimeMillis()
                    player?.let { p ->
                        val base = if (now - lastSeekTapTime < 900L && pendingSeekTargetMs != null) {
                            pendingSeekTargetMs!!
                        } else {
                            p.currentPosition
                        }
                        val dur = p.duration.takeIf { it > 0L }
                        val raw = if (isForward) base + 10_000L else (base - 10_000L).coerceAtLeast(0L)
                        val target = if (dur != null) raw.coerceIn(0L, dur) else raw
                        pendingSeekTargetMs = target
                        lastSeekTapTime = now
                        p.seekTo(target)
                    }
                },
                onHoldSpeed = { fast ->
                    isFastForwarding = fast
                    player?.let { p ->
                        if (fast) {
                            preHoldSpeed = p.playbackParameters.speed
                            p.setPlaybackSpeed(2.0f)
                        } else {
                            p.setPlaybackSpeed(preHoldSpeed)
                        }
                    }
                },
                onBoostChanged = { boostMb ->
                    currentBoostMb = boostMb
                    runCatching {
                        loudnessEnhancer?.setTargetGain(boostMb)
                    }
                },
                currentBoostMb = currentBoostMb,
                onAdjustStart = { if (visibleControls) notifyControls(false) },
                dragEnabled = !locked && !audioOnly,
                chromeVisible = controlsVisible,
                modifier = Modifier.matchParentSize()
            )
        }
        AnimatedVisibility(
            visible = isFastForwarding,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 24.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = ComposeColor.Black.copy(alpha = 0.85f),
                contentColor = ComposeColor(0xFF00E5FF)
            ) {
                Text(
                    text = "2X ▶▶ ফাস্ট-ফরোয়ার্ড",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        if (player != null) {
            IplusPlayerControls(
                player = player,
                visible = controlsVisible &&
                    !locked &&
                    !inPipMode &&
                    !decoderFailed &&
                    !playbackFailed,
                audioOnly = audioOnly,
                resizeMode = resizeMode,
                onCycleResizeMode = { resizeMode = resizeMode.next() },
                onInteraction = { interactionTick++ },
                onEnterPip = activity
                    ?.takeIf { pipAvailable }
                    ?.let { host -> { pipController.enter(host) } },
                onScreenshot = {
                    captureVideoFrame(playerViewRef, context) { success ->
                        screenshotNotice = if (success) "📸 ফ্রেম ক্যাপচার সফল! গ্যালারিতে সেভ হয়েছে" else "⚠️ স্ক্রিনশট নেওয়া যায়নি"
                    }
                },
                onLock = {
                    locked = true
                    lockHintVisible = true
                    notifyControls(false)
                },
                onPreferredAudioLanguage = onPreferredAudioLanguage,
                onPreferredSubtitleLanguage = onPreferredSubtitleLanguage
            )
        }
        if (locked) {
            AnimatedVisibility(
                visible = lockHintVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .navigationBarsPadding()
                    .padding(start = 24.dp)
            ) {
                FilledIconButton(
                    onClick = {
                        locked = false
                        lockHintVisible = false
                        notifyControls(true)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = ComposeColor.Black.copy(alpha = LOCK_ALPHA),
                        contentColor = ComposeColor.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = stringResource(Res.string.player_unlock_controls)
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = screenshotNotice != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 28.dp)
        ) {
            screenshotNotice?.let { notice ->
                val pillShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .liquidGlassBackground(
                            shape = pillShape,
                            tintAlpha = 0.90f,
                            borderAlpha = 0.60f,
                            borderWidth = 1.dp
                        )
                        .border(1.dp, ComposeColor(0xFF00E5FF).copy(alpha = 0.5f), pillShape)
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = notice,
                        color = ComposeColor(0xFF00E5FF),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/** Embedded cover art is used when present, with the track tags falling back to the file name. */
@Composable
private fun AudioStage(player: Player, fallbackTitle: String, modifier: Modifier = Modifier) {
    var metadata by remember(player) { mutableStateOf(player.mediaMetadata) }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                metadata = mediaMetadata
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    val artwork = remember(metadata) {
        metadata.artworkData?.let { bytes ->
            runCatching {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    val infinite = rememberInfiniteTransition(label = "audioVinylSpin")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "audioVinylRotation"
    )

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(270.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(ComposeColor(0xFF141824)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .clip(CircleShape)
                    .background(ComposeColor(0xFF0C0E14))
                    .rotate(if (isPlaying) rotation else 0f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(color = ComposeColor(0xFF1E232E), radius = size.minDimension / 2.2f)
                    drawCircle(color = ComposeColor(0xFF161B24), radius = size.minDimension / 2.6f)
                    drawCircle(color = ComposeColor(0xFF222836), radius = size.minDimension / 3.2f)
                    drawCircle(color = ComposeColor(0xFF141820), radius = size.minDimension / 4.0f)
                    drawCircle(color = ComposeColor(0xFF1F2432), radius = size.minDimension / 5.2f)

                    val sheenBrush = Brush.sweepGradient(
                        listOf(
                            ComposeColor.White.copy(alpha = 0.08f),
                            ComposeColor.Transparent,
                            ComposeColor.White.copy(alpha = 0.08f),
                            ComposeColor.Transparent,
                            ComposeColor.White.copy(alpha = 0.08f)
                        ),
                        center = center
                    )
                    drawCircle(brush = sheenBrush, radius = size.minDimension / 2.2f)
                }

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (artwork != null) {
                        Image(
                            bitmap = artwork,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = metadata.title?.toString() ?: fallbackTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 28.dp)
        )
        metadata.artist?.toString()?.let { artist ->
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        metadata.albumTitle?.toString()?.let { album ->
            Text(
                text = album,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun VideoSize.toRational(): Rational? =
    if (width > 0 && height > 0) Rational(width, height) else null

private const val CONTROLLER_TIMEOUT_MS = 3_000L
private const val LOCK_ALPHA = 0.7f
private const val LIGHT_LUMINANCE = 0.5f

@OptIn(UnstableApi::class)
private fun PlaybackException.failedDecoderName(): String? =
    generateSequence<Throwable>(this) { it.cause }
        .firstNotNullOfOrNull { cause ->
            when (cause) {
                is MediaCodecRenderer.DecoderInitializationException -> cause.codecInfo?.name
                is MediaCodecDecoderException -> cause.codecInfo?.name
                else -> null
            }
        }

@OptIn(UnstableApi::class)
private fun captureVideoFrame(
    view: PlayerView?,
    context: Context,
    onResult: (Boolean) -> Unit
) {
    val surfaceView = view?.videoSurfaceView as? SurfaceView
    if (surfaceView != null && surfaceView.holder.surface.isValid) {
        val width = surfaceView.width.coerceAtLeast(1)
        val height = surfaceView.height.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(
            surfaceView,
            bitmap,
            { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    CoroutineScope(Dispatchers.IO).launch {
                        var uri: Uri? = null
                        val ok = try {
                            val fileName = "iPlus_Cinema_${System.currentTimeMillis()}.png"
                            val contentValues = ContentValues().apply {
                                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/iPlusCinema")
                                    put(MediaStore.Images.Media.IS_PENDING, 1)
                                }
                            }
                            uri = context.contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )
                            val targetUri = uri
                            if (targetUri != null) {
                                val written = context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                } ?: false
                                if (!written) {
                                    runCatching { context.contentResolver.delete(targetUri, null, null) }
                                    false
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        contentValues.clear()
                                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                                        context.contentResolver.update(targetUri, contentValues, null, null)
                                    }
                                    true
                                }
                            } else {
                                false
                            }
                        } catch (_: Throwable) {
                            uri?.let { failedUri ->
                                runCatching { context.contentResolver.delete(failedUri, null, null) }
                            }
                            false
                        } finally {
                            if (!bitmap.isRecycled) bitmap.recycle()
                        }
                        withContext(Dispatchers.Main) {
                            onResult(ok)
                        }
                    }
                } else {
                    if (!bitmap.isRecycled) bitmap.recycle()
                    onResult(false)
                }
            },
            Handler(Looper.getMainLooper())
        )
    } else {
        onResult(false)
    }
}

