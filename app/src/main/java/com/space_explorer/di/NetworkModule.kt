package com.space_explorer.di

import com.space_explorer.BuildConfig
import com.space_explorer.core.Constants
import com.space_explorer.data.api.ApiKeyInterceptor
import com.space_explorer.data.api.NasaApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module that builds the HTTP + JSON stack used to talk to NASA.
 *
 * Design choices:
 *   * Timeouts are pulled from [Constants] so they are visible and tunable
 *     without modifying DI code.
 *   * The logging interceptor's level depends on [BuildConfig.DEBUG]; we use
 *     `BASIC` (request-line + status) rather than `BODY` to avoid leaking
 *     potentially large image payload metadata into logcat.
 *   * `ApiKeyInterceptor` is in front of the logging interceptor so the key
 *     appears in debug logs — handy for diagnosing 401s.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @Named("nasaApiKey")
    fun provideApiKey(): String = BuildConfig.NASA_API_KEY

    @Provides
    @Singleton
    @Named("nasaBaseUrl")
    fun provideBaseUrl(): String = BuildConfig.NASA_BASE_URL

    @Provides
    @Singleton
    fun provideApiKeyInterceptor(@Named("nasaApiKey") apiKey: String): ApiKeyInterceptor =
        ApiKeyInterceptor(apiKey)

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        apiKeyInterceptor: ApiKeyInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(apiKeyInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(Constants.HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        @Named("nasaBaseUrl") baseUrl: String
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideNasaApiService(retrofit: Retrofit): NasaApiService =
        retrofit.create(NasaApiService::class.java)
}
