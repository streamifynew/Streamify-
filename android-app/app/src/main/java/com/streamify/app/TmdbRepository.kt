package com.streamify.app

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * One description of "what to load" – used by every grid in the app.
 * movie / tv : which kinds of titles to fetch
 * languages  : original languages (several = merged, e.g. South cinema)
 * networks   : TV platforms/channels (pipe separated TMDB ids)
 * movieGenre / tvGenre : TMDB genre ids ("," = AND, "|" = OR)
 * newRelease : only titles released in the last ~4 months
 */
data class BrowseSpec(
    val movie: Boolean = false,
    val tv: Boolean = false,
    val languages: List<String?> = listOf(null),
    val networks: String? = null,
    val withoutGenres: String? = null,
    val movieGenre: String? = null,
    val tvGenre: String? = null,
    val newRelease: Boolean = false
)

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
    // BROWSE (every category / genre / new-release grid, paged)
    // ============================================================

    // already-loaded pages are remembered, so scrolling back is instant
    fun cached(spec: BrowseSpec, page: Int): List<TmdbItem>? = cache[cacheKey(spec, page)]

    suspend fun browse(spec: BrowseSpec, page: Int): List<TmdbItem> {

        cache[cacheKey(spec, page)]?.let { return it }

        val result = load(spec, page)
        if (result.isNotEmpty()) cache[cacheKey(spec, page)] = result
        return result
    }

    private suspend fun load(spec: BrowseSpec, page: Int): List<TmdbItem> {

        val from = if (spec.newRelease) dateOffset(-120) else null
        val to = if (spec.newRelease) dateOffset(0) else null

        val lists = mutableListOf<List<TmdbItem>>()

        for (lang in spec.languages) {

            if (spec.movie) {
                lists += fetch {
                    api.discoverMovies(
                        apiKey = apiKey,
                        page = page,
                        genres = spec.movieGenre,
                        language = lang,
                        releaseFrom = from,
                        releaseTo = to
                    ).results.map { it.copy(media_type = "movie") }
                }
            }

            if (spec.tv) {
                lists += fetch {
                    api.discoverTv(
                        apiKey = apiKey,
                        page = page,
                        genres = spec.tvGenre,
                        language = lang,
                        networks = spec.networks,
                        withoutGenres = spec.withoutGenres,
                        airFrom = from,
                        airTo = to
                    ).results.map { it.copy(media_type = "tv") }
                }
            }
        }

        return interleave(lists)
            .filter { it.isShowable(requireRating = !spec.newRelease) }
            .distinctBy { "${it.media_type}-${it.id}" }
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

    // ============================================================
    // STREAMIFY SCRAPER BACKEND (RENDER INTEGRATION)
    // ============================================================

    suspend fun fetchStreamingLinksForAnyMedia(item: TmdbItem): List<StreamingSource> {
        val mediaId = item.id
        val mediaType = item.media_type ?: "movie"
        if (mediaId == 0) return emptyList()

        return try {
            // Live Render Backend URL
            val renderBaseUrl = "https://core-9b8h.onrender.com"
            val response = api.getStreamingSources(renderBaseUrl, mediaType, mediaId)
            response.resources
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private suspend fun fetch(block: suspend () -> List<TmdbItem>): List<TmdbItem> =
        try {
            block()
        } catch (_: Exception) {
            emptyList()
        }

    // mixes several lists one by one so no single list dominates
    private fun interleave(lists: List<List<TmdbItem>>): List<TmdbItem> {
        val out = mutableListOf<TmdbItem>()
        val longest = lists.maxOfOrNull { it.size } ?: 0
        for (i in 0 until longest) {
            for (l in lists) {
                if (i < l.size) out.add(l[i])
            }
        }
        return out
    }

    private fun cacheKey(spec: BrowseSpec, page: Int) = "$spec#$page"

    private companion object {
        val cache = HashMap<String, List<TmdbItem>>()
    }

    private fun dateOffset(days: Int): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, days)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }
}

// hide empty entries: no poster, or (for normal lists) no rating yet
private fun TmdbItem.isShowable(requireRating: Boolean): Boolean {
    if (poster_path == null) return false
    return !requireRating || (vote_average ?: 0.0) > 0.0
}
