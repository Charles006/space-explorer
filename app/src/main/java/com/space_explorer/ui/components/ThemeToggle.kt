package com.space_explorer.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.space_explorer.R

@Composable
fun ThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.testTag("theme_toggle_button"),
    ) {
        AnimatedContent(
            targetState = isDarkTheme,
            transitionSpec = {
                (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
            },
            label = "theme_toggle",
        ) { dark ->
            if (dark) {
                Icon(
                    imageVector = Icons.Outlined.LightMode,
                    contentDescription = stringResource(R.string.theme_toggle_to_light),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = stringResource(R.string.theme_toggle_to_dark),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeToggleButtonDarkPreview() {
    ThemeToggleButton(isDarkTheme = true, onToggle = {})
}

@Preview(showBackground = true)
@Composable
private fun ThemeToggleButtonLightPreview() {
    ThemeToggleButton(isDarkTheme = false, onToggle = {})
}
