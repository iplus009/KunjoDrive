package com.novo.drive.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.presentation.platform.LocalAppVersion
import com.novo.drive.resources.Res
import com.novo.drive.resources.ic_launcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Kunjo Drive v5.1.1 — 01 Boot / Splash (HUSSUU UI Language)
 * Faceted crystal shard background, corner HUD brackets, Kunjo Drive emblem,
 * "STORE · MANAGE · SYNC · SECURE" tagline, and animated sweep loader.
 */
@Composable
fun KunjoSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appVersion = LocalAppVersion.current
    val cyan = Color(0xFF00E5FF)
    val teal = Color(0xFF14F1D9)
    val violet = Color(0xFF8B5CF6)
    val voidBg = Color(0xFF05070B)
    val mutedText = Color(0xFF61738A)

    val coreScale = remember { Animatable(0.82f) }
    val coreAlpha = remember { Animatable(0f) }
    val sheenSweep = remember { Animatable(-0.6f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleRise = remember { Animatable(16f) }

    val infinite = rememberInfiniteTransition(label = "hussuuSplashSweep")
    val loaderFraction by infinite.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loaderFraction"
    )

    LaunchedEffect(Unit) {
        launch {
            coreAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        launch {
            coreScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        delay(220)
        launch {
            sheenSweep.animateTo(
                targetValue = 1.6f,
                animationSpec = tween(durationMillis = 740, easing = FastOutSlowInEasing)
            )
        }
        launch {
            titleAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420)
            )
        }
        launch {
            titleRise.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        delay(980)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(voidBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSplashFinished
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(cyan.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.34f),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.5f, h * 0.34f),
                radius = w * 0.65f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(violet.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.72f),
                    radius = w * 0.6f
                ),
                center = Offset(w * 0.5f, h * 0.72f),
                radius = w * 0.6f
            )

            val topShard = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.62f, 0f)
                lineTo(w * 0.25f, h * 0.25f)
                lineTo(0f, h * 0.15f)
                close()
            }
            drawPath(
                path = topShard,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF0F2138).copy(alpha = 0.75f), Color(0xFF060A12).copy(alpha = 0.2f))
                )
            )

            val rightShard = Path().apply {
                moveTo(w, h * 0.08f)
                lineTo(w, h * 0.42f)
                lineTo(w * 0.58f, h * 0.30f)
                close()
            }
            drawPath(
                path = rightShard,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF1D1233).copy(alpha = 0.65f), Color(0xFF080D18).copy(alpha = 0.2f))
                )
            )

            val bottomShard = Path().apply {
                moveTo(0f, h * 0.72f)
                lineTo(w * 0.48f, h * 0.85f)
                lineTo(w * 0.28f, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = bottomShard,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF0A1D2E).copy(alpha = 0.65f), Color(0xFF05070B).copy(alpha = 0.2f))
                )
            )

            drawLine(
                color = cyan.copy(alpha = 0.28f),
                start = Offset(0f, h * 0.15f),
                end = Offset(w * 0.62f, 0f),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = violet.copy(alpha = 0.25f),
                start = Offset(w * 0.40f, h),
                end = Offset(w, h * 0.76f),
                strokeWidth = 1.dp.toPx()
            )

            val pad = 20.dp.toPx()
            val len = 18.dp.toPx()
            val stroke = 1.5.dp.toPx()
            val hudColor = cyan.copy(alpha = 0.55f)

            drawLine(hudColor, Offset(pad, pad), Offset(pad + len, pad), stroke)
            drawLine(hudColor, Offset(pad, pad), Offset(pad, pad + len), stroke)
            drawLine(hudColor, Offset(w - pad - len, pad), Offset(w - pad, pad), stroke)
            drawLine(hudColor, Offset(w - pad, pad), Offset(w - pad, pad + len), stroke)
            drawLine(hudColor, Offset(pad, h - pad - len), Offset(pad, h - pad), stroke)
            drawLine(hudColor, Offset(pad, h - pad), Offset(pad + len, h - pad), stroke)
            drawLine(hudColor, Offset(w - pad - len, h - pad), Offset(w - pad, h - pad), stroke)
            drawLine(hudColor, Offset(w - pad, h - pad - len), Offset(w - pad, h - pad), stroke)
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val emblemShape = CutCornerShape(topEnd = 22.dp, bottomStart = 22.dp)
            Box(
                modifier = Modifier
                    .size(134.dp)
                    .scale(coreScale.value)
                    .alpha(coreAlpha.value)
                    .clip(emblemShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF101926),
                                Color(0xFF0B0E16)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                cyan.copy(alpha = 0.85f),
                                teal.copy(alpha = 0.55f),
                                violet.copy(alpha = 0.65f)
                            )
                        ),
                        shape = emblemShape
                    )
                    .drawWithContent {
                        drawContent()
                        val sweep = sheenSweep.value
                        if (sweep in -0.5f..1.6f) {
                            val startX = size.width * sweep
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.30f),
                                        cyan.copy(alpha = 0.18f),
                                        Color.Transparent
                                    ),
                                    start = Offset(startX - size.width * 0.45f, 0f),
                                    end = Offset(startX + size.width * 0.15f, size.height)
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_launcher),
                    contentDescription = "Kunjo Drive Logo",
                    modifier = Modifier
                        .size(116.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = titleRise.value.dp)
                    .alpha(titleAlpha.value)
            ) {
                Text(
                    text = if (com.novo.drive.presentation.common.isBengaliUi()) "কুঞ্জ · KUNJO" else "KUNJO",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.2.sp,
                    color = Color(0xFFECF6FF)
                )
                Text(
                    text = "DRIVE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 6.0.sp,
                    color = cyan
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "STORE · MANAGE · SYNC · SECURE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.0.sp,
                    textAlign = TextAlign.Center,
                    color = mutedText
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .width(76.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, cyan, Color.Transparent)
                            )
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "POWERED BY iPlus ENGINE · v$appVersion",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF94A7BD)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barW = size.width * 0.42f
                    val startX = (size.width * loaderFraction).coerceIn(0f, size.width)
                    val endX = (startX + barW).coerceAtMost(size.width)
                    if (endX > startX) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, cyan, teal),
                                startX = startX,
                                endX = endX
                            ),
                            topLeft = Offset(startX, 0f),
                            size = androidx.compose.ui.geometry.Size(endX - startX, size.height)
                        )
                    }
                }
            }
            Text(
                text = "[ INITIALISING VAULT · INDEXING · E2E ]",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.8.sp,
                color = mutedText
            )
        }
    }
}

@Composable
fun IplusSplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    KunjoSplashScreen(onSplashFinished = onSplashFinished, modifier = modifier)
}

