package com.space_explorer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.space_explorer.data.local.dao.FavoriteDao
import com.space_explorer.data.local.entity.FavoriteEntity

@Database(
    entities = [FavoriteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SpaceExplorerDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "space_explorer.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorites ADD COLUMN videoUrl TEXT")
            }
        }
    }
}
