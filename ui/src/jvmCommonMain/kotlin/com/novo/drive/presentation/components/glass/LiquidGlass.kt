package com.novo.drive.presentation.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Kunjo Drive Optical Liquid Glass modifier powered by Glass-Kit GPU AGSL shaders.
 * Combines real lens refraction & vibrancy blur (Android 13+) with beveled specular rim highlights.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(18.dp),
    tintColor: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 2.dp,
    specularIntensity: Float = 0.28f,
    glowColor: Color? = null
): Modifier = this.then(
    Modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.18f),
            spotColor = Color.Black.copy(alpha = 0.24f)
        )
        .clip(shape)
)

@Composable
fun Modifier.liquidGlassBackground(
    shape: Shape = RoundedCornerShape(18.dp),
    tintAlpha: Float = 0.65f,
    borderAlpha: Float = 0.12f,
    borderWidth: Dp = 1.dp,
    useChromeBackdrop: Boolean = false
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val baseSurface = if (isDark) {
        Color(0xFF141925)
    } else {
        Color.White
    }

    val quietBorderColor = if (isDark) {
        Color.White.copy(alpha = (borderAlpha).coerceIn(0.06f, 0.18f))
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    return this
        .opticalGlassSurface(
            shape = shape,
            tint = baseSurface,
            tintAlpha = tintAlpha,
            blurRadius = if (useChromeBackdrop) 16.dp else 8.dp,
            refractionHeight = if (useChromeBackdrop) 10.dp else 6.dp,
            refractionAmount = if (useChromeBackdrop) 20.dp else 12.dp,
            useChromeBackdrop = useChromeBackdrop
        )
        .border(borderWidth, quietBorderColor, shape)
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 6.dp,
    tintAlpha: Float = 0.65f,
    borderWidth: Dp = 1.dp,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                elevation = elevation,
                glowColor = glowColor
            )
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = tintAlpha,
                borderWidth = borderWidth
            )
            .then(clickModifier),
        content = content
    )
}

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    accentColor: Color? = null,
    elevation: Dp = 4.dp,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val baseTint = accentColor ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                elevation = elevation,
                glowColor = baseTint
            )
            .opticalGlassSurface(
                shape = shape,
                tint = baseTint,
                tintAlpha = if (isDark) 0.28f else 0.22f,
                blurRadius = 8.dp,
                refractionHeight = 8.dp,
                refractionAmount = 18.dp
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.45f else 0.75f),
                        baseTint.copy(alpha = 0.55f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = baseTint),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tintAlpha: Float = 0.72f,
    borderAlpha: Float = 0.35f,
    borderWidth: Dp = 1.dp,
    glowColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .liquidGlass(
                shape = shape,
                elevation = 6.dp,
                glowColor = glowColor
            )
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = tintAlpha,
                borderAlpha = borderAlpha,
                borderWidth = borderWidth
            ),
        content = content
    )
}
