package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"log/slog"
	"os"
	"strconv"
	"time"

	"github.com/MaT1g3R/stats-tracker/internal/db"
)

func connectToDB(ctx context.Context, url string, logger *slog.Logger) *db.DB {
	const maxRetries = 5
	var database *db.DB
	var err error

	// Initial delay of 1 second, will double with each retry
	delay := time.Second

	for attempt := 1; attempt <= maxRetries; attempt++ {
		logger.Info("Attempting to connect to database", "attempt", attempt, "max_retries", maxRetries)

		database, err = db.New(ctx, url, logger)
		if err == nil {
			logger.Info("Successfully connected to database", "attempt", attempt)
			return database
		}

		if attempt == maxRetries {
			log.Fatal("Failed to connect to database after maximum retries:", err)
		}

		logger.Warn("Failed to connect to database, retrying...",
			"attempt", attempt,
			"next_retry_in", delay.String())

		// Wait before next attempt with exponential backoff
		time.Sleep(delay)

		// Double the delay for next attempt (exponential backoff)
		delay *= 2
	}

	// This should never be reached due to the log.Fatal above
	return database
}

func main() {
	var (
		databaseURL = flag.String("database-url", os.Getenv("DATABASE_URL"), "Database URL")
		cmd         = flag.String("cmd", "up", "Migration command: up, down, version, steps")
		steps       = flag.String("steps", "1", "Number of steps for down migration")
	)
	flag.Parse()

	if *databaseURL == "" {
		log.Fatal("DATABASE_URL is required")
	}

	ctx := context.Background()
	logger := slog.New(slog.NewTextHandler(os.Stdout, nil))
	slog.SetDefault(logger)

	database := connectToDB(ctx, *databaseURL, logger)
	defer database.Close()

	switch *cmd {
	case "up":
		if err := database.RunMigrations(); err != nil {
			log.Fatal("Migration failed:", err)
		}
		log.Println("Migrations completed successfully")

	case "down":
		stepCount, err := strconv.Atoi(*steps)
		if err != nil {
			log.Fatal("Invalid steps value:", err)
		}
		if err := database.MigrateDown(stepCount); err != nil {
			log.Fatal("Down migration failed:", err)
		}
		log.Printf("Successfully migrated down %d steps", stepCount)

	case "version":
		version, dirty, err := database.GetMigrationVersion()
		if err != nil {
			log.Fatal("Failed to get version:", err)
		}
		fmt.Printf("Current migration version: %d\n", version)
		fmt.Printf("Dirty state: %t\n", dirty)

	default:
		log.Fatal("Unknown command:", *cmd)
	}
}
