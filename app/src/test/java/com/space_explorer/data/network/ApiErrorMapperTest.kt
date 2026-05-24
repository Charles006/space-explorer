package com.space_explorer.data.network

import com.google.common.truth.Truth.assertThat
import com.space_explorer.domain.error.AstronomyError
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorMapperTest {

    @Test
    fun ioException_mapsToNetwork() {
        val result = ApiErrorMapper.runCatching<String> { throw IOException("offline") }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.Network::class.java)
    }

    @Test
    fun http401_mapsToUnauthorized() {
        val result = ApiErrorMapper.runCatching<String> { throw httpException(401) }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.Unauthorized::class.java)
    }

    @Test
    fun http403_mapsToUnauthorized() {
        val result = ApiErrorMapper.runCatching<String> { throw httpException(403) }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.Unauthorized::class.java)
    }

    @Test
    fun http429_mapsToRateLimited() {
        val result = ApiErrorMapper.runCatching<String> { throw httpException(429) }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.RateLimited::class.java)
    }

    @Test
    fun http500_mapsToServerUnavailable() {
        val result = ApiErrorMapper.runCatching<String> { throw httpException(500) }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.ServerUnavailable::class.java)
    }

    @Test
    fun http418_mapsToGenericHttpError() {
        val result = ApiErrorMapper.runCatching<String> { throw httpException(418) }
        val err = result.exceptionOrNull()
        assertThat(err).isInstanceOf(AstronomyError.HttpError::class.java)
        assertThat((err as AstronomyError.HttpError).code).isEqualTo(418)
    }

    @Test
    fun otherException_mapsToUnknown() {
        val result = ApiErrorMapper.runCatching<String> { throw IllegalStateException("weird") }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.Unknown::class.java)
    }

    @Test(expected = CancellationException::class)
    fun cancellation_propagates() {
        ApiErrorMapper.runCatching<String> { throw CancellationException("cancelled") }
    }

    @Test
    fun successPath_returnsResultSuccess() {
        val result = ApiErrorMapper.runCatching { 42 }
        assertThat(result.getOrThrow()).isEqualTo(42)
    }

    private fun httpException(code: Int): HttpException = HttpException(
        Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaTypeOrNull())),
    )
}
