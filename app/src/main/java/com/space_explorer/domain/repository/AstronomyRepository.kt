package com.space_explorer.domain.repository

import com.space_explorer.domain.model.Astronomy
import kotlinx.coroutines.flow.Flow

interface AstronomyRepository {
    suspend fun getAstronomyRange(startDate: String, endDate: String): Result<List<Astronomy>>
    suspend fun getAstronomyByDate(date: String): Result<Astronomy>
    fun getFavorites(): Flow<List<Astronomy>>
    suspend fun addFavorite(astronomy: Astronomy)
    suspend fun removeFavorite(astronomyId: String)
    suspend fun toggleFavorite(astronomy: Astronomy): Boolean
    fun observeFavoriteIds(): Flow<Set<String>>
}
