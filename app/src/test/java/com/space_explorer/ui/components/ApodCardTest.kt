package com.space_explorer.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.space_explorer.domain.model.Astronomy
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ApodCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sample = Astronomy(
        id = "2026-05-22",
        date = "2026-05-22",
        title = "Mars Sunrise",
        explanation = "A sunrise on Mars.",
        imageUrl = "https://image",
        hdImageUrl = null,
        videoUrl = null,
        mediaType = "image",
        copyright = null,
        isFavorite = false
    )

    @Test
    fun apodCard_displaysTitle() {
        composeRule.setContent {
            ApodCard(astronomy = sample, onClick = {}, onToggleFavorite = {})
        }

        composeRule.onAllNodesWithText("Mars Sunrise").assertCountEquals(1)
    }

    @Test
    fun apodCard_clickInvokesOnClick() {
        var clicked = false
        composeRule.setContent {
            ApodCard(astronomy = sample, onClick = { clicked = true }, onToggleFavorite = {})
        }

        composeRule.onNodeWithTag("apod_card_${sample.id}").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun apodCard_toggleFavoriteInvokesCallback() {
        var toggled = false
        composeRule.setContent {
            ApodCard(astronomy = sample, onClick = {}, onToggleFavorite = { toggled = true })
        }

        composeRule.onNodeWithTag("favorite_button_${sample.id}").performClick()
        assertThat(toggled).isTrue()
    }

    @Test
    fun apodCard_showsFilledStarWhenFavorite() {
        composeRule.setContent {
            ApodCard(
                astronomy = sample.copy(isFavorite = true),
                onClick = {},
                onToggleFavorite = {}
            )
        }

        val ctx = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithContentDescription(ctx.getString(com.space_explorer.R.string.cd_remove_from_favorites)).assertIsDisplayed()
    }
}
