package com.novo.drive.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.novo.drive.domain.model.AppTheme

data class IplusPalette(
    val theme: AppTheme,
    val name: String,
    val backgroundBrush: Brush,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val surfaceBorder: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val glowColor: Color,
    val folderPhotosColor: Color,
    val folderVideosColor: Color,
    val folderSongsColor: Color,
    val folderDocsColor: Color,
    val gaugeGradient: List<Color>,
    val navBarAura: Color
)

// Theme 1: Cyber Guard (Deep Emerald Matrix & Neon Green Shield)
val CyberGuardPalette = IplusPalette(
    theme = AppTheme.CYBER_GUARD,
    name = "Cyber Guard",
    backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFF011413), Color(0xFF042421), Color(0xFF021614))
    ),
    backgroundColor = Color(0xFF021A18),
    surfaceColor = Color(0xFF072B26),
    surfaceBorder = Color(0xFF00E676).copy(alpha = 0.35f),
    cardBackground = Color(0xFF08332D).copy(alpha = 0.78f),
    cardBorder = Color(0xFF00E676).copy(alpha = 0.45f),
    accentPrimary = Color(0xFF00E676),
    accentSecondary = Color(0xFF00B0FF),
    glowColor = Color(0xFF00E676),
    folderPhotosColor = Color(0xFF00E676),
    folderVideosColor = Color(0xFF00B0FF),
    folderSongsColor = Color(0xFF8B5CF6),
    folderDocsColor = Color(0xFF69F0AE),
    gaugeGradient = listOf(Color(0xFF00E676), Color(0xFF00B0FF), Color(0xFF69F0AE)),
    navBarAura = Color(0xFF00E676)
)

// Theme 2: Hussuu Cyber (Obsidian Cyber Deck — Electric Cyan & Cyber Violet)
val HussuuCyberPalette = IplusPalette(
    theme = AppTheme.HUSSUU_CYBER,
    name = "Hussuu Cyber",
    backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFF030712), Color(0xFF0B132B), Color(0xFF050814))
    ),
    backgroundColor = Color(0xFF050B18),
    surfaceColor = Color(0xFF0E1A33),
    surfaceBorder = Color(0xFF00E5FF).copy(alpha = 0.35f),
    cardBackground = Color(0xFF112040).copy(alpha = 0.75f),
    cardBorder = Color(0xFF00E5FF).copy(alpha = 0.45f),
    accentPrimary = Color(0xFF00E5FF),
    accentSecondary = Color(0xFF8B5CF6),
    glowColor = Color(0xFF00E5FF),
    folderPhotosColor = Color(0xFF00E5FF),
    folderVideosColor = Color(0xFF8B5CF6),
    folderSongsColor = Color(0xFFFF007F),
    folderDocsColor = Color(0xFF14F1D9),
    gaugeGradient = listOf(Color(0xFF00E5FF), Color(0xFF8B5CF6), Color(0xFF14F1D9)),
    navBarAura = Color(0xFF00E5FF)
)

// Theme 3: Minimal Drive (Midnight Slate Navy — Sky Cobalt Blue)
val MinimalDrivePalette = IplusPalette(
    theme = AppTheme.MINIMAL_DRIVE,
    name = "Minimal Drive",
    backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
    ),
    backgroundColor = Color(0xFF0F172A),
    surfaceColor = Color(0xFF1E293B),
    surfaceBorder = Color(0xFF38BDF8).copy(alpha = 0.25f),
    cardBackground = Color(0xFF1E293B).copy(alpha = 0.85f),
    cardBorder = Color(0xFF38BDF8).copy(alpha = 0.35f),
    accentPrimary = Color(0xFF38BDF8),
    accentSecondary = Color(0xFF34D399),
    glowColor = Color(0xFF38BDF8),
    folderPhotosColor = Color(0xFF38BDF8),
    folderVideosColor = Color(0xFFA78BFA),
    folderSongsColor = Color(0xFF8B5CF6),
    folderDocsColor = Color(0xFF34D399),
    gaugeGradient = listOf(Color(0xFF38BDF8), Color(0xFF34D399), Color(0xFF8B5CF6)),
    navBarAura = Color(0xFF38BDF8)
)

