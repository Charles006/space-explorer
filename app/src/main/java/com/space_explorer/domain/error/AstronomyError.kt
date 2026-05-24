package com.space_explorer.domain.error

import androidx.annotation.StringRes
import com.space_explorer.R

sealed class AstronomyError(
    @StringRes val messageRes: Int,
    val messageArgs: List<Any> = emptyList(),
    cause: Throwable? = null
) : Exception(cause) {

    class Network(cause: Throwable? = null) :
        AstronomyError(R.string.error_network, cause = cause)

    class Unauthorized(cause: Throwable? = null) :
        AstronomyError(R.string.error_unauthorized, cause = cause)

    class RateLimited(cause: Throwable? = null) :
        AstronomyError(R.string.error_rate_limited, cause = cause)

    class ServerUnavailable(cause: Throwable? = null) :
        AstronomyError(R.string.error_server_unavailable, cause = cause)

    class InvalidDate(value: String) :
        AstronomyError(R.string.error_invalid_date, listOf(value))

    class HttpError(val code: Int, httpMessage: String, cause: Throwable? = null) :
        AstronomyError(R.string.error_http, listOf(code, httpMessage), cause)

    class Unknown(cause: Throwable) :
        AstronomyError(R.string.error_unknown, cause = cause)
}
