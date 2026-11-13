package com.novo.drive.presentation.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.novo.drive.core.media.ThumbnailModel
import com.novo.drive.domain.model.MediaAlbum
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.theme.LocalIplusPalette
import com.novo.drive.resources.Res
import com.novo.drive.resources.album_not_in_folder
import com.novo.drive.resources.item_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AlbumCard(
    album: MediaAlbum,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val albumName = album.name ?: stringResource(Res.string.album_not_in_folder)
    val palette = LocalIplusPalette.current
    val isLight = MaterialTheme.colorScheme.background.red > 0.5f
    val shape = MaterialTheme.shapes.large

    Column(
        modifier = modifier
            .clip(shape)
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = if (isLight) 0.85f else 0.55f,
                borderAlpha = if (isLight) 0.35f else 0.28f,
                borderWidth = 1.dp
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.medium)
                .background(palette.cardBackground),
            contentAlignment = Alignment.Center
        ) {
            val coverFileId = album.coverFileId
            if (coverFileId != null) {
                AsyncImage(
                    model = ThumbnailModel(coverFileId),
                    contentDescription = albumName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = palette.folderPhotosColor
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
            Text(
                text = albumName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(Res.plurals.item_count, album.itemCount, album.itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
