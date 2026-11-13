package com.novo.drive.presentation.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.novo.drive.domain.model.AppTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KunjoDriveTheme(
    appTheme: AppTheme = AppTheme.CYBER_GUARD,
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val isDark = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> darkTheme
        else -> true
    }

    val dynamicScheme = if (appTheme == AppTheme.SYSTEM) {
        platformColorScheme(darkTheme = isDark, dynamicColor = dynamicColor)
    } else {
        null
    }
    val colorScheme = dynamicScheme ?: getColorSchemeForTheme(appTheme, isDark)
    val palette = getPaletteForTheme(appTheme, isDark)

    PlatformThemeSideEffects(isDark)

    CompositionLocalProvider(LocalKunjoPalette provides palette) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = IplusDriveTypography,
            shapes = IplusDriveShapes,
            content = content
        )
    }
}

@Composable
fun IplusDriveTheme(
    appTheme: AppTheme = AppTheme.CYBER_GUARD,
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = KunjoDriveTheme(appTheme, darkTheme, dynamicColor, content)
