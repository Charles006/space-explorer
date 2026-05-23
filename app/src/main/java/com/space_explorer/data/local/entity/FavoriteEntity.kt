package com.space_explorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity backing the `favorites` table.
 *
 * Note that mapping to/from [com.space_explorer.domain.model.Astronomy] is
 * delegated to [com.space_explorer.data.mapper.AstronomyMapper] to keep
 * persistence concerns separated from domain shape.
 *
 * [savedAt] is used to sort the favorites list newest-first; it is set by
 * the database layer at insert time and never read by the domain.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val date: String,
    val title: String,
    val explanation: String,
    val imageUrl: String,
    val hdImageUrl: String?,
    val mediaType: String,
    val copyright: String?,
    val savedAt: Long = System.currentTimeMillis()
)
