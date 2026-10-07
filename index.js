import express from 'express';
import 'dotenv/config';

const app = express();
app.use(express.json());

app.get('/api/health', (req, res) => {
    res.status(200).json({ status: 'OK' });
});

// Helper to resolve IMDb ID
async function getImdbId(query) {
    try {
        const url = `https://v3.sg.media-imdb.com/suggestion/x/${encodeURIComponent(query)}.json`;
        const response = await fetch(url);
        const data = await response.json();
        if (data && data.d && data.d.length > 0) {
            const match = data.d.find(item => item.id && (item.qid === 'movie' || item.qid === 'tvSeries')) || data.d[0];
            return { id: match.id, title: match.l, year: match.y || '', type: match.qid || 'movie' };
        }
    } catch (e) {
        console.log("ID resolution error:", e.message);
    }
    return null;
}

app.get('/api/sources', async (req, res) => {
    const query = req.query.q;
    if (!query) {
        return res.status(400).json({ error: "Query parameter 'q' is required" });
    }

    console.log(`Building multi-source aggregation for: ${query}`);
    const media = await getImdbId(query);

    let sources = [];

    if (media && media.id) {
        const id = media.id;
        const isTv = media.type === 'tvSeries';

        // Parallel extraction simulation across multiple top-tier providers
        sources.push(
            {
                source: "Streamify Titan Engine",
                title: `${media.title} (${media.year}) - Server Alpha`,
                url: isTv ? `https://vidsrc.xyz/embed/tv?imdb=${id}&season=1&episode=1` : `https://vidsrc.xyz/embed/movie?imdb=${id}`,
                quality: '1080p FHD',
                type: 'embed'
            },
            {
                source: "Streamify Vortex CDN",
                title: `${media.title} (${media.year}) - Server Beta`,
                url: isTv ? `https://vidsrc.to/embed/tv/${id}/1/1` : `https://vidsrc.to/embed/movie/${id}`,
                quality: '1080p HD',
                type: 'embed'
            },
            {
                source: "Streamify Global Hub",
                title: `${media.title} (${media.year}) - Multi-Audio`,
                url: isTv ? `https://multiembed.mov/?video_id=${id}&tmdb=1&s=1&e=1` : `https://multiembed.mov/?video_id=${id}&tmdb=1`,
                quality: '1080p',
                type: 'embed'
            }
        );
    }

    // Fallback if direct ID match fails
    if (sources.length === 0) {
        sources.push({
            source: "Streamify Universal Matrix",
            title: `${query} (Global Search Result)`,
            url: `https://vidsrc.xyz/embed/movie?q=${encodeURIComponent(query)}`,
            quality: 'HD',
            type: 'embed'
        });
    }

    res.json({
        query: query,
        totalSources: sources.length,
        resources: sources
    });
});

const PORT = process.env.PORT || 10000;
app.listen(PORT, () => {
    console.log(`Streamify Ultimate Backend running on port ${PORT}`);
});
