package com.space_explorer.data.api

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ApiKeyInterceptor @Inject constructor(
    private val apiKey: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.newBuilder()
            .addQueryParameter("api_key", apiKey)
            .build()
        val request = originalRequest.newBuilder().url(url).build()
        return chain.proceed(request)
    }
}
