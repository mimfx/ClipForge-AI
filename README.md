# ClipForge AI — full deployment

This version is a single-server build: the web UI and the processing backend run together. It needs a Docker-capable host because it uses yt-dlp, FFmpeg, Python and faster-whisper.

## Deploy

Use a Docker web service on a host such as Render. The included `render.yaml` is a Blueprint configuration. After deployment, open the service URL — the frontend and `/api/*` routes share the same origin.

## Important

The first AI job can take longer because faster-whisper downloads the selected model. `tiny` is used by default for lower CPU/RAM usage.

Only process videos you own or have permission to use.
