package com.space_explorer.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EmbeddedVideoPlayerTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Regression: video detail used to show a grey indefinite spinner because
    // Coil cannot render embed URLs. Player MUST start with a tappable play
    // button instead.
    @Test
    fun `initial state shows play button`() {
        composeRule.setContent {
            EmbeddedVideoPlayer(
                embedUrl = "https://youtube.com/embed/abc",
                thumbnailUrl = "https://img/x.jpg",
                contentDescription = "Test"
            )
        }
        composeRule.onNodeWithTag("embedded_video_play_button").assertIsDisplayed()
    }

    // Regression: tapping play used to open an external browser (Intent). It
    // must now swap into an in-process WebView.
    @Test
    fun `tapping play swaps to webview`() {
        composeRule.setContent {
            EmbeddedVideoPlayer(
                embedUrl = "https://youtube.com/embed/abc",
                thumbnailUrl = "",
                contentDescription = "Test"
            )
        }
        composeRule.onNodeWithTag("embedded_video_play_button").performClick()
        composeRule.onNodeWithTag("embedded_video_webview").assertIsDisplayed()
    }

    // Regression: the play state must survive recomposition (e.g. parent
    // recomposes because of unrelated state change) so playback does not
    // reset back to the thumbnail.
    @Test
    fun `play state survives recomposition`() {
        var contentDesc by mutableStateOf("Initial")
        composeRule.setContent {
            EmbeddedVideoPlayer(
                embedUrl = "https://youtube.com/embed/abc",
                thumbnailUrl = "",
                contentDescription = contentDesc
            )
        }
        composeRule.onNodeWithTag("embedded_video_play_button").performClick()
        composeRule.runOnIdle { contentDesc = "After recomposition" }
        composeRule.onNodeWithTag("embedded_video_webview").assertIsDisplayed()
    }
}
