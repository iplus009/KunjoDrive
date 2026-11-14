package com.novo.drive.presentation.video

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.model.FileQuerySpec
import com.novo.drive.domain.model.FileSortField
import com.novo.drive.domain.model.SortDirection
import com.novo.drive.domain.model.TransferTask
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.SettingsRepository
import com.novo.drive.domain.repository.TransferRepository
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.components.EmptyState
import com.novo.drive.presentation.components.FileThumbnail
import com.novo.drive.presentation.components.glass.liquidGlassBackground
import com.novo.drive.presentation.files.IplusLocalFileOperations
import com.novo.drive.presentation.platform.LocalFileOpener
import com.novo.drive.presentation.preview.PreviewSequence
import com.novo.drive.presentation.theme.DedicatedCinemaTheaterTheme
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel

enum class CinemaSourceTab { LIBRARY, DOWNLOADS, DEVICE }

data class LocalVideoItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val lastModified: Long
)

class IplusVideoPlayerViewModel(
    fileRepository: FileRepository,
    val settingsRepository: SettingsRepository,
    transferRepository: TransferRepository
) : ViewModel() {

    val cloudVideos: StateFlow<List<DriveFile>> = fileRepository.observeFiles(
        FileQuerySpec(
            categories = listOf(FileCategory.VIDEO),
            sortField = FileSortField.DATE_MODIFIED,
            sortDirection = SortDirection.DESCENDING
        )
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeDownloads: StateFlow<List<TransferTask>> = transferRepository.observeActiveDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deviceVideos = MutableStateFlow<List<LocalVideoItem>>(emptyList())
    val deviceVideos: StateFlow<List<LocalVideoItem>> = _deviceVideos

    init {
        scanDeviceVideos()
    }

    fun scanDeviceVideos() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                val found = mutableListOf<LocalVideoItem>()
                val roots = IplusLocalFileOperations.getStorageRoots()
                val targetFolderNames = listOf("DCIM", "Movies", "Download", "WhatsApp Video", "Telegram")
                for (root in roots) {
                    scanDir(root, found, targetFolderNames, depth = 0, maxDepth = 4)
                }
                found.sortedByDescending { it.lastModified }
            }
            _deviceVideos.value = list
        }
    }

    private fun scanDir(
        dir: File,
        results: MutableList<LocalVideoItem>,
        targetFolders: List<String>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth || !dir.canRead()) return
        val children = dir.listFiles() ?: return
        for (f in children) {
            if (f.isDirectory) {
                if (depth == 0) {
                    if (targetFolders.any { f.name.contains(it, ignoreCase = true) }) {
                        scanDir(f, results, targetFolders, depth + 1, maxDepth)
                    }
                } else {
                    scanDir(f, results, targetFolders, depth + 1, maxDepth)
                }
            } else {
                if (f.name.startsWith(".") || f.name.startsWith("pending-", ignoreCase = true) || f.length() < 1024L) {
                    continue
                }
                val ext = f.extension.lowercase()
                if (ext in VIDEO_EXTENSIONS) {
                    results.add(
                        LocalVideoItem(
                            file = f,
                            name = f.name,
                            sizeBytes = f.length(),
                            lastModified = f.lastModified()
                        )
                    )
                }
            }
        }
    }

    private companion object {
        val VIDEO_EXTENSIONS = listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "ts")
    }
}

private sealed interface CinemaDownloadItem {
    val key: String

    data class Active(
        val task: TransferTask,
        val matchedFile: DriveFile?
    ) : CinemaDownloadItem {
        override val key: String = "dl_active_${task.id}"
    }

