package com.space_explorer.ui.state

import androidx.compose.runtime.Immutable
import com.space_explorer.domain.model.Astronomy

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
    val isEmpty: Boolean
        get() = !isLoading && filteredItems.isEmpty() && errorMessage == null
}

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

@Immutable
data class DetailUiState(
    val astronomy: Astronomy? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
