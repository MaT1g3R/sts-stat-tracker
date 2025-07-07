# sts-stat-tracker
Slay the Spire stats tracker mod

## Overview
This is a mod for Slay the Spire that tracks your game statistics and sends them to a backend server for analysis and visualization.

## Components
- **Mod**: Java mod for Slay the Spire that collects game statistics
- **Backend**: Go server that receives, stores, and visualizes the statistics

## Docker
The backend server is containerized using Docker. The Dockerfile is located at `backend/Dockerfile`.

### GitHub Actions
This repository includes a GitHub Action workflow that automatically builds and publishes the Docker image to GitHub Container Registry (ghcr.io) when:
- Changes are pushed to the `main` branch
- A new tag is created (e.g., `v1.0.0`)

The Docker image can be pulled using:
```bash
docker pull ghcr.io/OWNER/REPOSITORY:TAG
```

For more details about the GitHub Actions workflow, see [.github/README.md](.github/README.md).
