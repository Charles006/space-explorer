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

    @Test
    fun getAstronomyRange_returnsItemsSortedNewestFirst() = runTest {
        val responses = listOf(
            apodResponse("2026-05-20", "Older"),
            apodResponse("2026-05-22", "Newest"),
            apodResponse("2026-05-21", "Middle"),
        )
        whenever(apiService.getApodRange(any(), any(), any())).thenReturn(responses)
        whenever(favoriteDao.exists(any())).thenReturn(false)

        val items = repository.getAstronomyRange("2026-05-20", "2026-05-22").getOrThrow()

        assertThat(items).hasSize(3)
        assertThat(items.first().title).isEqualTo("Newest")
        assertThat(items.last().title).isEqualTo("Older")
    }

    @Test
    fun getAstronomyRange_marksFavoritesFromDao() = runTest {
        whenever(apiService.getApodRange(any(), any(), any()))
            .thenReturn(listOf(apodResponse("2026-05-22", "Mars")))
        whenever(favoriteDao.exists("2026-05-22")).thenReturn(true)

        val items = repository.getAstronomyRange("2026-05-22", "2026-05-22").getOrThrow()

        assertThat(items.first().isFavorite).isTrue()
    }

    @Test
    fun getAstronomyRange_partialBadData_returnsFullBatchWithBlankUrlForBrokenItem() = runTest {
        val responses = listOf(
            apodResponse("2026-05-22", "Valid"),
            apodResponseWithEmptyUrl("2026-05-21", "Broken"),
            apodResponse("2026-05-20", "AlsoValid"),
        )
        whenever(apiService.getApodRange(any(), any(), any())).thenReturn(responses)
        whenever(favoriteDao.exists(any())).thenReturn(false)

        val items = repository.getAstronomyRange("2026-05-20", "2026-05-22").getOrThrow()

        assertThat(items.map { it.title }).containsExactly("Valid", "Broken", "AlsoValid")
        assertThat(items.single { it.title == "Broken" }.imageUrl).isEmpty()
    }

    @Test
    fun getAstronomyRange_ioException_returnsNetworkError() = runTest {
        whenever(apiService.getApodRange(any(), any(), any())).doSuspendableAnswer {
            throw IOException("offline")
        }

        val result = repository.getAstronomyRange("2026-05-20", "2026-05-22")

        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.Network::class.java)
    }

    @Test
    fun getAstronomyRange_http429_returnsRateLimited() = runTest {
        whenever(apiService.getApodRange(any(), any(), any())).doSuspendableAnswer {
            throw httpException(429)
        }

        val result = repository.getAstronomyRange("2026-05-20", "2026-05-22")

        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.RateLimited::class.java)
    }

    @Test
    fun getAstronomyRange_http401_returnsUnauthorized() = runTest {
        whenever(apiService.getApodRange(any(), any(), any())).doSuspendableAnswer {
            throw httpException(401)
        }

        val ex = repository.getAstronomyRange("2026-05-20", "2026-05-22").exceptionOrNull()

        assertThat(ex).isInstanceOf(AstronomyError.Unauthorized::class.java)
    }

    @Test
    fun getAstronomyRange_http503_returnsServerUnavailable() = runTest {
        whenever(apiService.getApodRange(any(), any(), any())).doSuspendableAnswer {
            throw httpException(503)
        }

        val ex = repository.getAstronomyRange("2026-05-20", "2026-05-22").exceptionOrNull()

        assertThat(ex).isInstanceOf(AstronomyError.ServerUnavailable::class.java)
    }

    @Test
    fun addFavorite_callsDao() = runTest {
        repository.addFavorite(sampleAstronomy())
        verify(favoriteDao).insert(any())
    }

    @Test
    fun removeFavorite_callsDao() = runTest {
        repository.removeFavorite("2026-05-22")
        verify(favoriteDao).deleteById("2026-05-22")
    }

    @Test
    fun toggleFavorite_whenAlreadyFavorite_removes() = runTest {
        whenever(favoriteDao.exists("2026-05-22")).thenReturn(true)

        val nowFavorite = repository.toggleFavorite(sampleAstronomy())

        assertThat(nowFavorite).isFalse()
        verify(favoriteDao).deleteById("2026-05-22")
    }

    @Test
    fun toggleFavorite_whenNotFavorite_inserts() = runTest {
        whenever(favoriteDao.exists("2026-05-22")).thenReturn(false)

        val nowFavorite = repository.toggleFavorite(sampleAstronomy())

        assertThat(nowFavorite).isTrue()
        verify(favoriteDao).insert(any())
    }

    @Test
    fun getFavorites_mapsEntitiesToDomain() = runTest {
        val entity = FavoriteEntity(
            id = "2026-05-22",
            date = "2026-05-22",
            title = "Mars",
            explanation = "Red planet",
            imageUrl = "https://image/x.jpg",
            hdImageUrl = null,
            videoUrl = null,
            mediaType = "image",
            copyright = null,
        )
        whenever(favoriteDao.observeAll()).thenReturn(flowOf(listOf(entity)))

        val collected = mutableListOf<List<Astronomy>>()
        repository.getFavorites().collect { collected.add(it) }

        val first = collected.first().first()
        assertThat(first.isFavorite).isTrue()
        assertThat(first.title).isEqualTo("Mars")
    }

    @Test
    fun getAstronomyByDate_marksFavoriteWhenDaoSaysSo() = runTest {
        whenever(apiService.getApodByDate(any(), any())).thenReturn(apodResponse("2026-05-22", "Mars"))
        whenever(favoriteDao.exists("2026-05-22")).thenReturn(true)

        val result = repository.getAstronomyByDate("2026-05-22").getOrThrow()

        assertThat(result.isFavorite).isTrue()
    }

    private fun apodResponse(date: String, title: String) = ApodResponse(
        date = date,
        title = title,
        explanation = "explanation $title",
        url = "https://image/$date.jpg",
        hdUrl = "https://image/$date.hd.jpg",
        mediaType = "image",
        copyright = null,
    )

    private fun apodResponseWithEmptyUrl(date: String, title: String) = ApodResponse(
        date = date,
        title = title,
        explanation = "explanation $title",
        url = "",
        hdUrl = null,
        mediaType = "image",
        copyright = null,
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
        isFavorite = false,
    )

    private fun httpException(code: Int): HttpException = HttpException(
        Response.error<Any>(
            code,
            "{}".toResponseBody("application/json".toMediaTypeOrNull()),
        ),
    )
}
