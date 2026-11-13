package com.novo.drive.presentation.gallery

import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VisibilityOff
import com.novo.drive.presentation.components.ActionMenu
import com.novo.drive.presentation.components.MenuAction
import com.novo.drive.resources.common_make_available_offline
import com.novo.drive.resources.common_organize
import com.novo.drive.resources.common_remove_from_offline
import com.novo.drive.resources.common_storage_actions
import com.novo.drive.resources.files_archive
import com.novo.drive.resources.files_hide
import com.novo.drive.resources.files_sort
import com.novo.drive.resources.preview_remove_favorites
import androidx.compose.material3.SnackbarHostState
import com.novo.drive.presentation.common.CollectSnackbarMessages
import com.novo.drive.presentation.components.BottomBarSnackbarHost
import com.novo.drive.presentation.components.CollectionShortcuts
import com.novo.drive.presentation.files.FolderPickerHost
import com.novo.drive.resources.files_move_to
import com.novo.drive.resources.files_move_here
import com.novo.drive.presentation.common.AppBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import com.novo.drive.presentation.theme.IplusTheme
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.novo.drive.resources.Res
import com.novo.drive.resources.common_actions
import com.novo.drive.resources.common_add_favorites
import com.novo.drive.resources.common_back
import com.novo.drive.resources.common_clear_selection
import com.novo.drive.resources.common_confirm_trash_count_title
import com.novo.drive.resources.common_deselect_all
import com.novo.drive.resources.common_download
import com.novo.drive.resources.common_move_trash
import com.novo.drive.resources.common_rename
import com.novo.drive.resources.common_rename_file
import com.novo.drive.resources.common_restore_trash_emptied
import com.novo.drive.resources.common_select_all
import com.novo.drive.resources.common_selection_count
import com.novo.drive.resources.common_upload
import com.novo.drive.resources.date_days_ago
import com.novo.drive.resources.date_today
import com.novo.drive.resources.date_yesterday
import com.novo.drive.resources.gallery_albums_appear_organise_media
import com.novo.drive.resources.gallery_no_albums
import com.novo.drive.resources.gallery_no_media
import com.novo.drive.resources.gallery_photos_videos_appear_here
import com.novo.drive.resources.gallery_title
import com.novo.drive.domain.model.MediaAlbum
import com.novo.drive.domain.model.ViewMode
import com.novo.drive.presentation.common.DayBucket
import com.novo.drive.presentation.common.Formatters
import com.novo.drive.presentation.common.isInitialLoad
import com.novo.drive.presentation.components.RefreshAction
import com.novo.drive.presentation.components.RefreshableContent
import com.novo.drive.presentation.components.ConfirmDialog
import com.novo.drive.presentation.components.EmptyState
import androidx.compose.ui.graphics.Color
import com.novo.drive.presentation.components.FileGridItem
import com.novo.drive.presentation.components.glass.GlassActionIconButton
import com.novo.drive.presentation.components.FileListItem
import com.novo.drive.presentation.components.LoadingState
import com.novo.drive.presentation.components.RenameDialog
import com.novo.drive.presentation.components.SortMenu
import com.novo.drive.presentation.components.keyboardCollection
import com.novo.drive.presentation.components.keyboardCollectionItem
import com.novo.drive.presentation.components.liftedTopAppBarColors
import com.novo.drive.presentation.components.pinchZoom
import com.novo.drive.presentation.components.rememberDragSelect
import com.novo.drive.presentation.components.rememberKeyboardCollection
import com.novo.drive.presentation.components.rememberToolbarLift
import com.novo.drive.presentation.navigation.LocalBottomBarInset
import com.novo.drive.presentation.preview.PreviewSequence
import com.novo.drive.presentation.common.rememberPosition

