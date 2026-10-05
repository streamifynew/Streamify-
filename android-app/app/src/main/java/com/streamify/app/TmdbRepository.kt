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
    // Originals from streaming platforms (Netflix, Prime Video, Disney+, Apple TV+,
    // Hulu, HBO/Max, Disney+ Hotstar), without daily soaps and talk shows.
    suspend fun getWebSeries(): List<TmdbItem> {

        var shows = api.discoverTv(
            apiKey = apiKey,
            networks = WEB_NETWORKS,
            withoutGenres = GENRE_SOAP
        ).results.filter { it.isShowable() }

        // second page, so the grid has plenty of titles
        if (shows.size < 18) {
            val more = api.discoverTv(
                apiKey = apiKey,
                page = 2,
                networks = WEB_NETWORKS,
                withoutGenres = GENRE_SOAP
            ).results.filter { it.isShowable() }

            shows = (shows + more).distinctBy { it.id }
        }

        return shows.map {
            it.copy(media_type = "tv")
        }
    }

    // Bollywood Series
    // Hindi web series from OTT platforms (Netflix, Prime Video, Disney+ Hotstar),
    // no daily soap serials, with some Hindi reality shows mixed in.
    suspend fun getBollywoodSeries(): List<TmdbItem> {

        var webSeries = api.discoverTv(
            apiKey = apiKey,
            language = "hi",
            networks = OTT_NETWORKS,
            withoutGenres = GENRE_SOAP
        ).results.filter { it.isShowable() }

        // fallback: if the OTT filter returns too little, use Hindi shows without soaps
        if (webSeries.size < 6) {
            val extra = api.discoverTv(
                apiKey = apiKey,
                language = "hi",
                withoutGenres = GENRE_SOAP
            ).results.filter { it.isShowable() }

            webSeries = (webSeries + extra).distinctBy { it.id }
        }

        val reality = api.discoverTv(
            apiKey = apiKey,
            genres = GENRE_REALITY,
            language = "hi"
        ).results.filter { it.isShowable() }

        // after every 3 web series, add 1 reality show
        val mixed = mutableListOf<TmdbItem>()
        var r = 0

        webSeries.forEachIndexed { index, item ->
            mixed.add(item)
            if (index % 3 == 2 && r < reality.size) {
                mixed.add(reality[r])
                r++
            }
        }

        while (r < reality.size) {
            mixed.add(reality[r])
            r++
        }

        return mixed
            .distinctBy { it.id }
            .map {
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

    private companion object {
        // TMDB network ids: Netflix, Amazon Prime Video, Disney+ Hotstar
        const val OTT_NETWORKS = "213|1024|3919"

        // Netflix, Prime Video, Disney+, Apple TV+, Hulu, HBO, Max, Disney+ Hotstar
        const val WEB_NETWORKS = "213|1024|2739|2552|453|49|3186|3919"

        // TMDB genre ids
        const val GENRE_SOAP = "10766"
        const val GENRE_REALITY = "10764"
    }
}

// ============================================================
// HELPERS
// ============================================================

private fun TmdbItem.popularityScore(): Double {
    return vote_average ?: 0.0
}

// hide unreleased / empty entries (no poster or no rating)
private fun TmdbItem.isShowable(): Boolean {
    return poster_path != null && (vote_average ?: 0.0) > 0.0
}
