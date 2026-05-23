package com.space_explorer.domain.repository

import com.space_explorer.domain.model.Astronomy
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for NASA Astronomy Picture of the Day content.
 *
 * Implementations are expected to:
 *   * Translate transport-level failures into [com.space_explorer.domain.error.AstronomyError]
 *     before returning them inside [Result.failure].
 *   * Coalesce remote data with locally persisted favorites so [Astronomy.isFavorite]
 *     is always accurate at the point of emission.
 *   * Be safe to call from any dispatcher; suspend functions perform their I/O
 *     internally on the appropriate thread.
 */
interface AstronomyRepository {

    /**
     * Fetch the APODs published between [startDate] and [endDate] (inclusive).
     * Dates must be in ISO-8601 (`yyyy-MM-dd`); validation is the caller's
     * responsibility — pass them through `DateUtils.isValidIsoDate` first.
     *
     * The returned list is sorted from newest to oldest.
     */
    suspend fun getAstronomyRange(startDate: String, endDate: String): Result<List<Astronomy>>

    /** Fetch a single APOD for the given ISO date. */
    suspend fun getAstronomyByDate(date: String): Result<Astronomy>

    /** Cold flow that emits the current favorites list whenever it changes. */
    fun getFavorites(): Flow<List<Astronomy>>

    /** Persist the given [astronomy] as a favorite. Idempotent (REPLACE on conflict). */
    suspend fun addFavorite(astronomy: Astronomy)

    /** Remove the favorite identified by [astronomyId]. No-op if it does not exist. */
    suspend fun removeFavorite(astronomyId: String)

    /**
     * Atomically toggle the favorite state of [astronomy]. Protected by a mutex
     * inside the implementation to avoid lost-update races when the UI emits
     * two rapid clicks.
     *
     * @return `true` if the item is now a favorite, `false` otherwise.
     */
    suspend fun toggleFavorite(astronomy: Astronomy): Boolean

    /** Stream of favorite ids — useful to keep UI lists reactive without re-fetching. */
    fun observeFavoriteIds(): Flow<Set<String>>
}
