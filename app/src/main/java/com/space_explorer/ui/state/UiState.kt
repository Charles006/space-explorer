package com.space_explorer.ui.state

import androidx.compose.runtime.Immutable
import com.space_explorer.domain.model.Astronomy

/**
 * Immutable view-state for [com.space_explorer.ui.screens.HomeScreen].
 *
 * Marked `@Immutable` to help Compose skip unnecessary recompositions; all
 * collections are read-only and all primitives are value types.
 *
 * State legend:
 *   * [isLoading]          – initial / full-screen spinner (first page).
 *   * [isLoadingMore]      – inline footer spinner while paginating.
 *   * [isRefreshing]       – pull-to-refresh trigger.
 *   * [isRemoteSearching]  – a date lookup is in flight.
 *   * [endReached]         – API returned no more results.
 */
@Immutable
data class HomeUiState(
    val items: List<Astronomy> = emptyList(),
    val filteredItems: List<Astronomy> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val remoteSearchDate: String? = null,
    val isRemoteSearching: Boolean = false,
    val endReached: Boolean = false
) {
    /** True iff the list is empty AND we are not loading/erroring. */
    val isEmpty: Boolean
        get() = !isLoading && filteredItems.isEmpty() && errorMessage == null
}

/** Immutable view-state for the Favorites tab. */
@Immutable
data class FavoritesUiState(
    val favorites: List<Astronomy> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val searchQuery: String = ""
) {
    val isEmpty: Boolean
        get() = !isLoading && favorites.isEmpty() && errorMessage == null
}

/** Immutable view-state for the Detail screen. */
@Immutable
data class DetailUiState(
    val astronomy: Astronomy? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
