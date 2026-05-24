package com.space_explorer.data.network

import com.space_explorer.domain.error.AstronomyError
import retrofit2.HttpException
import java.io.IOException

object ApiErrorMapper {

    inline fun <T> runCatching(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: IOException) {
        Result.failure(AstronomyError.Network(e))
    } catch (e: HttpException) {
        Result.failure(toDomainError(e))
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Result.failure(AstronomyError.Unknown(e))
    }

    fun toDomainError(e: HttpException): AstronomyError = when (val code = e.code()) {
        401, 403 -> AstronomyError.Unauthorized(e)
        429 -> AstronomyError.RateLimited(e)
        in 500..599 -> AstronomyError.ServerUnavailable(e)
        else -> AstronomyError.HttpError(code, e.message(), e)
    }
}
