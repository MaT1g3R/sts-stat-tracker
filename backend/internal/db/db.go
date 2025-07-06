package db

import (
	"context"
	"database/sql"
	"fmt"
	"log/slog"

	"github.com/MaT1g3R/stats-tracker/internal/config"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jackc/pgx/v5/stdlib"
)

type DB struct {
	logger *slog.Logger
	Pool   *pgxpool.Pool
	SQL    *sql.DB // For migrations only
}

// NewWithConfig initializes the database using configuration
func NewWithConfig(ctx context.Context, cfg *config.Config, logger *slog.Logger) (*DB, error) {
	db, err := New(ctx, cfg.DatabaseURL, logger)
	if err != nil {
		return nil, err
	}

	// Apply connection pool settings
	db.SQL.SetMaxOpenConns(cfg.DatabaseMaxOpenConns)
	db.SQL.SetMaxIdleConns(cfg.DatabaseMaxIdleConns)
	db.SQL.SetConnMaxLifetime(cfg.DatabaseConnLifetime)

	return db, nil
}

func New(ctx context.Context, databaseURL string, logger *slog.Logger) (*DB, error) {
	// Create pgxpool for application use
	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		return nil, fmt.Errorf("failed to create connection pool: %w", err)
	}

	// Test the connection
	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		return nil, fmt.Errorf("failed to ping database: %w", err)
	}

	// Create database/sql connection for migrations
	// This uses the same connection string but through database/sql interface
	sqlDB := stdlib.OpenDB(*pool.Config().ConnConfig)

	return &DB{
		logger: logger,
		Pool:   pool,
		SQL:    sqlDB,
	}, nil
}

func (db *DB) Close() {
	if db.Pool != nil {
		db.Pool.Close()
	}
	if db.SQL != nil {
		_ = db.SQL.Close()
	}
}

// Health check using pgx
func (db *DB) Health(ctx context.Context) error {
	return db.Pool.Ping(ctx)
}

// mapPlayerClassToCharacter maps the player class from the run data to database character names
func mapPlayerClassToCharacter(playerClass string) (string, error) {
	switch playerClass {
	case "IRONCLAD":
		return "ironclad", nil
	case "THE_SILENT":
		return "silent", nil
	case "DEFECT":
		return "defect", nil
	case "WATCHER":
		return "watcher", nil
	default:
		return "", fmt.Errorf("unknown player class: %s", playerClass)
	}
}
