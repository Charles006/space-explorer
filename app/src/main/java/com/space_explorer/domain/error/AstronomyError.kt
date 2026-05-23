package com.space_explorer.domain.error

/**
 * Typed errors surfaced by the domain layer.
 *
 * Using a sealed hierarchy lets the UI react to specific error categories
 * (e.g. retry on [Network], block input on [InvalidDate]) instead of pattern
 * matching on free-form strings.
 *
 * Each subtype carries a user-facing [userMessage] localized in Spanish so the
 * UI does not need a separate translation table. The underlying [cause] is
 * preserved for logging and crash reporting.
 */
sealed class AstronomyError(
    val userMessage: String,
    cause: Throwable? = null
) : Exception(userMessage, cause) {

    /** Device is offline or unable to reach NASA. */
    class Network(cause: Throwable? = null) : AstronomyError(
        userMessage = "Sin conexion a internet. Verifica tu red.",
        cause = cause
    )

    /** API key rejected by NASA (HTTP 401 / 403). */
    class Unauthorized(cause: Throwable? = null) : AstronomyError(
        userMessage = "API key invalida. Revisa tu NASA_API_KEY en local.properties.",
        cause = cause
    )

    /** Hourly rate-limit hit (HTTP 429). */
    class RateLimited(cause: Throwable? = null) : AstronomyError(
        userMessage = "Limite de peticiones alcanzado. Intenta de nuevo en unos minutos.",
        cause = cause
    )

    /** NASA backend returned 5xx. */
    class ServerUnavailable(cause: Throwable? = null) : AstronomyError(
        userMessage = "El servidor de NASA no responde. Intenta luego.",
        cause = cause
    )

    /** Date provided does not match yyyy-MM-dd. */
    class InvalidDate(value: String) : AstronomyError(
        userMessage = "Formato de fecha invalido. Usa YYYY-MM-DD (recibido: '$value')."
    )

    /** Catch-all for unexpected HTTP responses with the original status code. */
    class HttpError(val code: Int, message: String, cause: Throwable? = null) : AstronomyError(
        userMessage = "Error HTTP $code: $message",
        cause = cause
    )

    /** Fallback for any unclassified failure. */
    class Unknown(cause: Throwable) : AstronomyError(
        userMessage = cause.message ?: "Error desconocido",
        cause = cause
    )
}
