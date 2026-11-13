package com.novo.drive.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novo.drive.core.ocr.OcrIndexService
import com.novo.drive.domain.model.FileCategory
import com.novo.drive.domain.model.FileQuerySpec
import com.novo.drive.domain.model.FileSortField
import com.novo.drive.domain.model.SortDirection
import com.novo.drive.domain.repository.FileRepository
import com.novo.drive.domain.repository.TransferRepository
import com.novo.drive.domain.repository.TrashRepository
import com.novo.drive.domain.usecase.MakeAvailableOfflineUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/** Queries only the local index; nothing remote happens while typing. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val fileRepository: FileRepository,
    private val trashRepository: TrashRepository,
    private val transferRepository: TransferRepository,
    private val makeAvailableOffline: MakeAvailableOfflineUseCase,
    private val ocrIndexService: OcrIndexService
) : ViewModel() {

    init {
        viewModelScope.launch {
            runCatching { ocrIndexService.indexPendingBatch(30) }
        }
    }

    private val selection = MutableStateFlow<Set<String>>(emptySet())

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(SearchFilters())
    private val searching = MutableStateFlow(false)

    private val results = combine(
        query.debounce(250.milliseconds).distinctUntilChanged(),
        filters
    ) { text, filterValues -> text to filterValues }
        .flatMapLatest { (text, filterValues) ->
            if (text.isBlank() && filterValues.category == null &&
                !filterValues.backedUpOnly && !filterValues.notBackedUpOnly &&
                filterValues.minSizeMb == null
            ) {
                flowOf(emptyList())
            } else {
                searching.update { true }
                val fileMatchesFlow = fileRepository.observeFiles(
                    FileQuerySpec(
                        nameQuery = text.takeIf { it.isNotBlank() },
                        categories = filterValues.category?.let { listOf(it) } ?: emptyList(),
                        backedUpOnly = filterValues.backedUpOnly,
                        notBackedUpOnly = filterValues.notBackedUpOnly,
                        minSizeBytes = filterValues.minSizeMb?.let { it.toLong() * 1024 * 1024 },
                        showArchived = true,
                        sortField = filterValues.sortField,
                        sortDirection = filterValues.sortDirection
                    )
                )
                val ocrMatchesFlow = if (
                    text.isNotBlank() &&
                    (filterValues.category == null || filterValues.category == FileCategory.IMAGE)
                ) {
                    fileRepository.observeOcrMatchedFiles(
                        query = text,
                        showHidden = false,
                        showArchived = true,
                        limit = 100
                    )
                } else {
                    flowOf(emptyList())
                }
                combine(fileMatchesFlow, ocrMatchesFlow) { list, ocrList ->
                    searching.update { false }
                    val minBytes = filterValues.minSizeMb?.let { it.toLong() * 1024 * 1024 }
                    val filteredOcr = ocrList.filter { f ->
                        (!filterValues.backedUpOnly || f.hasRemoteCopy) &&
                            (!filterValues.notBackedUpOnly || !f.hasRemoteCopy) &&
                            (minBytes == null || f.sizeBytes >= minBytes)
                    }
                    (list + filteredOcr).distinctBy { it.id }.take(500)
                }
            }
        }

    private val folderResults = combine(
        query.debounce(250.milliseconds).distinctUntilChanged(),
        filters
    ) { text, filterValues -> text to filterValues }
        .flatMapLatest { (text, filterValues) ->
            val fileOnlyFilterActive = filterValues.category != null ||
                    filterValues.backedUpOnly ||
                    filterValues.notBackedUpOnly ||
                    filterValues.minSizeMb != null
            if (text.isBlank() || fileOnlyFilterActive) {
                flowOf(emptyList())
            } else {
                fileRepository.searchFolders(text, showArchived = true)
            }
        }

    private val base: Flow<SearchUiState> = combine(
        query,
        filters,
        results,
        searching,
        folderResults
    ) { text, filterValues, resultList, isSearching, folderList ->
        SearchUiState(
            query = text,
            filters = filterValues,
            results = resultList,
            folders = folderList,
            searching = isSearching,
            searched = text.isNotBlank() || filterValues != SearchFilters()
        )
    }

    val uiState: StateFlow<SearchUiState> = combine(base, selection) { state, selected ->
        // Results change as the query does, so a selection cannot outlive them.
        state.copy(selection = selected.intersect(state.results.map { it.id }.toSet()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun toggleSelection(id: String) = selection.update { current ->
        if (id in current) current - id else current + id
    }

    fun clearSelection() = selection.update { emptySet() }

    fun selectAll() {
        selection.value = uiState.value.results.map { it.id }.toSet()
    }

    fun downloadSelected() {
        val ids = selection.value.toList()
        clearSelection()
        viewModelScope.launch { ids.forEach { transferRepository.enqueueDownload(it) } }
    }

    fun favoriteSelected() {
        val ids = selection.value.toList()
        clearSelection()
        viewModelScope.launch { fileRepository.setFilesFavorite(ids, true) }
    }

    fun setSelectedAvailableOffline(available: Boolean) {
        val ids = selection.value.toList()
        clearSelection()
        viewModelScope.launch { makeAvailableOffline(ids, available = available) }
    }

    fun trashSelected() {
        val ids = selection.value.toList()
        clearSelection()
        viewModelScope.launch {
            withContext(NonCancellable) { trashRepository.moveFilesToTrash(ids) }
        }
    }

    fun setQuery(value: String) = query.update { value }

    fun setCategory(category: FileCategory?) = filters.update { it.copy(category = category) }

    fun setBackedUpOnly(value: Boolean) = filters.update {
        it.copy(backedUpOnly = value, notBackedUpOnly = if (value) false else it.notBackedUpOnly)
    }

    fun setNotBackedUpOnly(value: Boolean) = filters.update {
        it.copy(notBackedUpOnly = value, backedUpOnly = if (value) false else it.backedUpOnly)
    }

    fun setSort(field: FileSortField, direction: SortDirection) = filters.update {
        it.copy(sortField = field, sortDirection = direction)
    }
}
