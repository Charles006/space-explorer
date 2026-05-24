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

    // ---- URL normalization ----------------------------------------------
    // Regression: YouTube "error 152 - 4" happened because NASA returned a
    // watch URL (or a youtu.be short URL) and we fed it directly to the
    // iframe. Iframes can only load /embed/<id>, not /watch?v=<id>.

    @Test
    fun `normalizeToEmbedUrl converts youtube watch url to embed format`() {
        val embed = normalizeToEmbedUrl("https://www.youtube.com/watch?v=abc123")
        assertThat(embed).isEqualTo("https://www.youtube-nocookie.com/embed/abc123")
    }

    @Test
    fun `normalizeToEmbedUrl strips extra query params from watch url`() {
        val embed = normalizeToEmbedUrl("https://www.youtube.com/watch?v=abc123&t=30s&list=PL1")
        assertThat(embed).isEqualTo("https://www.youtube-nocookie.com/embed/abc123")
    }

    @Test
    fun `normalizeToEmbedUrl converts youtu_be short url to embed format`() {
        val embed = normalizeToEmbedUrl("https://youtu.be/abc123")
        assertThat(embed).isEqualTo("https://www.youtube-nocookie.com/embed/abc123")
    }

    @Test
    fun `normalizeToEmbedUrl upgrades plain youtube embed to nocookie domain`() {
        // youtube-nocookie has fewer embedding restrictions than youtube.com,
        // which is what triggered error 152 on certain NASA videos.
        val embed = normalizeToEmbedUrl("https://www.youtube.com/embed/abc123")
        assertThat(embed).isEqualTo("https://www.youtube-nocookie.com/embed/abc123")
    }

    @Test
    fun `normalizeToEmbedUrl preserves existing nocookie embed`() {
        val embed = normalizeToEmbedUrl("https://www.youtube-nocookie.com/embed/abc123")
        assertThat(embed).isEqualTo("https://www.youtube-nocookie.com/embed/abc123")
    }

    @Test
    fun `normalizeToEmbedUrl converts vimeo standard url to embed player`() {
        val embed = normalizeToEmbedUrl("https://vimeo.com/123456")
        assertThat(embed).isEqualTo("https://player.vimeo.com/video/123456")
    }

    @Test
    fun `normalizeToEmbedUrl passes through unrecognized urls unchanged`() {
        // We don't want to break MP4 direct links or other rare formats.
        val embed = normalizeToEmbedUrl("https://apod.nasa.gov/video.mp4")
        assertThat(embed).isEqualTo("https://apod.nasa.gov/video.mp4")
    }

    @Test
    fun `buildEmbedHtml normalizes the iframe src`() {
        // Integration: the HTML actually rendered must already point at /embed/,
        // never at /watch?v=. This is what would have prevented error 152.
        val html = buildEmbedHtml("https://www.youtube.com/watch?v=abc123")
        assertThat(html).contains("youtube-nocookie.com/embed/abc123")
        assertThat(html).doesNotContain("watch?v=")
    }
}
