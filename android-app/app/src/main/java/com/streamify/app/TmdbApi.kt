package com.streamify.app

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface TmdbApi {

    @GET("trending/all/day")
    suspend fun getTrending(
        @Query("api_key") apiKey: String
    ): TmdbResponse

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbResponse

    // ============================================================
    // MOVIES / TV DISCOVERY
    // ============================================================

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genres: String? = null,
        @Query("with_original_language") language: String? = null,
        @Query("primary_release_date.gte") releaseFrom: String? = null,
        @Query("primary_release_date.lte") releaseTo: String? = null,
        @Query("vote_count.gte") minVotes: Int? = null
    ): TmdbResponse

    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genres: String? = null,
        @Query("with_original_language") language: String? = null,
        @Query("with_networks") networks: String? = null,
        @Query("without_genres") withoutGenres: String? = null,
        @Query("first_air_date.gte") airFrom: String? = null,
        @Query("first_air_date.lte") airTo: String? = null,
        @Query("vote_count.gte") minVotes: Int? = null
    ): TmdbResponse

    // ============================================================
    // DETAILS
    // ============================================================

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbItem

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): TmdbItem

    // ============================================================
    // SIMILAR
    // ============================================================

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): TmdbResponse

    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarTv(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String
    ): TmdbResponse

    // ============================================================
    // STREAMIFY SCRAPER BACKEND
    // ============================================================
    @GET
    suspend fun getStreamingSources(
        @Url fullUrl: String,
        @Query("q") query: String
    ): ScraperResponse
}

data class TmdbResponse(
    val page: Int = 1,
    val results: List<TmdbItem> = emptyList(),
    val total_pages: Int = 1,
    val total_results: Int = 0
)

data class TmdbItem(
    val id: Int = 0,
    val title: String? = null,
    val name: String? = null,
    val poster_path: String? = null,
    val backdrop_path: String? = null,
    val overview: String? = null,
    val vote_average: Double? = null,
    val media_type: String? = null,

    val original_language: String? = null,

    val release_date: String? = null,
    val first_air_date: String? = null,

    val genres: List<TmdbGenre>? = null,

    val runtime: Int? = null,
    val number_of_seasons: Int? = null
)

data class TmdbGenre(
    val id: Int = 0,
    val name: String = ""
)

data class ScraperResponse(
    val query: String,
    val totalSources: Int,
    val resources: List<StreamingSource> = emptyList()
)

data class StreamingSource(
    val source: String,
    val title: String,
    val url: String,
    val quality: String,
    val size: String
)  
