package com.novo.drive.presentation.applock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.novo.drive.presentation.components.glass.GlassActionIconButton
import com.novo.drive.presentation.components.glass.LiquidGlassButton
import com.novo.drive.presentation.components.glass.LiquidGlassSurface
import com.novo.drive.presentation.platform.LocalDeviceOwnerGate
import com.novo.drive.presentation.theme.IplusTheme
import com.novo.drive.resources.Res
import com.novo.drive.resources.lock_prompt_title
import com.novo.drive.resources.lock_screen_title
import com.novo.drive.resources.lock_unlock
import org.jetbrains.compose.resources.stringResource

/** Falls back to the device credential when biometrics are unavailable or locked out. */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val deviceOwnerGate = LocalDeviceOwnerGate.current
    val palette = IplusTheme.palette
    var statusText by remember { mutableStateOf<String?>(null) }
    var promptTrigger by remember { mutableIntStateOf(0) }
    val promptTitle = stringResource(Res.string.lock_prompt_title)

    LaunchedEffect(promptTrigger) {
        deviceOwnerGate.require(
            title = promptTitle,
            subtitle = "",
            onDenied = { error -> statusText = error }
        ) {
            onUnlocked()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.backgroundBrush)
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            glowColor = palette.accentPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                GlassActionIconButton(
                    icon = Icons.Filled.Lock,
                    onClick = { promptTrigger++ },
                    size = 68.dp,
                    shape = RoundedCornerShape(22.dp),
                    tint = palette.accentPrimary,
                    glowColor = palette.accentPrimary
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = stringResource(Res.string.lock_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                statusText?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(28.dp))
                LiquidGlassButton(
                    onClick = { promptTrigger++ },
                    accentColor = palette.accentPrimary,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = palette.accentPrimary
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(Res.string.lock_unlock),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
