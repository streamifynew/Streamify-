import express from 'express';
import axios from 'axios';
import * as cheerio from 'cheerio';
import 'dotenv/config';

const app = express();
app.use(express.json());

// Health check route for Render
app.get('/api/health', (req, res) => {
    res.status(200).json({ status: 'OK' });
});

// Helper function to search links across providers
async function searchAllProviders(query) {
    let results = [];
    try {
        const vegaUrl = `https://vegamovies.nl/?s=${encodeURIComponent(query)}`; // apna active URL yahan rakhein
        console.log("Fetching URL:", vegaUrl);
        
        const { data } = await axios.get(vegaUrl, {
            headers: { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" }
        });
        
        const $ = cheerio.load(data);
        console.log("Page loaded successfully, items found:", $('div.title, .post-item').length);

        $('div.title, .post-item').each((_, element) => {
            const title = $(element).text().trim();
            const link = $(element).find('a').attr('href');
            if (link) {
                results.push({
                    source: "Vegamovies",
                    title: title || query,
                    url: link,
                    quality: title.toLowerCase().includes('1080p') ? '1080p' : (title.toLowerCase().includes('4k') ? '4K' : 'HD'),
                    size: 'Unknown'
                });
            }
        });
    } catch (err) {
        // Yeh line batayegi ki asli error kya aa raha hai (jaise 403 Forbidden ya Cloudflare block)
        console.log("Scraping Error Details:", err.message);
    }
    return results;
}


// API Endpoint jise app hit karegi
app.get('/api/sources', async (req, res) => {
    const movieQuery = req.query.q;
    if (!movieQuery) {
        return res.status(400).json({ error: 'Query parameter "q" is required' });
    }

    console.log(`Searching sources for: ${movieQuery}`);
    const sources = await searchAllProviders(movieQuery);

    res.json({
        query: movieQuery,
        totalSources: sources.length,
        resources: sources
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Streamify Scraper Backend running on port ${PORT}`);
});
