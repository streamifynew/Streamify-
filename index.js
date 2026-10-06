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

// Helper Function to search links across providers
async function searchAllProviders(query) {
    let results = [];
    try {
        const vegaUrl = `https://vegamovies.nl/?s=${encodeURIComponent(query)}`;
        console.log("Fetching URL:", vegaUrl);

        const { data } = await axios.get(vegaUrl, {
            headers: { 
                "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36" 
            }
        });

        const $ = cheerio.load(data);
        
        // Multiple common selectors for WordPress / movie blogs
        const items = $('article, .post-item, .trending-box, .movies-list div, h2.title a, .search-result-item');
        console.log("Page loaded successfully, items found:", items.length);

        // Fallback: agar specific class na mile toh saare main article links utha lo
        if (items.length === 0) {
            $('a').each((_, element) => {
                const href = $(element).attr('href');
                const title = $(element).text().trim();
                if (href && href.includes('/' ) && title.length > 10) {
                    if (title.toLowerCase().includes(query.toLowerCase().substring(0, 4))) {
                        results.push({
                            source: "Vegamovies",
                            title: title,
                            url: href,
                            quality: title.toLowerCase().includes('1080p') ? '1080p' : (title.toLowerCase().includes('4k') ? '4K' : 'HD'),
                            size: 'Unknown'
                        });
                    }
                }
            });
        } else {
            items.each((_, element) => {
                const titleEl = $(element).find('h2, h3, .title, a').first();
                const title = titleEl.text().trim() || $(element).text().trim();
                const link = $(element).is('a') ? $(element).attr('href') :$(element).find('a').attr('href');

                if (link && title) {
                    results.push({
                        source: "Vegamovies",
                        title: title,
                        url: link,
                        quality: title.toLowerCase().includes('1080p') ? '1080p' : (title.toLowerCase().includes('4k') ? '4K' : 'HD'),
                        size: 'Unknown'
                    });
                }
            });
        }

        // Remove duplicates based on URL
        results = Array.from(new Set(results.map(a => a.url)))
            .map(url => {
                return results.find(a => a.url === url);
            });

    } catch (err) {
        console.log("Scraping Error Details:", err.message);
    }
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
    console.log(`Streamify Scraper Backend running on port ${PORT}`);
});
