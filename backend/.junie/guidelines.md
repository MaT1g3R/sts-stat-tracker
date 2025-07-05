# Stats Tracker Project Guidelines

## Overview
Stats Tracker is a web application for tracking and analyzing game statistics. It's built as a web service with a modern UI using Go on the backend and a combination of htmx, templ, and templui for the frontend.

## Technology Stack
- **Backend**: Go 1.24.4+
- **Database**: PostgreSQL 17
- **Frontend**:
  - [htmx](https://htmx.org/) - For dynamic UI updates without writing JavaScript
  - [templ](https://github.com/a-h/templ) - Type-safe HTML templates for Go
  - [templui](https://github.com/axzilla/templui) - UI component library for templ
  - [Tailwind CSS](https://tailwindcss.com/) - Utility-first CSS framework
  - [DataTables](https://datatables.net/) - Advanced tables with sorting, filtering, and pagination

## Project Structure
- `/assets` - Static assets (CSS, JavaScript, images)
- `/bin` - Compiled binaries
- `/cmd` - Command-line tools (e.g., migration tool)
- `/components` - templui UI components organized by type
- `/internal` - Internal application code:
  - `/app` - Application logic and API handlers
  - `/clients` - External API clients (Auth, Twitch)
  - `/config` - Application configuration
  - `/db` - Database access and migrations
  - `/model` - Data models
  - `/ui` - UI components specific to the application:
    - `/components` - Custom UI components
    - `/layout` - Page layouts
    - `/pages` - Page templates
- `/utils` - Utility functions

## Key Locations
- Database schema: `internal/db/migrations`
- templui components: `components/`
- Application UI components: `internal/ui/components`
- Page templates: `internal/ui/pages`
- Page layouts: `internal/ui/layout`

## Development Workflow

### Prerequisites
- Go 1.24.4 or later
- Docker and Docker Compose
- PostgreSQL 17

### Setup
1. Clone the repository
2. Copy `.env.example` to `.env` and adjust values if needed
3. Start the database: `make db-start`
4. Run migrations: `make migrate`

### Development Commands
- `make dev` - Start all development watchers (recommended for development)
  - Tailwind CSS watcher
  - Templ template watcher
  - Air live reload server
- `make templ` - Run templ generation in watch mode
- `make tailwind` - Watch Tailwind CSS changes
- `make server` - Run the server with hot reload
- `make generate` - Generate templ files and fix linting issues
- `make build` - Build the application
- `make run` - Build and run the application
- `make test` - Run tests

### Database Commands
- `make db-start` - Start the PostgreSQL database using Docker
- `make db-stop` - Stop the database
- `make db-reset` - Reset the database (drop and recreate)
- `make migrate` - Run all pending migrations
- `make migrate-down` - Roll back the most recent migration

### Creating Database Migrations
```bash
# Create empty migration files
touch internal/db/migrations/$(date +%s)_name_of_migration.up.sql
touch internal/db/migrations/$(date +%s)_name_of_migration.down.sql
```

### Code Quality
- `make lint` - Run linters
- `make lint-fix` - Auto-fix linting issues where possible

## UI Development

### Components
- Use the templui components from the `/components` directory when possible
- Create custom components in `/internal/ui/components` when needed
- Follow the existing component structure and naming conventions

### Pages
- Page templates are in `/internal/ui/pages`
- Use the layouts from `/internal/ui/layout`
- For data tables, use DataTables (https://datatables.net/)

### CSS
- Use Tailwind CSS utility classes
- Custom CSS should be minimal and placed in `/assets/css/input.css`

## Best Practices
- Use `make generate` to generate templ templates
- Use templui components as much as possible. Keep templui components and custom HTML have consistent styling
- Follow Go coding conventions and project structure
- Write tests for new functionality
- Use structured logging with slog
- Handle errors appropriately
- Implement graceful shutdown for services
- Document new features and changes
