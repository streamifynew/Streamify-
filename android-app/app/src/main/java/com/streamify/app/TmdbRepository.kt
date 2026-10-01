package com.streamify.app

class TmdbRepository {

    private val api = TmdbClient.api

    private val apiKey: String
        get() = BuildConfig.TMDB_API_KEY

    // ---------------------------------------------------------
    // TRENDING
    // ---------------------------------------------------------

    suspend fun getTrending(): List<TmdbItem> {
        return api.getTrending(
            apiKey = apiKey
        ).results
    }


    // ---------------------------------------------------------
    // MOVIES
    // ---------------------------------------------------------

    suspend fun getMovies(): List<TmdbItem> {
        return api.discoverMovies(
            apiKey = apiKey
        ).results.map {
            it.copy(media_type = "movie")
        }
    }


    // ---------------------------------------------------------
    // TV
    // ---------------------------------------------------------

    suspend fun getTvShows(): List<TmdbItem> {
        return api.discoverTv(
            apiKey = apiKey
        ).results.map {
            it.copy(media_type = "tv")
        }
    }


    // ---------------------------------------------------------
    // DRAMA
    // ---------------------------------------------------------

    suspend fun getDrama(): List<TmdbItem> {

        val movies = api.discoverDramaMovies(
            apiKey = apiKey
        ).results.map {
            it.copy(media_type = "movie")
        }

        val tv = api.discoverDramaTv(
            apiKey = apiKey
        ).results.map {
            it.copy(media_type = "tv")
        }

        return (movies + tv)
            .distinctBy { "${it.media_type}-${it.id}" }
            .sortedByDescending { it.vote_average ?: 0.0 }
    }


    // ---------------------------------------------------------
    // ANIMATION / ANIME BASE
    // ---------------------------------------------------------

    suspend fun getAnime(): List<TmdbItem> {

        val movies = api.discoverAnimationMovies(
            apiKey = apiKey
        ).results.map {
            it.copy(media_type = "movie")
        }

        val tv = api.discoverAnimationTv(
            apiKey = apiKey
        ).results.map {
            it.copy(media_type = "tv")
        }

        return (movies + tv)
            .distinctBy { "${it.media_type}-${it.id}" }
            .sortedByDescending { it.vote_average ?: 0.0 }
    }


    // ---------------------------------------------------------
    // SEARCH
    // ---------------------------------------------------------

    suspend fun search(
        query: String
    ): List<TmdbItem> {

        if (query.isBlank()) {
            return emptyList()
        }

        return api.searchMulti(
            apiKey = apiKey,
            query = query.trim()
        ).results.filter {
            it.media_type == "movie" ||
                    it.media_type == "tv"
        }
    }


    // ---------------------------------------------------------
    // DETAILS
    // ---------------------------------------------------------

    suspend fun getMovieDetails(
        movieId: Int
    ): TmdbItem {

        return api.getMovieDetails(
            movieId = movieId,
            apiKey = apiKey
        ).copy(
            media_type = "movie"
        )
    }


    suspend fun getTvDetails(
        tvId: Int
    ): TmdbItem {

        return api.getTvDetails(
            tvId = tvId,
            apiKey = apiKey
        ).copy(
            media_type = "tv"
        )
    }


    suspend fun getDetails(
        item: TmdbItem
    ): TmdbItem {

        return if (item.media_type == "tv") {

            getTvDetails(item.id)

        } else {

            getMovieDetails(item.id)
        }
    }


    // ---------------------------------------------------------
    // SIMILAR
    // ---------------------------------------------------------

    suspend fun getSimilar(
        item: TmdbItem
    ): List<TmdbItem> {

        return if (item.media_type == "tv") {

            api.getSimilarTv(
                tvId = item.id,
                apiKey = apiKey
            ).results.map {
                it.copy(media_type = "tv")
            }

        } else {

            api.getSimilarMovies(
                movieId = item.id,
                apiKey = apiKey
            ).results.map {
                it.copy(media_type = "movie")
            }
        }
    }
}
