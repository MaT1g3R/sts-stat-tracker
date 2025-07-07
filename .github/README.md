# GitHub Actions Workflows

This directory contains GitHub Actions workflows for automating various tasks in the repository.

## Docker Image Build and Publish

The `docker-publish.yml` workflow builds and publishes a Docker image to GitHub Container Registry (ghcr.io).

### Workflow Details

- **Trigger**: The workflow is triggered on:
  - Pushes to the `main` branch
  - Pushes of tags matching the pattern `v*.*.*` (e.g., v1.0.0)
  - Pull requests to the `main` branch

- **Actions**:
  - Builds the Docker image using the Dockerfile located at `backend/Dockerfile`
  - Tags the image with appropriate tags based on the event type:
    - For pushes to `main`: `latest` and a SHA-based tag
    - For tags: The tag version (e.g., `v1.0.0`, `1.0`)
    - For PRs: A PR-specific tag
  - Pushes the image to GitHub Container Registry (only for pushes to `main` and tags, not for PRs)

### Usage

The Docker image will be available at:
```
ghcr.io/OWNER/REPOSITORY:TAG
```

Where:
- `OWNER` is the GitHub username or organization name
- `REPOSITORY` is the repository name
- `TAG` is one of the tags mentioned above

### Authentication

To pull the image, you may need to authenticate with GitHub Container Registry:

```bash
echo $GITHUB_TOKEN | docker login ghcr.io -u USERNAME --password-stdin
```

### Permissions

The workflow requires the following permissions:
- `contents: read` - To check out the repository
- `packages: write` - To push the image to GitHub Container Registry

These permissions are automatically granted by the workflow.