@Composable
private fun DayHeaderRow(dayStartMillis: Long, modifier: Modifier = Modifier) {
    Text(
        text = when (val bucket = Formatters.dayBucket(dayStartMillis)) {
            DayBucket.Today -> stringResource(Res.string.date_today)
            DayBucket.Yesterday -> stringResource(Res.string.date_yesterday)
            is DayBucket.DaysAgo -> stringResource(Res.string.date_days_ago, bucket.days)
            is DayBucket.Absolute -> bucket.text
        },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .semantics { heading() }
            .padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun GalleryTabSelector(
    pagerState: PagerState,
    onSelectTab: (GalleryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = GalleryTab.entries
    val coroutineScope = rememberCoroutineScope()
    val palette = IplusTheme.palette
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isDark) Color(0xFF0F1523)
                else Color(0xFFE2E8F0)
            )
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF1E2536) else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(3.dp)
    ) {
        val tabWidth = maxWidth / tabs.size
        val scrollPosition by remember(pagerState) {
            derivedStateOf {
                (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .coerceIn(0f, (tabs.size - 1).toFloat())
            }
        }
        val indicatorOffset by remember(pagerState, tabWidth, maxWidth) {
            derivedStateOf {
                (tabWidth * scrollPosition).coerceIn(0.dp, (maxWidth - tabWidth).coerceAtLeast(0.dp))
            }
        }

        // Real-time smooth sliding indicator pill (clean surface, zero dark seam or border artefact)
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(11.dp))
                .background(
                    if (isDark) Color(0xFF1E2638)
                    else Color.White
                )
                .border(
                    width = 1.dp,
                    color = if (isDark) palette.accentPrimary.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(11.dp)
                )
        )

        // Tab Labels
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { index, tab ->
                val distance = kotlin.math.abs(scrollPosition - index).coerceIn(0f, 1f)
                val isSelected = distance < 0.5f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                                onSelectTab(tab)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) {
                            if (isDark) Color.White else Color(0xFF0F172A)
                        } else {
                            if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<MediaAlbum>,
    columns: Int,
    bottomPadding: Dp,
    onOpenAlbum: (MediaAlbum) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    if (albums.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.PhotoLibrary,
            title = stringResource(Res.string.gallery_no_albums),
            description = stringResource(Res.string.gallery_albums_appear_organise_media)
        )
        return
    }
    val gridState = rememberLazyGridState()
    val keyboard = rememberKeyboardCollection(gridState, columns)
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize()
            .keyboardCollection(keyboard, albums.size)
            .pinchZoom(onZoomIn = onZoomIn, onZoomOut = onZoomOut),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = 12.dp,
            bottom = 12.dp + bottomPadding
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(
            albums,
            key = { _, album -> album.folderId ?: UNFILED_ALBUM_KEY }
        ) { index, album ->
            AlbumCard(
                album = album,
                onClick = { onOpenAlbum(album) },
                modifier = Modifier
                    .keyboardCollectionItem(keyboard, index)
                    .animateItem()
            )
        }
    }
}

