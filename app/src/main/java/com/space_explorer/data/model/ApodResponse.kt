package com.space_explorer.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Raw NASA APOD endpoint response.
 *
 * This DTO is intentionally close to the wire format and free of business
 * logic. Conversion to the domain model lives in
 * [com.space_explorer.data.mapper.AstronomyMapper] so that validation rules
 * are reviewable in one place.
 *
 * Reference: https://github.com/nasa/apod-api
 */
@JsonClass(generateAdapter = true)
data class ApodResponse(
    @Json(name = "date") val date: String,
    @Json(name = "title") val title: String,
    @Json(name = "explanation") val explanation: String,
    @Json(name = "url") val url: String,
    @Json(name = "hdurl") val hdUrl: String? = null,
    @Json(name = "media_type") val mediaType: String,
    @Json(name = "copyright") val copyright: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null
)
