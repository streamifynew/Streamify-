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
    
    // Attempt 1: Vegamovies
    try {
        const vegaUrl = `https://vegamovies.is/?s=${encodeURIComponent(query)}`;
        console.log("Fetching URL with cloudscraper:", vegaUrl);

        const data = await new Promise((resolve, reject) => {
            cloudscraper.get(vegaUrl, (error, response, body) => {
                if (error) reject(error);
                else resolve(body);
            });
        });

        const $= cheerio.load(data);$('a').each((_, element) => {
            const title = $(element).text().trim();
            const link = $(element).attr('href');

            if (link && title && title.length > 5) {
                const lowerTitle = title.toLowerCase();
                if (link.includes('vegamovies') || link.includes('.is/') || link.includes('.to/')) {
                    if (lowerTitle.includes('download') || lowerTitle.includes('1080p') || lowerTitle.includes('720p') || lowerTitle.includes('4k') || lowerTitle.includes(query.toLowerCase().substring(0, 3))) {
                        results.push({
                            source: "Vegamovies",
                            title: title,
                            url: link,
                            quality: lowerTitle.includes('1080p') ? '1080p' : (lowerTitle.includes('4k') ? '4K' : 'HD'),
                            size: 'Unknown'
                        });
                    }
                }
            }
        });
    } catch (err) {
        console.log("Vegamovies Error:", err.message);
    }

    // Attempt 2: Agar Vegamovies se 0 results aaye, toh HDHub4u try karo
    if (results.length === 0) {
        try {
            const altUrl = `https://hdhub4u.wtf/?s=${encodeURIComponent(query)}`;
            console.log("Fallback fetching URL:", altUrl);

            const dataAlt = await new Promise((resolve, reject) => {
                cloudscraper.get(altUrl, (error, response, body) => {
                    if (error) reject(error);
                    else resolve(body);
                });
            });

            const $alt = cheerio.load(dataAlt);$alt('a').each((_, element) => {
                const title = $alt(element).text().trim();
                const link = $alt(element).attr('href');

                if (link && title && title.length > 5) {
                    const lowerTitle = title.toLowerCase();
                    if (lowerTitle.includes(query.toLowerCase().substring(0, 3)) || lowerTitle.includes('1080p')) {
                        results.push({
                            source: "HDHub4u",
                            title: title,
                            url: link,
                            quality: lowerTitle.includes('1080p') ? '1080p' : 'HD',
                            size: 'Unknown'
                        });
                    }
                }
            });
        } catch (err) {
            console.log("Fallback Scraper Error:", err.message);
        }
    }

    // Remove duplicates based on URL
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
