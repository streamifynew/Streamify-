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

// Helper Function to search links across providers using cloudscraper
async function searchAllProviders(query) {
    let results = [];
    try {
        const vegaUrl = `https://vegamovies.is/?s=${encodeURIComponent(query)}`;
        console.log("Fetching URL with cloudscraper:", vegaUrl);

        const data = await new Promise((resolve, reject) => {
            cloudscraper.get(vegaUrl, (error, response, body) => {
                if (error) reject(error);
                else resolve(body);
            });
        });

        const $ = cheerio.load(data);
        console.log("Page loaded successfully. Parsing links...");

        // Site ke saare anchor tags ko check karo jo search results ya posts ke ho sakte hain
        $('a').each((_, element) => {
            const title = $(element).text().trim();
            const link = $(element).attr('href');

            // Filter out navigation, empty, or irrelevant links
            if (link && title && title.length > 5) {
                const lowerTitle = title.toLowerCase();
                // Check karo ki link me post/movie ka structure ho aur query se milti julti ho
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

        // Remove duplicates based on URL
        results = Array.from(new Set(results.map(a => a.url)))
            .map(url => results.find(a => a.url === url));

        console.log("Total items found after parsing:", results.length);

    } catch (err) {
        console.log("Scraping Error Details:", err.message);
    }
    return results;
}

        
        // Multiple common selectors for WordPress / movie blogs
        const items = $('article, .post-item, .trending-box, .movies-list div, h2.title a, .search-result-item');
        console.log("Page loaded successfully, items found:", items.length);

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