// Theme 4: Donut Neon (Synthwave Neon — Hot Pink, Cyan & Mint)
val DonutNeonPalette = IplusPalette(
    theme = AppTheme.DONUT_NEON,
    name = "Donut Neon",
    backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFF180628), Color(0xFF2C0B45), Color(0xFF150424))
    ),
    backgroundColor = Color(0xFF1A072E),
    surfaceColor = Color(0xFF2B0E47),
    surfaceBorder = Color(0xFFFF2A85).copy(alpha = 0.35f),
    cardBackground = Color(0xFF351257).copy(alpha = 0.78f),
    cardBorder = Color(0xFFFF2A85).copy(alpha = 0.45f),
    accentPrimary = Color(0xFFFF2A85),
    accentSecondary = Color(0xFF00E5FF),
    glowColor = Color(0xFFFF2A85),
    folderPhotosColor = Color(0xFFFF2A85),
    folderVideosColor = Color(0xFF06D6A0),
    folderSongsColor = Color(0xFF8B5CF6),
    folderDocsColor = Color(0xFF118AB2),
    gaugeGradient = listOf(Color(0xFFFF2A85), Color(0xFF8B5CF6), Color(0xFF06D6A0)),
    navBarAura = Color(0xFFFF2A85)
)

// Clean Light Palette: Real crisp ice light theme
val CleanLightPalette = IplusPalette(
    theme = AppTheme.LIGHT,
    name = "Clean Light",
    backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFF8FAFC))
    ),
    backgroundColor = Color(0xFFF8FAFC),
    surfaceColor = Color(0xFFFFFFFF),
    surfaceBorder = Color(0xFF0284C7).copy(alpha = 0.25f),
    cardBackground = Color(0xFFFFFFFF).copy(alpha = 0.92f),
    cardBorder = Color(0xFFE2E8F0),
    accentPrimary = Color(0xFF0284C7),
    accentSecondary = Color(0xFF0D9488),
    glowColor = Color(0xFF0284C7).copy(alpha = 0.25f),
    folderPhotosColor = Color(0xFF0284C7),
    folderVideosColor = Color(0xFF7C3AED),
    folderSongsColor = Color(0xFFEA580C),
    folderDocsColor = Color(0xFF16A34A),
    gaugeGradient = listOf(Color(0xFF0284C7), Color(0xFF0D9488), Color(0xFF6366F1)),
    navBarAura = Color(0xFF0284C7).copy(alpha = 0.25f)
)

// Dedicated Media Theme 1: Audiophile Luxury Studio (Music Player & Audio Experience)
val AcousticStudioPalette = IplusPalette(
    theme = AppTheme.DARK,
    name = "Acoustic Studio",
    backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFF0D111A), Color(0xFF07090E), Color(0xFF040508))
    ),
    backgroundColor = Color(0xFF07090E),
    surfaceColor = Color(0xFF111622),
    surfaceBorder = Color.White.copy(alpha = 0.12f),
    cardBackground = Color(0xFF131824).copy(alpha = 0.85f),
    cardBorder = Color.White.copy(alpha = 0.14f),
    accentPrimary = Color(0xFF00E5FF),
    accentSecondary = Color(0xFF38BDF8),
    glowColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
    folderPhotosColor = Color(0xFF00E5FF),
    folderVideosColor = Color(0xFF8B5CF6),
    folderSongsColor = Color(0xFF8B5CF6),
    folderDocsColor = Color(0xFF00E676),
    gaugeGradient = listOf(Color(0xFF00E5FF), Color(0xFFFF2A85), Color(0xFF8B5CF6)),
    navBarAura = Color(0xFF00E5FF)
)

// Dedicated Media Theme 2: Cinematic OLED Theater (Video Player & Cinema Experience)
val CinemaTheaterPalette = IplusPalette(
    theme = AppTheme.DARK,
    name = "Cinema Theater",
    backgroundBrush = Brush.verticalGradient(
        listOf(Color(0xFF000000), Color(0xFF0B0B10), Color(0xFF000000))
    ),
    backgroundColor = Color(0xFF000000),
    surfaceColor = Color(0xFF0E0E14),
    surfaceBorder = Color(0xFF00E5FF).copy(alpha = 0.35f),
    cardBackground = Color(0xFF13131D).copy(alpha = 0.85f),
    cardBorder = Color(0xFF00E5FF).copy(alpha = 0.45f),
    accentPrimary = Color(0xFF00E5FF),
    accentSecondary = Color(0xFF8B5CF6),
    glowColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
    folderPhotosColor = Color(0xFF00F0FF),
    folderVideosColor = Color(0xFF8B5CF6),
    folderSongsColor = Color(0xFFFF007F),
    folderDocsColor = Color(0xFF10B981),
    gaugeGradient = listOf(Color(0xFF00E5FF), Color(0xFF8B5CF6), Color(0xFFE63946)),
    navBarAura = Color(0xFF00E5FF)
)

typealias KunjoPalette = IplusPalette

val LocalKunjoPalette = staticCompositionLocalOf { HussuuCyberPalette }
val LocalIplusPalette = LocalKunjoPalette

