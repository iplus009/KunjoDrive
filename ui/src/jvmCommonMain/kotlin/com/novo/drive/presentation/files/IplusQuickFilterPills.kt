package com.novo.drive.presentation.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.theme.LocalIplusPalette

enum class IplusQuickFileFilter {
    ALL, MEDIA, DOCS, APKS, ARCHIVES
}

typealias QuickFileFilter = IplusQuickFileFilter

/**
 * iPlus Liquid Glass Quick Filter Bar.
 * Fast horizontal category filter bar for the Files screen.
 */
@Composable
fun IplusQuickFilterPills(
    selectedFilter: IplusQuickFileFilter,
    onFilterSelected: (IplusQuickFileFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isBn = com.novo.drive.presentation.common.isBengaliUi()
        FilterPill(
            icon = Icons.Filled.Folder,
            label = if (isBn) "সকল ফাইল" else "All Files",
            isSelected = selectedFilter == IplusQuickFileFilter.ALL,
            onClick = { onFilterSelected(IplusQuickFileFilter.ALL) }
        )
        FilterPill(
            icon = Icons.Filled.PhotoLibrary,
            label = if (isBn) "ছবি ও ভিডিও" else "Photos & Videos",
            isSelected = selectedFilter == IplusQuickFileFilter.MEDIA,
            onClick = { onFilterSelected(IplusQuickFileFilter.MEDIA) }
        )
        FilterPill(
            icon = Icons.Filled.Description,
            label = if (isBn) "ডকুমেন্টস" else "Documents",
            isSelected = selectedFilter == IplusQuickFileFilter.DOCS,
            onClick = { onFilterSelected(IplusQuickFileFilter.DOCS) }
        )
        FilterPill(
            icon = Icons.Filled.Android,
            label = if (isBn) "অ্যাপস (APKs)" else "Apps (APKs)",
            isSelected = selectedFilter == IplusQuickFileFilter.APKS,
            onClick = { onFilterSelected(IplusQuickFileFilter.APKS) }
        )
        FilterPill(
            icon = Icons.Filled.FolderZip,
            label = if (isBn) "আর্কাইভ (ZIP)" else "Archives (ZIP)",
            isSelected = selectedFilter == IplusQuickFileFilter.ARCHIVES,
            onClick = { onFilterSelected(IplusQuickFileFilter.ARCHIVES) }
        )
    }
}

@Composable
fun QuickFilterPills(
    selectedFilter: IplusQuickFileFilter,
    onFilterSelected: (IplusQuickFileFilter) -> Unit,
    modifier: Modifier = Modifier
) = IplusQuickFilterPills(selectedFilter, onFilterSelected, modifier)

@Composable
private fun FilterPill(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalIplusPalette.current
    val pillShape = RoundedCornerShape(12.dp)
    val tint = if (isSelected) palette.accentPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(pillShape)
            .liquidGlassBackground(
                shape = pillShape,
                tintAlpha = if (isSelected) 0.85f else 0.40f,
                borderAlpha = if (isSelected) 0.80f else 0.25f,
                borderWidth = 1.dp
            )
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = tint
            )
        }
    }
}

