package com.space_explorer.data.api

import com.space_explorer.data.model.ApodResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NasaApiService {

    @GET("planetary/apod")
    suspend fun getApodByDate(
        @Query("date") date: String,
        @Query("thumbs") thumbs: Boolean = true
    ): ApodResponse

    @GET("planetary/apod")
    suspend fun getApodRange(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("thumbs") thumbs: Boolean = true
    ): List<ApodResponse>
}
