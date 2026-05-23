package com.space_explorer.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.space_explorer.domain.model.Astronomy
import com.space_explorer.ui.components.ErrorState
import com.space_explorer.ui.components.LoadingState
import com.space_explorer.ui.components.ThemeToggleButton
import com.space_explorer.ui.util.DateUtils
import com.space_explorer.ui.viewmodel.DetailViewModel

/**
 * Detail screen for a single APOD.
 *
 * Reads [DetailViewModel.uiState] and renders the three possible UI shapes:
 *   * loading (full-screen spinner)
 *   * error   (centered card + retry)
 *   * content (scrollable image + metadata)
 *
 * Toggle-favorite errors are surfaced through a [SnackbarHost] rather than
 * blocking the whole screen, since the content remains usable even if the
 * write fails.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val astronomy = uiState.astronomy
    val snackbarHostState = remember { SnackbarHostState() }

    // Display toggle-favorite errors as snackbars. Content is still visible.
    LaunchedEffect(uiState.errorMessage, astronomy) {
        val message = uiState.errorMessage
        if (message != null && astronomy != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = astronomy?.title,
                astronomy = astronomy,
                isDarkTheme = isDarkTheme,
                onBack = onBack,
                onToggleTheme = onToggleTheme,
                onToggleFavorite = viewModel::toggleFavorite
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(innerPadding))

            astronomy == null && uiState.errorMessage != null -> ErrorState(
                message = uiState.errorMessage!!,
                onRetry = viewModel::retry,
                modifier = Modifier.padding(innerPadding)
            )

            astronomy != null -> DetailContent(
                astronomy = astronomy,
                scrollState = scrollState,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

// region ── Internals ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    title: String?,
    astronomy: Astronomy?,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    TopAppBar(
        title = { Text(title?.take(30) ?: "Detalle") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Volver"
                )
            }
        },
        actions = {
            ThemeToggleButton(isDarkTheme = isDarkTheme, onToggle = onToggleTheme)
            if (astronomy != null) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("detail_favorite_button")
                ) {
                    Icon(
                        imageVector = if (astronomy.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (astronomy.isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                        tint = if (astronomy.isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun DetailContent(
    astronomy: Astronomy,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .animateContentSize()
            .testTag("detail_content")
    ) {
        DetailCoverImage(astronomy)
        DetailMetadata(astronomy)
    }
}

@Composable
private fun DetailCoverImage(astronomy: Astronomy) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(DETAIL_COVER_ASPECT_RATIO)
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(astronomy.hdImageUrl ?: astronomy.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = astronomy.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            loading = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize()
                ) {}
            }
        )
    }
}

@Composable
private fun DetailMetadata(astronomy: Astronomy) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = astronomy.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = DateUtils.prettyPrint(astronomy.date),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        astronomy.copyright?.let { copyright ->
            Text(
                text = "Credito: $copyright",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = astronomy.explanation,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
        )
        Spacer(Modifier.height(24.dp))
    }
}

private const val DETAIL_COVER_ASPECT_RATIO: Float = 4f / 3f

// endregion
