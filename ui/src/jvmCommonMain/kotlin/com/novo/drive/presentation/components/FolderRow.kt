package com.novo.drive.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novo.drive.domain.model.DriveFolder
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.theme.IplusTheme
import com.novo.drive.resources.Res
import com.novo.drive.resources.common_content_description_available_offline
import com.novo.drive.resources.common_content_description_favorite
import com.novo.drive.resources.common_select
import org.jetbrains.compose.resources.stringResource

@Composable
fun FolderRow(
    folder: DriveFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showFavorite: Boolean = true,
    showAvailableOffline: Boolean = true,
    onLongClick: () -> Unit = {}
) {
    val compact = LocalCompactLayout.current
    val isLight = MaterialTheme.colorScheme.background.red > 0.5f
    val palette = IplusTheme.palette
    val rowShape = RoundedCornerShape(14.dp)

    val folderTint = androidx.compose.runtime.remember(folder.name, palette) {
        val lower = folder.name.lowercase()
        when {
            lower.contains("photo") || lower.contains("image") || lower.contains("camera") || lower.contains("ছবি") || lower.contains("dcim") -> palette.folderPhotosColor
            lower.contains("video") || lower.contains("movie") || lower.contains("ভিডিও") || lower.contains("cinema") -> palette.folderVideosColor
            lower.contains("music") || lower.contains("song") || lower.contains("audio") || lower.contains("গান") || lower.contains("record") -> palette.folderSongsColor
            lower.contains("doc") || lower.contains("pdf") || lower.contains("file") || lower.contains("ডকুমেন্ট") || lower.contains("নথি") -> palette.folderDocsColor
            else -> palette.accentPrimary
        }
    }

    val rowModifier = if (selected) {
        modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.50f))
            .border(1.dp, palette.accentPrimary.copy(alpha = 0.60f), rowShape)
    } else {
        modifier
            .fillMaxWidth()
            .clip(rowShape)
            .liquidGlassBackground(
                shape = rowShape,
                tintAlpha = if (isLight) 0.85f else 0.55f,
                borderAlpha = if (isLight) 0.35f else 0.28f,
                borderWidth = 1.dp
            )
    }

    Row(
        modifier = rowModifier
            .semantics { if (selected) this.selected = true }
            .combinedClickable(
                onLongClickLabel = stringResource(Res.string.common_select),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = if (compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconShape = CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp)
        Box(
            modifier = Modifier
                .size(if (compact) 38.dp else 46.dp)
                .clip(iconShape)
                .background(
                    brush = Brush.verticalGradient(
                        listOf(folderTint.copy(alpha = 0.20f), folderTint.copy(alpha = 0.06f))
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(folderTint.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    shape = iconShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = folderTint,
                modifier = Modifier.size(if (compact) 20.dp else 24.dp)
            )
        }
        Spacer(Modifier.width(if (compact) 10.dp else 14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = folder.contentsLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (folder.isFavorite && showFavorite) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = stringResource(Res.string.common_content_description_favorite),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
        if (folder.isAvailableOffline && showAvailableOffline) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.OfflinePin,
                contentDescription = stringResource(
                    Res.string.common_content_description_available_offline
                ),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
