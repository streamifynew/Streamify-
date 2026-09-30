package com.streamify.app

class TmdbRepository {

    private val api = TmdbClient.api

    suspend fun getTrending(): List<TmdbItem> {
        return api.getTrending(
            apiKey = BuildConfig.TMDB_API_KEY
        ).results
    }
}
