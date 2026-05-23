package com.space_explorer.di

import com.space_explorer.data.repository.AstronomyRepositoryImpl
import com.space_explorer.domain.repository.AstronomyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAstronomyRepository(
        impl: AstronomyRepositoryImpl
    ): AstronomyRepository
}
