package com.novo.drive.presentation.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

@Composable
actual fun OpticalGlassBackdropHost(
    modifier: Modifier,
    background: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), content = background)
        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}

@Composable
actual fun OpticalChromeBackdropHost(
    modifier: Modifier,
    pageContent: @Composable BoxScope.() -> Unit,
    chromeOverlay: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize(), content = pageContent)
        Box(modifier = Modifier.fillMaxSize(), content = chromeOverlay)
    }
}

@Composable
actual fun Modifier.opticalGlassSurface(
    shape: Shape,
    tint: Color,
    tintAlpha: Float,
    blurRadius: Dp,
    refractionHeight: Dp,
    refractionAmount: Dp,
    useChromeBackdrop: Boolean
): Modifier = this.background(
    brush = Brush.linearGradient(
        colors = listOf(
            tint.copy(alpha = tintAlpha.coerceIn(0.18f, 0.92f)),
            tint.copy(alpha = (tintAlpha * 0.82f).coerceIn(0.12f, 0.88f)),
            Color.White.copy(alpha = 0.06f)
        )
    ),
    shape = shape
)

