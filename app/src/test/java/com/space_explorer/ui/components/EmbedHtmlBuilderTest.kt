package com.space_explorer.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [buildEmbedHtml]. Keeps the query-string mutation logic
 * regression-tested without needing a WebView.
 */
class EmbedHtmlBuilderTest {

    // Regression: video did not autoplay because the iframe URL never got the
    // autoplay flag. Player flow assumed it would be there.
    @Test
    fun `buildEmbedHtml injects autoplay=1`() {
        val html = buildEmbedHtml("https://www.youtube.com/embed/abc")
        assertThat(html).contains("autoplay=1")
        assertThat(html).contains("playsinline=1")
    }

    // Regression: when NASA returned an embed that already had query params
    // (e.g. ?rel=0), naively appending broke the URL.
    @Test
    fun `buildEmbedHtml respects existing query string`() {
        val html = buildEmbedHtml("https://www.youtube.com/embed/abc?rel=0")
        assertThat(html).contains("rel=0&autoplay=1")
    }

    // Regression: if the embed explicitly opted out of autoplay, we must not
    // override or duplicate the flag.
    @Test
    fun `buildEmbedHtml does not duplicate existing autoplay param`() {
        val html = buildEmbedHtml("https://www.youtube.com/embed/abc?autoplay=0")
        assertThat(html.split("autoplay=").size - 1).isEqualTo(1)
    }
}
