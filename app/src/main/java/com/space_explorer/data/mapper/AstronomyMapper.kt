package com.space_explorer.data.mapper

import com.space_explorer.data.local.entity.FavoriteEntity
import com.space_explorer.data.model.ApodResponse
import com.space_explorer.domain.model.Astronomy

object AstronomyMapper {

    private const val MEDIA_TYPE_VIDEO = "video"
    private val ALLOWED_MEDIA_TYPES = setOf("image", "video")

    fun fromApi(response: ApodResponse, isFavorite: Boolean): Astronomy {
        require(response.date.isNotBlank()) { "APOD response without date" }
        require(response.title.isNotBlank()) { "APOD response without title" }
        require(response.mediaType in ALLOWED_MEDIA_TYPES) {
            "Unsupported media_type: ${response.mediaType}"
        }

        val isVideo = response.mediaType == MEDIA_TYPE_VIDEO
        val imageUrl = if (isVideo) {
            response.thumbnailUrl.toDisplayUrlOrEmpty()
        } else {
            response.url.toDisplayUrlOrEmpty()
        }
        val videoUrl = if (isVideo) {
            response.url.toDisplayUrlOrEmpty().ifBlank { null }
        } else {
            null
        }

        return Astronomy(
            id = response.date,
            date = response.date,
            title = response.title,
            explanation = response.explanation,
            imageUrl = imageUrl,
            hdImageUrl = response.hdUrl,
            videoUrl = videoUrl,
            mediaType = response.mediaType,
            copyright = response.copyright,
            isFavorite = isFavorite,
        )
    }

    fun fromEntity(entity: FavoriteEntity): Astronomy = Astronomy(
        id = entity.id,
        date = entity.date,
        title = entity.title,
        explanation = entity.explanation,
        imageUrl = entity.imageUrl,
        hdImageUrl = entity.hdImageUrl,
        videoUrl = entity.videoUrl,
        mediaType = entity.mediaType,
        copyright = entity.copyright,
        isFavorite = true,
    )

    fun toEntity(astronomy: Astronomy): FavoriteEntity = FavoriteEntity(
        id = astronomy.id,
        date = astronomy.date,
        title = astronomy.title,
        explanation = astronomy.explanation,
        imageUrl = astronomy.imageUrl,
        hdImageUrl = astronomy.hdImageUrl,
        videoUrl = astronomy.videoUrl,
        mediaType = astronomy.mediaType,
        copyright = astronomy.copyright,
    )

    private fun String?.toDisplayUrlOrEmpty(): String =
        if (this != null && startsWith("http", ignoreCase = true)) this else ""
}
