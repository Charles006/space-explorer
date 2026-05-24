package com.space_explorer.data.repository

import com.space_explorer.data.api.NasaApiService
import com.space_explorer.data.local.dao.FavoriteDao
import com.space_explorer.data.mapper.AstronomyMapper
import com.space_explorer.data.network.ApiErrorMapper
import com.space_explorer.domain.model.Astronomy
import com.space_explorer.domain.repository.AstronomyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AstronomyRepositoryImpl @Inject constructor(
    private val apiService: NasaApiService,
    private val favoriteDao: FavoriteDao
) : AstronomyRepository {

    private val favoriteMutex = Mutex()

    override suspend fun getAstronomyRange(
        startDate: String,
        endDate: String
    ): Result<List<Astronomy>> = ApiErrorMapper.runCatching {
        val responses = apiService.getApodRange(startDate, endDate)
        val favoriteIds = collectFavoriteIds(responses.map { it.date })
        responses
            .map { AstronomyMapper.fromApi(it, isFavorite = it.date in favoriteIds) }
            .sortedByDescending { it.date }
    }

    override suspend fun getAstronomyByDate(date: String): Result<Astronomy> =
        ApiErrorMapper.runCatching {
            val response = apiService.getApodByDate(date)
            val isFavorite = favoriteDao.exists(response.date)
            AstronomyMapper.fromApi(response, isFavorite = isFavorite)
        }

    override fun getFavorites(): Flow<List<Astronomy>> =
        favoriteDao.observeAll().map { it.map(AstronomyMapper::fromEntity) }

    override suspend fun addFavorite(astronomy: Astronomy) = favoriteMutex.withLock {
        favoriteDao.insert(AstronomyMapper.toEntity(astronomy))
    }

    override suspend fun removeFavorite(astronomyId: String) = favoriteMutex.withLock {
        favoriteDao.deleteById(astronomyId)
    }

    override suspend fun toggleFavorite(astronomy: Astronomy): Boolean = favoriteMutex.withLock {
        if (favoriteDao.exists(astronomy.id)) {
            favoriteDao.deleteById(astronomy.id)
            false
        } else {
            favoriteDao.insert(AstronomyMapper.toEntity(astronomy.copy(isFavorite = true)))
            true
        }
    }

    override fun observeFavoriteIds(): Flow<Set<String>> =
        favoriteDao.observeFavoriteIds().map { it.toSet() }

    private suspend fun collectFavoriteIds(dates: List<String>): Set<String> {
        if (dates.isEmpty()) return emptySet()
        return dates.filter { favoriteDao.exists(it) }.toSet()
    }
}
