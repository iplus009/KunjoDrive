package com.novo.drive.presentation.preview

import android.app.Activity
import android.media.AudioManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.resources.Res
import com.novo.drive.resources.player_brightness
import com.novo.drive.resources.player_toggle_controls
import com.novo.drive.resources.player_volume
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Left half sets brightness, right half volume; the brightness override lives on the window, so it
 * reverts when the player leaves.
 */
@Composable
fun IplusPlayerGestureArea(
    onTap: () -> Unit,
    onDoubleTapSeek: ((Boolean) -> Unit)? = null,
    onHoldSpeed: ((Boolean) -> Unit)? = null,
    onBoostChanged: ((Int) -> Unit)? = null,
    currentBoostMb: Int = 0,
    onAdjustStart: () -> Unit,
    dragEnabled: Boolean,
    chromeVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val tap by rememberUpdatedState(onTap)
    val adjustStart by rememberUpdatedState(onAdjustStart)
    val activeBoostMb by rememberUpdatedState(currentBoostMb)

    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }
    val maxVolume = remember(audioManager) {
        (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1).coerceAtLeast(1)
    }

    var brightness by remember(activity) { mutableFloatStateOf(systemBrightness(activity)) }
    var volume by remember(audioManager) {
        mutableFloatStateOf(
            currentVolume(
                audioManager,
                maxVolume
            )
        )
    }
    var adjustment by remember { mutableStateOf<AdjustmentLevel?>(null) }
    var seekNotice by remember { mutableStateOf<String?>(null) }
    var cumulativeSeekSeconds by remember { mutableIntStateOf(0) }
    var lastDoubleTapTime by remember { mutableLongStateOf(0L) }
    var lastDoubleTapIsForward by remember { mutableStateOf(true) }

    LaunchedEffect(chromeVisible) {
        if (chromeVisible) {
            adjustment = null
            seekNotice = null
        }
    }

    LaunchedEffect(adjustment) {
        if (adjustment == null) return@LaunchedEffect
        delay(HUD_TIMEOUT_MS.milliseconds)
        adjustment = null
    }

    LaunchedEffect(seekNotice) {
        if (seekNotice == null) return@LaunchedEffect
        delay(800.milliseconds)
        seekNotice = null
    }

    val toggleControlsLabel = stringResource(Res.string.player_toggle_controls)
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .semantics {
                    onClick(label = toggleControlsLabel) {
                        tap()
                        true
                    }
                }
                .pointerInput(dragEnabled) {
                    var isHoldingSpeed = false
                    detectTapGestures(
                        onTap = { tap() },
                        onDoubleTap = { offset ->
                            val now = System.currentTimeMillis()
                            val isForward = offset.x > size.width / 2f
                            if (now - lastDoubleTapTime < 800L && lastDoubleTapIsForward == isForward) {
                                cumulativeSeekSeconds += 10
                            } else {
                                cumulativeSeekSeconds = 10
                                lastDoubleTapIsForward = isForward
                            }
                            lastDoubleTapTime = now
                            seekNotice = if (isForward) "+${cumulativeSeekSeconds}s ▶▶" else "◀◀ -${cumulativeSeekSeconds}s"
                            onDoubleTapSeek?.invoke(isForward)
                        },
                        onLongPress = {
                            isHoldingSpeed = true
                            onHoldSpeed?.invoke(true)
                        },
                        onPress = {
                            tryAwaitRelease()
                            if (isHoldingSpeed) {
                                isHoldingSpeed = false
                                onHoldSpeed?.invoke(false)
                            }
                        }
                    )
                }
                .pointerInput(dragEnabled, maxVolume) {
                    if (!dragEnabled) return@pointerInput
                    var onLeftHalf = false
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            onLeftHalf = offset.x < size.width / 2f
                            volume = if (!onLeftHalf && activeBoostMb > 0) {
                                10.0.pow(activeBoostMb / 2000.0).toFloat().coerceIn(1f, 2f)
                            } else {
                                currentVolume(audioManager, maxVolume)
                            }
                            adjustStart()
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val step = (-dragAmount / size.height.toFloat()) * 1.2f
                            if (onLeftHalf) {
                                brightness = (brightness + step).coerceIn(MIN_BRIGHTNESS, 1f)
                                applyBrightness(activity, brightness)
                                adjustment = AdjustmentLevel(Adjustment.BRIGHTNESS, brightness)
                            } else {
                                volume = (volume + step).coerceIn(0f, 2.0f)
                                if (volume <= 1.0f) {
                                    audioManager?.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        (volume * maxVolume).roundToInt(),
                                        0
                                    )
                                    onBoostChanged?.invoke(0)
                                } else {
                                    audioManager?.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        maxVolume,
                                        0
                                    )
                                    // Formula: gain g (1.0..2.0), mB = 20 * log10(g) * 100
                                    val boostMb = (2000.0 * kotlin.math.log10(volume.toDouble())).roundToInt()
                                    onBoostChanged?.invoke(boostMb)
                                }
                                adjustment = AdjustmentLevel(Adjustment.VOLUME, volume)
                            }
                        }
                    )
                }
        )
        AnimatedVisibility(
            visible = adjustment != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            adjustment?.let { AdjustmentHud(it) }
        }
        AnimatedVisibility(
            visible = seekNotice != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            seekNotice?.let { notice ->
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
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = notice,
                        color = Color(0xFF00E5FF),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdjustmentHud(adjustment: AdjustmentLevel) {
    val brightnessLabel = stringResource(Res.string.player_brightness)
    val volumeLabel = stringResource(Res.string.player_volume)
    val label = when (adjustment.kind) {
        Adjustment.BRIGHTNESS -> brightnessLabel
        Adjustment.VOLUME -> volumeLabel
    }
    val icon = when (adjustment.kind) {
        Adjustment.BRIGHTNESS -> when {
            adjustment.level <= MIN_BRIGHTNESS -> Icons.Filled.BrightnessLow
            adjustment.level <= HALF -> Icons.Filled.BrightnessMedium
            else -> Icons.Filled.BrightnessHigh
        }

        Adjustment.VOLUME -> when {
            adjustment.level <= 0f -> Icons.AutoMirrored.Filled.VolumeOff
            adjustment.level <= HALF -> Icons.AutoMirrored.Filled.VolumeDown
            else -> Icons.AutoMirrored.Filled.VolumeUp
        }
    }

    val isBoost = adjustment.kind == Adjustment.VOLUME && adjustment.level > 1.0f
    val hudShape = RoundedCornerShape(22.dp)
    val activeColor = when {
        isBoost -> Color(0xFFFF3D00) // Neon Red Audio Boost
        adjustment.kind == Adjustment.BRIGHTNESS -> Color(0xFF00E5FF) // Neon Cyan Brightness
        else -> Color(0xFF00E5FF) // Neon Cyan Volume
    }

    Box(
        modifier = Modifier
            .clip(hudShape)
            .liquidGlassBackground(
                shape = hudShape,
                tintAlpha = 0.92f,
                borderAlpha = 0.55f,
                borderWidth = 1.dp
            )
            .border(
                width = 1.dp,
                color = activeColor.copy(alpha = 0.40f),
                shape = hudShape
            )
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = activeColor,
                modifier = Modifier.size(26.dp)
            )
            LinearProgressIndicator(
                progress = { if (isBoost) (adjustment.level - 1.0f) else adjustment.level },
                modifier = Modifier
                    .width(HUD_BAR_WIDTH)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = activeColor,
                trackColor = Color.White.copy(alpha = 0.18f)
            )
            Text(
                text = if (isBoost) "BOOST ${(adjustment.level * 100).roundToInt()}%" else "${(adjustment.level * 100).roundToInt()}%",
                color = activeColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun currentVolume(audioManager: AudioManager?, maxVolume: Int): Float {
    val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    return (current.toFloat() / maxVolume).coerceIn(0f, 1f)
}

private fun systemBrightness(activity: Activity?): Float {
    val override = activity?.window?.attributes?.screenBrightness ?: -1f
    if (override >= 0f) return override
    val resolver = activity?.contentResolver ?: return DEFAULT_BRIGHTNESS
    val value = runCatching {
        Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
    }.getOrNull() ?: return DEFAULT_BRIGHTNESS
    return (value / MAX_SYSTEM_BRIGHTNESS).coerceIn(0f, 1f)
}

private fun applyBrightness(activity: Activity?, value: Float) {
    val window = activity?.window ?: return
    window.attributes = window.attributes.apply { screenBrightness = value }
}

private const val MIN_BRIGHTNESS = 0.01f
private const val DEFAULT_BRIGHTNESS = 0.5f
private const val MAX_SYSTEM_BRIGHTNESS = 255f
private const val HALF = 0.5f
private const val HUD_TIMEOUT_MS = 900L
private val HUD_BAR_WIDTH = 120.dp

