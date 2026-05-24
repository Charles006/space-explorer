package com.space_explorer.domain.error

sealed class AstronomyError(
    val userMessage: String,
    cause: Throwable? = null
) : Exception(userMessage, cause) {

    class Network(cause: Throwable? = null) : AstronomyError(
        "Sin conexion a internet. Verifica tu red.",
        cause
    )

    class Unauthorized(cause: Throwable? = null) : AstronomyError(
        "API key invalida. Revisa tu NASA_API_KEY en local.properties.",
        cause
    )

    class RateLimited(cause: Throwable? = null) : AstronomyError(
        "Limite de peticiones alcanzado. Intenta de nuevo en unos minutos.",
        cause
    )

    class ServerUnavailable(cause: Throwable? = null) : AstronomyError(
        "El servidor de NASA no responde. Intenta luego.",
        cause
    )

    class InvalidDate(value: String) : AstronomyError(
        "Formato de fecha invalido. Usa YYYY-MM-DD (recibido: '$value')."
    )

    class HttpError(val code: Int, message: String, cause: Throwable? = null) : AstronomyError(
        "Error HTTP $code: $message",
        cause
    )

    class Unknown(cause: Throwable) : AstronomyError(
        cause.message ?: "Error desconocido",
        cause
    )
}
