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

// Helper Function to search real streaming/download providers
async function searchAllProviders(query) {
    let results = [];
    
    // Attempt 1: UHDMovies
    try {
        const searchUrl = `https://uhdmovies.skin/?s=${encodeURIComponent(query)}`;
        console.log("Fetching UHDMovies:", searchUrl);

        const data = await new Promise((resolve, reject) => {
            cloudscraper.get(searchUrl, (error, response, body) => {
                if (error) reject(error);
                else resolve(body);
            });
        });

        const $= cheerio.load(data);$('a').each((_, element) => {
            const title = $(element).text().trim();
            const link = $(element).attr('href');

            if (link && title && title.length > 5) {
                const lowerTitle = title.toLowerCase();
                if (link.includes('uhdmovies') && (lowerTitle.includes('1080p') || lowerTitle.includes('720p') || lowerTitle.includes('4k') || lowerTitle.includes('download'))) {
                    results.push({
                        source: "UHDMovies",
                        title: title,
                        url: link,
                        quality: lowerTitle.includes('4k') ? '4K' : (lowerTitle.includes('1080p') ? '1080p' : 'HD'),
                        size: 'Unknown'
                    });
                }
            }
        });
    } catch (err) {
        console.log("UHDMovies Error:", err.message);
    }

    // Attempt 2: Fallback to Bollyflix if 0 results
    if (results.length === 0) {
        try {
            const altUrl = `https://bollyflix.party/?s=${encodeURIComponent(query)}`;
            console.log("Fetching Bollyflix Fallback:", altUrl);

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
                    if (link.includes('bollyflix') && (lowerTitle.includes('1080p') || lowerTitle.includes('720p') || lowerTitle.includes('download'))) {
                        results.push({
                            source: "Bollyflix",
                            title: title,
                            url: link,
                            quality: lowerTitle.includes('1080p') ? '1080p' : 'HD',
                            size: 'Unknown'
                        });
                    }
                }
            });
        } catch (err) {
            console.log("Bollyflix Error:", err.message);
        }
    }

    // Remove duplicates based on URL
    results = Array.from(new Set(results.map(a => a.url)))
        .map(url => results.find(a => a.url === url));

    console.log("Total streaming sources found:", results.length);
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
