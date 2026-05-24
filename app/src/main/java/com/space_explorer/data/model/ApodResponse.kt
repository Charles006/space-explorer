package com.space_explorer.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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
