package com.space_explorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity backing the `favorites` table.
 *
 * Mapping to/from [com.space_explorer.domain.model.Astronomy] is delegated to
 * [com.space_explorer.data.mapper.AstronomyMapper] so persistence concerns
 * stay separate from domain shape.
 *
 * Schema history:
 *   * v1 — initial fields (no [videoUrl]).
 *   * v2 — adds nullable [videoUrl] column to support in-app video playback.
 *
 * [savedAt] is set by the database layer at insert time and never read by
 * the domain; it is only used to sort the favorites list newest-first.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val date: String,
    val title: String,
    val explanation: String,
    val imageUrl: String,
    val hdImageUrl: String?,
    val videoUrl: String?,
    val mediaType: String,
    val copyright: String?,
    val savedAt: Long = System.currentTimeMillis()
)
