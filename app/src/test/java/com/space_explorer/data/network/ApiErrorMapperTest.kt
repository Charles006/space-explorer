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

/**
 * Tests that the data-layer error mapper translates every supported failure
 * mode into the right domain error subtype. This is the only place where
 * Retrofit/OkHttp types are allowed to leak — everything else in the
 * application consumes [AstronomyError].
 */
class ApiErrorMapperTest {

    @Test
    fun `IOException maps to Network`() {
        val result = ApiErrorMapper.runCatching<String> { throw IOException("offline") }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.Network::class.java)
    }

    @Test
    fun `401 maps to Unauthorized`() {
        val result = ApiErrorMapper.runCatching<String> { throw httpException(401) }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.Unauthorized::class.java)
    }

    @Test
    fun `403 also maps to Unauthorized`() {
        val result = ApiErrorMapper.runCatching<String> { throw httpException(403) }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.Unauthorized::class.java)
    }

    @Test
    fun `429 maps to RateLimited`() {
        val result = ApiErrorMapper.runCatching<String> { throw httpException(429) }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.RateLimited::class.java)
    }

    @Test
    fun `500 maps to ServerUnavailable`() {
        val result = ApiErrorMapper.runCatching<String> { throw httpException(500) }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.ServerUnavailable::class.java)
    }

    @Test
    fun `418 falls through to HttpError carrying code`() {
        val result = ApiErrorMapper.runCatching<String> { throw httpException(418) }
        val err = result.exceptionOrNull()
        assertThat(err).isInstanceOf(AstronomyError.HttpError::class.java)
        assertThat((err as AstronomyError.HttpError).code).isEqualTo(418)
    }

    @Test
    fun `arbitrary exception maps to Unknown`() {
        val result = ApiErrorMapper.runCatching<String> { throw IllegalStateException("weird") }
        assertThat(result.exceptionOrNull()).isInstanceOf(AstronomyError.Unknown::class.java)
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException is propagated, not swallowed`() {
        ApiErrorMapper.runCatching<String> { throw CancellationException("cancelled") }
    }

    @Test
    fun `successful block returns Result success`() {
        val result = ApiErrorMapper.runCatching { 42 }
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo(42)
    }

    private fun httpException(code: Int): HttpException = HttpException(
        Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaTypeOrNull()))
    )
}
