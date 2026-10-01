package com.streamify.app

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    // ---------------------------------------------------------
    // TRENDING
    // ---------------------------------------------------------

    @GET("trending/all/day")
    suspend fun getTrending(
        @Query("api_key") apiKey: String
    ): TmdbResponse


    // ---------------------------------------------------------
    // SEARCH
    // ---------------------------------------------------------

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbResponse


    // ---------------------------------------------------------
    // MOVIES
    // ---------------------------------------------------------

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genres: String? = null
    ): TmdbResponse


    // ---------------------------------------------------------
    // TV
    // ---------------------------------------------------------

    @GET("discover/tv")
    suspend fun discoverTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genres: String? = null
    ): TmdbResponse


    // ---------------------------------------------------------
    // DRAMA
    // TMDB Drama genre = 18
    // ---------------------------------------------------------

    @GET("discover/movie")
    suspend fun discoverDramaMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genres: String = "18"
    ): TmdbResponse


    @GET("discover/tv")
    suspend fun discoverDramaTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genres: String = "18"
    ): TmdbResponse


    // ---------------------------------------------------------
    // ANIMATION
    // TMDB Animation genre = 16
    // ---------------------------------------------------------

    @GET("discover/movie")
    suspend fun discoverAnimationMovies(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genres: String = "16"
    ): TmdbResponse


    @GET("discover/tv")
    suspend fun discoverAnimationTv(
        @Query("api_key") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("with_genres") genres: String = "16"
    ): TmdbResponse


    // ---------------------------------------------------------
    // DETAILS
    // ---------------------------------------------------------

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


    // ---------------------------------------------------------
    // SIMILAR
    // ---------------------------------------------------------

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
}


// ---------------------------------------------------------
// TMDB RESPONSE
// ---------------------------------------------------------

data class TmdbResponse(
    val page: Int = 1,
    val results: List<TmdbItem> = emptyList(),
    val total_pages: Int = 1,
    val total_results: Int = 0
)


// ---------------------------------------------------------
// TMDB ITEM
// ---------------------------------------------------------

data class TmdbItem(
    val id: Int = 0,

    val title: String? = null,

    val name: String? = null,

    val poster_path: String? = null,

    val backdrop_path: String? = null,

    val overview: String? = null,

    val vote_average: Double? = null,

    val media_type: String? = null,

    val release_date: String? = null,

    val first_air_date: String? = null,

    val genres: List<TmdbGenre>? = null,

    val runtime: Int? = null,

    val number_of_seasons: Int? = null
)


// ---------------------------------------------------------
// TMDB GENRE
// ---------------------------------------------------------

data class TmdbGenre(
    val id: Int = 0,
    val name: String = ""
)
