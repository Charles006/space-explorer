package com.space_explorer.di

import android.content.Context
import androidx.room.Room
import com.space_explorer.data.local.dao.FavoriteDao
import com.space_explorer.data.local.database.SpaceExplorerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpaceExplorerDatabase =
        Room.databaseBuilder(
            context,
            SpaceExplorerDatabase::class.java,
            SpaceExplorerDatabase.DATABASE_NAME
        )
            // Explicit migration so existing users keep their favorites when
            // we bump the schema to v2 (videoUrl column). Without this Room
            // would crash on first launch after the update.
            .addMigrations(SpaceExplorerDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: SpaceExplorerDatabase): FavoriteDao =
        database.favoriteDao()
}
