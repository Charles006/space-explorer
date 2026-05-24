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
 * All mappers are pure functions: no side effects, no null-only fallbacks
 * that would silently hide upstream contract violations. Required fields are
 * validated and any breach surfaces as a thrown [IllegalArgumentException]
 * caught upstream by [com.space_explorer.data.network.ApiErrorMapper].
 */
object AstronomyMapper {

    private const val MEDIA_TYPE_VIDEO = "video"
    private val ALLOWED_MEDIA_TYPES = setOf("image", "video")

    /**
     * Convert a NASA APOD API response into the domain [Astronomy].
     *
     * @param response Raw API response.
     * @param isFavorite Whether the item exists in the local favorites table.
     * @throws IllegalArgumentException if the response violates the documented contract.
     */
    fun fromApi(response: ApodResponse, isFavorite: Boolean): Astronomy {
        require(response.date.isNotBlank()) { "APOD response without date" }
        require(response.title.isNotBlank()) { "APOD response without title" }
        require(response.mediaType in ALLOWED_MEDIA_TYPES) {
            "Unsupported media_type: ${response.mediaType}"
        }

        val displayUrl = resolveDisplayUrl(response)
        return Astronomy(
            id = response.date,
            date = response.date,
            title = response.title,
            explanation = response.explanation,
            imageUrl = displayUrl,
            hdImageUrl = response.hdUrl,
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
        mediaType = astronomy.mediaType,
        copyright = astronomy.copyright
    )

    /**
     * Resolves the URL Coil should render for this APOD.
     *
     * For videos NASA returns an embed URL in [ApodResponse.url]; the actual
     * frame we want is [ApodResponse.thumbnailUrl] when present. Images use
     * [ApodResponse.url] directly.
     *
     * NASA occasionally returns items with a blank `url` or an unsupported
     * scheme (interactive HTML, very recent posts pending media upload,
     * experimental formats). [toDisplayUrlOrEmpty] normalizes those into an
     * empty string so a single bad item never tumbles an entire batch — Coil
     * already renders an error placeholder for blank URLs, so the user still
     * sees title / date / explanation.
     */
    private fun resolveDisplayUrl(response: ApodResponse): String = when (response.mediaType) {
        MEDIA_TYPE_VIDEO -> (response.thumbnailUrl ?: response.url).toDisplayUrlOrEmpty()
        else -> response.url.toDisplayUrlOrEmpty()
    }

    /**
     * Returns the receiver if it is an http(s) URL, otherwise an empty string.
     * Tolerates `null`, blank strings, and unsupported schemes (ftp, data, file).
     */
    private fun String?.toDisplayUrlOrEmpty(): String =
        if (this != null && startsWith("http", ignoreCase = true)) this else ""
}
