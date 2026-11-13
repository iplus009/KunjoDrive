package com.novo.drive.presentation.components.glass

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.presentation.theme.IplusTheme
import kotlin.math.cos
import kotlin.math.sin

// --- 1. Liquid Glass Action Button (Reference Photo 7) ---

@Composable
fun GlassActionIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    tint: Color = if (IplusTheme.palette.theme == com.novo.drive.domain.model.AppTheme.LIGHT) Color(0xFF0F172A) else Color.White,
    glowColor: Color? = null,
    hasRedDot: Boolean = false,
    contentDescription: String? = null
) {
    val currentGlow = glowColor ?: IplusTheme.palette.glowColor

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF222C3D).copy(alpha = 0.70f), Color(0xFF0F1522).copy(alpha = 0.90f))
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        currentGlow.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.48f)
        )
        if (hasRedDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3D00))
            )
        }
    }
}

// --- 2. Liquid Glass Circular Button (Reference Photo 7) ---

@Composable
fun GlassCircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color = if (IplusTheme.palette.theme == com.novo.drive.domain.model.AppTheme.LIGHT) Color(0xFF0F172A) else Color.White,
    glowColor: Color? = null,
    contentDescription: String? = null
) {
    val currentGlow = glowColor ?: IplusTheme.palette.glowColor

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF202A3B).copy(alpha = 0.75f), Color(0xFF0C121D).copy(alpha = 0.92f))
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        currentGlow.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

// --- 3. Liquid Glass Pill Action Button (Reference Photo 6 & 7) ---

@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActive: Boolean = false,
    accentColor: Color? = null
) {
    val currentAccent = accentColor ?: IplusTheme.palette.accentPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = if (isActive) {
                    Brush.verticalGradient(
                        listOf(currentAccent.copy(alpha = 0.85f), currentAccent.copy(alpha = 0.65f))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E2738).copy(alpha = 0.82f), Color(0xFF0F1522).copy(alpha = 0.94f))
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isActive) 0.7f else 0.4f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        val pillContentColor = if (IplusTheme.palette.theme == com.novo.drive.domain.model.AppTheme.LIGHT) Color(0xFF0F172A) else Color.White
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = pillContentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = text,
                color = pillContentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// --- 4. Liquid Glass Segmented Tab Pills (Reference Photo 7: Tab 1, Tab 2, Tab 3) ---

@Composable
fun GlassSegmentedTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = IplusTheme.palette.accentPrimary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF151D2A).copy(alpha = 0.80f), Color(0xFF0A0F18).copy(alpha = 0.90f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(Color(0xFF263347).copy(alpha = 0.90f), Color(0xFF151E2D).copy(alpha = 0.95f))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        listOf(accent.copy(alpha = 0.60f), Color.White.copy(alpha = 0.10f))
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        } else {
                            Modifier
                        }
                    )
                    .clickable(role = Role.Tab, onClick = { onTabSelected(index) })
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.60f)
                    )
                }
            }
        }
    }
}

// --- 5. Liquid Glass Audio Equalizer Card (Reference Photo 7) ---

@Composable
fun GlassEqualizerCard(
    modifier: Modifier = Modifier,
    barCount: Int = 7
) {
    val accent = IplusTheme.palette.accentPrimary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF161E2D).copy(alpha = 0.85f), Color(0xFF0B101A).copy(alpha = 0.95f))
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.05f))
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sampleLevels = listOf(0.7f, 0.4f, 0.85f, 0.35f, 0.6f, 0.9f, 0.5f)
            for (i in 0 until barCount) {
                val level = sampleLevels[i % sampleLevels.size]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(level)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        listOf(accent, accent.copy(alpha = 0.40f))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

