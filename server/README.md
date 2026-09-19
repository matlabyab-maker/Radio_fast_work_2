# Fast Radio server

This server is optional for original playback. When configured in the app, `/stream` performs real server-side transcoding and `/record/start` + `/record/stop` create AMR-NB recordings from the selected stream.

## Run
1. Copy `.env.example` to `.env` and set a long API_KEY.
2. `docker compose up -d --build`
3. Test `GET /health`.
4. Enter the public base URL and the same API key in Fast Radio > SET.

Do not put a fake URL in the APK. A public VPS/domain is required for real internet access.
