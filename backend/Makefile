# Build variables
BUILD_DIR = ./bin
APP_NAME = stats-tracker

# Database variables
DB_NAME = stats_tracker
DB_USER = postgres
DB_PASSWORD = postgres
DB_HOST = localhost
DB_PORT = 5432
DATABASE_URL = postgres://$(DB_USER):$(DB_PASSWORD)@$(DB_HOST):$(DB_PORT)/$(DB_NAME)?sslmode=disable

.PHONY: build clean run test migrate migrate-down db-start db-stop db-reset templ tailwind dev lint lint-fix

# Run templ generation in watch mode
templ:
	go tool templ generate --watch --proxy="http://localhost:8090" --open-browser=false

generate:
	go tool templ fmt .
	go tool templ generate
	make lint-fix

# Run air for Go hot reload
server:
	go tool air \
    --build.cmd "go build -o tmp/bin/main ./main.go" \
    --build.bin "tmp/bin/main" \
    --build.delay "100" \
    --build.exclude_dir "node_modules" \
    --build.include_ext "go" \
    --build.stop_on_error "false" \
    --misc.clean_on_exit true

# Watch Tailwind CSS changes
tailwind:
	tailwindcss -i ./assets/css/input.css -o ./assets/css/output.css --watch

# Start development server with all watchers
dev:
	trap 'kill 0' INT; make -j3 tailwind templ server

# Run linters
lint:
	golangci-lint run ./...
	@echo "Running goimports check..."
	goimports -l -d $$(find . -type f -name '*.go' | grep -v /vendor/ | grep -v /third_party/)

# Auto-fix linting issues where possible
lint-fix:
	golangci-lint run --fix ./...
	@echo "Running goimports format..."
	goimports -l -w $$(find . -type f -name '*.go' | grep -v /vendor/ | grep -v /third_party/)

# Build the application
build:
	go build -o $(BUILD_DIR)/$(APP_NAME) ./main.go

# Clean build artifacts
clean:
	rm -rf $(BUILD_DIR)

# Run the application
run: build
	DATABASE_URL=$(DATABASE_URL) $(BUILD_DIR)/$(APP_NAME)

# Run tests
test:
	go test -v ./...

# Build migration tool
build-migrate:
	go build -o $(BUILD_DIR)/migrate ./cmd/migrate

# Run migrations up
migrate: build-migrate
	$(BUILD_DIR)/migrate -database-url "$(DATABASE_URL)" -cmd up

# Run migrations down
migrate-down: build-migrate
	$(BUILD_DIR)/migrate -database-url "$(DATABASE_URL)" -cmd down -steps "1"

# Start the database
db-start:
	docker-compose up -d postgres

# Stop the database
db-stop:
	docker-compose down

# Reset the database (drop and recreate)
db-reset:
	docker-compose down -v
	docker-compose up -d postgres
	sleep 3
	$(MAKE) migrate
