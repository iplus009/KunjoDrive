package com.novo.drive.presentation.music

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import kotlin.math.abs
import kotlin.math.sin

/**
 * iPlus Liquid Glass Waveform Scrubber.
 * Bespoke interactive 64-bar amplitude scrubber with time-pill scrub HUD and specular highlights.
 */
@Composable
fun IplusWaveformScrubber(
    progress: Float,
    durationMs: Long,
    currentPositionMs: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 64,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = Color.White.copy(alpha = 0.22f)
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }

    val displayProgress = if (isDragging) dragProgress else progress
    val previewTimeMs = (displayProgress * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(1L))

    val amplitudes = remember(barCount) {
        FloatArray(barCount) { i ->
            val norm = i.toFloat() / barCount
            val wave1 = abs(sin(norm * Math.PI.toFloat() * 3.2f))
            val wave2 = abs(sin(norm * Math.PI.toFloat() * 7.5f)) * 0.45f
            val wave3 = abs(sin(norm * Math.PI.toFloat() * 14.0f)) * 0.25f
            val base = (wave1 + wave2 + wave3) / 1.7f
            base.coerceIn(0.18f, 1.0f)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val pillShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .clip(pillShape)
                    .liquidGlassBackground(
                        shape = pillShape,
                        tintAlpha = 0.85f,
                        borderAlpha = 0.60f,
                        borderWidth = 1.dp
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${Formatters.duration(previewTimeMs)} / ${Formatters.duration(durationMs)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = activeColor
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            val newProg = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek(newProg)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeek(dragProgress)
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val newProg = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragProgress = newProg
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val totalBars = amplitudes.size
                val barSpacing = 2.dp.toPx()
                val totalSpacing = barSpacing * (totalBars - 1)
                val barWidth = ((width - totalSpacing) / totalBars).coerceAtLeast(1.5f)
                val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                val activeCutoffIndex = (displayProgress * totalBars).toInt()

                for (i in 0 until totalBars) {
                    val x = i * (barWidth + barSpacing)
                    val barHeight = (amplitudes[i] * height * 0.82f).coerceAtLeast(4.dp.toPx())
                    val y = (height - barHeight) / 2f

                    val isActive = i <= activeCutoffIndex
                    val brush = if (isActive) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.95f),
                                activeColor,
                                activeColor.copy(alpha = 0.70f)
                            ),
                            startY = y,
                            endY = y + barHeight
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                inactiveColor.copy(alpha = 0.35f),
                                inactiveColor,
                                inactiveColor.copy(alpha = 0.15f)
                            ),
                            startY = y,
                            endY = y + barHeight
                        )
                    }

                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = cornerRadius
                    )

                    if (isActive) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.85f),
                            radius = barWidth / 2.2f,
                            center = Offset(x + barWidth / 2f, y + barWidth / 2f)
                        )
                    }
                }

                val playheadX = (displayProgress * width).coerceIn(0f, width)
                drawLine(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            activeColor,
                            Color.Transparent
                        )
                    ),
                    start = Offset(playheadX, 0f),
                    end = Offset(playheadX, height),
                    strokeWidth = 2.dp.toPx()
                )

                drawCircle(
                    color = activeColor.copy(alpha = 0.40f),
                    radius = 8.dp.toPx(),
                    center = Offset(playheadX, height / 2f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(playheadX, height / 2f)
                )
            }
        }
    }
}

