package com.space_explorer.data.mapper

import com.google.common.truth.Truth.assertThat
import com.space_explorer.data.model.ApodResponse
import org.junit.Test

class AstronomyMapperTest {

    @Test
    fun fromApi_imageResponse_mapsAllFields() {
        val response = imageResponse()

        val astronomy = AstronomyMapper.fromApi(response, isFavorite = false)

        assertThat(astronomy.id).isEqualTo(response.date)
        assertThat(astronomy.imageUrl).isEqualTo(response.url)
        assertThat(astronomy.isImage).isTrue()
        assertThat(astronomy.isFavorite).isFalse()
    }

    @Test
    fun fromApi_videoWithThumbnail_usesThumbnailAsImage() {
        val response = videoResponse(thumbnailUrl = "https://thumb/x.jpg")

        val astronomy = AstronomyMapper.fromApi(response, isFavorite = true)

        assertThat(astronomy.imageUrl).isEqualTo("https://thumb/x.jpg")
        assertThat(astronomy.isImage).isFalse()
        assertThat(astronomy.isFavorite).isTrue()
    }

    @Test
    fun fromApi_videoWithoutThumbnail_imageUrlEmptyVideoUrlSet() {
        val response = videoResponse(thumbnailUrl = null, url = "https://video/y.mp4")

        val astronomy = AstronomyMapper.fromApi(response, isFavorite = false)

        assertThat(astronomy.imageUrl).isEmpty()
        assertThat(astronomy.videoUrl).isEqualTo("https://video/y.mp4")
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromApi_blankDate_throws() {
        AstronomyMapper.fromApi(imageResponse().copy(date = ""), isFavorite = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromApi_blankTitle_throws() {
        AstronomyMapper.fromApi(imageResponse().copy(title = ""), isFavorite = false)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromApi_unsupportedMediaType_throws() {
        AstronomyMapper.fromApi(imageResponse().copy(mediaType = "audio"), isFavorite = false)
    }

    @Test
    fun fromApi_emptyUrl_imageUrlEmpty() {
        val astronomy = AstronomyMapper.fromApi(
            imageResponse().copy(url = ""),
            isFavorite = false
        )
        assertThat(astronomy.imageUrl).isEmpty()
        assertThat(astronomy.title).isEqualTo("Mars Sunrise")
    }

    @Test
    fun fromApi_nonHttpUrl_imageUrlEmpty() {
        val astronomy = AstronomyMapper.fromApi(
            imageResponse().copy(url = "ftp://invalid"),
            isFavorite = false
        )
        assertThat(astronomy.imageUrl).isEmpty()
    }

    @Test
    fun fromApi_videoYoutubeEmbed_populatesVideoUrlWithEmbedAndImageUrlWithThumbnail() {
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

    @Test
    fun fromApi_videoVimeoNoThumbnail_imageUrlEmptyVideoUrlSet() {
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

    @Test
    fun fromApi_image_videoUrlNull() {
        val a = AstronomyMapper.fromApi(imageResponse(), isFavorite = false)
        assertThat(a.videoUrl).isNull()
    }

    @Test
    fun roundTrip_toEntityAndBack_preservesFields() {
        val astronomy = AstronomyMapper.fromApi(imageResponse(), isFavorite = true)
        val entity = AstronomyMapper.toEntity(astronomy)
        val back = AstronomyMapper.fromEntity(entity)

        assertThat(back.id).isEqualTo(astronomy.id)
        assertThat(back.title).isEqualTo(astronomy.title)
        assertThat(back.imageUrl).isEqualTo(astronomy.imageUrl)
        assertThat(back.isFavorite).isTrue()
    }

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
}
