# Stats Tracker Backend

This is the backend server for the Slay the Spire Stats Tracker mod. It provides API endpoints for storing and retrieving run statistics, user authentication, and leaderboard functionality.

## Project Structure

```
backend/
├── assets/             # Frontend assets (CSS, JS, images)
├── bin/                # Compiled binaries
├── cmd/                # Command-line applications
│   └── migrate/        # Database migration tool
├── components/         # Vendored TemplUI components for the web interface
├── docs/               # Documentation files
├── internal/           # Internal packages
│   ├── app/            # Application logic
│   │   └── stats/      # Stats processing and analysis
│   ├── clients/        # External API clients (Auth, Twitch)
│   ├── config/         # Configuration management
│   ├── db/             # Database access and models
│   │   └── migrations/ # Database migration files
│   ├── model/          # Data models
│   └── ui/             # Web UI implementation
│       ├── components/ # UI components
│       ├── layout/     # Page layouts
│       └── pages/      # Page implementations
└── utils/              # Utility functions
```

## Technology Stack

- **Language**: Go
- **Database**: PostgreSQL
- **Web Framework**: Custom HTTP server
- **Frontend**: [templ](https://templ.guide/), [templui](https://templui.io/), [datatables](https://datatables.net/)
- **Authentication**: External auth service integration
- **Deployment**: Docker containerization

## Development Setup

### Prerequisites

- Go 1.24.4 or higher
- PostgreSQL 17 or higher
- Docker and Docker Compose (for containerized development)

### Environment Configuration

1. Copy the example environment file:
   ```
   cp .env.example .env
   ```

2. Edit the `.env` file with your local configuration settings:
   ```
   DATABASE_URL=postgres://postgres:postgres@localhost:5432/stats_tracker?sslmode=disable
   PORT=8090
   HOST=

   AUTH_API_URL=https://slay-the-relics.baalorlord.tv
   TWITCH_CLIENT_ID=your_twitch_client_id
   TWITCH_CLIENT_SECRET=your_twitch_client_secret

   LOG_LEVEL=info
   ENVIRONMENT=development
   ```

### Running Locally

1. Start the database:
   ```
   make db-start
   ```

2. Run database migrations:
   ```
   make migrate
   ```

3. Start the server:
   ```
   make run
   ```

   For development with hot reload:
   ```
   make dev
   ```

4. The server will be available at http://localhost:8090

### Using Docker Compose

To run the entire stack with Docker Compose:

```
docker-compose up -d
```

To stop the services:

```
docker-compose down
```

To reset the database (drop and recreate):

```
make db-reset
```

## API Documentation

The backend provides the following main API endpoints:

- `GET /api/health` - Health check endpoint
- `POST /api/v1/upload-all` - Upload all run data
- `GET /api/v1/increment` - Get increment data
- `GET /api/v1/players/search` - Search for players
- `GET /api/v1/players/{name}/stats` - Get stats for a specific player

## Database Migrations

Database migrations are managed using a custom migration tool:

```
make migrate
```

To roll back the last migration:

```
make migrate-down
```

To create a new migration:

1. Create 2 SQL files in `internal/db/migrations/`
2. Name it with a timestamp prefix: `$UNIX_TIME_STAMP_description.up.sql`, `$UNIX_TIME_STAMP_description.down.sql`
3. Add your SQL statements to the file
4. Run the migration tool using `make migrate`

## Testing

Run the test suite with:

```
make test
```

## Deployment

The application is containerized using Docker and can be deployed using the provided Dockerfile:

```
docker build -t stats-tracker-test .
docker run -p 8090:8090 --env-file .env stats-tracker-test
```

For production deployments, we use GitHub Actions to build and publish the Docker image.
