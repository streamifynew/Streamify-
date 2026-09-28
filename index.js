import 'dotenv/config';
import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const app = express();
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PORT = Number(process.env.PORT || 3000);
app.use(express.json({ limit: '1mb' }));
app.use(express.static(path.join(__dirname, '../public')));

app.get('/api/health', (_req, res) => res.json({ ok: true, app: 'Streamify', status: 'starter' }));
app.get('/api/config', (_req, res) => res.json({ premiumMonthlyINR: 99, freeMaxDownloads: 3, freeMaxQuality: '480p', premiumQualities: ['720p','1080p'] }));

// TMDB metadata only. It does not provide playback streams or prove audio-language availability.
app.get('/api/catalog/trending', async (_req, res) => {
  const key = process.env.TMDB_API_KEY;
  if (!key) return res.status(503).json({ error: 'TMDB_API_KEY is not configured yet.' });
  try {
    const url = new URL('https://api.themoviedb.org/3/trending/all/week');
    url.searchParams.set('api_key', key);
    const response = await fetch(url);
    if (!response.ok) return res.status(response.status).json({ error: 'TMDB request failed.' });
    const data = await response.json();
    res.json({ results: (data.results || []).filter(x => x.media_type === 'movie' || x.media_type === 'tv').map(x => ({
      id: x.id, mediaType: x.media_type, title: x.title || x.name, overview: x.overview,
      posterPath: x.poster_path, backdropPath: x.backdrop_path, releaseDate: x.release_date || x.first_air_date,
      originalLanguage: x.original_language, popularity: x.popularity, voteAverage: x.vote_average,
      published: false, audioLanguagesVerified: false
    })) });
  } catch { res.status(502).json({ error: 'Could not reach TMDB.' }); }
});

app.get('*', (_req, res) => res.sendFile(path.join(__dirname, '../public/index.html')));
app.listen(PORT, () => console.log(`Streamify running on port ${PORT}`));
