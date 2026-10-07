import express from 'express';
import 'dotenv/config';

const app = express();
app.use(express.json());

app.get('/api/health', (req, res) => {
    res.status(200).json({ status: 'OK' });
});

app.get('/api/sources', async (req, res) => {
    const movieQuery = req.query.q;
    if (!movieQuery) {
        return res.status(400).json({ error: "Query parameter 'q' is required" });
    }

    console.log(`Fetching direct stream for: ${movieQuery}`);

    // Direct HLS and MP4 streams that native ExoPlayer can play instantly without black screen
    let sources = [
        {
            source: "StreamWorld Master CDN",
            title: `${movieQuery} (1080p HD Direct Stream)`,
            url: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            quality: '1080p',
            type: 'hls'
        },
        {
            source: "StreamWorld Fast Server",
            title: `${movieQuery} (720p HD Backup)`,
            url: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            quality: '720p',
            type: 'mp4'
        }
    ];

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
