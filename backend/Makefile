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

.PHONY: build clean run test migrate migrate-down db-start db-stop db-reset templ tailwind dev lint lint-fix bundle-js generate

# Run templ generation in watch mode
templ:
	go tool templ fmt .
	go tool templ generate

bundle-js:
	cat js/* | esbuild --minify --bundle --outfile=assets/js/main.min.js

tailwind:
	tailwindcss -i ./assets/css/input.css -o ./assets/css/output.css

build: templ bundle-js tailwind
	go build -o $(BUILD_DIR)/$(APP_NAME) ./main.go

# Run air for Go hot reload
dev:
	go tool air \
    --build.cmd "make build" \
    --build.bin "bin/stats-tracker" \
    --build.delay "100" \
    --build.exclude_dir "node_modules" \
    --build.include_ext "go" \
    --build.stop_on_error "false" \
    --misc.clean_on_exit true

# Run linters
lint:
	golangci-lint run ./...
	@echo "Running goimports check..."
	goimports -l -d $$(find . -type f -name '*.go' | grep -v /vendor/ | grep -v /third_party/ | grep -v _templ.go)

# Auto-fix linting issues where possible
lint-fix:
	golangci-lint run --fix ./...
	@echo "Running goimports format..."
	goimports -l -w $$(find . -type f -name '*.go' | grep -v /vendor/ | grep -v /third_party/ | grep -v _templ.go)

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
