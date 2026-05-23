package com.space_explorer.data.network

import com.space_explorer.domain.error.AstronomyError
import retrofit2.HttpException
import java.io.IOException

/**
 * Translates low-level network/HTTP failures into [AstronomyError] domain
 * exceptions. Keeping this mapping in a single place avoids duplicating the
 * status-code `when` block in every repository method and makes the contract
 * explicit: the data layer never leaks Retrofit/OkHttp types upward.
 */
object ApiErrorMapper {

    /**
     * Executes [block] and converts any thrown exception into a [Result]
     * failure carrying the appropriate [AstronomyError] subtype.
     */
    inline fun <T> runCatching(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: IOException) {
        Result.failure(AstronomyError.Network(e))
    } catch (e: HttpException) {
        Result.failure(toDomainError(e))
    } catch (e: Exception) {
        // Note: We intentionally do NOT catch CancellationException here —
        // structured concurrency requires cancellation to propagate.
        if (e is kotlinx.coroutines.CancellationException) throw e
        Result.failure(AstronomyError.Unknown(e))
    }

    /** Maps an [HttpException] to its closest [AstronomyError] subtype. */
    fun toDomainError(e: HttpException): AstronomyError = when (val code = e.code()) {
        401, 403 -> AstronomyError.Unauthorized(e)
        429 -> AstronomyError.RateLimited(e)
        in 500..599 -> AstronomyError.ServerUnavailable(e)
        else -> AstronomyError.HttpError(code = code, message = e.message(), cause = e)
    }
}
