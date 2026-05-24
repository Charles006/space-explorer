package com.space_explorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val savedAt: Long = System.currentTimeMillis(),
)
