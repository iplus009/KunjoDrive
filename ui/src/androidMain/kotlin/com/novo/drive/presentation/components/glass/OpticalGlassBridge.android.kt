package com.novo.drive.presentation.components.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

private val LocalPageGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
private val LocalChromeGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

private val isOpticalShaderSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
actual fun OpticalGlassBackdropHost(
    modifier: Modifier,
    background: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            content = background
        )
        CompositionLocalProvider(LocalPageGlassBackdrop provides backdrop) {
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}

@Composable
actual fun OpticalChromeBackdropHost(
    modifier: Modifier,
    pageContent: @Composable BoxScope.() -> Unit,
    chromeOverlay: @Composable BoxScope.() -> Unit
) {
    val chromeBackdrop = rememberLayerBackdrop()
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(chromeBackdrop),
            content = pageContent
        )
        CompositionLocalProvider(LocalChromeGlassBackdrop provides chromeBackdrop) {
            Box(modifier = Modifier.fillMaxSize(), content = chromeOverlay)
        }
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
): Modifier {
    val backdrop = if (useChromeBackdrop) {
        LocalChromeGlassBackdrop.current ?: LocalPageGlassBackdrop.current
    } else {
        LocalPageGlassBackdrop.current
    }

    return if (useChromeBackdrop && backdrop != null && isOpticalShaderSupported) {
        this.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(blurRadius.toPx())
                lens(refractionHeight.toPx(), refractionAmount.toPx())
            },
            onDrawSurface = {
                drawRect(tint.copy(alpha = (tintAlpha * 0.62f).coerceIn(0.08f, 0.88f)))
            }
        )
    } else {
        this.background(
            brush = Brush.linearGradient(
                colors = listOf(
                    tint.copy(alpha = tintAlpha.coerceIn(0.15f, 0.94f)),
                    tint.copy(alpha = (tintAlpha * 0.84f).coerceIn(0.10f, 0.90f)),
                    Color.White.copy(alpha = 0.05f)
                )
            ),
            shape = shape
        )
    }
}

