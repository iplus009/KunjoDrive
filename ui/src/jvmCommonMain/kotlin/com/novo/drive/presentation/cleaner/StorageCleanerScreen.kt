package com.novo.drive.presentation.cleaner

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.novo.drive.core.common.AppResult
import com.novo.drive.domain.model.AppTheme
import com.novo.drive.domain.model.DriveFile
import com.novo.drive.domain.model.FileSortField
import com.novo.drive.domain.model.SortDirection
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.TrashRepository
import com.novo.drive.presentation.common.CollectSnackbarMessages
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.common.UiText
import com.novo.drive.presentation.common.toUiText
import com.novo.drive.presentation.components.BottomBarSnackbarHost
import com.novo.drive.presentation.components.ConfirmDialog
import com.novo.drive.presentation.components.EmptyState
import com.novo.drive.presentation.components.FileThumbnail
import com.novo.drive.presentation.components.glass.GlassPillButton
import com.novo.drive.presentation.components.glass.GlassSegmentedTabs
import com.novo.drive.presentation.components.glass.LiquidGlassCard
import com.novo.drive.presentation.components.liftedTopAppBarColors
import com.novo.drive.presentation.components.rememberToolbarLift
import com.novo.drive.presentation.preview.PreviewSequence
import com.novo.drive.presentation.theme.LocalIplusPalette
import com.novo.drive.resources.Res
import com.novo.drive.resources.common_back
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

data class StorageCleanerUiState(
    val duplicateGroups: List<List<DriveFile>> = emptyList(),
    val largestFiles: List<DriveFile> = emptyList(),
    val cleaning: Boolean = false
) {
    val duplicateExtraFiles: List<DriveFile>
        get() = duplicateGroups.flatMap { group ->
            group.sortedBy { it.addedAt }.drop(1)
        }

    val duplicateReclaimableBytes: Long
        get() = duplicateExtraFiles.sumOf { it.sizeBytes }
}

