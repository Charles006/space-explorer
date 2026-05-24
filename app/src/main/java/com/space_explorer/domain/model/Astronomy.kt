package com.space_explorer.domain.model

import androidx.compose.runtime.Immutable

/**
 * Domain model for a single NASA APOD entry.
 *
 * Field semantics:
 *   * [imageUrl]    – Always something Coil can render (a thumbnail or a still
 *                     image). For videos this is the YouTube/Vimeo thumbnail
 *                     when NASA provides one, or empty otherwise.
 *   * [hdImageUrl]  – Optional high-resolution still. `null` for videos.
 *   * [videoUrl]    – YouTube/Vimeo embed URL for video-type APODs; `null`
 *                     for image-type APODs. Lives separately from [imageUrl]
 *                     so the UI does not have to guess what kind of URL it
 *                     is dealing with.
 *
 * The separation is the antidote to a real bug: the previous shape overloaded
 * `imageUrl` with either a thumbnail OR an embed depending on context, which
 * fed an embed to Coil and produced an indefinite grey placeholder on the
 * detail screen.
 */
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
    val isFavorite: Boolean = false
) {
    val isImage: Boolean get() = mediaType == "image"
    val isVideo: Boolean get() = mediaType == "video"
}
