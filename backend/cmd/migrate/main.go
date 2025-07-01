package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"os"
	"strconv"

	"github.com/MaT1g3R/stats-tracker/internal/db"
)

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
	database, err := db.New(ctx, *databaseURL)
	if err != nil {
		log.Fatal("Failed to connect to database:", err)
	}
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
