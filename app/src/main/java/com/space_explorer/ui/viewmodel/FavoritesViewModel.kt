package com.space_explorer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.space_explorer.core.Constants
import com.space_explorer.domain.error.AstronomyError
import com.space_explorer.domain.model.Astronomy
import com.space_explorer.domain.repository.AstronomyRepository
import com.space_explorer.ui.state.FavoritesUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Favorites screen.
 *
 * Differences from [AstronomyViewModel]:
 *   * No pagination — the entire favorites table is small and lives in memory.
 *   * No remote calls — read path is purely local (Room Flow).
 *   * Composed reactively via [combine] so a new favorite or a query change
 *     are both reflected in a single state emission.
 *
 * Loading semantics:
 *   * `isLoading = true` only on the very first emission, before Room has
 *     produced any value. As soon as the DB query completes (even with an
 *     empty list), `isLoading` flips to `false` and stays there.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: AstronomyRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<FavoritesUiState> = combine(
        repository.getFavorites(),
        searchQuery
    ) { favorites, query ->
        FavoritesUiState(
            favorites = applyFilter(favorites, query),
            isLoading = false,
            errorMessage = null,
            searchQuery = query
        )
    }
        .onStart {
            // Surface a spinner until Room produces the first value.
            emit(FavoritesUiState(isLoading = true))
        }
        .catch { throwable ->
            emit(
                FavoritesUiState(
                    favorites = emptyList(),
                    isLoading = false,
                    errorMessage = throwable.userMessage()
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(Constants.STATE_FLOW_STOP_TIMEOUT_MS),
            initialValue = FavoritesUiState(isLoading = true)
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun removeFavorite(astronomy: Astronomy) {
        viewModelScope.launch {
            repository.removeFavorite(astronomy.id)
        }
    }

    private fun applyFilter(favorites: List<Astronomy>, query: String): List<Astronomy> {
        if (query.isBlank()) return favorites
        val normalized = query.trim().lowercase()
        return favorites.filter {
            it.title.lowercase().contains(normalized) ||
                it.explanation.lowercase().contains(normalized) ||
                it.date.contains(normalized)
        }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is AstronomyError -> userMessage
        else -> message ?: "Error desconocido"
    }
}
