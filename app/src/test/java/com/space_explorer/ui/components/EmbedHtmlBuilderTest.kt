package com.space_explorer.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EmbedHtmlBuilderTest {

    @Test
    fun buildEmbedHtml_injectsAutoplay() {
        val html = buildEmbedHtml("https://www.youtube.com/embed/abc")
        assertThat(html).contains("autoplay=1")
        assertThat(html).contains("playsinline=1")
    }

    @Test
    fun buildEmbedHtml_preservesExistingQueryString() {
        val html = buildEmbedHtml("https://www.youtube.com/embed/abc?rel=0")
        assertThat(html).contains("rel=0&autoplay=1")
    }

    @Test
    fun buildEmbedHtml_doesNotDuplicateExistingAutoplay() {
        val html = buildEmbedHtml("https://www.youtube.com/embed/abc?autoplay=0")
        assertThat(html.split("autoplay=").size - 1).isEqualTo(1)
    }

    @Test
    fun normalizeToEmbedUrl_watchUrl_returnsEmbed() {
        val embed = normalizeToEmbedUrl("https://www.youtube.com/watch?v=abc123")
        assertThat(embed).isEqualTo("https://www.youtube-nocookie.com/embed/abc123")
    }

    @Test
    fun normalizeToEmbedUrl_watchUrlWithExtraParams_dropsThem() {
        val embed = normalizeToEmbedUrl("https://www.youtube.com/watch?v=abc123&t=30s&list=PL1")
        assertThat(embed).isEqualTo("https://www.youtube-nocookie.com/embed/abc123")
    }

    @Test
    fun normalizeToEmbedUrl_youtuBeShortLink_returnsEmbed() {
        val embed = normalizeToEmbedUrl("https://youtu.be/abc123")
        assertThat(embed).isEqualTo("https://www.youtube-nocookie.com/embed/abc123")
    }

    @Test
    fun normalizeToEmbedUrl_plainEmbed_upgradesToNocookie() {
        val embed = normalizeToEmbedUrl("https://www.youtube.com/embed/abc123")
        assertThat(embed).isEqualTo("https://www.youtube-nocookie.com/embed/abc123")
    }

    @Test
    fun normalizeToEmbedUrl_nocookieEmbed_returnsUnchanged() {
        val embed = normalizeToEmbedUrl("https://www.youtube-nocookie.com/embed/abc123")
        assertThat(embed).isEqualTo("https://www.youtube-nocookie.com/embed/abc123")
    }

    @Test
    fun normalizeToEmbedUrl_vimeoStandard_returnsPlayerEmbed() {
        val embed = normalizeToEmbedUrl("https://vimeo.com/123456")
        assertThat(embed).isEqualTo("https://player.vimeo.com/video/123456")
    }

    @Test
    fun normalizeToEmbedUrl_unrecognizedUrl_passesThrough() {
        val embed = normalizeToEmbedUrl("https://apod.nasa.gov/video.mp4")
        assertThat(embed).isEqualTo("https://apod.nasa.gov/video.mp4")
    }

    @Test
    fun buildEmbedHtml_normalizesWatchUrlInIframeSrc() {
        val html = buildEmbedHtml("https://www.youtube.com/watch?v=abc123")
        assertThat(html).contains("youtube-nocookie.com/embed/abc123")
        assertThat(html).doesNotContain("watch?v=")
    }

    @Test
    fun buildEmbedHtml_mp4DirectUrl_usesVideoTag() {
        val url = "https://apod.nasa.gov/apod/image/2605/MarsEclipse_perseverance.mp4"
        val html = buildEmbedHtml(url)
        assertThat(html).contains("<video")
        assertThat(html).contains("<source src=\"$url\"")
        assertThat(html).doesNotContain("<iframe")
    }

    @Test
    fun buildEmbedHtml_mp4WithQueryString_stillDetectedAsDirectFile() {
        val html = buildEmbedHtml("https://example.com/clip.mp4?token=abc")
        assertThat(html).contains("<video")
        assertThat(html).doesNotContain("<iframe")
    }

    @Test
    fun buildEmbedHtml_webmDirectUrl_usesVideoTag() {
        val html = buildEmbedHtml("https://example.com/clip.webm")
        assertThat(html).contains("<video")
    }

    @Test
    fun buildEmbedHtml_youtubeUrl_stillUsesIframe() {
        val html = buildEmbedHtml("https://www.youtube.com/embed/abc123")
        assertThat(html).contains("<iframe")
        assertThat(html).doesNotContain("<video")
    }
}
