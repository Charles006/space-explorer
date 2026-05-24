package com.space_explorer.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Astronomy(
    val id: String,
    val date: String,
    val title: String,
    val explanation: String,
    val imageUrl: String,
    val hdImageUrl: String?,
    val videoUrl: String?,
    val mediaType: String,
    val copyright: String?,
    val isFavorite: Boolean = false,
) {
    val isImage: Boolean get() = mediaType == "image"
    val isVideo: Boolean get() = mediaType == "video"
}