private const val UNFILED_ALBUM_KEY = "__unfiled__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onOpenFile: (String, PreviewSequence) -> Unit,
    onOpenAlbum: (MediaAlbum) -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: GalleryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    var confirmTrash by remember { mutableStateOf(false) }
    var showSelectionOverflow by remember { mutableStateOf(false) }
    var showMovePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val renameTarget by viewModel.renameTarget.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = state.tab.ordinal,
        pageCount = { GalleryTab.entries.size }
    )

    // Synchronize only when the user's swipe settles, preventing touch-fighting jumps
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settled ->
            val targetTab = GalleryTab.entries.getOrNull(settled) ?: return@collect
            if (state.tab != targetTab) {
                viewModel.setTab(targetTab)
            }
        }
    }

    CollectSnackbarMessages(viewModel.messages, snackbarHostState)

    if (showMovePicker) {
        FolderPickerHost(
            title = stringResource(Res.string.files_move_to),
            confirmLabel = stringResource(Res.string.files_move_here),
            loadChildren = viewModel::childFolders,
            createFolder = viewModel::createFolderIn,
            onConfirm = { target ->
                showMovePicker = false
                viewModel.moveSelected(target)
            },
            onDismiss = { showMovePicker = false }
        )
    }

    if (confirmTrash) {
        ConfirmDialog(
            title = stringResource(
                Res.string.common_confirm_trash_count_title,
                state.selection.size
            ),
            message = stringResource(Res.string.common_restore_trash_emptied),
            confirmLabel = stringResource(Res.string.common_move_trash),
            destructive = true,
            onConfirm = {
                confirmTrash = false
                viewModel.trashSelected()
            },
            onDismiss = { confirmTrash = false }
        )
    }

    renameTarget?.let { file ->
        RenameDialog(
            title = stringResource(Res.string.common_rename_file),
            initialValue = file.name,
            confirmLabel = stringResource(Res.string.common_rename),
            onConfirm = viewModel::confirmRename,
            onDismiss = viewModel::dismissRename
        )
    }

    AppBackHandler(enabled = state.selectionMode) { viewModel.clearSelection() }

    val mediaListState = rememberSaveable(state.tab, saver = LazyListState.Saver) {
        LazyListState()
    }
    val allSelected by viewModel.allSelected.collectAsStateWithLifecycle()
    val previewSequence by viewModel.previewSequence.collectAsStateWithLifecycle()
    val mediaGridState = rememberSaveable(state.tab, saver = LazyGridState.Saver) {
        LazyGridState()
    }
    val scrolled = if (state.viewMode == ViewMode.LIST) mediaListState else mediaGridState
    val lifted by rememberToolbarLift(scrolled)

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            BottomBarSnackbarHost(
                hostState = snackbarHostState,
                applyInset = !state.selectionMode
            )
        },
        topBar = {
            if (state.selectionMode) {
                TopAppBar(
                    colors = liftedTopAppBarColors(lifted),
                    title = {
                        Text(
                            text = stringResource(
                                Res.string.common_selection_count,
                                state.selection.size
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(Res.string.common_clear_selection)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (allSelected) viewModel.clearSelection() else viewModel.selectAll()
                            }
                        ) {
                            Icon(
                                imageVector = if (allSelected) {
                                    Icons.Filled.Deselect
                                } else {
                                    Icons.Filled.SelectAll
                                },
                                contentDescription = if (allSelected) {
                                    stringResource(Res.string.common_deselect_all)
                                } else {
                                    stringResource(Res.string.common_select_all)
                                }
                            )
                        }
                        Box {
                            IconButton(onClick = { showSelectionOverflow = true }) {
                                Icon(
                                    Icons.Filled.MoreVert,
                                    contentDescription = stringResource(Res.string.common_actions)
                                )
                            }
                            val storage = buildList {
                                if (state.capabilities.canDownload) {
                                    add(
                                        MenuAction(
                                            label = stringResource(Res.string.common_download),
                                            icon = Icons.Filled.Download
                                        ) { viewModel.downloadSelected() }
                                    )
                                }
                                if (state.capabilities.canUpload) {
                                    add(
                                        MenuAction(
                                            label = stringResource(Res.string.common_upload),
                                            icon = Icons.Filled.Upload
                                        ) { viewModel.uploadSelected() }
                                    )
                                }
                            }
                            val organize = buildList {
                                add(
                                    MenuAction(
                                        label = stringResource(
                                            if (state.allSelectedFavorite) {
                                                Res.string.preview_remove_favorites
                                            } else {
                                                Res.string.common_add_favorites
                                            }
                                        ),
                                        icon = if (state.allSelectedFavorite) {
                                            Icons.Filled.Star
                                        } else {
                                            Icons.Filled.StarOutline
                                        }
                                    ) { viewModel.favoriteSelected(!state.allSelectedFavorite) }
                                )
                                add(
                                    MenuAction(
                                        label = stringResource(
                                            if (state.allSelectedAvailableOffline) {
                                                Res.string.common_remove_from_offline
                                            } else {
                                                Res.string.common_make_available_offline
                                            }
                                        ),
                                        icon = Icons.Filled.OfflinePin
                                    ) { viewModel.setSelectedAvailableOffline(!state.allSelectedAvailableOffline) }
                                )
                                add(
                                    MenuAction(
                                        label = stringResource(Res.string.files_hide),
                                        icon = Icons.Filled.VisibilityOff
                                    ) { viewModel.hideSelected(true) }
                                )
                                add(
                                    MenuAction(
                                        label = stringResource(Res.string.files_archive),
                                        icon = Icons.Filled.Archive
                                    ) { viewModel.archiveSelected(true) }
                                )
                            }
                            val actions = buildList {
                                if (state.selection.size == 1) {
                                    add(
                                        MenuAction(
                                            label = stringResource(Res.string.common_rename),
                                            icon = Icons.Filled.DriveFileRenameOutline
                                        ) { viewModel.requestRenameSelected() }
                                    )
                                }
                                add(
                                    MenuAction(
                                        label = stringResource(Res.string.files_move_to),
                                        icon = Icons.AutoMirrored.Filled.DriveFileMove
                                    ) { showMovePicker = true }
                                )
                                if (storage.isNotEmpty()) {
                                    add(
                                        MenuAction(
                                            label = stringResource(Res.string.common_storage_actions),
                                            icon = Icons.Filled.Storage,
                                            children = storage
                                        )
                                    )
                                }
                                add(
                                    MenuAction(
                                        label = stringResource(Res.string.common_organize),
                                        icon = Icons.Filled.Tune,
                                        children = organize
                                    )
                                )
                                add(
                                    MenuAction(
                                        label = stringResource(Res.string.common_move_trash),
                                        icon = Icons.Filled.DeleteOutline
                                    ) { confirmTrash = true }
                                )
                            }
                            ActionMenu(
                                expanded = showSelectionOverflow,
                                onDismissRequest = { showSelectionOverflow = false },
                                actions = actions
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    colors = liftedTopAppBarColors(lifted),
                    title = { Text(state.albumTitle ?: stringResource(Res.string.gallery_title)) },
                    navigationIcon = {
                        onBack?.let {
                            GlassActionIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                onClick = it,
                                size = 38.dp,
                                modifier = Modifier.padding(start = 6.dp),
                                contentDescription = stringResource(Res.string.common_back)
                            )
                        }
                    },
                    actions = {
                        if (state.isAlbumView || state.tab != GalleryTab.ALBUMS) {
                            var showSortMenu by remember { mutableStateOf(false) }
                            GlassActionIconButton(
                                icon = Icons.AutoMirrored.Filled.Sort,
                                onClick = { showSortMenu = true },
                                size = 38.dp,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                contentDescription = stringResource(Res.string.files_sort)
                            )
                            SortMenu(
                                expanded = showSortMenu,
                                current = state.sortField,
                                direction = state.sortDirection,
                                onDismiss = { showSortMenu = false },
                                onSelect = { field, direction ->
                                    showSortMenu = false
                                    viewModel.setSort(field, direction)
                                }
                            )
                        }
                        RefreshAction(refreshing = refreshing, onRefresh = viewModel::refresh)
                    }
                )
            }
        }
    ) { padding ->
        var tabsVisible by remember { mutableStateOf(true) }
        val mediaScrolls by remember(scrolled) {
            derivedStateOf { scrolled.canScrollForward || scrolled.canScrollBackward }
        }
        LaunchedEffect(state.tab) { tabsVisible = true }
        LaunchedEffect(mediaScrolls) { if (!mediaScrolls) tabsVisible = true }
        val tabScrollConnection = remember {
            object : NestedScrollConnection {
                @Suppress("SameReturnValue")
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    if (!mediaScrolls || pagerState.isScrollInProgress) return Offset.Zero
                    when {
                        available.y < -TAB_SCROLL_THRESHOLD -> tabsVisible = false
                        available.y > TAB_SCROLL_THRESHOLD -> tabsVisible = true
                    }
                    return Offset.Zero
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .nestedScroll(tabScrollConnection)
        ) {
            if (!state.isAlbumView) {
                AnimatedVisibility(
                    visible = tabsVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    GalleryTabSelector(
                        pagerState = pagerState,
                        onSelectTab = viewModel::setTab
                    )
                }
            }

            val renderMedia: @Composable (GalleryTab) -> Unit = { targetTab ->
                val media = remember(targetTab) { viewModel.pagedMediaFor(targetTab) }.collectAsLazyPagingItems()
                if (state.viewMode == ViewMode.LIST) {
                    mediaListState.rememberPosition(viewModel.listPosition, media.itemCount)
                } else {
                    mediaGridState.rememberPosition(viewModel.listPosition, media.itemCount)
                }
                val mediaIdAt: (Int) -> String? = { index ->
                    (media.peek(index) as? GalleryListItem.Media)?.file?.id
                }
                val listDragSelect = rememberDragSelect(
                    listState = mediaListState,
                    onStart = viewModel::startRangeSelection,
                    onRange = { range ->
                        viewModel.extendRangeSelection(range.mapNotNull(mediaIdAt))
                    },
                    onEnd = viewModel::endRangeSelection
                )
                val gridDragSelect = rememberDragSelect(
                    gridState = mediaGridState,
                    onStart = viewModel::startRangeSelection,
                    onRange = { range ->
                        viewModel.extendRangeSelection(range.mapNotNull(mediaIdAt))
                    },
                    onEnd = viewModel::endRangeSelection
                )
                val isMedia: (Int) -> Boolean = { index ->
                    index < media.itemCount && media.peek(index) is GalleryListItem.Media
                }
                val listKeyboard = rememberKeyboardCollection(mediaListState, isItem = isMedia)
                val gridKeyboard = rememberKeyboardCollection(mediaGridState, state.gridSize, isMedia)
                val selectIfNone: (Int) -> Unit = { index ->
                    if (!state.selectionMode) mediaIdAt(index)?.let(viewModel::toggleSelection)
                }
                val shortcuts = CollectionShortcuts(
                    onToggleSelect = { index -> mediaIdAt(index)?.let(viewModel::toggleSelection) },
                    onSelectAll = viewModel::selectAll,
                    onDelete = { index ->
                        selectIfNone(index)
                        if (viewModel.hasSelection()) confirmTrash = true
                    },
                    onRename = { index -> mediaIdAt(index)?.let(viewModel::requestRename) },
                    onCut = { index ->
                        selectIfNone(index)
                        viewModel.cutSelected()
                    },
                    onBack = onBack,
                    onClear = if (state.selectionMode) viewModel::clearSelection else null
                )
                RefreshableContent(
                    isRefreshing = refreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        media.isInitialLoad || !state.loaded -> LoadingState()

                        media.itemCount == 0 -> EmptyState(
                            icon = Icons.Outlined.Photo,
                            title = stringResource(Res.string.gallery_no_media),
                            description = stringResource(Res.string.gallery_photos_videos_appear_here)
                        )

                        state.viewMode == ViewMode.LIST -> LazyColumn(
                            state = mediaListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .keyboardCollection(listKeyboard, media.itemCount, shortcuts)
                                .pinchZoom(
                                    onZoomIn = viewModel::zoomIn,
                                    onZoomOut = viewModel::zoomOut
                                )
                                .then(listDragSelect),
                            contentPadding = PaddingValues(
                                start = 8.dp,
                                end = 8.dp,
                                top = 8.dp,
                                bottom = 36.dp + padding.calculateBottomPadding() + LocalBottomBarInset.current
                            )
                        ) {
                            items(
                                count = media.itemCount,
                                key = media.itemKey { it.key }
                            ) { index ->
                                when (val item = media[index]) {
                                    is GalleryListItem.DayHeader -> DayHeaderRow(
                                        dayStartMillis = item.dayStartMillis,
                                        modifier = Modifier.animateItem()
                                    )

                                    is GalleryListItem.Media -> {
                                        val file = item.file
                                        FileListItem(
                                            file = file,
                                            selected = file.id in state.selection,
                                            selectionMode = state.selectionMode,
                                            onClick = {
                                                if (state.selectionMode) {
                                                    viewModel.toggleSelection(file.id)
                                                } else {
                                                    onOpenFile(file.id, previewSequence)
                                                }
                                            },
                                            onLongClick = { viewModel.toggleSelection(file.id) },
                                            modifier = Modifier
                                                .keyboardCollectionItem(listKeyboard, index)
                                                .animateItem()
                                        )
                                    }

                                    null -> Unit
                                }
                            }
                        }

                        else -> LazyVerticalGrid(
                            state = mediaGridState,
                            columns = GridCells.Fixed(state.gridSize),
                            modifier = Modifier
                                .fillMaxSize()
                                .keyboardCollection(gridKeyboard, media.itemCount, shortcuts)
                                .pinchZoom(
                                    onZoomIn = viewModel::zoomIn,
                                    onZoomOut = viewModel::zoomOut
                                )
                                .then(gridDragSelect),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 12.dp,
                                bottom = 36.dp + padding.calculateBottomPadding() + LocalBottomBarInset.current
                            ),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                count = media.itemCount,
                                key = media.itemKey { it.key },
                                span = { index ->
                                    if (media.peek(index) is GalleryListItem.DayHeader) {
                                        GridItemSpan(maxLineSpan)
                                    } else {
                                        GridItemSpan(1)
                                    }
                                }
                            ) { index ->
                                when (val item = media[index]) {
                                    is GalleryListItem.DayHeader -> DayHeaderRow(
                                        dayStartMillis = item.dayStartMillis,
                                        modifier = Modifier.animateItem()
                                    )

                                    is GalleryListItem.Media -> {
                                        val file = item.file
                                        FileGridItem(
                                            file = file,
                                            selected = file.id in state.selection,
                                            compact = true,
                                            onClick = {
                                                if (state.selectionMode) {
                                                    viewModel.toggleSelection(file.id)
                                                } else {
                                                    onOpenFile(file.id, previewSequence)
                                                }
                                            },
                                            onLongClick = { viewModel.toggleSelection(file.id) },
                                            modifier = Modifier
                                                .keyboardCollectionItem(gridKeyboard, index)
                                                .animateItem()
                                        )
                                    }

                                    null -> Unit
                                }
                            }
                        }
                    }
                }
            }

            if (!state.isAlbumView) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (GalleryTab.entries.getOrNull(page)) {
                        GalleryTab.ALBUMS -> {
                            if (!state.loaded) {
                                LoadingState()
                            } else {
                                AlbumGrid(
                                    albums = state.albums,
                                    columns = state.albumGridSize,
                                    bottomPadding = 36.dp + padding.calculateBottomPadding() + LocalBottomBarInset.current,
                                    onOpenAlbum = onOpenAlbum,
                                    onZoomIn = viewModel::zoomAlbumsIn,
                                    onZoomOut = viewModel::zoomAlbumsOut
                                )
                            }
                        }
                        GalleryTab.PHOTOS -> key(GalleryTab.PHOTOS) {
                            renderMedia(GalleryTab.PHOTOS)
                        }
                        GalleryTab.VIDEOS -> key(GalleryTab.VIDEOS) {
                            renderMedia(GalleryTab.VIDEOS)
                        }
                        GalleryTab.ALL -> key(GalleryTab.ALL) {
                            renderMedia(GalleryTab.ALL)
                        }
                        null -> Unit
                    }
                }
            } else {
                key(state.tab) { renderMedia(state.tab) }
            }
        }
    }
}

private const val TAB_SCROLL_THRESHOLD = 6f
