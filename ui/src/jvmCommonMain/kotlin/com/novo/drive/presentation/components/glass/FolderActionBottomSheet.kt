package com.novo.drive.presentation.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderActionBottomSheet(
    title: String,
    subtitle: String,
    isArchived: Boolean = false,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onDetail: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        dragHandle = null,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF151D2C).copy(alpha = 0.95f), Color(0xFF0A0F1A).copy(alpha = 0.98f))
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.04f))
                    ),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Drag handle pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                )

                Spacer(Modifier.height(4.dp))

                // Header Info: Folder Icon + Title + Metadata Subtitle (Photo 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.14f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Actions List
                ActionItem(icon = Icons.Filled.Edit, label = "Rename (নাম পরিবর্তন)", onClick = { onDismiss(); onRename() })
                ActionItem(icon = Icons.Filled.DriveFileMove, label = "Move (স্থানান্তর)", onClick = { onDismiss(); onMove() })
                ActionItem(icon = Icons.Filled.ContentCopy, label = "Duplicate (কপি তৈরি)", onClick = { onDismiss(); onDuplicate() })
                ActionItem(
                    icon = if (isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                    label = if (isArchived) "Unarchive (আর্কাইভ থেকে ফেরান)" else "Archive (আর্কাইভ সংরক্ষণ)",
                    onClick = { onDismiss(); onArchive() }
                )
                ActionItem(icon = Icons.Filled.FolderZip, label = "Protected .ZIP Share (পাসওয়ার্ড জিপ শেয়ার)", onClick = { onDismiss(); onShare() })
                ActionItem(icon = Icons.Filled.Link, label = "Copy Cloud Path (কুঞ্জ ক্লাউড পাথ কপি)", onClick = { onDismiss(); onCopyLink() })
                ActionItem(icon = Icons.Filled.Info, label = "Detail & Activity (বিস্তারিত তথ্য)", onClick = { onDismiss(); onDetail() })
                ActionItem(
                    icon = Icons.Filled.Delete,
                    label = "Delete (মুছে ফেলুন)",
                    isDestructive = true,
                    onClick = { onDismiss(); onDelete() }
                )
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

