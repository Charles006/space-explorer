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
            .addMigrations(SpaceExplorerDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: SpaceExplorerDatabase): FavoriteDao =
        database.favoriteDao()
}
