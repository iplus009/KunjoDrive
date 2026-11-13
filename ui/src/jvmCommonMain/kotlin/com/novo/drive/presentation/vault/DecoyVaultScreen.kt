package com.novo.drive.presentation.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.domain.model.DriveFolder
import com.novo.drive.domain.model.FileSortField
import com.novo.drive.domain.model.SortDirection
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.TrashRepository
import com.novo.drive.presentation.common.AppBackHandler
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.components.ConfirmDialog
import com.novo.drive.presentation.components.EmptyState
import com.novo.drive.presentation.components.FileThumbnail
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.preview.PreviewSequence
import com.novo.drive.presentation.theme.LocalIplusPalette
import com.novo.drive.resources.Res
import com.novo.drive.resources.common_back
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Vault 2 Screen — Displays files transferred to the second tier vault.
 * Seamless, indistinguishable liquid-glass secure vault.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoyVaultScreen(
    onBack: () -> Unit,
    onOpenFile: (String, PreviewSequence) -> Unit = { _, _ -> },
    onOpenFolder: (String) -> Unit = {},
    settingsRepository: SettingsRepository = koinInject(),
    fileRepository: FileRepository = koinInject(),
    trashRepository: TrashRepository = koinInject()
) {
    val palette = LocalIplusPalette.current
    val isLight = palette.theme == AppTheme.LIGHT
    val primaryText = if (isLight) Color(0xFF0F172A) else Color.White
    val secondaryText = primaryText.copy(alpha = 0.65f)
    val coroutineScope = rememberCoroutineScope()

    val prefs by settingsRepository.preferences.collectAsState(initial = null)
    val secondVaultFileIds = prefs?.secondVaultFileIds ?: emptySet()
    val secondVaultFolderIds = prefs?.secondVaultFolderIds ?: emptySet()

    val files by remember(secondVaultFileIds) {
        if (secondVaultFileIds.isEmpty()) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            fileRepository.observeFilesByIds(secondVaultFileIds.toList())
        }
    }.collectAsState(initial = emptyList())

    val folders by remember(secondVaultFolderIds) {
        fileRepository.observeHiddenFolders().map { all ->
            all.filter { it.id in secondVaultFolderIds }
        }
    }.collectAsState(initial = emptyList())

    var selectedFileIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFolderIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectionMode = selectedFileIds.isNotEmpty() || selectedFolderIds.isNotEmpty()
    val totalSelected = selectedFileIds.size + selectedFolderIds.size
    var confirmTrashDialog by remember { mutableStateOf(false) }

    AppBackHandler(enabled = selectionMode) {
        selectedFileIds = emptySet()
        selectedFolderIds = emptySet()
    }

    if (confirmTrashDialog) {
        ConfirmDialog(
            title = "ট্র্যাশে স্থানান্তর করবেন?",
            message = "নির্বাচিত $totalSelected টি আইটেম ট্র্যাশে পাঠানো হবে।",
            confirmLabel = "ট্র্যাশে পাঠান",
            destructive = true,
            onConfirm = {
                confirmTrashDialog = false
                val fIds = selectedFileIds.toList()
                val dIds = selectedFolderIds.toList()
                selectedFileIds = emptySet()
                selectedFolderIds = emptySet()
                coroutineScope.launch {
                    if (fIds.isNotEmpty()) trashRepository.moveFilesToTrash(fIds)
                    dIds.forEach { trashRepository.moveFolderToTrash(it) }
                    settingsRepository.update { current ->
                        current.copy(
                            secondVaultFileIds = current.secondVaultFileIds - fIds.toSet(),
                            secondVaultFolderIds = current.secondVaultFolderIds - dIds.toSet()
                        )
                    }
                }
            },
            onDismiss = { confirmTrashDialog = false }
        )
    }

    val previewSeq = remember {
        PreviewSequence(
            hiddenOnly = true,
            sortField = FileSortField.DATE_ADDED,
            sortDirection = SortDirection.DESCENDING
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectionMode) "$totalSelected টি নির্বাচিত" else "সিকিউর ভল্ট (Secure Vault)",
                        fontWeight = FontWeight.Bold,
                        color = primaryText,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectionMode) {
                            selectedFileIds = emptySet()
                            selectedFolderIds = emptySet()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (selectionMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back),
                            tint = primaryText
                        )
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            if (totalSelected == files.size + folders.size) {
                                selectedFileIds = emptySet()
                                selectedFolderIds = emptySet()
                            } else {
                                selectedFileIds = files.map { it.id }.toSet()
                                selectedFolderIds = folders.map { it.id }.toSet()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.SelectAll,
                                contentDescription = "Select All",
                                tint = primaryText
                            )
                        }

                        // Restore to 1st Vault
                        IconButton(onClick = {
                            val fIds = selectedFileIds
                            val dIds = selectedFolderIds
                            selectedFileIds = emptySet()
                            selectedFolderIds = emptySet()
                            coroutineScope.launch {
                                settingsRepository.update { current ->
                                    current.copy(
                                        secondVaultFileIds = current.secondVaultFileIds - fIds,
                                        secondVaultFolderIds = current.secondVaultFolderIds - dIds
                                    )
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "১ম ভল্টে ফেরত পাঠান",
                                tint = palette.accentSecondary
                            )
                        }

                        // Unhide to Main Drive
                        IconButton(onClick = {
                            val fIds = selectedFileIds.toList()
                            val dIds = selectedFolderIds.toList()
                            selectedFileIds = emptySet()
                            selectedFolderIds = emptySet()
                            coroutineScope.launch {
                                if (fIds.isNotEmpty()) fileRepository.setFilesHidden(fIds, false)
                                dIds.forEach { fileRepository.setFolderTreeHidden(it, false) }
                                settingsRepository.update { current ->
                                    current.copy(
                                        secondVaultFileIds = current.secondVaultFileIds - fIds.toSet(),
                                        secondVaultFolderIds = current.secondVaultFolderIds - dIds.toSet()
                                    )
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = "আনহাইড করুন",
                                tint = palette.accentPrimary
                            )
                        }

                        // Delete
                        IconButton(onClick = { confirmTrashDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "মুছে ফেলুন",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (files.isEmpty() && folders.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Lock,
                title = "কোনো গোপন ফাইল নেই",
                description = "১ম ভল্ট থেকে ফাইল সিলেক্ট করে '২য় ভল্টে স্থানান্তর করুন' বাটনে চাপলে তা এখানে সম্পূর্ণ সুরক্ষিত থাকবে।",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Folder items
                items(folders, key = { "folder-${it.id}" }) { folder ->
                    val isSelected = folder.id in selectedFolderIds
                    VaultFolderRow(
                        folder = folder,
                        isSelected = isSelected,
                        selectionMode = selectionMode,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = {
                            if (selectionMode) {
                                selectedFolderIds = if (isSelected) selectedFolderIds - folder.id else selectedFolderIds + folder.id
                            } else {
                                onOpenFolder(folder.id)
                            }
                        },
                        onLongClick = {
                            selectedFolderIds = selectedFolderIds + folder.id
                        }
                    )
                }

                // File items
                items(files, key = { "file-${it.id}" }) { file ->
                    val isSelected = file.id in selectedFileIds
                    VaultFileRow(
                        file = file,
                        isSelected = isSelected,
                        selectionMode = selectionMode,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClick = {
                            if (selectionMode) {
                                selectedFileIds = if (isSelected) selectedFileIds - file.id else selectedFileIds + file.id
                            } else {
                                onOpenFile(file.id, previewSeq)
                            }
                        },
                        onLongClick = {
                            selectedFileIds = selectedFileIds + file.id
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultFolderRow(
    folder: DriveFolder,
    isSelected: Boolean,
    selectionMode: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalIplusPalette.current
    val shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = if (isSelected) 0.85f else 0.65f,
                borderAlpha = if (isSelected) 0.80f else 0.35f,
                borderWidth = if (isSelected) 1.5.dp else 1.dp
            )
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp))
                    .background(palette.accentPrimary.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = palette.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folder.fileCount} টি ফাইল • ${Formatters.bytes(folder.totalBytes)}",
                    fontSize = 11.sp,
                    color = secondaryText,
                    maxLines = 1
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultFileRow(
    file: DriveFile,
    isSelected: Boolean,
    selectionMode: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalIplusPalette.current
    val shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = if (isSelected) 0.85f else 0.65f,
                borderAlpha = if (isSelected) 0.80f else 0.35f,
                borderWidth = if (isSelected) 1.5.dp else 1.dp
            )
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileThumbnail(
                file = file,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CutCornerShape(8.dp))
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = file.extension.uppercase().ifBlank { "FILE" },
                    fontSize = 11.sp,
                    color = secondaryText,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .clip(CutCornerShape(4.dp))
                    .background(palette.accentPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = Formatters.bytes(file.sizeBytes),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accentPrimary
                )
            }
        }
    }
}
