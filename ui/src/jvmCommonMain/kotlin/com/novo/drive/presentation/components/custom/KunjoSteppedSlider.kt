package com.novo.drive.presentation.components.custom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.novo.drive.presentation.theme.LocalIplusPalette

/**
 * Tactile Stepped Slider with Engraved Glass Notches and Glowing Ring Bead.
 * Replaces standard Android generic Slider.
 */
@Composable
fun KunjoSteppedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    valueRange: ClosedFloatingPointRange<Float> = range,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LocalIplusPalette.current
    val accentColor = palette.accentPrimary

    val effectiveRange = if (valueRange != 0f..1f || range == 0f..1f) valueRange else range
    val rangeSpan = effectiveRange.endInclusive - effectiveRange.start
    val normalizedFraction = if (rangeSpan > 0f) {
        ((value - effectiveRange.start) / rangeSpan).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(enabled, effectiveRange) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val newValue = effectiveRange.start + fraction * rangeSpan
                    onValueChange(snapToStep(newValue, effectiveRange, steps))
                }
            }
            .pointerInput(enabled, effectiveRange) {
                if (!enabled) return@pointerInput
                detectDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val newValue = effectiveRange.start + fraction * rangeSpan
                    onValueChange(snapToStep(newValue, effectiveRange, steps))
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val trackHeight = 8.dp.toPx()
            val beadRadius = 10.dp.toPx()
            val centerY = size.height / 2f
            val startX = beadRadius
            val endX = size.width - beadRadius
            val trackWidth = endX - startX

            // Inactive Glass Track
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))
                ),
                topLeft = Offset(startX, centerY - trackHeight / 2f),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )

            // Active Glowing Fill Track
            val activeWidth = trackWidth * normalizedFraction
            if (activeWidth > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.5f), accentColor)
                    ),
                    topLeft = Offset(startX, centerY - trackHeight / 2f),
                    size = Size(activeWidth, trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                )
            }

            // Engraved Glass Step Notches
            if (steps > 0) {
                val totalSteps = steps + 1
                for (i in 0..totalSteps) {
                    val stepFraction = i.toFloat() / totalSteps.toFloat()
                    val notchX = startX + trackWidth * stepFraction
                    drawCircle(
                        color = if (stepFraction <= normalizedFraction) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f),
                        radius = 2.dp.toPx(),
                        center = Offset(notchX, centerY)
                    )
                }
            }

            // Tactile Glowing Ring Bead (Thumb)
            val thumbX = startX + trackWidth * normalizedFraction
            // Outer Glow Aura
            drawCircle(
                color = accentColor.copy(alpha = 0.35f),
                radius = beadRadius + 4.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
            // Bead Solid Core
            drawCircle(
                color = Color.White,
                radius = beadRadius,
                center = Offset(thumbX, centerY)
            )
            // Inner Accent Ring
            drawCircle(
                color = accentColor,
                radius = beadRadius - 3.dp.toPx(),
                center = Offset(thumbX, centerY)
            )
        }
    }
}

private fun snapToStep(value: Float, range: ClosedFloatingPointRange<Float>, steps: Int): Float {
    if (steps <= 0) return value
    val stepSize = (range.endInclusive - range.start) / (steps + 1)
    val stepIndex = Math.round((value - range.start) / stepSize)
    return (range.start + stepIndex * stepSize).coerceIn(range.start, range.endInclusive)
}

