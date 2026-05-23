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

/**
 * Default [AstronomyRepository] implementation.
 *
 * Responsibilities:
 *   * Talks to [NasaApiService] for remote APOD data.
 *   * Talks to [FavoriteDao] for the local favorites table.
 *   * Merges both into the domain [Astronomy] so the UI never has to know
 *     where data comes from.
 *
 * Concurrency notes:
 *   * Favorite mutations (add/remove/toggle) are guarded by [favoriteMutex]
 *     to prevent lost-update races when the UI fires two rapid taps.
 *   * Read paths are lock-free; Room's [Flow] emissions handle consistency.
 */
@Singleton
class AstronomyRepositoryImpl @Inject constructor(
    private val apiService: NasaApiService,
    private val favoriteDao: FavoriteDao
) : AstronomyRepository {

    /** Serializes mutating favorite operations to avoid lost-update races. */
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
        favoriteDao.observeAll().map { entities ->
            entities.map(AstronomyMapper::fromEntity)
        }

    override suspend fun addFavorite(astronomy: Astronomy): Unit = favoriteMutex.withLock {
        favoriteDao.insert(AstronomyMapper.toEntity(astronomy))
    }

    override suspend fun removeFavorite(astronomyId: String): Unit = favoriteMutex.withLock {
        favoriteDao.deleteById(astronomyId)
    }

    override suspend fun toggleFavorite(astronomy: Astronomy): Boolean = favoriteMutex.withLock {
        val wasFavorite = favoriteDao.exists(astronomy.id)
        if (wasFavorite) {
            favoriteDao.deleteById(astronomy.id)
            false
        } else {
            favoriteDao.insert(AstronomyMapper.toEntity(astronomy.copy(isFavorite = true)))
            true
        }
    }

    override fun observeFavoriteIds(): Flow<Set<String>> =
        favoriteDao.observeFavoriteIds().map { it.toSet() }

    /**
     * Single round-trip to check which of the given [dates] are already
     * favorites. Avoids the N+1 pattern of calling `exists()` per item.
     */
    private suspend fun collectFavoriteIds(dates: List<String>): Set<String> {
        if (dates.isEmpty()) return emptySet()
        // The favorites table is small (user-curated) so a single observe-style
        // query would also work, but exists() is cheap and avoids loading rows.
        return dates.filter { favoriteDao.exists(it) }.toSet()
    }
}