object KunjoTheme {
    val palette: KunjoPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalKunjoPalette.current
}

object IplusTheme {
    val palette: KunjoPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalKunjoPalette.current
}

fun getPaletteForTheme(theme: AppTheme, isDark: Boolean = true): IplusPalette = when (theme) {
    AppTheme.CYBER_GUARD -> CyberGuardPalette
    AppTheme.HUSSUU_CYBER -> HussuuCyberPalette
    AppTheme.MINIMAL_DRIVE -> MinimalDrivePalette
    AppTheme.DONUT_NEON -> DonutNeonPalette
    AppTheme.LIGHT -> CleanLightPalette
    AppTheme.DARK -> HussuuCyberPalette
    AppTheme.SYSTEM -> if (isDark) HussuuCyberPalette else CleanLightPalette
}

fun getColorSchemeForTheme(theme: AppTheme, isDark: Boolean = true): ColorScheme = when (theme) {
    AppTheme.CYBER_GUARD -> darkColorScheme(
        primary = Color(0xFF00E676),
        onPrimary = Color(0xFF003915),
        primaryContainer = Color(0xFF005322),
        onPrimaryContainer = Color(0xFFB9F6CA),
        secondary = Color(0xFF00B0FF),
        onSecondary = Color(0xFF00325B),
        secondaryContainer = Color(0xFF00497D),
        onSecondaryContainer = Color(0xFFBBE9FF),
        background = Color(0xFF021A18),
        onBackground = Color(0xFFE8F8F5),
        surface = Color(0xFF072B26),
        onSurface = Color(0xFFE8F8F5),
        surfaceVariant = Color(0xFF08332D),
        onSurfaceVariant = Color(0xFF80CBC4),
        surfaceContainerLowest = Color(0xFF011413),
        surfaceContainerLow = Color(0xFF042421),
        surfaceContainer = Color(0xFF072B26),
        surfaceContainerHigh = Color(0xFF0D3B34),
        surfaceContainerHighest = Color(0xFF134C43),
        outline = Color(0xFF00E676).copy(alpha = 0.40f),
        outlineVariant = Color(0xFF00E676).copy(alpha = 0.20f)
    )
    AppTheme.HUSSUU_CYBER, AppTheme.DARK -> darkColorScheme(
        primary = Color(0xFF00E5FF),
        onPrimary = Color(0xFF031820),
        primaryContainer = Color(0xFF0A2E3D),
        onPrimaryContainer = Color(0xFF7DF9FF),
        secondary = Color(0xFF8B5CF6),
        onSecondary = Color(0xFF2E1065),
        secondaryContainer = Color(0xFF4C1D95),
        onSecondaryContainer = Color(0xFFDDD6FE),
        background = Color(0xFF050B18),
        onBackground = Color(0xFFEAF4FF),
        surface = Color(0xFF0E1A33),
        onSurface = Color(0xFFEAF4FF),
        surfaceVariant = Color(0xFF112040),
        onSurfaceVariant = Color(0xFF94A3B8),
        surfaceContainerLowest = Color(0xFF030712),
        surfaceContainerLow = Color(0xFF081022),
        surfaceContainer = Color(0xFF0E1A33),
        surfaceContainerHigh = Color(0xFF15264A),
        surfaceContainerHighest = Color(0xFF1E3463),
        outline = Color(0xFF00E5FF).copy(alpha = 0.45f),
        outlineVariant = Color(0xFF00E5FF).copy(alpha = 0.20f)
    )
    AppTheme.MINIMAL_DRIVE -> darkColorScheme(
        primary = Color(0xFF38BDF8),
        onPrimary = Color(0xFF082F49),
        primaryContainer = Color(0xFF0C4A6E),
        onPrimaryContainer = Color(0xFFBAE6FD),
        secondary = Color(0xFF34D399),
        onSecondary = Color(0xFF064E3B),
        secondaryContainer = Color(0xFF065F46),
        onSecondaryContainer = Color(0xFFA7F3D0),
        background = Color(0xFF0F172A),
        onBackground = Color(0xFFF1F5F9),
        surface = Color(0xFF1E293B),
        onSurface = Color(0xFFF1F5F9),
        surfaceVariant = Color(0xFF27354A),
        onSurfaceVariant = Color(0xFF94A3B8),
        surfaceContainerLowest = Color(0xFF0F172A),
        surfaceContainerLow = Color(0xFF172033),
        surfaceContainer = Color(0xFF1E293B),
        surfaceContainerHigh = Color(0xFF28364E),
        surfaceContainerHighest = Color(0xFF334155),
        outline = Color(0xFF38BDF8).copy(alpha = 0.35f),
        outlineVariant = Color(0xFF38BDF8).copy(alpha = 0.18f)
    )
    AppTheme.DONUT_NEON -> darkColorScheme(
        primary = Color(0xFFFF2A85),
        onPrimary = Color(0xFF4A0022),
        primaryContainer = Color(0xFF700037),
        onPrimaryContainer = Color(0xFFFFD9E2),
        secondary = Color(0xFF00E5FF),
        onSecondary = Color(0xFF00363D),
        secondaryContainer = Color(0xFF004F58),
        onSecondaryContainer = Color(0xFF70F5FF),
        background = Color(0xFF1A072E),
        onBackground = Color(0xFFFDF2F8),
        surface = Color(0xFF2B0E47),
        onSurface = Color(0xFFFDF2F8),
        surfaceVariant = Color(0xFF351257),
        onSurfaceVariant = Color(0xFFD8B4FE),
        surfaceContainerLowest = Color(0xFF140424),
        surfaceContainerLow = Color(0xFF200938),
        surfaceContainer = Color(0xFF2B0E47),
        surfaceContainerHigh = Color(0xFF3B1560),
        surfaceContainerHighest = Color(0xFF4A1C78),
        outline = Color(0xFFFF2A85).copy(alpha = 0.45f),
        outlineVariant = Color(0xFF00E5FF).copy(alpha = 0.25f)
    )
    AppTheme.LIGHT -> lightColorScheme(
        primary = Color(0xFF0284C7),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE0F2FE),
        onPrimaryContainer = Color(0xFF0369A1),
        secondary = Color(0xFF0D9488),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCFBF1),
        onSecondaryContainer = Color(0xFF115E59),
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF0F172A),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF475569),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF8FAFC),
        surfaceContainer = Color(0xFFF1F5F9),
        surfaceContainerHigh = Color(0xFFE2E8F0),
        surfaceContainerHighest = Color(0xFFCBD5E1),
        outline = Color(0xFFCBD5E1),
        outlineVariant = Color(0xFFE2E8F0)
    )
    AppTheme.SYSTEM -> if (isDark) {
        getColorSchemeForTheme(AppTheme.HUSSUU_CYBER, true)
    } else {
        getColorSchemeForTheme(AppTheme.LIGHT, false)
    }
}

val AcousticStudioColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF70F5FF),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color(0xFF280056),
    secondaryContainer = Color(0xFF411475),
    onSecondaryContainer = Color(0xFFE8DDFF),
    background = Color(0xFF0A0317),
    onBackground = Color(0xFFF3EBFF),
    surface = Color(0xFF180A30),
    onSurface = Color(0xFFF3EBFF),
    surfaceVariant = Color(0xFF241045),
    onSurfaceVariant = Color(0xFFD6C2F0),
    surfaceContainerLowest = Color(0xFF07020F),
    surfaceContainerLow = Color(0xFF0E0420),
    surfaceContainer = Color(0xFF180A30),
    surfaceContainerHigh = Color(0xFF26124D),
    surfaceContainerHighest = Color(0xFF341A66),
    outline = Color(0xFF00E5FF).copy(alpha = 0.45f),
    outlineVariant = Color(0xFFFF2A85).copy(alpha = 0.25f)
)

val CinemaTheaterColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF70F5FF),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color(0xFF280056),
    secondaryContainer = Color(0xFF411475),
    onSecondaryContainer = Color(0xFFE8DDFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF0F0F2),
    surface = Color(0xFF0E0E14),
    onSurface = Color(0xFFF0F0F2),
    surfaceVariant = Color(0xFF181822),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF08080C),
    surfaceContainer = Color(0xFF0E0E14),
    surfaceContainerHigh = Color(0xFF161620),
    surfaceContainerHighest = Color(0xFF22222E),
    outline = Color(0xFF00E5FF).copy(alpha = 0.40f),
    outlineVariant = Color(0xFF8B5CF6).copy(alpha = 0.22f)
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DedicatedAcousticStudioTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalKunjoPalette provides AcousticStudioPalette,
        LocalIplusPalette provides AcousticStudioPalette
    ) {
        androidx.compose.material3.MaterialExpressiveTheme(
            colorScheme = AcousticStudioColorScheme,
            typography = IplusDriveTypography,
            shapes = IplusDriveShapes,
            content = content
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DedicatedCinemaTheaterTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalKunjoPalette provides CinemaTheaterPalette,
        LocalIplusPalette provides CinemaTheaterPalette
    ) {
        androidx.compose.material3.MaterialExpressiveTheme(
            colorScheme = CinemaTheaterColorScheme,
            typography = IplusDriveTypography,
            shapes = IplusDriveShapes,
            content = content
        )
    }
}

