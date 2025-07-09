# Project Guidelines for Slay the Spire Stats Tracker

## Project Overview
Slay the Spire Stats Tracker is a comprehensive statistics tracking tool for the game Slay the Spire. The project consists of two main components:

1. **Java Mod Component**: Integrates with the Slay the Spire game to collect run statistics, display them in-game, and optionally upload them to the backend server.
2. **Go Backend Component**: Provides API endpoints for storing and retrieving run statistics, user authentication, and leaderboard functionality.

## Project Structure
The project is organized into two main directories:

### Java Mod Component (`src/`)

Use `src/README.md` for more detailed instructions

### Go Backend Component (`backend/`)

Use `backend/README.md` for more detailed instructions

## Testing Guidelines
### Java Mod Component
When making changes to the Java mod component, you should:
1. Build the mod using `./gradlew buildJAR`
2. Test the mod in-game to ensure your changes work as expected
3. Verify that statistics are correctly collected and displayed
4. If your changes affect the backend communication, test the upload functionality

### Go Backend Component
When making changes to the Go backend component, you should:
1. Run the test suite with `make test`
2. Verify API endpoints using tools like curl or Postman
3. Check database migrations if you've made schema changes
4. Test the web UI if your changes affect it

## Code Style Guidelines
### Java Mod Component
- Follow standard Java naming conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused on a single responsibility
- Use proper exception handling

### Go Backend Component
- Follow standard Go code style (gofmt)
- Use `templui` components for UI as much as possible. Do not use the `templui` datepicker as it's broken
- Use `datatables` to display data in a table
- When editing templ components, run `make templ` to generate the Go code. The Makefile is `backend/Makefile`
- Use meaningful package and function names
- Add comments for exported functions
- Handle errors appropriately
- Use context for cancellation where appropriate

## Submission Guidelines
Before submitting changes:
1. Ensure all tests pass
2. Verify that your changes work as expected
3. Document any new features or changes in the appropriate README files
4. If your changes affect both components, test the integration between them
