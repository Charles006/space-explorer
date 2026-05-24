package com.space_explorer.data.repository

import com.google.common.truth.Truth.assertThat
import com.space_explorer.data.api.NasaApiService
import com.space_explorer.data.local.dao.FavoriteDao
import com.space_explorer.data.local.entity.FavoriteEntity
import com.space_explorer.data.model.ApodResponse
import com.space_explorer.domain.error.AstronomyError
import com.space_explorer.domain.model.Astronomy
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AstronomyRepositoryImplTest {

    private lateinit var apiService: NasaApiService
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var repository: AstronomyRepositoryImpl

    @Before
    fun setUp() {
        apiService = mock()
        favoriteDao = mock()
        repository = AstronomyRepositoryImpl(apiService, favoriteDao)
    }

    // region ── getAstronomyRange ─────────────────────────────────────────

    @Test
    fun `getAstronomyRange returns success with newest-first sort`() = runTest {
        val responses = listOf(
            apodResponse("2026-05-20", "Older"),
            apodResponse("2026-05-22", "Newest"),
            apodResponse("2026-05-21", "Middle")
        )
        whenever(apiService.getApodRange(any(), any(), any())).thenReturn(responses)
        whenever(favoriteDao.exists(any())).thenReturn(false)

        val result = repository.getAstronomyRange("2026-05-20", "2026-05-22")

        assertThat(result.isSuccess).isTrue()
        val items = result.getOrThrow()
        assertThat(items).hasSize(3)
        assertThat(items.first().title).isEqualTo("Newest")
        assertThat(items.last().title).isEqualTo("Older")
    }

    // Regression: a single item with empty 'url' tumbled the whole 10-item page,
    // so pagination stalled at 10 items. See screenshot bug "Invalid media url
    // returned by API: ''". The batch must survive partial bad data.
    @Test
    fun `getAstronomyRange returns full batch even when one item has empty url`() = runTest {
        val responses = listOf(
            apodResponse("2026-05-22", "Valid"),
            apodResponseWithEmptyUrl("2026-05-21", "Broken"),
            apodResponse("2026-05-20", "AlsoValid")
        )
        whenever(apiService.getApodRange(any(), any(), any())).thenReturn(responses)
        whenever(favoriteDao.exists(any())).thenReturn(false)

        val result = repository.getAstronomyRange("2026-05-20", "2026-05-22")

        assertThat(result.isSuccess).isTrue()
        val items = result.getOrThrow()
        assertThat(items).hasSize(3)
        assertThat(items.map { it.title }).containsExactly("Valid", "Broken", "AlsoValid")
        // The broken item still surfaces metadata; its imageUrl is empty so Coil
        // renders the error placeholder.
        assertThat(items.single { it.title == "Broken" }.imageUrl).isEmpty()
    }

    @Test
    fun `getAstronomyRange marks favorites correctly`() = runTest {
        val responses = listOf(apodResponse("2026-05-22", "Mars"))
        whenever(apiService.getApodRange(any(), any(), any())).thenReturn(responses)
        whenever(favoriteDao.exists("2026-05-22")).thenReturn(true)

        val result = repository.getAstronomyRange("2026-05-22", "2026-05-22")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().first().isFavorite).isTrue()
    }

    @Test
    fun `getAstronomyRange maps IOException to AstronomyError_Network`() = runTest {
        whenever(apiService.getApodRange(any(), any(), any())).doSuspendableAnswer {
            throw IOException("offline")
        }

        val result = repository.getAstronomyRange("2026-05-20", "2026-05-22")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.Network::class.java)
    }

    @Test
    fun `getAstronomyRange maps 429 to AstronomyError_RateLimited`() = runTest {
        whenever(apiService.getApodRange(any(), any(), any())).doSuspendableAnswer {
            throw httpException(429)
        }

        val result = repository.getAstronomyRange("2026-05-20", "2026-05-22")

        val ex = result.exceptionOrNull()
        assertThat(ex).isInstanceOf(AstronomyError.RateLimited::class.java)
        assertThat((ex as AstronomyError).userMessage).contains("Limite")
    }

    @Test
    fun `getAstronomyRange maps 401 to AstronomyError_Unauthorized`() = runTest {
        whenever(apiService.getApodRange(any(), any(), any())).doSuspendableAnswer {
            throw httpException(401)
        }

        val ex = repository.getAstronomyRange("2026-05-20", "2026-05-22").exceptionOrNull()

        assertThat(ex).isInstanceOf(AstronomyError.Unauthorized::class.java)
    }

    @Test
    fun `getAstronomyRange maps 503 to AstronomyError_ServerUnavailable`() = runTest {
        whenever(apiService.getApodRange(any(), any(), any())).doSuspendableAnswer {
            throw httpException(503)
        }

        val ex = repository.getAstronomyRange("2026-05-20", "2026-05-22").exceptionOrNull()

        assertThat(ex).isInstanceOf(AstronomyError.ServerUnavailable::class.java)
    }

    // endregion

    // region ── Favorites ─────────────────────────────────────────────────

    @Test
    fun `addFavorite delegates to dao with mapped entity`() = runTest {
        repository.addFavorite(sampleAstronomy())
        verify(favoriteDao).insert(any())
    }

    @Test
    fun `removeFavorite delegates to dao`() = runTest {
        repository.removeFavorite("2026-05-22")
        verify(favoriteDao).deleteById("2026-05-22")
    }

    @Test
    fun `toggleFavorite removes when previously a favorite`() = runTest {
        whenever(favoriteDao.exists("2026-05-22")).thenReturn(true)

        val nowFavorite = repository.toggleFavorite(sampleAstronomy())

        assertThat(nowFavorite).isFalse()
        verify(favoriteDao).deleteById("2026-05-22")
    }

    @Test
    fun `toggleFavorite inserts when not previously a favorite`() = runTest {
        whenever(favoriteDao.exists("2026-05-22")).thenReturn(false)

        val nowFavorite = repository.toggleFavorite(sampleAstronomy())

        assertThat(nowFavorite).isTrue()
        verify(favoriteDao).insert(any())
    }

    @Test
    fun `getFavorites maps entities to domain`() = runTest {
        val entity = FavoriteEntity(
            id = "2026-05-22",
            date = "2026-05-22",
            title = "Mars",
            explanation = "Red planet",
            imageUrl = "https://image/x.jpg",
            hdImageUrl = null,
            videoUrl = null,
            mediaType = "image",
            copyright = null
        )
        whenever(favoriteDao.observeAll()).thenReturn(flowOf(listOf(entity)))

        val collected = mutableListOf<List<Astronomy>>()
        repository.getFavorites().collect { collected.add(it) }

        assertThat(collected).hasSize(1)
        val first = collected.first().first()
        assertThat(first.isFavorite).isTrue()
        assertThat(first.title).isEqualTo("Mars")
    }

    @Test
    fun `getAstronomyByDate marks isFavorite when dao exists returns true`() = runTest {
        whenever(apiService.getApodByDate(any(), any())).thenReturn(apodResponse("2026-05-22", "Mars"))
        whenever(favoriteDao.exists("2026-05-22")).thenReturn(true)

        val result = repository.getAstronomyByDate("2026-05-22")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().isFavorite).isTrue()
    }

    // endregion

    // region ── Helpers ───────────────────────────────────────────────────

    private fun apodResponse(date: String, title: String) = ApodResponse(
        date = date,
        title = title,
        explanation = "explanation $title",
        url = "https://image/$date.jpg",
        hdUrl = "https://image/$date.hd.jpg",
        mediaType = "image",
        copyright = null
    )

    /** Mimics the NASA edge case that triggered the pagination bug. */
    private fun apodResponseWithEmptyUrl(date: String, title: String) = ApodResponse(
        date = date,
        title = title,
        explanation = "explanation $title",
        url = "",
        hdUrl = null,
        mediaType = "image",
        copyright = null
    )

    private fun sampleAstronomy() = Astronomy(
        id = "2026-05-22",
        date = "2026-05-22",
        title = "Mars",
        explanation = "Red planet",
        imageUrl = "https://image/x.jpg",
        hdImageUrl = null,
        videoUrl = null,
        mediaType = "image",
        copyright = null,
        isFavorite = false
    )

    private fun httpException(code: Int): HttpException = HttpException(
        Response.error<Any>(
            code,
            "{}".toResponseBody("application/json".toMediaTypeOrNull())
        )
    )

    // endregion
}
