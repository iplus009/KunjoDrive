package com.novo.drive.presentation.files

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.unit.sp
import com.novo.drive.core.common.AppResult
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.TransferRepository
import com.novo.drive.presentation.common.AppBackHandler
import com.novo.drive.presentation.components.ConfirmDialog
import com.novo.drive.presentation.components.EmptyState
import com.novo.drive.presentation.components.iconFor
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.platform.LocalFileOpener
import com.novo.drive.presentation.theme.LocalIplusPalette
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

enum class ClipboardAction { CUT, COPY }

data class LocalClipboard(
    val action: ClipboardAction,
    val items: List<File>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IplusLocalFileManagerScreen(
    onBack: () -> Unit,
    fileRepository: FileRepository = koinInject(),
    transferRepository: TransferRepository = koinInject(),
    settingsRepository: SettingsRepository = koinInject()
) {
    val palette = LocalIplusPalette.current
    val fileOpener = LocalFileOpener.current
    val isLight = palette.theme == AppTheme.LIGHT
    val primaryText = if (isLight) Color(0xFF0F172A) else Color.White
    val secondaryText = primaryText.copy(alpha = 0.65f)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val roots = remember { LocalFileOperations.getStorageRoots() }
    var currentDirectory by remember { mutableStateOf(roots.firstOrNull() ?: File("/")) }
    var items by remember { mutableStateOf<List<LocalStorageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var sortBy by remember { mutableStateOf("NAME") }
    var sortAscending by remember { mutableStateOf(true) }

    val filteredItems = remember(items, selectedCategoryFilter, sortBy, sortAscending) {
        val filtered = when (selectedCategoryFilter) {
            "VIDEOS" -> items.filter { it.isDirectory || it.extension.lowercase() in listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "ts") }
            "AUDIO" -> items.filter { it.isDirectory || it.extension.lowercase() in listOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "opus") }
            "PHOTOS" -> items.filter { it.isDirectory || it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp") }
            "DOCS" -> items.filter { it.isDirectory || it.extension.lowercase() in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt") }
            "APKS" -> items.filter { it.isDirectory || it.extension.lowercase() in listOf("apk", "zip", "rar", "7z", "tar", "gz") }
            else -> items
        }
        filtered.sortedWith { a, b ->
            if (a.isDirectory != b.isDirectory) {
                if (a.isDirectory) -1 else 1
            } else {
                val cmp = when (sortBy) {
                    "DATE" -> a.lastModified.compareTo(b.lastModified)
                    "SIZE" -> a.sizeBytes.compareTo(b.sizeBytes)
                    else -> a.name.lowercase().compareTo(b.name.lowercase())
                }
                if (sortAscending) cmp else -cmp
            }
        }
    }

    val isBn = com.novo.drive.presentation.common.isBengaliUi()
    val primaryRoot = remember(roots) { roots.firstOrNull() ?: File("/") }
    val standardShortcuts = remember(primaryRoot, isBn) {
        listOf(
            Triple(if (isBn) "ডাউনলোড" else "Downloads", Icons.Filled.Download, File(primaryRoot, "Download")),
            Triple(if (isBn) "ক্যামেরা" else "Camera", Icons.Filled.PhotoCamera, File(primaryRoot, "DCIM")),
            Triple(if (isBn) "মুভি" else "Movies", Icons.Filled.Movie, File(primaryRoot, "Movies")),
            Triple(if (isBn) "গান" else "Music", Icons.Filled.MusicNote, File(primaryRoot, "Music")),
            Triple(if (isBn) "ডকুমেন্টস" else "Documents", Icons.Filled.Description, File(primaryRoot, "Documents")),
            Triple(if (isBn) "মূল স্টোরেজ" else "Internal Storage", Icons.Filled.Storage, primaryRoot)
        )
    }

    var selectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    val selectionMode = selectedFiles.isNotEmpty()

    var clipboard by remember { mutableStateOf<LocalClipboard?>(null) }
    var renameTarget by remember { mutableStateOf<File?>(null) }
    var renameNewName by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Storage permission gate — Android 11+ needs MANAGE_EXTERNAL_STORAGE
    val storageAccessGranted = rememberStorageAccessGranted()
    val openStorageSettings = openAllFilesAccessSettings()
    if (!storageAccessGranted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Storage,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = secondaryText
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (isBn) "স্টোরেজ অ্যাক্সেস প্রয়োজন" else "Storage Access Required",
                style = MaterialTheme.typography.titleLarge,
                color = primaryText
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isBn) {
                    "লোকাল ফাইল ম্যানেজার ব্যবহার করতে 'সব ফাইলে অ্যাক্সেস' অনুমতি দিন।"
                } else {
                    "Grant 'All files access' permission to use the local file manager."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryText
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = openStorageSettings) {
                Text(if (isBn) "অনুমতি দিন" else "Grant Permission")
            }
        }
        return
    }

    // Load folder contents
    val loadFolder = { dir: File ->
        isLoading = true
        coroutineScope.launch {
            val list = withContext(Dispatchers.IO) {
                runCatching {
                    dir.listFiles()?.mapNotNull { f ->
                        LocalStorageItem(
                            file = f,
                            name = f.name,
                            isDirectory = f.isDirectory,
                            sizeBytes = if (f.isDirectory) 0L else f.length(),
                            lastModified = f.lastModified(),
                            extension = f.extension
                        )
                    }?.sortedWith(compareByDescending<LocalStorageItem> { it.isDirectory }.thenBy { it.name.lowercase() })
                }.getOrDefault(emptyList())
            }
            items = list ?: emptyList()
            isLoading = false
        }
    }

    LaunchedEffect(currentDirectory) {
        selectedFiles = emptySet()
        loadFolder(currentDirectory)
    }

    AppBackHandler(enabled = true) {
        if (selectionMode) {
            selectedFiles = emptySet()
        } else {
            val parent = currentDirectory.parentFile
            val isAtRoot = roots.any { it.absolutePath == currentDirectory.absolutePath } || parent == null
            if (!isAtRoot && parent.canRead()) {
                currentDirectory = parent
            } else {
                onBack()
            }
        }
    }

    // Rename Dialog
    if (renameTarget != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(if (isBn) "রিনেম করুন" else "Rename", fontWeight = FontWeight.Bold, color = primaryText) },
            text = {
                OutlinedTextField(
                    value = renameNewName,
                    onValueChange = { renameNewName = it },
                    label = { Text(if (isBn) "নতুন নাম" else "New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = renameTarget ?: return@TextButton
                    val name = renameNewName.trim()
                    if (name.isNotEmpty() && name != target.name) {
                        coroutineScope.launch {
                            val ok = LocalFileOperations.rename(target, name)
                            if (ok) {
                                snackbarHostState.showSnackbar(
                                    if (isBn) "নাম সফলভাবে পরিবর্তন করা হয়েছে" else "Renamed successfully"
                                )
                                loadFolder(currentDirectory)
                            } else {
                                snackbarHostState.showSnackbar(
                                    if (isBn) "রিনেম ব্যর্থ হয়েছে" else "Rename failed"
                                )
                            }
                        }
                    }
                    renameTarget = null
                }) {
                    Text(if (isBn) "সংরক্ষণ" else "Save", fontWeight = FontWeight.Bold, color = palette.accentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(if (isBn) "বাতিল" else "Cancel", color = secondaryText)
                }
            }
        )
    }

    // Delete Confirm Dialog
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = if (isBn) "মুছে ফেলবেন?" else "Delete?",
            message = if (isBn) {
                "নির্বাচিত ${selectedFiles.size} টি ফাইল/ফোল্ডার স্থায়ীভাবে মুছে ফেলা হবে।"
            } else {
                "${selectedFiles.size} selected file(s)/folder(s) will be permanently deleted."
            },
            confirmLabel = if (isBn) "মুছে ফেলুন" else "Delete",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                val toDelete = selectedFiles.toList()
                selectedFiles = emptySet()
                coroutineScope.launch {
                    var successCount = 0
                    for (file in toDelete) {
                        if (LocalFileOperations.deleteRecursivelySafe(file)) successCount++
                    }
                    snackbarHostState.showSnackbar(
                        if (isBn) "$successCount টি আইটেম মুছে ফেলা হয়েছে" else "$successCount item(s) deleted"
                    )
                    loadFolder(currentDirectory)
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectionMode) {
                                if (isBn) "${selectedFiles.size} টি নির্বাচিত" else "${selectedFiles.size} selected"
                            } else {
                                if (isBn) "ডিভাইস ফাইল ম্যানেজার" else "Device File Manager"
                            },
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            fontSize = 17.sp
                        )
                        if (!selectionMode) {
                            Text(
                                text = currentDirectory.name.ifEmpty { if (isBn) "স্টোরেজ" else "Storage" },
                                fontSize = 11.sp,
                                color = palette.accentPrimary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectionMode) {
                            selectedFiles = emptySet()
                        } else {
                            val parent = currentDirectory.parentFile
                            val isAtRoot = roots.any { it.absolutePath == currentDirectory.absolutePath } || parent == null
                            if (!isAtRoot && parent.canRead()) {
                                currentDirectory = parent
                            } else {
                                onBack()
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (selectionMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryText
                        )
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectedFiles = if (selectedFiles.size == items.size) {
                                emptySet()
                            } else {
                                items.map { it.file }.toSet()
                            }
                        }) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "Select All", tint = primaryText)
                        }
                    } else if (roots.size > 1) {
                        // Storage switcher icon
                        IconButton(onClick = {
                            val nextRoot = roots.firstOrNull { it != currentDirectory } ?: roots.first()
                            currentDirectory = nextRoot
                        }) {
                            Icon(Icons.Filled.SdCard, contentDescription = "Switch Storage", tint = palette.accentPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            // Floating Context Action Bar
            AnimatedVisibility(
                visible = selectionMode || clipboard != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(CutCornerShape(16.dp))
                        .liquidGlassBackground(
                            shape = CutCornerShape(16.dp),
                            tintAlpha = 0.90f,
                            borderAlpha = 0.50f,
                            borderWidth = 1.dp
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    if (selectionMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cut
                            ActionButton(
                                icon = Icons.Filled.ContentCut,
                                label = if (isBn) "কাট" else "Cut",
                                color = palette.accentSecondary,
                                onClick = {
                                    clipboard = LocalClipboard(ClipboardAction.CUT, selectedFiles.toList())
                                    selectedFiles = emptySet()
                                }
                            )

                            // Copy
                            ActionButton(
                                icon = Icons.Filled.ContentCopy,
                                label = if (isBn) "কপি" else "Copy",
                                color = palette.accentPrimary,
                                onClick = {
                                    clipboard = LocalClipboard(ClipboardAction.COPY, selectedFiles.toList())
                                    selectedFiles = emptySet()
                                }
                            )

                            // Rename (Single file only)
                            if (selectedFiles.size == 1) {
                                ActionButton(
                                    icon = Icons.Filled.DriveFileRenameOutline,
                                    label = if (isBn) "রিনেম" else "Rename",
                                    color = Color(0xFF8B5CF6),
                                    onClick = {
                                        val single = selectedFiles.first()
                                        renameTarget = single
                                        renameNewName = single.name
                                    }
                                )
                            }

                            // Upload to Cloud
                            ActionButton(
                                icon = Icons.Filled.CloudUpload,
                                label = if (isBn) "ক্লাউড" else "Cloud",
                                color = Color(0xFF00E676),
                                onClick = {
                                    coroutineScope.launch {
                                        val prefs = settingsRepository.preferences.first()
                                        if (prefs.kunjoAccountType == "SUB" && !prefs.kunjoCanUpload) {
                                            snackbarHostState.showSnackbar(
                                                if (isBn) "সাব-অ্যাকাউন্টে ক্লাউড আপলোডের অনুমতি নেই"
                                                else "Cloud upload is not permitted on sub-accounts"
                                            )
                                            return@launch
                                        }
                                        val filesToUpload = selectedFiles.filter { !it.isDirectory }
                                        selectedFiles = emptySet()
                                        var count = 0
                                        for (f in filesToUpload) {
                                            val result = fileRepository.importLocalFile(f.absolutePath, null)
                                            if (result is AppResult.Success) {
                                                val driveFile = result.value
                                                if (!driveFile.hasRemoteCopy) {
                                                    transferRepository.enqueueUpload(driveFile.id)
                                                }
                                                count++
                                            }
                                        }
                                        snackbarHostState.showSnackbar(
                                            if (isBn) "$count টি ফাইল কুঞ্জ ক্লাউডে ব্যাকআপ নেওয়া শুরু হয়েছে"
                                            else "Started backing up $count file(s) to Kunjo Cloud"
                                        )
                                    }
                                }
                            )

                            // Delete
                            ActionButton(
                                icon = Icons.Filled.Delete,
                                label = if (isBn) "মুছুন" else "Delete",
                                color = MaterialTheme.colorScheme.error,
                                onClick = { showDeleteConfirm = true }
                            )
                        }
                    } else if (clipboard != null) {
                        // Paste Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ContentPaste, contentDescription = null, tint = palette.accentPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) {
                                        "${clipboard?.items?.size} টি ফাইল ${if (clipboard?.action == ClipboardAction.CUT) "মুভ" else "কপি"} করতে প্রস্তুত"
                                    } else {
                                        "${clipboard?.items?.size} item(s) ready to ${if (clipboard?.action == ClipboardAction.CUT) "move" else "copy"}"
                                    },
                                    fontSize = 13.sp,
                                    color = primaryText
                                )
                            }
                            Row {
                                TextButton(onClick = { clipboard = null }) {
                                    Text(if (isBn) "বাতিল" else "Cancel", color = secondaryText)
                                }
                                TextButton(onClick = {
                                    val clip = clipboard ?: return@TextButton
                                    clipboard = null
                                    coroutineScope.launch {
                                        var success = 0
                                        for (src in clip.items) {
                                            val dest = File(currentDirectory, src.name)
                                            val ok = if (clip.action == ClipboardAction.CUT) {
                                                LocalFileOperations.moveFile(src, dest)
                                            } else {
                                                LocalFileOperations.copyFileZeroCopy(src, dest)
                                            }
                                            if (ok) success++
                                        }
                                        snackbarHostState.showSnackbar(
                                            if (isBn) "$success টি ফাইল সফলভাবে পেস্ট করা হয়েছে"
                                            else "Pasted $success item(s) successfully"
                                        )
                                        loadFolder(currentDirectory)
                                    }
                                }) {
                                    Text(
                                        if (isBn) "পেস্ট করুন" else "Paste",
                                        fontWeight = FontWeight.Bold,
                                        color = palette.accentPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Storage Summary Gauge Card (Fossify / MiXplorer Style)
            val totalSpace = currentDirectory.totalSpace
            val freeSpace = currentDirectory.freeSpace
            val usedSpace = (totalSpace - freeSpace).coerceAtLeast(0L)
            val storageFraction = if (totalSpace > 0L) (usedSpace.toFloat() / totalSpace.toFloat()).coerceIn(0f, 1f) else 0f

            if (totalSpace > 0L) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(CutCornerShape(10.dp))
                        .liquidGlassBackground(
                            shape = CutCornerShape(10.dp),
                            tintAlpha = 0.70f,
                            borderAlpha = 0.40f,
                            borderWidth = 1.dp
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Storage, contentDescription = null, tint = palette.accentPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (isBn) "ডিভাইস স্টোরেজ · ${(storageFraction * 100).toInt()}% ব্যবহৃত"
                                    else "Device Storage · ${(storageFraction * 100).toInt()}% used",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryText
                                )
                            }
                            Text(
                                if (isBn) "ফাঁকা: ${LocalFileOperations.formatFileSize(freeSpace)}"
                                else "Free: ${LocalFileOperations.formatFileSize(freeSpace)}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = palette.accentSecondary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { storageFraction },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CutCornerShape(2.dp)),
                            color = palette.accentPrimary,
                            trackColor = palette.surfaceColor.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isBn) {
                                "${LocalFileOperations.formatFileSize(usedSpace)} ব্যবহৃত / সর্বমোট ${LocalFileOperations.formatFileSize(totalSpace)}"
                            } else {
                                "${LocalFileOperations.formatFileSize(usedSpace)} used / ${LocalFileOperations.formatFileSize(totalSpace)} total"
                            },
                            fontSize = 10.sp,
                            color = secondaryText
                        )
                    }
                }
            }

            // Quick Shortcut Chips (Fossify Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                standardShortcuts.forEach { (label, icon, targetDir) ->
                    val isActive = currentDirectory.absolutePath == targetDir.absolutePath
                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(6.dp))
                            .liquidGlassBackground(
                                shape = CutCornerShape(6.dp),
                                tintAlpha = if (isActive) 0.85f else 0.45f,
                                borderAlpha = if (isActive) 0.80f else 0.25f,
                                borderWidth = 1.dp
                            )
                            .clickable {
                                if (targetDir.exists() && targetDir.canRead()) {
                                    currentDirectory = targetDir
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(icon, contentDescription = null, tint = if (isActive) palette.accentPrimary else secondaryText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(label, fontSize = 11.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal, color = if (isActive) primaryText else secondaryText)
                        }
                    }
                }
            }

            // Category Filter Chips + Sort Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filterOptions = listOf(
                    "ALL" to if (isBn) "সব ফাইল" else "All Files",
                    "VIDEOS" to if (isBn) "ভিডিও" else "Videos",
                    "AUDIO" to if (isBn) "অডিও" else "Audio",
                    "PHOTOS" to if (isBn) "ছবি" else "Images",
                    "DOCS" to if (isBn) "ডকুমেন্ট" else "Docs",
                    "APKS" to if (isBn) "ইনস্টলার/জিপ" else "Installers/Zip"
                )
                filterOptions.forEach { (key, label) ->
                    val isSelected = selectedCategoryFilter == key
                    Box(
                        modifier = Modifier
                            .clip(CutCornerShape(6.dp))
                            .background(if (isSelected) palette.accentPrimary.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { selectedCategoryFilter = key }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) palette.accentPrimary else secondaryText
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Sort toggle button
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(6.dp))
                        .clickable {
                            when (sortBy) {
                                "NAME" -> sortBy = "DATE"
                                "DATE" -> sortBy = "SIZE"
                                else -> {
                                    sortBy = "NAME"
                                    sortAscending = !sortAscending
                                }
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Sort, contentDescription = null, tint = palette.accentSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = (when (sortBy) {
                                "DATE" -> if (isBn) "তারিখ" else "Date"
                                "SIZE" -> if (isBn) "আকার" else "Size"
                                else -> if (isBn) "নাম" else "Name"
                            }) + if (sortAscending) " ↑" else " ↓",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentSecondary
                        )
                    }
                }
            }

            // Breadcrumbs Bar
            BreadcrumbsBar(
                currentDirectory = currentDirectory,
                roots = roots,
                primaryText = primaryText,
                onNavigate = { currentDirectory = it }
            )

            if (filteredItems.isEmpty() && !isLoading) {
                EmptyState(
                    icon = Icons.Filled.Folder,
                    title = if (isBn) "ফোল্ডারটি ফাঁকা" else "Folder is empty",
                    description = if (isBn) "এই ডিরেক্টরিতে কোনো ফাইল বা ফোল্ডার পাওয়া যায়নি।" else "No files or folders found in this directory.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.file.absolutePath }) { item ->
                        val isSelected = item.file in selectedFiles
                        LocalFileItemRow(
                            item = item,
                            isSelected = isSelected,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onClick = {
                                if (selectionMode) {
                                    selectedFiles = if (isSelected) selectedFiles - item.file else selectedFiles + item.file
                                } else {
                                    if (item.isDirectory) {
                                        currentDirectory = item.file
                                    } else {
                                        fileOpener.openFile(item.file)
                                    }
                                }
                            },
                            onLongClick = {
                                selectedFiles = selectedFiles + item.file
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbsBar(
    currentDirectory: File,
    roots: List<File>,
    primaryText: Color,
    onNavigate: (File) -> Unit
) {
    val scrollState = rememberScrollState()
    val segments = remember(currentDirectory) {
        val list = mutableListOf<File>()
        var curr: File? = currentDirectory
        while (curr != null) {
            list.add(curr)
            if (roots.any { it.absolutePath == curr.absolutePath }) break
            curr = curr.parentFile
        }
        list.reversed()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = primaryText.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        segments.forEachIndexed { index, seg ->
            val isLast = index == segments.size - 1
            val name = if (roots.any { it.absolutePath == seg.absolutePath }) "Internal Storage" else seg.name
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                color = if (isLast) LocalIplusPalette.current.accentPrimary else primaryText.copy(alpha = 0.7f),
                modifier = Modifier
                    .clip(CutCornerShape(4.dp))
                    .clickable(enabled = !isLast) { onNavigate(seg) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            if (!isLast) {
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = primaryText.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun LocalFileItemRow(
    item: LocalStorageItem,
    isSelected: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalIplusPalette.current
    val shape = CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp)
    val dateStr = remember(item.lastModified) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(item.lastModified))
    }

    val category = remember(item.file) {
        if (item.isDirectory) FileCategory.OTHER
        else when (item.file.extension.lowercase()) {
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> FileCategory.IMAGE
            "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "ts" -> FileCategory.VIDEO
            "mp3", "m4a", "wav", "flac", "ogg", "aac", "opus" -> FileCategory.AUDIO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "json", "xml" -> FileCategory.DOCUMENT
            "zip", "rar", "7z", "tar", "gz", "apk" -> FileCategory.ARCHIVE
            else -> FileCategory.OTHER
        }
    }
    val badgeColor = remember(category, item.isDirectory, palette) {
        if (item.isDirectory) palette.accentPrimary
        else when (category) {
            FileCategory.IMAGE -> Color(0xFF38BDF8)
            FileCategory.VIDEO -> Color(0xFFA855F7)
            FileCategory.AUDIO -> Color(0xFFFB923C)
            FileCategory.DOCUMENT -> Color(0xFF34D399)
            FileCategory.ARCHIVE -> Color(0xFFF43F5E)
            else -> palette.accentSecondary
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = if (isSelected) 0.85f else 0.60f,
                borderAlpha = if (isSelected) 0.80f else 0.30f,
                borderWidth = if (isSelected) 1.5.dp else 1.dp
            )
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with Category Badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CutCornerShape(topEnd = 8.dp, bottomStart = 8.dp))
                    .background(badgeColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isDirectory) Icons.Filled.Folder else iconFor(category),
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Details
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = secondaryText,
                    maxLines = 1
                )
            }

            // Size HUD on Right
            if (!item.isDirectory) {
                Box(
                    modifier = Modifier
                        .clip(CutCornerShape(6.dp))
                        .background(palette.accentPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = LocalFileOperations.formatFileSize(item.sizeBytes),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CutCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
fun LocalFileManagerScreen(
    onBack: () -> Unit
) = IplusLocalFileManagerScreen(onBack = onBack)


