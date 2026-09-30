package com.streamify.app

import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {

    @GET("trending/all/day")
    suspend fun getTrending(
        @Query("api_key") apiKey: String
    ): TmdbResponse
}

data class TmdbResponse(
    val page: Int,
    val results: List<TmdbItem>
)

data class TmdbItem(
    val id: Int,
    val title: String?,
    val name: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val overview: String?,
    val vote_average: Double?,
    val media_type: String?
)
