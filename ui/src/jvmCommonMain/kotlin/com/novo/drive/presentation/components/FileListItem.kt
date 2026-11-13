package com.novo.drive.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.theme.IplusTheme
import com.novo.drive.resources.Res
import com.novo.drive.resources.common_content_description_available_offline
import com.novo.drive.resources.common_content_description_favorite
import com.novo.drive.resources.common_content_description_selected
import com.novo.drive.resources.common_file_size_and_date
import com.novo.drive.resources.common_open
import com.novo.drive.resources.common_select
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    file: DriveFile,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    showFavorite: Boolean = true,
    showAvailableOffline: Boolean = true,
    status: (@Composable () -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
) {
    val compact = LocalCompactLayout.current
    val isLight = MaterialTheme.colorScheme.background.red > 0.5f
    val palette = IplusTheme.palette
    val rowShape = RoundedCornerShape(14.dp)

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
            .combinedClickable(
                onClickLabel = stringResource(
                    if (selectionMode) Res.string.common_select else Res.string.common_open
                ),
                onLongClickLabel = stringResource(Res.string.common_select),
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = if (compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            FileThumbnail(
                file = file,
                contentDescription = null,
                modifier = Modifier
                    .size(if (compact) 38.dp else 46.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(Res.string.common_content_description_selected),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.width(if (compact) 10.dp else 14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (file.versionNumber > 1) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "v${file.versionNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            if (!compact) {
                Text(
                    text = stringResource(
                        Res.string.common_file_size_and_date,
                        Formatters.bytes(file.sizeBytes),
                        Formatters.date(file.modifiedAt)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (file.isFavorite && showFavorite) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = stringResource(Res.string.common_content_description_favorite),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }
        if (file.isAvailableOffline && showAvailableOffline) {
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
        if (status != null) {
            Spacer(Modifier.width(8.dp))
            status()
        }
        Spacer(Modifier.width(8.dp))
        BackupStateBadge(file = file)
        if (onMoreClick != null && !selectionMode) {
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
