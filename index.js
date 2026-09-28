
import 'dotenv/config';
import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const app = express();
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PORT = Number(process.env.PORT || 3000);

app.use(express.json({ limit: '1mb' }));

// Frontend files are in the repository root.
app.get('/', (_req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.get('/styles.css', (_req, res) => {
  res.sendFile(path.join(__dirname, 'styles.css'));
});

app.get('/app.js', (_req, res) => {
  res.sendFile(path.join(__dirname, 'app.js'));
});

// Health check
app.get('/api/health', (_req, res) => {
  res.json({
    ok: true,
    app: 'Streamify',
    status: 'starter'
  });
});

// App plan settings
app.get('/api/config', (_req, res) => {
  res.json({
    premiumMonthlyINR: 99,
    freeMaxDownloads: 3,
    freeMaxQuality: '480p',
    premiumQualities: ['720p', '1080p']
  });
});

// TMDB provides metadata only, not playback streams.
app.get('/api/catalog/trending', async (_req, res) => {
  const key = process.env.TMDB_API_KEY;

  if (!key) {
    return res.status(503).json({
      error: 'TMDB_API_KEY is not configured yet.'
    });
  }

  try {
    const url = new URL(
      'https://api.themoviedb.org/3/trending/all/week'
    );
    url.searchParams.set('api_key', key);

    const response = await fetch(url);

    if (!response.ok) {
      return res.status(response.status).json({
        error: 'TMDB request failed.'
      });
    }

    const data = await response.json();

    res.json({
      results: (data.results || [])
        .filter(
          item =>
            item.media_type === 'movie' ||
            item.media_type === 'tv'
        )
        .map(item => ({
          id: item.id,
          mediaType: item.media_type,
          title: item.title || item.name,
          overview: item.overview,
          posterPath: item.poster_path,
          backdropPath: item.backdrop_path,
          releaseDate:
            item.release_date || item.first_air_date,
          originalLanguage: item.original_language,
          popularity: item.popularity,
          voteAverage: item.vote_average,
          published: false,
          audioLanguagesVerified: false
        }))
    });
  } catch (error) {
    console.error('TMDB error:', error);
    res.status(502).json({
      error: 'Could not reach TMDB.'
    });
  }
});

// Unknown routes
app.get('*', (_req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`Streamify running on port ${PORT}`);
});
