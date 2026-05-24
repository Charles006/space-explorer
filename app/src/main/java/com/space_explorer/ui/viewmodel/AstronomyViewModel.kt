package com.space_explorer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space_explorer.core.Constants
import com.space_explorer.domain.error.AstronomyError
import com.space_explorer.domain.model.Astronomy
import com.space_explorer.domain.repository.AstronomyRepository
import com.space_explorer.ui.state.HomeUiState
import com.space_explorer.ui.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Home (timeline) screen.
 *
 * Owns:
 *   * The paginated timeline of APODs, fetched backwards in [Constants.PAGE_SIZE]-day chunks.
 *   * Local in-memory filtering by query (title / explanation / date).
 *   * Remote date lookup (`yyyy-MM-dd` → single APOD).
 *   * Reactive favorite-flag updates so star icons stay in sync with the DB.
 *
 * Threading: all I/O happens inside [viewModelScope] via suspend functions;
 * the StateFlow is updated on the main dispatcher implicitly through
 * `MutableStateFlow.update`.
 */
@HiltViewModel
class AstronomyViewModel @Inject constructor(
    private val repository: AstronomyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Oldest date currently loaded; serves as the upper bound for the next page. */
    private var oldestLoadedDate: String = DateUtils.today()

    init {
        observeFavorites()
        loadInitialData()
    }

    // region ── Public API ────────────────────────────────────────────────

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val today = DateUtils.today()
            val start = DateUtils.daysAgo(Constants.PAGE_SIZE - 1)
            repository.getAstronomyRange(start, today)
                .onSuccess { items -> applyInitialItems(items, fallbackStart = start) }
                .onFailure { throwable -> setError(throwable, clearLoading = true) }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isLoading || state.endReached || state.isRemoteSearching) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val end = DateUtils.previousDay(oldestLoadedDate)
            val start = DateUtils.subtractDays(end, Constants.PAGE_SIZE - 1)
            repository.getAstronomyRange(start, end)
                .onSuccess { newItems -> appendPage(newItems, fallbackStart = start) }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            errorMessage = throwable.userMessage()
                        )
                    }
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val today = DateUtils.today()
            val start = DateUtils.daysAgo(Constants.PAGE_SIZE - 1)
            repository.getAstronomyRange(start, today)
                .onSuccess { items ->
                    oldestLoadedDate = items.minOfOrNull { it.date } ?: start
                    _uiState.update { current ->
                        current.copy(
                            items = items,
                            filteredItems = applyLocalFilter(items, current.searchQuery),
                            isRefreshing = false,
                            endReached = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isRefreshing = false, errorMessage = throwable.userMessage())
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredItems = applyLocalFilter(current.items, query)
            )
        }
    }

    fun onRemoteSearch(date: String) {
        if (!DateUtils.isValidIsoDate(date)) {
            _uiState.update { it.copy(errorMessage = AstronomyError.InvalidDate(date).userMessage) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRemoteSearching = true, errorMessage = null) }
            repository.getAstronomyByDate(date)
                .onSuccess { astronomy ->
                    _uiState.update { current ->
                        val merged = (listOf(astronomy) + current.items).distinctBy { it.id }
                        current.copy(
                            items = merged,
                            filteredItems = applyLocalFilter(merged, current.searchQuery),
                            isRemoteSearching = false,
                            remoteSearchDate = date
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isRemoteSearching = false, errorMessage = throwable.userMessage())
                    }
                }
        }
    }

    fun toggleFavorite(astronomy: Astronomy) {
        viewModelScope.launch {
            repository.toggleFavorite(astronomy)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // endregion

    // region ── Internal ──────────────────────────────────────────────────

    /** Observes the favorites table and reflects changes on the in-memory list. */
    private fun observeFavorites() {
        repository.observeFavoriteIds()
            .onEach { favoriteIds ->
                _uiState.update { current ->
                    val updatedItems = current.items.map {
                        it.copy(isFavorite = it.id in favoriteIds)
                    }
                    current.copy(
                        items = updatedItems,
                        filteredItems = applyLocalFilter(updatedItems, current.searchQuery)
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun applyInitialItems(items: List<Astronomy>, fallbackStart: String) {
        oldestLoadedDate = items.minOfOrNull { it.date } ?: fallbackStart
        _uiState.update { current ->
            current.copy(
                items = items,
                filteredItems = applyLocalFilter(items, current.searchQuery),
                isLoading = false,
                errorMessage = null,
                endReached = false
            )
        }
    }

    private fun appendPage(newItems: List<Astronomy>, fallbackStart: String) {
        if (newItems.isEmpty()) {
            _uiState.update { it.copy(isLoadingMore = false, endReached = true) }
            return
        }
        oldestLoadedDate = newItems.minOfOrNull { it.date } ?: fallbackStart
        _uiState.update { current ->
            val merged = (current.items + newItems).distinctBy { it.id }
            current.copy(
                items = merged,
                filteredItems = applyLocalFilter(merged, current.searchQuery),
                isLoadingMore = false,
                errorMessage = null
            )
        }
    }

    private fun setError(throwable: Throwable, clearLoading: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = if (clearLoading) false else it.isLoading,
                errorMessage = throwable.userMessage()
            )
        }
    }

    private fun applyLocalFilter(items: List<Astronomy>, query: String): List<Astronomy> {
        if (query.isBlank()) return items
        val normalized = query.trim().lowercase()
        return items.filter {
            it.title.lowercase().contains(normalized) ||
                it.explanation.lowercase().contains(normalized) ||
                it.date.contains(normalized)
        }
    }

    /**
     * Domain errors expose a localized [AstronomyError.userMessage]; any other
     * Throwable falls back to its own message.
     */
    private fun Throwable.userMessage(): String = when (this) {
        is AstronomyError -> userMessage
        else -> message ?: "Error desconocido"
    }

    // endregion
}