    data class Completed(
        val video: DriveFile
    ) : CinemaDownloadItem {
        override val key: String = "dl_done_${video.id}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IplusVideoPlayerScreen(
    onBack: () -> Unit,
    onOpenFile: (String, PreviewSequence) -> Unit,
    viewModel: IplusVideoPlayerViewModel = koinViewModel()
) {
    DedicatedCinemaTheaterTheme {
        val isBn = com.novo.drive.presentation.common.isBengaliUi()
        val fileOpener = LocalFileOpener.current

        var currentTab by remember { mutableStateOf(CinemaSourceTab.LIBRARY) }
        var searchQuery by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("ALL") }
        var downloadsCategory by remember { mutableStateOf("ALL") }
        var activeMiniPlayerFile by remember { mutableStateOf<DriveFile?>(null) }

        val cloudVideos by viewModel.cloudVideos.collectAsState()
        val activeDownloads by viewModel.activeDownloads.collectAsState()
        val deviceVideos by viewModel.deviceVideos.collectAsState()

        val videoSequence = remember {
            PreviewSequence(
                categories = listOf(FileCategory.VIDEO),
                sortField = FileSortField.DATE_MODIFIED,
                sortDirection = SortDirection.DESCENDING
            )
        }

        // Storage calculations for downloads hub
        val (totalStorageBytes, freeStorageBytes) = remember {
            runCatching {
                val root = IplusLocalFileOperations.getStorageRoots().firstOrNull()
                    ?: File(System.getProperty("user.home") ?: "/")
                val total = root.totalSpace.let { if (it > 0L) it else 128L * 1024 * 1024 * 1024 }
                val free = root.usableSpace.let { if (it > 0L) it else root.freeSpace }
                Pair(total, free)
            }.getOrDefault(Pair(128L * 1024 * 1024 * 1024, 64L * 1024 * 1024 * 1024))
        }
        val usedStorageBytes = (totalStorageBytes - freeStorageBytes).coerceAtLeast(0L)
        val storageUsedRatio = (usedStorageBytes.toFloat() / totalStorageBytes.toFloat()).coerceIn(0f, 1f)
        val storageUsedAngle = 360f * storageUsedRatio
        val usedStorageGb = (usedStorageBytes / (1024.0 * 1024.0 * 1024.0)).roundToInt()
        val freeStorageGb = (freeStorageBytes / (1024.0 * 1024.0 * 1024.0)).roundToInt()

        val filteredCloudVideos = remember(cloudVideos, searchQuery, selectedCategory) {
            cloudVideos.filter { video ->
                val matchesSearch = searchQuery.isBlank() || video.name.contains(searchQuery, ignoreCase = true)
                val matchesCategory = when (selectedCategory) {
                    "MOVIES" -> video.name.contains("movie", ignoreCase = true) || video.sizeBytes > 700 * 1024 * 1024L
                    "SERIES" -> video.name.contains("s0", ignoreCase = true) || video.name.contains("ep", ignoreCase = true)
                    "CLIPS" -> video.sizeBytes < 150 * 1024 * 1024L
                    "HD" -> video.name.contains("1080", ignoreCase = true) || video.name.contains("720", ignoreCase = true) || video.name.contains("hd", ignoreCase = true)
                    else -> true
                }
                matchesSearch && matchesCategory
            }
        }

        val cloudVideoById = remember(cloudVideos) { cloudVideos.associateBy { it.id } }
        val downloadItems = remember(cloudVideos, activeDownloads, searchQuery, downloadsCategory, cloudVideoById) {
            val activeVideoTasks = activeDownloads.filter { task ->
                val isVideoTask = task.fileId?.let { it in cloudVideoById } == true ||
                    task.displayName.substringAfterLast('.', "").lowercase() in listOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "ts")
                isVideoTask && (searchQuery.isBlank() || task.displayName.contains(searchQuery, ignoreCase = true))
            }.map { task ->
                CinemaDownloadItem.Active(task = task, matchedFile = task.fileId?.let { cloudVideoById[it] })
            }
            val activeFileIds = activeVideoTasks.mapNotNull { it.task.fileId }.toSet()
            val offlineVideos = cloudVideos.filter { video ->
                video.hasLocalCopy &&
                    video.id !in activeFileIds &&
                    (searchQuery.isBlank() || video.name.contains(searchQuery, ignoreCase = true))
            }.map { CinemaDownloadItem.Completed(it) }

            when (downloadsCategory) {
                "ACTIVE" -> activeVideoTasks
                "COMPLETED" -> offlineVideos
                else -> activeVideoTasks + offlineVideos
            }
        }

