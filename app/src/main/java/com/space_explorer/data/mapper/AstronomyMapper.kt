package com.space_explorer.data.mapper

import com.space_explorer.data.local.entity.FavoriteEntity
import com.space_explorer.data.model.ApodResponse
import com.space_explorer.domain.model.Astronomy

/**
 * Bidirectional mappers between data-layer DTOs/entities and the domain model.
 *
 * Centralizing mapping here keeps DTOs (`ApodResponse`, `FavoriteEntity`)
 * focused on serialization concerns and the domain model free of framework
 * annotations.
 *
 * Mapping rules for media URLs:
 *   * Image-type APOD       → imageUrl = response.url, videoUrl = null
 *   * Video-type APOD       → imageUrl = response.thumbnailUrl (or empty),
 *                             videoUrl = response.url (the YouTube/Vimeo embed)
 *   * Blank / non-http URLs are normalized to empty string instead of
 *     throwing so a single bad item never tumbles an entire page.
 */
object AstronomyMapper {

    private const val MEDIA_TYPE_VIDEO = "video"
    private val ALLOWED_MEDIA_TYPES = setOf("image", "video")

    /**
     * Convert a NASA APOD API response into the domain [Astronomy].
     *
     * @param response Raw API response.
     * @param isFavorite Whether the item exists in the local favorites table.
     * @throws IllegalArgumentException if `date`, `title` or `mediaType`
     *         violate the contract (these are non-negotiable identifiers).
     */
    fun fromApi(response: ApodResponse, isFavorite: Boolean): Astronomy {
        require(response.date.isNotBlank()) { "APOD response without date" }
        require(response.title.isNotBlank()) { "APOD response without title" }
        require(response.mediaType in ALLOWED_MEDIA_TYPES) {
            "Unsupported media_type: ${response.mediaType}"
        }

        val isVideo = response.mediaType == MEDIA_TYPE_VIDEO
        val imageUrl: String = if (isVideo) {
            // For videos: imageUrl is strictly the thumbnail (or empty).
            // Never fall back to the embed URL — Coil cannot render that.
            response.thumbnailUrl.toDisplayUrlOrEmpty()
        } else {
            response.url.toDisplayUrlOrEmpty()
        }
        val videoUrl: String? = if (isVideo) {
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
            isFavorite = isFavorite
        )
    }

    /** Convert a persisted favorite into the domain model. */
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
        isFavorite = true
    )

    /** Convert the domain model into a persistable entity. */
    fun toEntity(astronomy: Astronomy): FavoriteEntity = FavoriteEntity(
        id = astronomy.id,
        date = astronomy.date,
        title = astronomy.title,
        explanation = astronomy.explanation,
        imageUrl = astronomy.imageUrl,
        hdImageUrl = astronomy.hdImageUrl,
        videoUrl = astronomy.videoUrl,
        mediaType = astronomy.mediaType,
        copyright = astronomy.copyright
    )

    /**
     * Returns the receiver if it is an http(s) URL, otherwise an empty string.
     * Tolerates `null`, blank strings and unsupported schemes (ftp, data, file).
     */
    private fun String?.toDisplayUrlOrEmpty(): String =
        if (this != null && startsWith("http", ignoreCase = true)) this else ""
}
