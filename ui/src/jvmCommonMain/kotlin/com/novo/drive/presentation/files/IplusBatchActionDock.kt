package com.novo.drive.presentation.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.theme.LocalIplusPalette

/**
 * iPlus Floating Liquid Glass Batch Action Dock.
 * Displays when multiple files/folders are selected in Files view.
 */
@Composable
fun IplusBatchActionDock(
    selectedCount: Int,
    onDownload: () -> Unit,
    onMove: () -> Unit,
    onSecureZip: () -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalIplusPalette.current
    val isVisible = selectedCount > 0

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        val dockShape = RoundedCornerShape(26.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(dockShape)
                .liquidGlassBackground(
                    shape = dockShape,
                    tintAlpha = 0.94f,
                    borderAlpha = 0.55f,
                    borderWidth = 1.dp
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Selected Count Capsule Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.accentPrimary.copy(alpha = 0.20f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$selectedCount টি ফাইল",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentPrimary
                        )
                    }

                    IconButton(
                        onClick = onClearSelection,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Batch Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DockActionButton(
                        icon = Icons.Filled.FileDownload,
                        label = "ডাউনলোড",
                        tint = palette.accentPrimary,
                        onClick = onDownload
                    )
                    DockActionButton(
                        icon = Icons.Filled.DriveFileMove,
                        label = "মুভ",
                        tint = Color.White,
                        onClick = onMove
                    )
                    DockActionButton(
                        icon = Icons.Filled.Lock,
                        label = "লক জিপ",
                        tint = Color(0xFFFFD54F), // PRESERVED Part 2 Yellow #13
                        onClick = onSecureZip
                    )
                    DockActionButton(
                        icon = Icons.Filled.Delete,
                        label = "ডিলিট",
                        tint = Color(0xFFFF5252),
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun BatchActionDock(
    selectedCount: Int,
    onDownload: () -> Unit,
    onMove: () -> Unit,
    onSecureZip: () -> Unit,
    onDelete: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) = IplusBatchActionDock(
    selectedCount = selectedCount,
    onDownload = onDownload,
    onMove = onMove,
    onSecureZip = onSecureZip,
    onDelete = onDelete,
    onClearSelection = onClearSelection,
    modifier = modifier
)

@Composable
private fun DockActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    val buttonShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(buttonShape)
            .liquidGlassBackground(
                shape = buttonShape,
                tintAlpha = 0.50f,
                borderAlpha = 0.40f,
                borderWidth = 1.dp
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

