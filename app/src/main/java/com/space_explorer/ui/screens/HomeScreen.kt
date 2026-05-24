package com.space_explorer.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space_explorer.core.Constants
import com.space_explorer.domain.model.Astronomy
import com.space_explorer.ui.components.ApodCard
import com.space_explorer.ui.components.AstronomySearchBar
import com.space_explorer.ui.components.EmptyState
import com.space_explorer.ui.components.ErrorState
import com.space_explorer.ui.components.InlineLoadingFooter
import com.space_explorer.ui.components.LoadingState
import com.space_explorer.ui.components.ThemeToggleButton
import com.space_explorer.ui.state.HomeUiState
import com.space_explorer.ui.viewmodel.AstronomyViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAstronomyClick: (Astronomy) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: AstronomyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchQuery) { viewModel.onSearchQueryChanged(searchQuery) }

    PaginationEffect(
        listState = listState,
        endReached = uiState.endReached,
        onLoadMore = viewModel::loadNextPage
    )
    ErrorSnackbarEffect(
        errorMessage = uiState.errorMessage,
        hostState = snackbarHostState,
        onDismiss = viewModel::dismissError
    )

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > Constants.SCROLL_TO_TOP_THRESHOLD }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Space Explorer") },
                actions = {
                    ThemeToggleButton(isDarkTheme = isDarkTheme, onToggle = onToggleTheme)
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refrescar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = showScrollToTop, enter = fadeIn(), exit = fadeOut()) {
                FloatingActionButton(onClick = {
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                }) {
                    Icon(Icons.Outlined.ArrowUpward, contentDescription = "Volver arriba")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        HomeBody(
            modifier = Modifier.padding(innerPadding),
            uiState = uiState,
            listState = listState,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            onRemoteSearch = viewModel::onRemoteSearch,
            onRetry = viewModel::loadInitialData,
            onAstronomyClick = onAstronomyClick,
            onToggleFavorite = viewModel::toggleFavorite
        )
    }
}

@Composable
private fun HomeBody(
    modifier: Modifier,
    uiState: HomeUiState,
    listState: LazyListState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRemoteSearch: (String) -> Unit,
    onRetry: () -> Unit,
    onAstronomyClick: (Astronomy) -> Unit,
    onToggleFavorite: (Astronomy) -> Unit
) {
    when {
        uiState.isLoading && uiState.items.isEmpty() ->
            LoadingState(modifier = modifier)

        uiState.errorMessage != null && uiState.items.isEmpty() ->
            ErrorState(message = uiState.errorMessage, onRetry = onRetry, modifier = modifier)

        else -> HomeList(
            modifier = modifier,
            uiState = uiState,
            listState = listState,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onRemoteSearch = onRemoteSearch,
            onAstronomyClick = onAstronomyClick,
            onToggleFavorite = onToggleFavorite
        )
    }
}

@Composable
private fun HomeList(
    modifier: Modifier,
    uiState: HomeUiState,
    listState: LazyListState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onRemoteSearch: (String) -> Unit,
    onAstronomyClick: (Astronomy) -> Unit,
    onToggleFavorite: (Astronomy) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("home_list"),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AstronomySearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onRemoteSearch = onRemoteSearch
            )
        }

        if (uiState.isEmpty) {
            item {
                EmptyState(
                    title = "Sin resultados",
                    description = "Intenta con otra busqueda o limpia el filtro."
                )
            }
        } else {
            items(uiState.filteredItems, key = { it.id }) { astronomy ->
                ApodCard(
                    astronomy = astronomy,
                    onClick = { onAstronomyClick(astronomy) },
                    onToggleFavorite = { onToggleFavorite(astronomy) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (uiState.isLoadingMore || uiState.isRemoteSearching) {
            item { InlineLoadingFooter() }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PaginationEffect(
    listState: LazyListState,
    endReached: Boolean,
    onLoadMore: () -> Unit
) {
    LaunchedEffect(listState, endReached) {
        snapshotFlow { shouldPrefetch(listState) }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}

private fun shouldPrefetch(listState: LazyListState): Boolean {
    val info = listState.layoutInfo
    if (info.totalItemsCount == 0) return false
    val lastVisibleIndex = info.visibleItemsInfo.lastOrNull()?.index ?: return false
    return lastVisibleIndex >= info.totalItemsCount - Constants.PAGINATION_PREFETCH_DISTANCE
}

@Composable
private fun ErrorSnackbarEffect(
    errorMessage: String?,
    hostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            hostState.showSnackbar(message = message, actionLabel = "OK")
            onDismiss()
        }
    }
}
