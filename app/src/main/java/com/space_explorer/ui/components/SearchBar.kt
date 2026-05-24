package com.space_explorer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.space_explorer.ui.util.DateUtils

@Composable
fun AstronomySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onRemoteSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val isValidDate = DateUtils.isValidIsoDate(query)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field"),
            singleLine = true,
            placeholder = { Text("Buscar por titulo o fecha (YYYY-MM-DD)") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Limpiar")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Ascii
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (isValidDate) onRemoteSearch(query)
                    keyboard?.hide()
                }
            )
        )

        AnimatedVisibility(
            visible = isValidDate,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { onRemoteSearch(query) },
                    label = { Text("Buscar fecha en NASA") },
                    leadingIcon = {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    },
                    modifier = Modifier.testTag("remote_search_chip")
                )
            }
        }
    }
}

private class SearchBarPreviewProvider : PreviewParameterProvider<String> {
    override val values = sequenceOf("", "Mars", "2024-01-15")
}

@Preview(showBackground = true)
@Composable
private fun AstronomySearchBarPreview(
    @PreviewParameter(SearchBarPreviewProvider::class) query: String
) {
    AstronomySearchBar(query = query, onQueryChange = {}, onRemoteSearch = {})
}
