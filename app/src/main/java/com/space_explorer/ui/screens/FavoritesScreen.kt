package com.space_explorer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space_explorer.domain.model.Astronomy
import com.space_explorer.ui.components.ApodCard
import com.space_explorer.ui.components.EmptyFavorites
import com.space_explorer.ui.components.ErrorState
import com.space_explorer.ui.components.LoadingState
import com.space_explorer.ui.components.ThemeToggleButton
import com.space_explorer.ui.viewmodel.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onAstronomyClick: (Astronomy) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(searchQuery) { viewModel.onSearchQueryChanged(searchQuery) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favoritos") },
                actions = {
                    ThemeToggleButton(isDarkTheme = isDarkTheme, onToggle = onToggleTheme)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading ->
                LoadingState(modifier = Modifier.padding(innerPadding))

            uiState.errorMessage != null ->
                ErrorState(
                    message = uiState.errorMessage!!,
                    modifier = Modifier.padding(innerPadding)
                )

            else -> FavoritesList(
                modifier = Modifier.padding(innerPadding),
                favorites = uiState.favorites,
                isEmpty = uiState.isEmpty,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onAstronomyClick = onAstronomyClick,
                onRemove = viewModel::removeFavorite
            )
        }
    }
}

@Composable
private fun FavoritesList(
    modifier: Modifier,
    favorites: List<Astronomy>,
    isEmpty: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onAstronomyClick: (Astronomy) -> Unit,
    onRemove: (Astronomy) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_list"),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FavoritesFilterField(query = searchQuery, onQueryChange = onSearchQueryChange)
        }

        if (isEmpty) {
            item { EmptyFavorites() }
        } else {
            items(favorites, key = { it.id }) { favorite ->
                ApodCard(
                    astronomy = favorite,
                    onClick = { onAstronomyClick(favorite) },
                    onToggleFavorite = { onRemove(favorite) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun FavoritesFilterField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        placeholder = { Text("Filtrar favoritos") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = "Limpiar")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
    )
}