        val filteredDeviceVideos = remember(deviceVideos, searchQuery) {
            if (searchQuery.isBlank()) deviceVideos
            else deviceVideos.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF07090E))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0D121F),
                            Color(0xFF07090E),
                            Color(0xFF040609)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF8B5CF6).copy(alpha = 0.20f),
                                Color(0xFF00E5FF).copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = if (isBn) "আইপ্লাস সিনেমা · iPlus CINEMA" else "iPlus CINEMA",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = if (isBn) "২০০% ভলিউম বুস্ট • স্ট্রিমিং ও অফলাইন ডাউনলোড হাব" else "200% Volume Boost • Streaming & Offline Download Hub",
                                    fontSize = 10.sp,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                bottomBar = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .liquidGlassBackground(
                                shape = RoundedCornerShape(24.dp),
                                tintAlpha = 0.90f,
                                borderAlpha = 0.25f,
                                borderWidth = 1.dp
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CinemaBottomNavTab(
                                label = if (isBn) "লাইব্রেরি" else "Library",
                                icon = Icons.Filled.Movie,
                                isSelected = currentTab == CinemaSourceTab.LIBRARY,
                                accentColor = Color(0xFF00E5FF),
                                onClick = { currentTab = CinemaSourceTab.LIBRARY }
                            )
                            CinemaBottomNavTab(
                                label = if (isBn) "ডাউনলোড হাব" else "Downloads",
                                icon = Icons.Filled.CloudDownload,
                                isSelected = currentTab == CinemaSourceTab.DOWNLOADS,
                                accentColor = Color(0xFF8B5CF6),
                                onClick = { currentTab = CinemaSourceTab.DOWNLOADS }
                            )
                            CinemaBottomNavTab(
                                label = if (isBn) "ডিভাইস" else "Device",
                                icon = Icons.Filled.PhoneAndroid,
                                isSelected = currentTab == CinemaSourceTab.DEVICE,
                                accentColor = Color(0xFF00E676),
                                onClick = { currentTab = CinemaSourceTab.DEVICE }
                            )
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    when (currentTab) {
                        CinemaSourceTab.LIBRARY -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    placeholder = {
                                        Text(
                                            if (isBn) "মুভি বা ভিডিও সার্চ করুন..." else "Search movies or videos...",
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF00E5FF)) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        focusedIndicatorColor = Color(0xFF00E5FF),
                                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.15f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            val categories = listOf(
                                "ALL" to if (isBn) "সব ভিডিও" else "All Videos",
                                "MOVIES" to if (isBn) "মুভি" else "Movies",
                                "SERIES" to if (isBn) "ওয়েব সিরিজ" else "Series",
                                "CLIPS" to if (isBn) "ক্লিপস" else "Clips",
                                "HD" to if (isBn) "এইচডি" else "HD"
                            )
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(categories) { (catKey, catLabel) ->
                                    val isSelected = selectedCategory == catKey
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 38.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                            .clickable { selectedCategory = catKey }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = catLabel,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                if (filteredCloudVideos.isEmpty()) {
                                    EmptyState(
                                        icon = Icons.Filled.Movie,
                                        title = if (isBn) "কোনো ক্লাউড ভিডিও পাওয়া যায়নি" else "No Cloud Videos Found",
                                        description = if (isBn) "টেলিগ্রাম কুঞ্জ ড্রাইভে ভিডিও আপলোড করলে তা সরাসরি এখানে স্ট্রিমিং ও দেখা যাবে।" else "Upload videos to Kunjo Drive to stream and watch them right here.",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(filteredCloudVideos, key = { it.id }) { video ->
                                            CinemaVideoListRow(
                                                video = video,
                                                onPlay = {
                                                    activeMiniPlayerFile = video
                                                    onOpenFile(video.id, videoSequence)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            activeMiniPlayerFile?.let { miniVideo ->
                                CinemaMiniPlayerBar(
                                    video = miniVideo,
                                    onExpand = { onOpenFile(miniVideo.id, videoSequence) },
                                    onClose = { activeMiniPlayerFile = null }
                                )
                            }
                        }

                        CinemaSourceTab.DOWNLOADS -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .liquidGlassBackground(
                                        shape = RoundedCornerShape(22.dp),
                                        tintAlpha = 0.88f,
                                        borderAlpha = 0.25f,
                                        borderWidth = 1.dp
                                    )
                                    .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.40f), RoundedCornerShape(22.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(72.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawArc(
                                                color = Color.White.copy(alpha = 0.12f),
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                            drawArc(
                                                brush = Brush.sweepGradient(
                                                    listOf(
                                                        Color(0xFF00E5FF),
                                                        Color(0xFF8B5CF6),
                                                        Color(0xFF00E5FF)
                                                    )
                                                ),
                                                startAngle = -90f,
                                                sweepAngle = storageUsedAngle,
                                                useCenter = false,
                                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }
                                        Text(
                                            text = "${(storageUsedRatio * 100).roundToInt()}%",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }

                                    Spacer(Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBn) "ডিভাইস স্টোরেজ স্পেস" else "Device Storage Space",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = if (isBn) "$usedStorageGb GB ব্যবহৃত • $freeStorageGb GB অবশিষ্ট" else "$usedStorageGb GB used • $freeStorageGb GB free",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.70f)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF00E676))
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (isBn) {
                                                    "সক্রিয় ডাউনলোড: ${activeDownloads.size} • অফলাইন: ${cloudVideos.count { it.hasLocalCopy }}"
                                                } else {
                                                    "Active downloads: ${activeDownloads.size} • Offline: ${cloudVideos.count { it.hasLocalCopy }}"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF00E676)
                                            )
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    placeholder = {
                                        Text(
                                            if (isBn) "ডাউনলোড ফাইলে অনুসন্ধান..." else "Search downloaded videos...",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                        focusedIndicatorColor = Color(0xFF8B5CF6),
                                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.15f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(Modifier.height(6.dp))

                                val downloadChips = listOf(
                                    "ALL" to if (isBn) "সব ডাউনলোড" else "All Downloads",
                                    "ACTIVE" to if (isBn) "চলমান" else "Active",
                                    "COMPLETED" to if (isBn) "সম্পূর্ণ" else "Completed"
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(downloadChips) { (chipKey, chipLabel) ->
                                        val isSelected = downloadsCategory == chipKey
                                        Box(
                                            modifier = Modifier
                                                .heightIn(min = 38.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable { downloadsCategory = chipKey }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = chipLabel,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                if (downloadItems.isEmpty()) {
                                    EmptyState(
                                        icon = Icons.Filled.CloudDownload,
                                        title = if (downloadsCategory == "ACTIVE") {
                                            if (isBn) "কোনো চলমান ডাউনলোড নেই" else "No Active Downloads"
                                        } else {
                                            if (isBn) "কোনো ডাউনলোডকৃত ভিডিও নেই" else "No Downloaded Videos"
                                        },
                                        description = if (isBn) {
                                            "ক্লাউড লাইব্রেরির যেকোনো ভিডিও অফলাইনে সেভ করলে এখানে অগ্রগতি ও অফলাইন ফাইল দেখতে পাবেন।"
                                        } else {
                                            "Save any video from your cloud library for offline playback to track progress and view it here."
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(downloadItems, key = { it.key }) { item ->
                                            when (item) {
                                                is CinemaDownloadItem.Active -> CinemaActiveDownloadRow(
                                                    task = item.task,
                                                    video = item.matchedFile,
                                                    onOpen = {
                                                        item.matchedFile?.let { onOpenFile(it.id, videoSequence) }
                                                    }
                                                )

                                                is CinemaDownloadItem.Completed -> CinemaCompletedDownloadRow(
                                                    video = item.video,
                                                    onOpen = { onOpenFile(item.video.id, videoSequence) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        CinemaSourceTab.DEVICE -> {
                            Box(modifier = Modifier.weight(1f)) {
                                if (filteredDeviceVideos.isEmpty()) {
                                    EmptyState(
                                        icon = Icons.Filled.SmartDisplay,
                                        title = if (isBn) "কোনো ডিভাইস ভিডিও পাওয়া যায়নি" else "No Device Videos Found",
                                        description = if (isBn) "ফোনের ক্যামেরা বা মুভি ফোল্ডারে ভিডিও থাকলে তা সরাসরি এখানে দেখা যাবে।" else "Videos in your device's camera or movies folders will appear here automatically.",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(filteredDeviceVideos, key = { it.file.absolutePath }) { item ->
                                            CinemaDeviceVideoListRow(
                                                item = item,
                                                onPlay = { fileOpener.openFile(item.file) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CinemaVideoListRow(
    video: DriveFile,
    onPlay: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val ext = remember(video.name) {
        video.name.substringAfterLast('.', "").uppercase().ifBlank { "VIDEO" }
    }
    val statusLabel = if (video.hasLocalCopy) "OFFLINE" else "STREAM"
    val statusColor = if (video.hasLocalCopy) Color(0xFF00E676) else Color(0xFF00E5FF)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(shape)
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = 0.85f,
                borderAlpha = 0.20f,
                borderWidth = 1.dp
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .clickable(role = Role.Button, onClick = onPlay)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                FileThumbnail(file = video, modifier = Modifier.fillMaxSize())

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = ext,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = video.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = Formatters.bytes(video.sizeBytes),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.70f)
                    )

                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.40f)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CinemaActiveDownloadRow(
    task: TransferTask,
    video: DriveFile?,
    onOpen: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(shape)
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = 0.85f,
                borderAlpha = 0.20f,
                borderWidth = 1.dp
            )
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f), shape)
            .clickable(enabled = video != null, role = Role.Button, onClick = onOpen)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                if (video != null) {
                    FileThumbnail(file = video, modifier = Modifier.fillMaxSize())
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = task.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = Color(0xFF00E5FF),
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${Formatters.bytes(task.transferredBytes)} / ${Formatters.bytes(task.sizeBytes)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    val speedText = if (task.speedBytesPerSecond > 0) {
                        "${Formatters.bytes(task.speedBytesPerSecond)}/s"
                    } else {
                        "${(task.progress * 100).roundToInt()}%"
                    }
                    Text(
                        text = speedText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}

@Composable
private fun CinemaCompletedDownloadRow(
    video: DriveFile,
    onOpen: () -> Unit
) {
    val isBn = com.novo.drive.presentation.common.isBengaliUi()
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(shape)
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = 0.85f,
                borderAlpha = 0.20f,
                borderWidth = 1.dp
            )
            .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.30f), shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                FileThumbnail(file = video, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = video.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { 1.0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape),
                    color = Color(0xFF8B5CF6),
                    trackColor = Color.White.copy(alpha = 0.15f)
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isBn) "${Formatters.bytes(video.sizeBytes)} • অফলাইন প্রস্তুত" else "${Formatters.bytes(video.sizeBytes)} • Offline Ready",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                    Text(
                        text = "READY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                }
            }
        }
    }
}

@Composable
private fun CinemaDeviceVideoListRow(
    item: LocalVideoItem,
    onPlay: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(shape)
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = 0.85f,
                borderAlpha = 0.20f,
                borderWidth = 1.dp
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .clickable(role = Role.Button, onClick = onPlay)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF00E676).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Movie,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = IplusLocalFileOperations.formatFileSize(item.sizeBytes),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00E676).copy(alpha = 0.15f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "LOCAL",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CinemaMiniPlayerBar(
    video: DriveFile,
    onExpand: () -> Unit,
    onClose: () -> Unit
) {
    val isBn = com.novo.drive.presentation.common.isBengaliUi()
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(56.dp)
            .clip(shape)
            .liquidGlassBackground(
                shape = shape,
                tintAlpha = 0.92f,
                borderAlpha = 0.35f,
                borderWidth = 1.dp
            )
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.40f), shape)
            .clickable(onClick = onExpand)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                FileThumbnail(file = video, modifier = Modifier.fillMaxSize())
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isBn) "ট্যাপ করে ফুলস্ক্রিনে দেখুন" else "Tap to watch fullscreen",
                    fontSize = 10.sp,
                    color = Color(0xFF00E5FF)
                )
            }

            IconButton(onClick = onExpand) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color(0xFF00E5FF))
            }

            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Clear, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun CinemaBottomNavTab(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) accentColor.copy(alpha = 0.60f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) accentColor else Color.White.copy(alpha = 0.60f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.60f)
            )
        }
    }
}

