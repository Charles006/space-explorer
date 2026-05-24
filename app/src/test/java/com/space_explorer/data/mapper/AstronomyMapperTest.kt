package com.space_explorer.data.mapper

import com.google.common.truth.Truth.assertThat
import com.space_explorer.data.model.ApodResponse
import org.junit.Test

/**
 * Validation of [AstronomyMapper]. These tests pin down the contract that
 * other layers rely on:
 *
 *  * Required fields are enforced (date, title, mediaType).
 *  * Video items use `thumbnail_url` when available, fall back to `url`.
 *  * Malformed URLs are rejected early instead of bubbling up to Coil.
 */
class AstronomyMapperTest {

    @Test
    fun `fromApi maps image response correctly`() {
        val response = imageResponse()

        val astronomy = AstronomyMapper.fromApi(response, isFavorite = false)

        assertThat(astronomy.id).isEqualTo(response.date)
        assertThat(astronomy.imageUrl).isEqualTo(response.url)
        assertThat(astronomy.isImage).isTrue()
        assertThat(astronomy.isFavorite).isFalse()
    }

    @Test
    fun `fromApi uses thumbnailUrl for videos when present`() {
        val response = videoResponse(thumbnailUrl = "https://thumb/x.jpg")

        val astronomy = AstronomyMapper.fromApi(response, isFavorite = true)

        assertThat(astronomy.imageUrl).isEqualTo("https://thumb/x.jpg")
        assertThat(astronomy.isImage).isFalse()
        assertThat(astronomy.isFavorite).isTrue()
    }

    // Contract change: before the split, video items without thumbnail used to
    // expose the embed URL in imageUrl (so Coil tried to render it). Now imageUrl
    // is strictly a thumbnail or empty, and the embed URL lives in videoUrl.
    @Test
    fun `fromApi video without thumbnail keeps imageUrl empty and exposes embed via videoUrl`() {
        val response = videoResponse(thumbnailUrl = null, url = "https://video/y.mp4")

        val astronomy = AstronomyMapper.fromApi(response, isFavorite = false)

        assertThat(astronomy.imageUrl).isEmpty()
        assertThat(astronomy.videoUrl).isEqualTo("https://video/y.mp4")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromApi rejects blank date`() {
        AstronomyMapper.fromApi(imageResponse().copy(date = ""), isFavorite = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromApi rejects blank title`() {
        AstronomyMapper.fromApi(imageResponse().copy(title = ""), isFavorite = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fromApi rejects unsupported media type`() {
        AstronomyMapper.fromApi(imageResponse().copy(mediaType = "audio"), isFavorite = false)
    }

    // Regression: HomeScreen pagination was stuck at 10 items because NASA returned
    // an item with empty 'url' and the mapper threw, killing the whole 10-item batch.
    // See screenshot bug "Invalid media url returned by API: ''".
    @Test
    fun `fromApi does not throw when url is empty`() {
        val astronomy = AstronomyMapper.fromApi(
            imageResponse().copy(url = ""),
            isFavorite = false
        )
        assertThat(astronomy.imageUrl).isEmpty()
        assertThat(astronomy.title).isEqualTo("Mars Sunrise")
    }

    // Regression: same root cause as the empty-url case, with ftp/data/file schemes.
    @Test
    fun `fromApi does not throw when url uses unsupported scheme`() {
        val astronomy = AstronomyMapper.fromApi(
            imageResponse().copy(url = "ftp://invalid"),
            isFavorite = false
        )
        assertThat(astronomy.imageUrl).isEmpty()
    }

    // Regression: videos used to overload imageUrl with the embed URL, which
    // Coil cannot render. New shape: imageUrl = thumbnail, videoUrl = embed.
    @Test
    fun `fromApi video populates videoUrl with embed and imageUrl with thumbnail`() {
        val response = ApodResponse(
            date = "2026-05-22",
            title = "Comet flyby",
            explanation = "...",
            url = "https://youtube.com/embed/abc",
            hdUrl = null,
            mediaType = "video",
            copyright = null,
            thumbnailUrl = "https://img.youtube.com/vi/abc/maxres.jpg"
        )

        val a = AstronomyMapper.fromApi(response, isFavorite = false)

        assertThat(a.videoUrl).isEqualTo("https://youtube.com/embed/abc")
        assertThat(a.imageUrl).isEqualTo("https://img.youtube.com/vi/abc/maxres.jpg")
        assertThat(a.isVideo).isTrue()
    }

    // Regression: video without thumbnail used to fall back to embed as imageUrl,
    // breaking Coil. New rule: imageUrl stays empty, videoUrl still works.
    @Test
    fun `fromApi video without thumbnail leaves imageUrl empty but keeps videoUrl`() {
        val response = ApodResponse(
            date = "2026-05-22",
            title = "X",
            explanation = "...",
            url = "https://vimeo.com/embed/xyz",
            hdUrl = null,
            mediaType = "video",
            copyright = null,
            thumbnailUrl = null
        )

        val a = AstronomyMapper.fromApi(response, isFavorite = false)

        assertThat(a.imageUrl).isEmpty()
        assertThat(a.videoUrl).isEqualTo("https://vimeo.com/embed/xyz")
    }

    // Regression: ensures the new videoUrl column is null for image-type APODs.
    @Test
    fun `fromApi image leaves videoUrl null`() {
        val a = AstronomyMapper.fromApi(imageResponse(), isFavorite = false)
        assertThat(a.videoUrl).isNull()
    }

    @Test
    fun `toEntity and fromEntity round-trip preserve data`() {
        val astronomy = AstronomyMapper.fromApi(imageResponse(), isFavorite = true)
        val entity = AstronomyMapper.toEntity(astronomy)
        val back = AstronomyMapper.fromEntity(entity)

        assertThat(back.id).isEqualTo(astronomy.id)
        assertThat(back.title).isEqualTo(astronomy.title)
        assertThat(back.imageUrl).isEqualTo(astronomy.imageUrl)
        assertThat(back.isFavorite).isTrue()
    }

    // region ── Helpers ───────────────────────────────────────────────────

    private fun imageResponse() = ApodResponse(
        date = "2026-05-22",
        title = "Mars Sunrise",
        explanation = "Cool",
        url = "https://nasa/x.jpg",
        hdUrl = "https://nasa/x-hd.jpg",
        mediaType = "image",
        copyright = "NASA"
    )

    private fun videoResponse(
        thumbnailUrl: String?,
        url: String = "https://nasa/embed/x"
    ) = ApodResponse(
        date = "2026-05-22",
        title = "Some Video",
        explanation = "Cool",
        url = url,
        hdUrl = null,
        mediaType = "video",
        copyright = null,
        thumbnailUrl = thumbnailUrl
    )

    // endregion
}
