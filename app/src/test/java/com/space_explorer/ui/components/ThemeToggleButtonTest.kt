package com.space_explorer.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ThemeToggleButtonTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsDarkModeIconWhenLightTheme() {
        composeRule.setContent {
            ThemeToggleButton(isDarkTheme = false, onToggle = {})
        }

        composeRule.onNodeWithContentDescription("Activar modo oscuro").assertIsDisplayed()
    }

    @Test
    fun showsLightModeIconWhenDarkTheme() {
        composeRule.setContent {
            ThemeToggleButton(isDarkTheme = true, onToggle = {})
        }

        composeRule.onNodeWithContentDescription("Activar modo claro").assertIsDisplayed()
    }

    @Test
    fun clickInvokesOnToggle() {
        var clicked = false
        composeRule.setContent {
            ThemeToggleButton(isDarkTheme = false, onToggle = { clicked = true })
        }

        composeRule.onNodeWithTag("theme_toggle_button").performClick()
        assertThat(clicked).isTrue()
    }
}
