import express from 'express';
import 'dotenv/config';

const app = express();
app.use(express.json());

app.get('/api/health', (req, res) => {
    res.status(200).json({ status: 'OK' });
});

// Professional IMDb Resolver to fetch exact Movie/Series ID for StreamWorld
async function getImdbIdAndDetails(query) {
    try {
        console.log(`Resolving real media ID for: ${query}`);
        const url = `https://v3.sg.media-imdb.com/suggestion/x/${encodeURIComponent(query)}.json`;
        const response = await fetch(url);
        const data = await response.json();
        
        if (data && data.d && data.d.length > 0) {
            // Pick the best matching movie or TV series
            const bestMatch = data.d.find(item => item.id && (item.qid === 'movie' || item.qid === 'tvSeries' || item.q === 'feature')) || data.d[0];
            return {
                id: bestMatch.id, // e.g. tt0499549 for Avatar
                title: bestMatch.l,
                year: bestMatch.y || '',
                type: bestMatch.qid || 'movie'
            };
        }
    } catch (e) {
        console.log("IMDb resolution error:", e.message);
    }
    return null;
}

app.get('/api/sources', async (req, res) => {
    const movieQuery = req.query.q;
    if (!movieQuery) {
        return res.status(400).json({ error: "Query parameter 'q' is required" });
    }

    console.log(`Fetching high-performance streams for query: ${movieQuery}`);
    const mediaInfo = await getImdbIdAndDetails(movieQuery);

    let sources = [];

    if (mediaInfo && mediaInfo.id) {
        const isTv = mediaInfo.type === 'tvSeries';
        const id = mediaInfo.id;

        // Multi-Server Pro Stream Embeds for StreamWorld
        sources.push({
            source: "StreamWorld Pro (Server 1)",
            title: `${mediaInfo.title} (${mediaInfo.year}) - 1080p`,
            url: isTv 
                ? `https://vidsrc.xyz/embed/tv?imdb=${id}&season=1&episode=1` 
                : `https://vidsrc.xyz/embed/movie?imdb=${id}`,
            quality: '1080p Full HD',
            type: 'embed'
        });

        sources.push({
            source: "StreamWorld Ultra (Server 2)",
            title: `${mediaInfo.title} (${mediaInfo.year}) - Fast HD`,
            url: isTv 
                ? `https://vidsrc.to/embed/tv/${id}/1/1` 
                : `https://vidsrc.to/embed/movie/${id}`,
            quality: '1080p HD',
            type: 'embed'
        });

        sources.push({
            source: "StreamWorld Global (Server 3)",
            title: `${mediaInfo.title} (${mediaInfo.year}) - Multi-Audio`,
            url: isTv 
                ? `https://multiembed.mov/?video_id=${id}&tmdb=1&s=1&e=1` 
                : `https://multiembed.mov/?video_id=${id}&tmdb=1`,
            quality: '1080p',
            type: 'embed'
        });
    }

    // Fallback if specific ID mapping fails
    if (sources.length === 0) {
        sources.push({
            source: "StreamWorld Direct CDN",
            title: `${movieQuery} (Universal Stream)`,
            url: `https://vidsrc.xyz/embed/movie?q=${encodeURIComponent(movieQuery)}`,
            quality: 'HD',
            type: 'embed'
        });
    }

    res.json({
        query: movieQuery,
        totalSources: sources.length,
        resources: sources
    });
});

const PORT = process.env.PORT || 10000;
app.listen(PORT, () => {
    console.log(`StreamWorld Backend running on port ${PORT}`);
});
