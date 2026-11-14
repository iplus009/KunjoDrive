package com.novo.drive.presentation.preview

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.resources.Res
import com.novo.drive.resources.player_audio_track
import com.novo.drive.resources.player_fewer_controls
import com.novo.drive.resources.player_hide_subtitles
import com.novo.drive.resources.player_lock_controls
import com.novo.drive.resources.player_more_controls
import com.novo.drive.resources.player_mute
import com.novo.drive.resources.player_pause
import com.novo.drive.resources.player_picture_in_picture
import com.novo.drive.resources.player_play
import com.novo.drive.resources.player_repeat_off
import com.novo.drive.resources.player_repeat_one
import com.novo.drive.resources.player_replay
import com.novo.drive.resources.player_show_subtitles
import com.novo.drive.resources.player_subtitles
import com.novo.drive.resources.player_subtitles_off
import com.novo.drive.resources.player_track
import com.novo.drive.resources.player_unmute
import com.novo.drive.resources.preview_back_10_seconds
import com.novo.drive.resources.preview_forward_10_seconds
import com.novo.drive.resources.preview_playback_speed
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Not the Media3 controller: it ignores window insets, leaves no side padding and anchors its popup
 * to the top.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IplusPlayerControls(
    player: Player,
    visible: Boolean,
    audioOnly: Boolean,
    resizeMode: IplusPlayerResizeMode,
    onCycleResizeMode: () -> Unit,
    onInteraction: () -> Unit,
    onLock: () -> Unit = {},
    onEnterPip: (() -> Unit)? = null,
    onScreenshot: (() -> Unit)? = null,
    onPreferredAudioLanguage: (String) -> Unit = {},
    onPreferredSubtitleLanguage: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var playing by remember(player) { mutableStateOf(player.isPlaying) }
    var ended by remember(player) { mutableStateOf(player.playbackState == Player.STATE_ENDED) }
    var playbackState by remember(player) { mutableIntStateOf(player.playbackState) }
    var playWhenReady by remember(player) { mutableStateOf(player.playWhenReady) }
    var duration by remember(player) { mutableLongStateOf(player.duration.coerceAtLeast(0)) }
    var position by remember(player) { mutableLongStateOf(player.currentPosition.coerceAtLeast(0)) }
    var scrubTarget by remember(player) { mutableStateOf<Float?>(null) }
    var pendingSeek by remember(player) { mutableStateOf<Long?>(null) }
    var speed by remember(player) { mutableFloatStateOf(player.playbackParameters.speed) }
    var muted by remember(player) { mutableStateOf(player.volume == 0f) }
    var repeatOne by remember(player) {
        mutableStateOf(player.repeatMode == Player.REPEAT_MODE_ONE)
    }
    var tracks by remember(player) { mutableStateOf(player.currentTracks) }
    var subtitlesOn by remember(player) { mutableStateOf(false) }
    var rotation by remember { mutableStateOf(IplusPlayerRotation.AUTO) }
    var expanded by remember { mutableStateOf(false) }
    var showSpeeds by remember { mutableStateOf(false) }
    var showAudioTracks by remember { mutableStateOf(false) }
    var showTextTracks by remember { mutableStateOf(false) }

    val activity = LocalActivity.current

    /**
     * A seek on a stream can take a moment, so the button reports waiting for data instead of
     * showing play.
     */
    val waitingForData = playbackState == Player.STATE_BUFFERING ||
        (playWhenReady && !playing && !ended && playbackState != Player.STATE_IDLE)

    val trackWord = stringResource(Res.string.player_track)
    val textTracks: List<IplusPlayerTrack> = tracks.playerTracks(C.TRACK_TYPE_TEXT, trackWord)
    val audioTracks: List<IplusPlayerTrack> = tracks.playerTracks(C.TRACK_TYPE_AUDIO, trackWord)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                ended = state == Player.STATE_ENDED
                duration = player.duration.coerceAtLeast(0)
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                speed = playbackParameters.speed
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                    position = newPosition.positionMs.coerceAtLeast(0)
                    pendingSeek = null
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReadyNow: Boolean, reason: Int) {
                playWhenReady = playWhenReadyNow
            }

            override fun onTracksChanged(current: Tracks) {
                tracks = current
                subtitlesOn = current.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    DisposableEffect(activity) {
        onDispose {
            if (rotation != IplusPlayerRotation.AUTO) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    LaunchedEffect(player, visible, playing) {
        while (visible) {
            val current = player.currentPosition.coerceAtLeast(0)
            val target = pendingSeek
            if (target == null || abs(current - target) <= SEEK_TOLERANCE_MS) {
                pendingSeek = null
                position = current
            }
            duration = player.duration.coerceAtLeast(0)
            delay(PROGRESS_INTERVAL_MS.milliseconds)
        }
    }

    LaunchedEffect(visible) {
        if (!visible) expanded = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        CompositionLocalProvider(LocalOverVideo provides !audioOnly) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = TOP_BAR_HEIGHT + 4.dp, end = 14.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CompositionLocalProvider(
                                    LocalLayoutDirection provides LayoutDirection.Ltr
                                ) {
                                    ExtraControl(expanded || audioOnly) {
                                        ControlButton(
                                            icon = if (muted) {
                                                Icons.AutoMirrored.Filled.VolumeOff
                                            } else {
                                                Icons.AutoMirrored.Filled.VolumeUp
                                            },
                                            description = if (muted) {
                                                stringResource(Res.string.player_unmute)
                                            } else {
                                                stringResource(Res.string.player_mute)
                                            },
                                            active = muted,
                                            onClick = {
                                                onInteraction()
                                                muted = !muted
                                                player.volume = if (muted) 0f else 1f
                                            }
                                        )
                                    }
                                    if (onEnterPip != null && !audioOnly) {
                                        ControlButton(
                                            icon = Icons.Filled.PictureInPictureAlt,
                                            description = stringResource(
                                                Res.string.player_picture_in_picture
                                            ),
                                            onClick = {
                                                onInteraction()
                                                onEnterPip()
                                            }
                                        )
                                    }
                                    if (onScreenshot != null && !audioOnly) {
                                        ControlButton(
                                            icon = Icons.Filled.PhotoCamera,
                                            description = if (com.novo.drive.presentation.common.isBengaliUi()) "ক্যাপচার স্ক্রিনশট" else "Capture screenshot",
                                            onClick = {
                                                onInteraction()
                                                onScreenshot()
                                            }
                                        )
                                    }
                                    if (!audioOnly) {
                                        ControlButton(
                                            icon = Icons.Filled.Lock,
                                            description = stringResource(Res.string.player_lock_controls),
                                            onClick = {
                                                onInteraction()
                                                onLock()
                                            }
                                        )
                                    }
                                    if (!audioOnly) {
                                        ExtraControl(expanded) {
                                            ControlButton(
                                                icon = rotation.icon,
                                                description = rotation.description,
                                                active = rotation != IplusPlayerRotation.AUTO,
                                                onClick = {
                                                    onInteraction()
                                                    rotation = rotation.next()
                                                    activity?.requestedOrientation = rotation.orientation
                                                }
                                            )
                                        }
                                    }
                                    if (!audioOnly) {
                                        ExtraControl(expanded) {
                                            ControlButton(
                                                icon = resizeMode.icon,
                                                description = resizeMode.description,
                                                onClick = {
                                                    onInteraction()
                                                    onCycleResizeMode()
                                                }
                                            )
                                        }
                                    }
                                    ExtraControl(expanded) {
                                        ControlButton(
                                            icon = Icons.Filled.SlowMotionVideo,
                                            description = stringResource(Res.string.preview_playback_speed),
                                            active = speed != 1f,
                                            onClick = {
                                                onInteraction()
                                                showSpeeds = true
                                            }
                                        )
                                    }
                                    ExtraControl(expanded) {
                                        ControlButton(
                                            icon = if (repeatOne) {
                                                Icons.Filled.RepeatOne
                                            } else {
                                                Icons.Filled.Repeat
                                            },
                                            description = if (repeatOne) {
                                                stringResource(Res.string.player_repeat_off)
                                            } else {
                                                stringResource(Res.string.player_repeat_one)
                                            },
                                            active = repeatOne,
                                            onClick = {
                                                onInteraction()
                                                repeatOne = !repeatOne
                                                player.repeatMode = if (repeatOne) {
                                                    Player.REPEAT_MODE_ONE
                                                } else {
                                                    Player.REPEAT_MODE_OFF
                                                }
                                            }
                                        )
                                    }
                                    if (!audioOnly) {
                                        ExtraControl(expanded || textTracks.isNotEmpty()) {
                                            ControlButton(
                                                icon = if (subtitlesOn) {
                                                    Icons.Filled.Subtitles
                                                } else {
                                                    Icons.Filled.SubtitlesOff
                                                },
                                                description = if (subtitlesOn) {
                                                    stringResource(Res.string.player_hide_subtitles)
                                                } else {
                                                    stringResource(Res.string.player_show_subtitles)
                                                },
                                                active = subtitlesOn,
                                                enabled = textTracks.isNotEmpty(),
                                                onClick = {
                                                    onInteraction()
                                                    showTextTracks = true
                                                }
                                            )
                                        }
                                    }
                                    ExtraControl(expanded || audioTracks.size > 1) {
                                        ControlButton(
                                            icon = Icons.Filled.Audiotrack,
                                            description = stringResource(Res.string.player_audio_track),
                                            enabled = audioTracks.isNotEmpty(),
                                            onClick = {
                                                onInteraction()
                                                showAudioTracks = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        ControlButton(
                            icon = if (expanded) Icons.Filled.ChevronRight else Icons.Filled.ChevronLeft,
                            description = if (expanded) {
                                stringResource(Res.string.player_fewer_controls)
                            } else {
                                stringResource(Res.string.player_more_controls)
                            },
                            onClick = {
                                onInteraction()
                                expanded = !expanded
                            }
                        )
                    }
                }

                if (!audioOnly) {
                    TransportRow(
                        player = player,
                        playing = playing,
                        ended = ended,
                        waiting = waitingForData,
                        onInteraction = onInteraction,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Floating Frosted Glass Cinema HUD Panel
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0B0D13).copy(alpha = 0.88f))
                        .border(
                            width = 1.2.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (audioOnly) {
                            TransportRow(
                                player = player,
                                playing = playing,
                                ended = ended,
                                waiting = waitingForData,
                                onInteraction = onInteraction,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                            )
                        }

                        // Neon Seeker Bar
                        Slider(
                            value = scrubTarget ?: position.toFloat(),
                            onValueChange = { value ->
                                onInteraction()
                                scrubTarget = value
                            },
                            onValueChangeFinished = {
                                scrubTarget?.let { target ->
                                    val millis = target.toLong()
                                    position = millis
                                    pendingSeek = millis
                                    player.seekTo(millis)
                                }
                                scrubTarget = null
                            },
                            valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                            ),
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(2.5.dp, Color(0xFF00E5FF), CircleShape)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Timestamps and Quick Utility Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val curPos = scrubTarget?.toLong() ?: position
                            Text(
                                text = Formatters.duration(curPos),
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            // Speed Pill
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        onInteraction()
                                        showSpeeds = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = speedLabel(speed),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (speed != 1f) Color(0xFF00E5FF) else Color.White
                                )
                            }

                            if (textTracks.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (subtitlesOn) Color(0xFF00E5FF).copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f))
                                        .border(0.8.dp, if (subtitlesOn) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            onInteraction()
                                            showTextTracks = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (subtitlesOn) Icons.Filled.Subtitles else Icons.Filled.SubtitlesOff,
                                        contentDescription = "Subtitles",
                                        tint = if (subtitlesOn) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.70f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (audioTracks.size > 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.10f))
                                        .border(0.8.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            onInteraction()
                                            showAudioTracks = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Audiotrack,
                                        contentDescription = "Audio track",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))
                            val rem = (duration - curPos).coerceAtLeast(0L)
                            Text(
                                text = "-${Formatters.duration(rem)}",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.70f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSpeeds) {
        ModalBottomSheet(onDismissRequest = { showSpeeds = false }) {
            Text(
                text = stringResource(Res.string.preview_playback_speed),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
            )
            SPEEDS.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = speed == option,
                            role = Role.RadioButton,
                            onClick = {
                                speed = option
                                player.setPlaybackSpeed(option)
                                onInteraction()
                            }
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = speed == option, onClick = null)
                    Text(
                        text = speedLabel(option),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .height(8.dp)
            )
        }
    }

    if (showAudioTracks) {
        IplusTrackPickerSheet(
            title = stringResource(Res.string.player_audio_track),
            tracks = audioTracks,
            offLabel = null,
            offSelected = false,
            onSelectOff = {},
            onSelect = { track ->
                onInteraction()
                showAudioTracks = false
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    .setOverrideForType(
                        TrackSelectionOverride(track.group.mediaTrackGroup, track.index)
                    )
                    .setPreferredAudioLanguage(track.language.ifEmpty { null })
                    .build()
                onPreferredAudioLanguage(track.language)
            },
            onDismiss = { showAudioTracks = false }
        )
    }

    if (showTextTracks) {
        IplusTrackPickerSheet(
            title = stringResource(Res.string.player_subtitles),
            tracks = textTracks,
            offLabel = stringResource(Res.string.player_subtitles_off),
            offSelected = !subtitlesOn,
            onSelectOff = {
                onInteraction()
                showTextTracks = false
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .setPreferredTextLanguage(null)
                    .build()
                subtitlesOn = false
                onPreferredSubtitleLanguage("")
            },
            onSelect = { track ->
                onInteraction()
                showTextTracks = false
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setOverrideForType(
                        TrackSelectionOverride(track.group.mediaTrackGroup, track.index)
                    )
                    .setPreferredTextLanguage(track.language.ifEmpty { null })
                    .build()
                subtitlesOn = true
                onPreferredSubtitleLanguage(track.language)
            },
            onDismiss = { showTextTracks = false }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TransportRow(
    player: Player,
    playing: Boolean,
    ended: Boolean,
    waiting: Boolean,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.2.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                .clickable {
                    onInteraction()
                    player.seekTo((player.currentPosition - SEEK_STEP_MS).coerceAtLeast(0))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Replay10,
                contentDescription = stringResource(Res.string.preview_back_10_seconds),
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF00E5FF),
                            Color(0xFF0284C7)
                        )
                    )
                )
                .border(
                    width = 2.5.dp,
                    color = Color.White.copy(alpha = 0.90f),
                    shape = CircleShape
                )
                .clickable {
                    onInteraction()
                    when {
                        ended -> {
                            player.seekTo(0)
                            player.play()
                        }
                        player.isPlaying -> player.pause()
                        else -> player.play()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (waiting) {
                LoadingIndicator(
                    color = Color.Black,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    imageVector = when {
                        ended -> Icons.Filled.Replay
                        playing -> Icons.Filled.Pause
                        else -> Icons.Filled.PlayArrow
                    },
                    contentDescription = when {
                        ended -> stringResource(Res.string.player_replay)
                        playing -> stringResource(Res.string.player_pause)
                        else -> stringResource(Res.string.player_play)
                    },
                    tint = Color.Black,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.2.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                .clickable {
                    onInteraction()
                    val limit = player.duration.coerceAtLeast(0)
                    player.seekTo((player.currentPosition + SEEK_STEP_MS).coerceAtMost(limit))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Forward10,
                contentDescription = stringResource(Res.string.preview_forward_10_seconds),
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun ExtraControl(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
        exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
    ) {
        content()
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
    enabled: Boolean = true
) {
    val buttonShape = RoundedCornerShape(12.dp)
    val activeColor = Color(0xFF00E5FF)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(buttonShape)
            .background(if (active) activeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = if (active) activeColor else Color.White.copy(alpha = 0.18f),
                shape = buttonShape
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) activeColor else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

private val LocalOverVideo = compositionLocalOf { true }

private fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x"

private val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f, 4f)
private const val SEEK_STEP_MS = 10_000L
private const val PROGRESS_INTERVAL_MS = 250L
private const val SEEK_TOLERANCE_MS = 500L
private val TOP_BAR_HEIGHT = 64.dp

