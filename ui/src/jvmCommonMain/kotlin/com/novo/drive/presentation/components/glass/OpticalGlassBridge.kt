package com.novo.drive.presentation.components.glass

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Multiplatform bridge to Kunjo Drive's Glass-Kit GPU AGSL optical refraction engine.
 * - Android 13+ (API 33+): Real hardware-accelerated AGSL lens refraction, vibrancy & backdrop blur.
 * - Android 8–12 (API 26–32) & Desktop: Smooth multi-stop specular translucent glass fallback.
 */
@Composable
expect fun OpticalGlassBackdropHost(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
)

/**
 * Records scrolling page content into a dedicated chrome backdrop layer so floating
 * bars (such as FloatingNavigationBar) optically refract live content passing underneath.
 */
@Composable
expect fun OpticalChromeBackdropHost(
    modifier: Modifier = Modifier,
    pageContent: @Composable BoxScope.() -> Unit,
    chromeOverlay: @Composable BoxScope.() -> Unit
)

@Composable
expect fun Modifier.opticalGlassSurface(
    shape: Shape,
    tint: Color,
    tintAlpha: Float,
    blurRadius: Dp = 10.dp,
    refractionHeight: Dp = 10.dp,
    refractionAmount: Dp = 24.dp,
    useChromeBackdrop: Boolean = false
): Modifier

