package com.space_explorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.space_explorer.R

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val cd = stringResource(R.string.cd_loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = cd },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.state_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ErrorState(
    message: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val resolved = message ?: stringResource(R.string.state_error_default_message)
    val announcement = stringResource(R.string.state_error_announcement, resolved)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .semantics { contentDescription = announcement },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.state_error_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = resolved,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.state_action_retry))
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String? = null,
    description: String? = null,
    icon: ImageVector = Icons.Outlined.Search,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .wrapContentHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title ?: stringResource(R.string.state_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description ?: stringResource(R.string.state_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EmptyFavorites(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.favorites_empty_title),
        description = stringResource(R.string.favorites_empty_description),
        icon = Icons.Outlined.Star,
        modifier = modifier,
    )
}

@Composable
fun InlineLoadingFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
    }
}

private enum class PreviewState { LOADING, ERROR, EMPTY, EMPTY_FAVORITES, INLINE_LOADING }

private class StatePreviewProvider : PreviewParameterProvider<PreviewState> {
    override val values = PreviewState.entries.asSequence()
}

@Preview(showBackground = true)
@Composable
private fun StatePreview(
    @PreviewParameter(StatePreviewProvider::class) state: PreviewState,
) {
    when (state) {
        PreviewState.LOADING -> LoadingState()
        PreviewState.ERROR -> ErrorState(message = "No se pudo conectar con el servidor", onRetry = {})
        PreviewState.EMPTY -> EmptyState()
        PreviewState.EMPTY_FAVORITES -> EmptyFavorites()
        PreviewState.INLINE_LOADING -> InlineLoadingFooter()
    }
}