class StorageCleanerViewModel(
    fileRepository: FileRepository,
    private val trashRepository: TrashRepository
) : ViewModel() {

    private val cleaning = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<UiText>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiText> = _messages.asSharedFlow()

    val uiState: StateFlow<StorageCleanerUiState> = combine(
        fileRepository.observeDuplicateGroups(),
        fileRepository.observeLargestFiles(50),
        cleaning
    ) { duplicates, largest, isCleaning ->
        StorageCleanerUiState(
            duplicateGroups = duplicates,
            largestFiles = largest,
            cleaning = isCleaning
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageCleanerUiState())

    fun cleanDuplicateGroup(group: List<DriveFile>) {
        val extras = group.sortedBy { it.addedAt }.drop(1)
        if (extras.isEmpty() || cleaning.value) return
        moveIdsToTrash(extras.map { it.id }, extras.sumOf { it.sizeBytes })
    }

    fun cleanAllDuplicates() {
        val extras = uiState.value.duplicateExtraFiles
        if (extras.isEmpty() || cleaning.value) return
        moveIdsToTrash(extras.map { it.id }, extras.sumOf { it.sizeBytes })
    }

    fun trashSingleFile(file: DriveFile) {
        if (cleaning.value) return
        moveIdsToTrash(listOf(file.id), file.sizeBytes)
    }

    private fun moveIdsToTrash(ids: List<String>, bytes: Long) {
        cleaning.value = true
        viewModelScope.launch {
            try {
                val result = withContext(NonCancellable) {
                    trashRepository.moveFilesToTrash(ids)
                }
                when (result) {
                    is AppResult.Success -> _messages.tryEmit(
                        UiText.Plain("${ids.size} টি ফাইল ট্র্যাশে সরানো হয়েছে (${Formatters.bytes(bytes)} খালি হয়েছে)")
                    )
                    is AppResult.Failure -> _messages.tryEmit(result.error.toUiText())
                }
            } finally {
                cleaning.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageCleanerScreen(
    onBack: () -> Unit,
    onOpenFile: (String, PreviewSequence) -> Unit,
    viewModel: StorageCleanerViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalIplusPalette.current
    val isLight = palette.theme == AppTheme.LIGHT
    val primaryText = if (isLight) Color(0xFF0F172A) else Color.White
    val secondaryText = if (isLight) Color(0xFF475569) else Color.White.copy(alpha = 0.68f)

    var selectedTab by remember { mutableIntStateOf(0) }
    var confirmCleanAll by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    CollectSnackbarMessages(viewModel.messages, snackbarHostState)

    val listState = rememberLazyListState()
    val lifted by rememberToolbarLift(listState)

    if (confirmCleanAll) {
        ConfirmDialog(
            title = "সব ডুপ্লিকেট ফাইল পরিষ্কার করবেন?",
            message = "প্রতিটি গ্রুপের সবচেয়ে পুরনো আসল ফাইলটি রেখে অতিরিক্ত ${state.duplicateExtraFiles.size} টি কপি (${Formatters.bytes(state.duplicateReclaimableBytes)}) ট্র্যাশে সরানো হবে।",
            confirmLabel = "পরিষ্কার করুন",
            destructive = true,
            onConfirm = {
                confirmCleanAll = false
                viewModel.cleanAllDuplicates()
            },
            onDismiss = { confirmCleanAll = false }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { BottomBarSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = liftedTopAppBarColors(lifted),
                title = {
                    Text(
                        text = "Smart Cleaner (ডুপ্লিকেট ও বড় ফাইল)",
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.common_back),
                            tint = primaryText
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "summary-tabs") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LiquidGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        glowColor = palette.accentPrimary
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ডুপ্লিকেট স্পেস: ${Formatters.bytes(state.duplicateReclaimableBytes)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )
                                Text(
                                    text = "${state.duplicateGroups.size} টি SHA-256 হ্যাশ গ্রুপ • ${state.largestFiles.size} টি বড় ফাইল",
                                    fontSize = 12.sp,
                                    color = secondaryText
                                )
                            }
                            if (state.duplicateExtraFiles.isNotEmpty()) {
                                GlassPillButton(
                                    text = "Clean All",
                                    icon = Icons.Filled.AutoFixHigh,
                                    isActive = true,
                                    onClick = { confirmCleanAll = true }
                                )
                            }
                        }
                    }

                    GlassSegmentedTabs(
                        tabs = listOf(
                            "Exact Duplicates (${state.duplicateGroups.size})",
                            "Largest Files (${state.largestFiles.size})"
                        ),
                        selectedIndex = selectedTab,
                        onTabSelected = { selectedTab = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (selectedTab == 0) {
                if (state.duplicateGroups.isEmpty()) {
                    item(key = "no-duplicates") {
                        EmptyState(
                            icon = Icons.Filled.CheckCircle,
                            title = "কোনো ডুপ্লিকেট ফাইল নেই!",
                            description = "SHA-256 হ্যাশ যাচাইয়ে আপনার ড্রাইভ শতভাগ ডুপ্লিকেটমুক্ত।"
                        )
                    }
                } else {
                    items(
                        items = state.duplicateGroups,
                        key = { group -> "dup-${group.firstOrNull()?.contentHash ?: group.first().id}" }
                    ) { group ->
                        val sorted = remember(group) { group.sortedBy { it.addedAt } }
                        val keeper = sorted.first()
                        val extraBytes = sorted.drop(1).sumOf { it.sizeBytes }
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FileThumbnail(
                                        file = keeper,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = keeper.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${sorted.size} টি অভিন্ন কপি • প্রতিটি ${Formatters.bytes(keeper.sizeBytes)}",
                                            fontSize = 12.sp,
                                            color = secondaryText
                                        )
                                    }
                                    GlassPillButton(
                                        text = "Free ${Formatters.bytes(extraBytes)}",
                                        icon = Icons.Filled.CleaningServices,
                                        onClick = { viewModel.cleanDuplicateGroup(sorted) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (state.largestFiles.isEmpty()) {
                    item(key = "no-large") {
                        EmptyState(
                            icon = Icons.Filled.CleaningServices,
                            title = "কোনো ফাইল পাওয়া যায়নি",
                            description = "ড্রাইভে ফাইল যোগ হলে সবচেয়ে বড় ফাইলগুলো এখানে দেখাবে।"
                        )
                    }
                } else {
                    items(state.largestFiles, key = { "large-${it.id}" }) { file ->
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) {
                                        onOpenFile(
                                            file.id,
                                            PreviewSequence(
                                                sortField = FileSortField.SIZE,
                                                sortDirection = SortDirection.DESCENDING
                                            )
                                        )
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FileThumbnail(
                                    file = file,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = primaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${Formatters.bytes(file.sizeBytes)} • ${Formatters.date(file.modifiedAt)}",
                                        fontSize = 12.sp,
                                        color = palette.accentPrimary
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.trashSingleFile(file) },
                                    enabled = !state.cleaning
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Move to Trash",
                                        tint = Color(0xFFFF5252)
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

