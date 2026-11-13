package com.novo.drive.presentation.components.custom

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.novo.drive.presentation.theme.LocalIplusPalette

/**
 * Bespoke Liquid Glass Capsule Switch replacing standard generic Android M3 switch.
 * Features 3D refractive edge lighting, spring-damped thumb gliding, and ambient neon glow.
 */
@Composable
fun KunjoCapsuleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LocalIplusPalette.current
    val accentColor = palette.accentPrimary
    val interactionSource = remember { MutableInteractionSource() }

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "thumbOffset"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (checked) 0.85f else 0.15f,
        label = "glowAlpha"
    )

    val capsuleShape = RoundedCornerShape(13.dp)

    Box(
        modifier = modifier
            .width(48.dp)
            .height(26.dp)
            .clip(capsuleShape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (checked) {
                        listOf(accentColor.copy(alpha = 0.9f), Color.White.copy(alpha = 0.6f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.2f))
                    }
                ),
                shape = capsuleShape
            )
            .background(
                brush = Brush.radialGradient(
                    colors = if (checked) {
                        listOf(accentColor.copy(alpha = 0.7f), accentColor.copy(alpha = 0.25f))
                    } else {
                        listOf(Color.White.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.35f))
                    }
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                onCheckedChange(!checked)
            }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Gliding Frosted Glass Thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .shadow(
                    elevation = if (checked) 4.dp else 2.dp,
                    shape = CircleShape,
                    spotColor = if (checked) accentColor else Color.Black
                )
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (checked) {
                            listOf(Color.White, accentColor.copy(alpha = 0.4f))
                        } else {
                            listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                        }
                    )
                )
        )
    }
}

