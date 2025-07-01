# Stats Tracker

A web application for tracking and analyzing game statistics.

## Prerequisites

- Go 1.24.4 or later
- Docker and Docker Compose
- PostgreSQL 17

## Setup

1. Clone the repository
2. Copy `.env.example` to `.env` and adjust values if needed
3. Start the database:
   ```
   make db-start
   ```
4. Run migrations:
   ```
   make migrate
   ```

## Development

Start the development server with all watchers:

```
make dev
```

This will start:
- Tailwind CSS watcher
- Templ template watcher
- Air live reload server

## Database Migrations

Migrations are managed using the golang-migrate library.

To create a new migration:

```bash
# Create empty migration files
touch internal/db/migrations/$(date +%s)_name_of_migration.up.sql
touch internal/db/migrations/$(date +%s)_name_of_migration.down.sql
```

To run migrations:

```bash
make migrate       # Run all pending migrations
make migrate-down  # Rollback the most recent migration
```

## Building

```bash
make build
```

The executable will be placed in the `bin` directory.

## Testing

```bash
make test
```

## Docker

The application includes a Docker Compose setup for PostgreSQL:

```bash
docker-compose up -d  # Start services
docker-compose down    # Stop services
```

## License

[MIT](LICENSE)
