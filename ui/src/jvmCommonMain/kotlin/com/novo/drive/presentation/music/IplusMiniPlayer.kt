package com.novo.drive.presentation.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novo.drive.core.bridge.KunjoBridge
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.theme.IplusTheme

/**
 * iPlus Floating Liquid Glass Mini Player.
 * Floats cleanly above bottom navigation bars across all screens.
 */
@Composable
fun IplusMiniPlayer(
    bridge: KunjoBridge,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAudio by bridge.media.currentAudio.collectAsStateWithLifecycle()
    val isVisible by bridge.media.isMiniPlayerVisible.collectAsStateWithLifecycle()

    val showPlayer = isVisible && currentAudio != null

    AnimatedVisibility(
        visible = showPlayer,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        currentAudio?.let { audio ->
            val palette = IplusTheme.palette
            val shape = RoundedCornerShape(22.dp)

            val infinite = rememberInfiniteTransition(label = "miniVinylSpin")
            val rotation by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing)
                ),
                label = "miniVinylRotation"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(shape)
                    .liquidGlassBackground(
                        shape = shape,
                        tintAlpha = 0.90f,
                        borderAlpha = 0.50f,
                        borderWidth = 1.dp
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = onOpenStudio
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF101318))
                            .rotate(if (audio.isPlaying) rotation else 0f),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color(0xFF1E2430), radius = size.minDimension / 2.3f)
                            drawCircle(color = Color(0xFF141922), radius = size.minDimension / 3.2f)
                        }
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(palette.accentPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true
                    val displayTitle = audio.title.replace(AUDIO_EXT_REGEX, "")
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = palette.accentPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (audio.isPlaying) {
                                    if (isBn) "এখন বাজছে" else "Now playing"
                                } else {
                                    if (isBn) "স্থগিত" else "Paused"
                                },
                                fontSize = 11.sp,
                                color = palette.accentSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (audio.isPlaying) bridge.media.pauseAudio()
                                else bridge.media.resumeAudio()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(palette.accentPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (audio.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (audio.isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { bridge.media.skipNext() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { bridge.media.stopAudio() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private val AUDIO_EXT_REGEX = Regex("\\.(mp3|m4a|flac|wav|ogg|aac|opus|wma)$", RegexOption.IGNORE_CASE)

