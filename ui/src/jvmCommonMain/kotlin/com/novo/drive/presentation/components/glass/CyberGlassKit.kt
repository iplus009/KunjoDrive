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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
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
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.presentation.theme.IplusTheme
import kotlin.math.cos
import kotlin.math.sin

// --- 1. Liquid Glass Floating Navigation Bar (Reference Image 1) ---

data class GlassNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0
)

@Composable
fun LiquidGlassNavBar(
    items: List<GlassNavItem>,
    selectedId: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    topActionLabel: String? = null,
    topActionIcon: ImageVector = Icons.Filled.Upload,
    onTopActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Optional Top Floating Action Pill (e.g. [+ Upload] or [📷 Capture] from Image 1)
        if (topActionLabel != null && onTopActionClick != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFF1E2838).copy(alpha = 0.85f), Color(0xFF0F1522).copy(alpha = 0.92f))
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable(role = Role.Button, onClick = onTopActionClick)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val topActionColor = if (IplusTheme.palette.theme == com.novo.drive.domain.model.AppTheme.LIGHT) Color(0xFF0F172A) else Color.White
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = topActionIcon,
                        contentDescription = topActionLabel,
                        tint = topActionColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = topActionLabel,
                        color = topActionColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Main Floating Glass Navigation Bar (Image 1)
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .fillMaxWidth()
                .height(68.dp)
                // Subtle ambient backlight (neon green/cyan glow behind bar)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E676).copy(alpha = 0.18f), Color.Transparent),
                        radius = 350f
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                // Outer Frosted Glass Container
                .clip(RoundedCornerShape(36.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF1A2333).copy(alpha = 0.88f), Color(0xFF0B101C).copy(alpha = 0.95f))
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.40f), Color.White.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = item.id == selectedId
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.55f,
                        animationSpec = tween(durationMillis = 250)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(28.dp))
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.16f),
                                                    Color.White.copy(alpha = 0.04f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.50f),
                                                    Color.White.copy(alpha = 0.10f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(28.dp)
                                        )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab,
                                onClick = { onItemSelected(item.id) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) Color.White else Color.White.copy(alpha = animatedAlpha),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = animatedAlpha)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 2. Liquid Glass Icon Button (Reference Image 2) ---

@Composable
fun LiquidGlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    tint: Color = if (IplusTheme.palette.theme == com.novo.drive.domain.model.AppTheme.LIGHT) Color(0xFF0F172A) else Color.White,
    glowColor: Color? = null,
    badgeText: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1E2838).copy(alpha = 0.75f), Color(0xFF0F1522).copy(alpha = 0.88f))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        glowColor?.copy(alpha = 0.6f) ?: Color.White.copy(alpha = 0.35f),
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
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.48f)
        )
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .background(Color(0xFFFF3D00), CircleShape)
                    .size(8.dp)
            )
        }
    }
}

// --- 3. Liquid Glass Search Bar (Reference Image 2) ---

@Composable
fun LiquidGlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onMicClick: (() -> Unit)? = null
) {
    val isLight = MaterialTheme.colorScheme.background.red > 0.5f
    val bgBrush = if (isLight) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.95f), Color(0xFFF1F5F9).copy(alpha = 0.95f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xFF172030).copy(alpha = 0.75f), Color(0xFF0B101C).copy(alpha = 0.88f))
        )
    }
    val borderBrush = if (isLight) {
        Brush.verticalGradient(
            listOf(Color(0xFF0284C7).copy(alpha = 0.35f), Color(0xFFCBD5E1))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.05f))
        )
    }
    val iconColor = if (isLight) Color(0xFF475569) else Color.White.copy(alpha = 0.70f)
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val placeholderColor = if (isLight) Color(0xFF64748B) else Color.White.copy(alpha = 0.45f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(brush = bgBrush)
            .border(
                width = 1.2.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = placeholderColor,
                        fontSize = 14.sp
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = textColor,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Clear search",
                    tint = Color.White.copy(alpha = 0.80f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(role = Role.Button) { onQueryChange("") }
                )
            } else if (onMicClick != null) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Mic",
                    tint = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(role = Role.Button, onClick = onMicClick)
                )
            }
        }
    }
}

// --- 4. Liquid Glass Switch / Toggle (Reference Image 2) ---

@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    Box(
        modifier = modifier
            .width(52.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = if (checked) {
                    Brush.horizontalGradient(
                        listOf(Color(0xFF00E5FF).copy(alpha = 0.85f), Color(0xFF00B0FF).copy(alpha = 0.90f))
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color(0xFF1B2332).copy(alpha = 0.85f), Color(0xFF101622).copy(alpha = 0.90f))
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (checked) Color(0xFF84FFFF) else Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFD4E1F0))
                    )
                )
                .shadow(elevation = 4.dp, shape = CircleShape)
        )
    }
}

// --- 5. Liquid Glass Ring Gauge (Reference Image 2 - 75% Circular Storage Meter) ---

@Composable
fun LiquidGlassRingGauge(
    percentage: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
    strokeWidth: Dp = 10.dp,
    glowColor: Color = Color(0xFF00E5FF),
    label: String = "${(percentage * 100).toInt()}%"
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    val isLight = IplusTheme.palette.theme == com.novo.drive.domain.model.AppTheme.LIGHT

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.toPx() - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val arcSize = Size(diameter, diameter)

            // Background muted ring track
            drawArc(
                color = if (isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.10f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Glowing cyan active progress arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.70f),
                        Color(0xFF00E676),
                        glowColor
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isLight) Color(0xFF0F172A) else Color.White
            )
            Text(
                text = "Used",
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = if (isLight) Color(0xFF475569) else Color.White.copy(alpha = 0.65f)
            )
        }
    }
}

// --- 6. Liquid Glass Dot Loading Spinner (Reference Image 2) ---

@Composable
fun LiquidGlassSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    dotCount: Int = 8,
    activeColor: Color = Color(0xFF00E5FF)
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
        val radius = size.toPx() / 2.5f
        val dotRadius = size.toPx() / 14f

        for (i in 0 until dotCount) {
            val angleDeg = (i * (360f / dotCount) + rotation) % 360f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val x = center.x + radius * cos(angleRad).toFloat()
            val y = center.y + radius * sin(angleRad).toFloat()
            val alpha = (i + 1f) / dotCount.toFloat()

            drawCircle(
                color = activeColor.copy(alpha = alpha),
                radius = dotRadius,
                center = Offset(x, y)
            )
        }
    }
}

