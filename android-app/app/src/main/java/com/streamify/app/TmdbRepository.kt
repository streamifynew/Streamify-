package com.streamify.app

class TmdbRepository {

    private val api = TmdbClient.api

    private val apiKey: String
        get() = BuildConfig.TMDB_API_KEY

    // ============================================================
    // TRENDING
    // ============================================================

    suspend fun getTrending(): List<TmdbItem> {
        return api.getTrending(
            apiKey = apiKey
        ).results
            .filter {
                it.media_type == "movie" ||
                        it.media_type == "tv"
            }
    }

    // ============================================================
    // MOVIES
    // ============================================================

    suspend fun getMovies(): List<TmdbItem> {

        return api.discoverMovies(
            apiKey = apiKey
        ).results.map {
            it.copy(media_type = "movie")
        }
    }

    // Hollywood
    suspend fun getHollywoodMovies(): List<TmdbItem> {

        return api.discoverMovies(
            apiKey = apiKey,
            language = "en"
        ).results.map {
            it.copy(media_type = "movie")
        }
    }

    // Bollywood Cinema
    suspend fun getBollywoodMovies(): List<TmdbItem> {

        return api.discoverMovies(
            apiKey = apiKey,
            language = "hi"
        ).results.map {
            it.copy(media_type = "movie")
        }
    }

    // South Cinema
    suspend fun getSouthMovies(): List<TmdbItem> {

        val tamil = api.discoverMovies(
            apiKey = apiKey,
            language = "ta"
        ).results

        val telugu = api.discoverMovies(
            apiKey = apiKey,
            language = "te"
        ).results

        val malayalam = api.discoverMovies(
            apiKey = apiKey,
            language = "ml"
        ).results

        val kannada = api.discoverMovies(
            apiKey = apiKey,
            language = "kn"
        ).results

        return (
            tamil +
                    telugu +
                    malayalam +
                    kannada
            )
            .distinctBy { it.id }
            .map {
                it.copy(media_type = "movie")
            }
            .sortedByDescending {
                it.popularityScore()
            }
    }

    // ============================================================
    // TV
    // ============================================================

    suspend fun getTvShows(): List<TmdbItem> {

        return api.discoverTv(
            apiKey = apiKey
        ).results.map {
            it.copy(media_type = "tv")
        }
    }

    // Web Series
    suspend fun getWebSeries(): List<TmdbItem> {

        return api.discoverTv(
            apiKey = apiKey
        ).results.map {
            it.copy(media_type = "tv")
        }
    }

    // Bollywood Series
    suspend fun getBollywoodSeries(): List<TmdbItem> {

        return api.discoverTv(
            apiKey = apiKey,
            language = "hi"
        ).results.map {
            it.copy(media_type = "tv")
        }
    }

    // TV Shows
    suspend fun getEnglishTvShows(): List<TmdbItem> {

        return api.discoverTv(
            apiKey = apiKey,
            language = "en"
        ).results.map {
            it.copy(media_type = "tv")
        }
    }

    // ============================================================
    // DRAMA
    // ============================================================

    suspend fun getDrama(): List<TmdbItem> {

        val movies = api.discoverMovies(
            apiKey = apiKey,
            genres = "18"
        ).results.map {
            it.copy(media_type = "movie")
        }

        val tv = api.discoverTv(
            apiKey = apiKey,
            genres = "18"
        ).results.map {
            it.copy(media_type = "tv")
        }

        return (
            movies + tv
        )
            .distinctBy {
                "${it.media_type}-${it.id}"
            }
            .sortedByDescending {
                it.popularityScore()
            }
    }

    // K-Drama
    suspend fun getKDrama(): List<TmdbItem> {

        return api.discoverTv(
            apiKey = apiKey,
            genres = "18",
            language = "ko"
        ).results.map {
            it.copy(media_type = "tv")
        }
    }

    // Turkish Drama
    suspend fun getTurkishDrama(): List<TmdbItem> {

        return api.discoverTv(
            apiKey = apiKey,
            genres = "18",
            language = "tr"
        ).results.map {
            it.copy(media_type = "tv")
        }
    }

    // Pakistani Drama
    suspend fun getPakistaniDrama(): List<TmdbItem> {

        return api.discoverTv(
            apiKey = apiKey,
            genres = "18",
            language = "ur"
        ).results.map {
            it.copy(media_type = "tv")
        }
    }

    // ============================================================
    // ANIME / ANIMATION
    // ============================================================

    suspend fun getAnime(): List<TmdbItem> {

        val movies = api.discoverMovies(
            apiKey = apiKey,
            genres = "16",
            language = "ja"
        ).results.map {
            it.copy(media_type = "movie")
        }

        val tv = api.discoverTv(
            apiKey = apiKey,
            genres = "16",
            language = "ja"
        ).results.map {
            it.copy(media_type = "tv")
        }

        return (
            movies + tv
        )
            .distinctBy {
                "${it.media_type}-${it.id}"
            }
            .sortedByDescending {
                it.popularityScore()
            }
    }

    // Animated Movies / Shows
    suspend fun getAnimatedContent(): List<TmdbItem> {

        val movies = api.discoverMovies(
            apiKey = apiKey,
            genres = "16"
        ).results.map {
            it.copy(media_type = "movie")
        }

        val tv = api.discoverTv(
            apiKey = apiKey,
            genres = "16"
        ).results.map {
            it.copy(media_type = "tv")
        }

        return (
            movies + tv
        )
            .distinctBy {
                "${it.media_type}-${it.id}"
            }
            .sortedByDescending {
                it.popularityScore()
            }
    }

    // Cartoon Shows
    suspend fun getCartoonShows(): List<TmdbItem> {

        return api.discoverTv(
            apiKey = apiKey,
            genres = "16",
            language = "en"
        ).results.map {
            it.copy(media_type = "tv")
        }
    }

    // ============================================================
    // SEARCH
    // ============================================================

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

    // ============================================================
    // DETAILS
    // ============================================================

    suspend fun getMovieDetails(
        movieId: Int
    ): TmdbItem =
        api.getMovieDetails(
            movieId,
            apiKey
        ).copy(
            media_type = "movie"
        )

    suspend fun getTvDetails(
        tvId: Int
    ): TmdbItem =
        api.getTvDetails(
            tvId,
            apiKey
        ).copy(
            media_type = "tv"
        )

    suspend fun getDetails(
        item: TmdbItem
    ): TmdbItem =
        if (item.media_type == "tv") {
            getTvDetails(item.id)
        } else {
            getMovieDetails(item.id)
        }

    // ============================================================
    // SIMILAR
    // ============================================================

    suspend fun getSimilar(
        item: TmdbItem
    ): List<TmdbItem> {

        return if (item.media_type == "tv") {

            api.getSimilarTv(
                item.id,
                apiKey
            ).results.map {
                it.copy(media_type = "tv")
            }

        } else {

            api.getSimilarMovies(
                item.id,
                apiKey
            ).results.map {
                it.copy(media_type = "movie")
            }
        }
    }
}

// ============================================================
// HELPER
// ============================================================

private fun TmdbItem.popularityScore(): Double {
    return vote_average ?: 0.0
}
