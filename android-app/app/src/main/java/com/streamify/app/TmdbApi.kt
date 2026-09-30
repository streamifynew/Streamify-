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
    val page: Int = 1,
    val results: List<TmdbItem> = emptyList()
)

data class TmdbItem(
    val id: Int = 0,
    val title: String? = null,
    val name: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val overview: String? = null,
    val vote_average: Double? = null,
    val media_type: String? = null
)
