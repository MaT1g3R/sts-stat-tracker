package main

import (
	"context"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"github.com/go-co-op/gocron/v2"

	"github.com/MaT1g3R/stats-tracker/internal/clients"

	"github.com/MaT1g3R/stats-tracker/internal/app"

	"github.com/joho/godotenv"

	"github.com/MaT1g3R/stats-tracker/internal/config"
	"github.com/MaT1g3R/stats-tracker/internal/db"
)

//nolint:funlen
func main() {
	// Load .env file if it exists
	_ = godotenv.Load()

	// Setup structured logger
	logger := slog.New(slog.NewTextHandler(os.Stdout, nil))
	slog.SetDefault(logger)

	// Load configuration
	cfg, err := config.Load()
	if err != nil {
		slog.Error("Failed to load configuration", "error", err)
		os.Exit(1)
	}

	// Update log level from configuration
	logger = slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{
		Level: cfg.LogLevel,
	}))
	slog.SetDefault(logger)

	// Initialize database
	ctx := context.Background()
	database, err := db.NewWithConfig(ctx, cfg, logger)
	if err != nil {
		slog.Error("Failed to connect to database", "error", err)
		os.Exit(1)
	}
	defer database.Close()

	// Run database migrations
	if err := database.RunMigrations(); err != nil {
		slog.Error("Failed to run database migrations", "error", err)
		os.Exit(1)
	}

	scheduler, err := gocron.NewScheduler()
	if err != nil {
		slog.Error("Failed to start scheduler", "error", err)
		os.Exit(1)
	}
	_, err = database.RemoveOldStatsCache(scheduler)
	if err != nil {
		slog.Error("Failed to add cron job", "error", err)
		os.Exit(1)
	}
	scheduler.Start()

	authClient := clients.NewAuthClient(cfg.AuthAPIURL)
	twitchClient := clients.NewTwitchClient(cfg.TwitchClientID, cfg.TwitchClientSecret)
	api := app.NewApp(cfg, logger, database, authClient, twitchClient)
	server := api.Server

	// Start the server in a goroutine
	go func() {
		slog.Info("Starting server", "address", server.Addr, "environment", cfg.Environment)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("Server failed", "error", err)
			os.Exit(1)
		}
	}()

	// Graceful shutdown
	sig := make(chan os.Signal, 1)
	signal.Notify(sig, syscall.SIGINT, syscall.SIGTERM)
	s := <-sig

	slog.Info("Received signal, shutting down", "signal", s)

	shutdownCtx, cancel := context.WithTimeout(context.Background(), cfg.ShutdownTimeout)
	defer cancel()

	if err := server.Shutdown(shutdownCtx); err != nil {
		slog.Error("Server shutdown failed", "error", err)
	}
	if err := scheduler.Shutdown(); err != nil {
		slog.Error("Scheduler shutdown failed", "error", err)
	}

	slog.Info("Server stopped gracefully")
}
