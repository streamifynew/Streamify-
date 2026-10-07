import express from 'express';
import cloudscraper from 'cloudscraper';
import * as cheerio from 'cheerio';
import 'dotenv/config';

const app = express();
app.use(express.json());

// Health check route for Render
app.get('/api/health', (req, res) => {
    res.status(200).json({ status: 'OK' });
});

// Helper Function to search links across providers
async function searchAllProviders(query) {
    let results = [];
    
    // Direct Google Custom Search ya ek stable alternative scraper try karte hain
    try {
        const altUrl = `https://v3.sg.media-imdb.com/suggestion/x/${encodeURIComponent(query)}.json`;
        console.log("Fetching alternative source:", altUrl);

        const response = await fetch(altUrl);
        const data = await response.json();

        if (data && data.d) {
            data.d.forEach(item => {
                if (item.l && item.id) {
                    results.push({
                        source: "StreamWorld-Catalog",
                        title: `${item.l} (${item.y || 'N/A'})`,
                        url: `https://www.imdb.com/title/${item.id}`,
                        quality: 'HD',
                        size: 'Unknown'
                    });
                }
            });
        }
    } catch (err) {
        console.log("Catalog Error:", err.message);
    }

    // Unique links filter
    results = Array.from(new Set(results.map(a => a.url)))
        .map(url => results.find(a => a.url === url));

    console.log("Total items found after parsing (All providers):", results.length);
    return results;
}

// API Endpoint jise app hit karegi
app.get('/api/sources', async (req, res) => {
    const movieQuery = req.query.q;
    if (!movieQuery) {
        return res.status(400).json({ error: "Query parameter 'q' is required" });
    }

    console.log(`Searching sources for: ${movieQuery}`);
    const sources = await searchAllProviders(movieQuery);

    res.json({
        query: movieQuery,
        totalSources: sources.length,
        resources: sources
    });
});

const PORT = process.env.PORT || 10000;
app.listen(PORT, () => {
    console.log(`StreamWorld Scraper Backend running on port ${PORT}`);
});
