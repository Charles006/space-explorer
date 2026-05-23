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

    @Test
    fun `fromApi falls back to url when video has no thumbnail`() {
        val response = videoResponse(thumbnailUrl = null, url = "https://video/y.mp4")

        val astronomy = AstronomyMapper.fromApi(response, isFavorite = false)

        assertThat(astronomy.imageUrl).isEqualTo("https://video/y.mp4")
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

    @Test(expected = IllegalArgumentException::class)
    fun `fromApi rejects non-http url`() {
        AstronomyMapper.fromApi(imageResponse().copy(url = "ftp://x"), isFavorite = false)
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
