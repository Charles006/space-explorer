package com.space_explorer.data.api

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class NasaApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: NasaApiService

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiKeyInterceptor("TEST_KEY"))
            .build()
        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NasaApiService::class.java)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getApodByDate parses response successfully`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setBody(SINGLE_APOD_JSON).setResponseCode(200),
        )

        val response = apiService.getApodByDate("2026-05-22")

        assertThat(response.title).isEqualTo("Mars Sunrise")
        assertThat(response.date).isEqualTo("2026-05-22")
        assertThat(response.mediaType).isEqualTo("image")
    }

    @Test
    fun `getApodRange parses array response`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setBody(RANGE_APOD_JSON).setResponseCode(200),
        )

        val list = apiService.getApodRange("2026-05-20", "2026-05-22")

        assertThat(list).hasSize(2)
        assertThat(list[0].date).isEqualTo("2026-05-22")
    }

    @Test
    fun `getApodByDate appends api_key query parameter`() = runTest {
        mockWebServer.enqueue(
            MockResponse().setBody(SINGLE_APOD_JSON).setResponseCode(200),
        )

        apiService.getApodByDate("2026-05-22")

        val request = mockWebServer.takeRequest()
        assertThat(request.requestUrl.toString()).contains("api_key=TEST_KEY")
        assertThat(request.requestUrl.toString()).contains("date=2026-05-22")
    }

    @Test(expected = HttpException::class)
    fun `getApodByDate raises HttpException on 401`(): Unit = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("{\"error\":\"unauthorized\"}"))
        apiService.getApodByDate("2026-05-22")
    }

    companion object {
        private const val SINGLE_APOD_JSON = """
        {
          "date": "2026-05-22",
          "title": "Mars Sunrise",
          "explanation": "A sunrise on Mars.",
          "url": "https://apod.nasa.gov/mars.jpg",
          "hdurl": "https://apod.nasa.gov/mars-hd.jpg",
          "media_type": "image",
          "copyright": "NASA"
        }
        """

        private const val RANGE_APOD_JSON = """
        [
          {
            "date": "2026-05-22",
            "title": "Mars Sunrise",
            "explanation": "A sunrise on Mars.",
            "url": "https://apod.nasa.gov/mars.jpg",
            "media_type": "image"
          },
          {
            "date": "2026-05-21",
            "title": "Lunar Eclipse",
            "explanation": "A lunar eclipse.",
            "url": "https://apod.nasa.gov/eclipse.jpg",
            "media_type": "image"
          }
        ]
        """
    }
}
