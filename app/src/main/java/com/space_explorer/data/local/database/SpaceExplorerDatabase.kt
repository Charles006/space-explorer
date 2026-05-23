package com.space_explorer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.space_explorer.data.local.dao.FavoriteDao
import com.space_explorer.data.local.entity.FavoriteEntity

@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SpaceExplorerDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "space_explorer.db"
    }
}
