package com.novo.drive.presentation.components.glass

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.model.StorageSlice
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.theme.LocalIplusPalette

@Composable
fun CyberStorageGaugeCard(
    slices: List<StorageSlice>,
    modifier: Modifier = Modifier,
    quotaBytes: Long = 0L,
    isFamilyMember: Boolean = false,
    isEncrypted: Boolean = false,
    isSyncing: Boolean = false,
    onSyncClick: () -> Unit = {}
) {
    val palette = LocalIplusPalette.current
    val isLight = palette.theme == AppTheme.LIGHT
    val isBn = com.novo.drive.presentation.common.isBengaliUi()
    val primaryText = if (isLight) Color(0xFF0F172A) else Color(0xFFECF6FF)
    val secondaryText = if (isLight) Color(0xFF475569) else Color(0xFF8DA2BA)

    val totalBytes = remember(slices) { slices.sumOf { it.totalBytes } }
    val totalFiles = remember(slices) { slices.sumOf { it.fileCount } }

    val hasQuota = isFamilyMember && quotaBytes > 0L
    val displayPercent = remember(totalBytes, quotaBytes, hasQuota) {
        when {
            totalBytes <= 0L -> 0f
            hasQuota -> (totalBytes.toDouble() / quotaBytes.toDouble()).toFloat().coerceIn(0.01f, 1f)
            else -> {
                val gb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                (kotlin.math.ln(1.0 + gb) / kotlin.math.ln(1.0 + 128.0)).toFloat().coerceIn(0.05f, 0.96f)
            }
        }
    }

    val ringLabel = remember(totalBytes, hasQuota, displayPercent) {
        when {
            hasQuota -> "${(displayPercent * 100).toInt()}%"
            totalBytes <= 0L -> "0%"
            else -> "∞"
        }
    }

    val docsBytes = remember(slices) {
        slices.filter { it.category == FileCategory.DOCUMENT || it.category == FileCategory.ARCHIVE }
            .sumOf { it.totalBytes }
    }
    val videosBytes = remember(slices) {
        slices.filter { it.category == FileCategory.VIDEO }.sumOf { it.totalBytes }
    }
    val photosBytes = remember(slices) {
        slices.filter { it.category == FileCategory.IMAGE }.sumOf { it.totalBytes }
    }
    val otherBytes = remember(slices) {
        slices.filter { it.category == FileCategory.AUDIO || it.category == FileCategory.OTHER }
            .sumOf { it.totalBytes }
    }

    val panelShape = if (palette.theme == AppTheme.MINIMAL_DRIVE) {
        RoundedCornerShape(22.dp)
    } else {
        CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        palette.cardBackground,
                        palette.surfaceColor.copy(alpha = if (isLight) 0.95f else 0.85f)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        palette.accentPrimary.copy(alpha = if (isLight) 0.40f else 0.55f),
                        palette.accentSecondary.copy(alpha = if (isLight) 0.25f else 0.40f)
                    )
                ),
                shape = panelShape
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (palette.theme) {
                AppTheme.MINIMAL_DRIVE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Kunjo Cloud Storage",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryText
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (totalBytes > 0L) Formatters.bytes(totalBytes) else "0 B",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                        }
                        StatusSyncChip(isSyncing, isFamilyMember, primaryText, palette.accentSecondary, onSyncClick)
                    }
                    Text(
                        text = if (hasQuota) {
                            "Used of ${Formatters.bytes(quotaBytes)} Family Quota • Max 2 GB/file"
                        } else {
                            if (isBn) {
                                "$totalFiles টি ফাইল • সীমাহীন কুঞ্জ ক্লাউড স্টোরেজ"
                            } else {
                                "$totalFiles file(s) • Unlimited Kunjo Cloud Storage"
                            }
                        },
                        fontSize = 12.sp,
                        color = secondaryText
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(primaryText.copy(alpha = 0.10f))
                    ) {
                        if (totalBytes > 0L) {
                            val sum = totalBytes.toFloat()
                            if (docsBytes > 0L) {
                                Box(
                                    modifier = Modifier
                                        .weight((docsBytes.toDouble() / totalBytes.toDouble()).toFloat())
                                        .fillMaxHeight()
                                        .background(palette.folderDocsColor)
                                )
                            }
                            if (videosBytes > 0L) {
                                Box(
                                    modifier = Modifier
                                        .weight((videosBytes.toDouble() / totalBytes.toDouble()).toFloat())
                                        .fillMaxHeight()
                                        .background(palette.folderVideosColor)
                                )
                            }
                            if (photosBytes > 0L) {
                                Box(
                                    modifier = Modifier
                                        .weight((photosBytes.toDouble() / totalBytes.toDouble()).toFloat())
                                        .fillMaxHeight()
                                        .background(palette.folderPhotosColor)
                                )
                            }
                            if (otherBytes > 0L) {
                                Box(
                                    modifier = Modifier
                                        .weight((otherBytes.toDouble() / totalBytes.toDouble()).toFloat())
                                        .fillMaxHeight()
                                        .background(palette.folderSongsColor)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendDotItem("Docs", palette.folderDocsColor, secondaryText)
                        LegendDotItem("Videos", palette.folderVideosColor, secondaryText)
                        LegendDotItem("Images", palette.folderPhotosColor, secondaryText)
                        LegendDotItem("Audio/Other", palette.folderSongsColor, secondaryText)
                    }
                }

                else -> {
                    // Theme-driven Precision Readout Panel (Cyber Guard / Hussuu Cyber / Donut Neon / Clean Light)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LiquidGlassRingGauge(
                            percentage = displayPercent,
                            size = 96.dp,
                            strokeWidth = 9.dp,
                            glowColor = palette.glowColor,
                            label = ringLabel
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (totalBytes > 0L) Formatters.bytes(totalBytes) else "0.0 GB",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                color = primaryText
                            )
                            Text(
                                text = if (hasQuota) {
                                    "OF ${Formatters.bytes(quotaBytes)} FAMILY QUOTA"
                                } else {
                                    "OF ∞ KUNJO CLOUD · OCCUPIED"
                                },
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.2.sp,
                                color = secondaryText
                            )

                            Spacer(Modifier.height(4.dp))

                            HudTagRow(
                                label = if (isBn) "মোট ক্লাউড ফাইল" else "Total Cloud Files",
                                count = if (isBn) "$totalFiles টি ফাইল" else "$totalFiles file(s)",
                                color = palette.accentPrimary,
                                textColor = primaryText
                            )
                            HudTagRow(
                                label = if (isBn) "ক্লাউড নিরাপত্তা" else "Cloud Security",
                                count = if (isEncrypted) {
                                    if (isBn) "AES-256 লকড" else "AES-256 Locked"
                                } else {
                                    if (isBn) "কুঞ্জ শিল্ড সক্রিয়" else "Kunjo Shield Active"
                                },
                                color = palette.accentSecondary,
                                textColor = primaryText
                            )
                        }
                    }

                    // Bottom Beveled Action Bar
                    val actionAccent = palette.accentPrimary
                    val barShape = CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(barShape)
                            .background(actionAccent.copy(alpha = if (isLight) 0.12f else 0.10f))
                            .border(1.dp, actionAccent.copy(alpha = if (isLight) 0.45f else 0.35f), barShape)
                            .clickable(role = Role.Button, onClick = onSyncClick)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSyncing) {
                                "SYNCING KUNJO CLOUD VAULT..."
                            } else if (isFamilyMember) {
                                "FAMILY CLOUD · MAX 2 GB/FILE"
                            } else {
                                "STORAGE DETAILS · SYNC NOW"
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.6.sp,
                            color = actionAccent
                        )
                        Text(
                            text = "›",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = actionAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusSyncChip(
    isSyncing: Boolean,
    isFamilyMember: Boolean,
    primaryText: Color,
    accent: Color,
    onSyncClick: () -> Unit
) {
    val isBn = com.novo.drive.presentation.common.isBengaliUi()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.40f), RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onSyncClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = when {
                    isSyncing -> if (isBn) "সিঙ্ক হচ্ছে..." else "Syncing..."
                    isFamilyMember -> if (isBn) "ফ্যামিলি ক্লাউড" else "Family Cloud"
                    else -> if (isBn) "কুঞ্জ ক্লাউড" else "Kunjo Cloud"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = primaryText
            )
            Icon(
                imageVector = if (isSyncing) Icons.Filled.Sync else Icons.Filled.CloudDone,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun LegendDotItem(label: String, color: Color, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = textColor
        )
    }
}

@Composable
private fun HudTagRow(label: String, count: String, color: Color, textColor: Color) {
    val cutShape = CutCornerShape(topEnd = 6.dp, bottomStart = 6.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cutShape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), cutShape)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 10.sp, color = textColor.copy(alpha = 0.85f))
            Text(text = count, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}
