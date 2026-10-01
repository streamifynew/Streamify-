package com.streamify.app

class TmdbRepository {

    private val api = TmdbClient.api

    private val apiKey: String
        get() = BuildConfig.TMDB_API_KEY

    suspend fun getTrending(): List<TmdbItem> {
        return api.getTrending(
            apiKey = apiKey
        ).results
    }

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

    suspend fun getMovieDetails(
        movieId: Int
    ): TmdbItem {
        return api.getMovieDetails(
            movieId = movieId,
            apiKey = apiKey
        )
    }

    suspend fun getTvDetails(
        tvId: Int
    ): TmdbItem {
        return api.getTvDetails(
            tvId = tvId,
            apiKey = apiKey
        )
    }

    suspend fun getSimilar(
        item: TmdbItem
    ): List<TmdbItem> {

        return if (item.media_type == "tv") {

            api.getSimilarTv(
                tvId = item.id,
                apiKey = apiKey
            ).results

        } else {

            api.getSimilarMovies(
                movieId = item.id,
                apiKey = apiKey
            ).results
        }
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
}
