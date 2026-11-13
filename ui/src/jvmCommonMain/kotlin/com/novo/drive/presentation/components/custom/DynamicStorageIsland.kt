package com.novo.drive.presentation.components.custom

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.presentation.theme.LocalIplusPalette

/**
 * Kunjo Drive Precision Storage Hub
 * Ultra-fast, zero-recomposition CPU drain, human-crafted modern UI.
 */
@Composable
fun DynamicStorageIsland(
    usedBytes: Long,
    totalQuotaBytes: Long,
    fileCount: Int,
    cloudStatusText: String,
    modifier: Modifier = Modifier,
    onSyncClick: () -> Unit = {}
) {
    val palette = LocalIplusPalette.current
    val accentColor = palette.accentPrimary
    val isLight = palette.theme.name == "LIGHT"
    val isBn = LocalLocaleList.current.firstOrNull()?.language?.startsWith("bn") == true

    val usedGb = usedBytes / (1024f * 1024f * 1024f)
    val quotaGb = if (totalQuotaBytes > 0) totalQuotaBytes / (1024f * 1024f * 1024f) else 0f
    val targetFraction = if (totalQuotaBytes > 0) {
        (usedBytes.toFloat() / totalQuotaBytes.toFloat()).coerceIn(0.02f, 1f)
    } else {
        0.18f
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(dampingRatio = 0.85f),
        label = "storageFraction"
    )

    val cardShape = RoundedCornerShape(20.dp)
    val cardBg = if (isLight) Color(0xFFFFFFFF) else Color(0xFF0F1523)
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val subTextColor = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    val trackBg = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E2536)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.20f)
            )
            .clip(cardShape)
            .background(cardBg)
            .border(
                width = 1.dp,
                color = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.10f),
                shape = cardShape
            )
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top Row: Cloud Status & Sync Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = cloudStatusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLight) Color(0xFF047857) else Color(0xFF34D399)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E2536))
                        .border(
                            1.dp,
                            if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSyncClick() }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sync",
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isBn) "সিঙ্ক" else "Sync",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }

            // Middle: Large Storage Numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "%.1f GB".format(usedGb),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif,
                            color = textColor
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (quotaGb > 0) "/ %.0f GB".format(quotaGb) else (if (isBn) "/ আনলিমিটেড ক্লাউড" else "/ Unlimited Cloud"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = subTextColor,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }
                    val securedLabel = if (isBn) "${fileCount}টি ফাইল সংরক্ষিত" else "$fileCount files secured"
                    Text(
                        text = securedLabel,
                        fontSize = 12.sp,
                        color = subTextColor
                    )
                }

                val percentVal = if (totalQuotaBytes > 0) ((usedBytes.toFloat() / totalQuotaBytes.toFloat()) * 100).toInt() else 0
                if (percentVal > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$percentVal%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }

            // Bottom: Sleek Precision Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(trackBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(accentColor)
                )
            }
        }
    }
}
