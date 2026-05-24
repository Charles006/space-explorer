package com.space_explorer.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.space_explorer.data.local.dao.FavoriteDao
import com.space_explorer.data.local.entity.FavoriteEntity

/**
 * Room database for the Space Explorer app.
 *
 * Schema history:
 *   * v1 — initial favorites table.
 *   * v2 — adds nullable `videoUrl` column to favorites.
 */
@Database(
    entities = [FavoriteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SpaceExplorerDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "space_explorer.db"

        /**
         * v1 -> v2: add nullable `videoUrl` column for video-type APODs.
         * Pre-existing favorites get `NULL`, which the domain interprets as
         * "image-type entry, no embed URL".
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorites ADD COLUMN videoUrl TEXT")
            }
        }
    }
}
