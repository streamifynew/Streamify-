# Streamify — starter project

A clean starting point for the Streamify OTT app. This package is designed to be uploaded to the currently empty GitHub repository and deployed as a Node web service on Render.

## Included
- Responsive dark OTT landing page
- Express backend and `/api/health`
- TMDB trending metadata endpoint (`/api/catalog/trending`)
- Render Blueprint (`render.yaml`)
- Environment variable template
- Plan constants: free 480p / up to 3 downloads; Premium ₹99/month / 720p–1080p / unlimited downloads

## Important limitations
- TMDB is used for metadata only; it does not provide playback streams or verify Hindi/multi-audio availability.
- Playback, user authentication, Firebase, payments, downloads, ad insertion, persistent database, and admin controls are not implemented in this starter yet.
- Do not publish content until you have authorized playback sources and verified audio-language data.
- The Premium button is informational only; it does not charge users.

## Run locally
1. Install Node.js 20+.
2. Copy `.env.example` to `.env` and add your TMDB API key.
3. Run `npm install` then `npm start`.
4. Open `http://localhost:3000`.

## Deploy to Render
1. Push this repository to GitHub.
2. In Render, create **New + → Blueprint** and select this repository, or create a Node Web Service.
3. Set `TMDB_API_KEY` in Render environment variables. Set a long random `ADMIN_API_KEY` for future admin routes.
4. Build command: `npm install`; Start command: `npm start`.
5. After deploy, check `/api/health`.

## GitHub upload from phone
Extract the ZIP. In the repository on GitHub, use **Add file → Upload files** and upload the extracted files/folders (not the ZIP itself). Commit to the default branch. If GitHub mobile does not allow folder upload, use GitHub's browser desktop mode or GitHub Desktop on a computer.
