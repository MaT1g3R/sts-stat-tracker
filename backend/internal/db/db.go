package db

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"log/slog"
	"time"

	"github.com/MaT1g3R/stats-tracker/internal/model"
	"github.com/jackc/pgx/v5"

	"github.com/MaT1g3R/stats-tracker/internal/config"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jackc/pgx/v5/stdlib"
)

type DB struct {
	logger *slog.Logger
	Pool   *pgxpool.Pool
	SQL    *sql.DB // For migrations only
}

// RunRow represents a database row for the runs table
type RunRow struct {
	Username          string
	ProfileName       string
	RunTimestamp      time.Time
	CharacterName     string
	Victory           bool
	Abandoned         bool
	Score             int
	FloorReached      int
	PlaytimeMinutes   int
	GameVersion       string
	DataSchemaVersion int
	RunData           string
}

// ToSlice converts RunRow to a slice of interfaces for bulk operations
func (r RunRow) ToSlice() []any {
	return []any{
		r.Username,
		r.ProfileName,
		r.RunTimestamp,
		r.CharacterName,
		r.Victory,
		r.Abandoned,
		r.Score,
		r.FloorReached,
		r.PlaytimeMinutes,
		r.GameVersion,
		r.DataSchemaVersion,
		r.RunData,
	}
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

func (db *DB) GetUser(ctx context.Context, name string) (model.User, error) {
	var user model.User
	err := db.Pool.QueryRow(ctx, `
		SELECT username, created_at, last_seen
		FROM users
		WHERE username = $1
	`, name).Scan(&user.Username, &user.CreatedAt, &user.LastSeenAt)

	if errors.Is(err, pgx.ErrNoRows) {
		return model.User{}, fmt.Errorf("%w: user not found: %s", err, name)
	}
	if err != nil {
		return model.User{}, fmt.Errorf("failed to get user: %w", err)
	}

	return user, nil
}

func (db *DB) CreateOrGetUser(ctx context.Context, username string) (model.User, error) {
	var user model.User
	now := time.Now()

	err := db.Pool.QueryRow(ctx, `
		INSERT INTO users (username, created_at, last_seen)
		VALUES ($1, $2, $2)
		ON CONFLICT (username) DO UPDATE
		SET last_seen = $2
		RETURNING username, created_at, last_seen
	`, username, now).Scan(&user.Username, &user.CreatedAt, &user.LastSeenAt)

	if err != nil {
		return model.User{}, fmt.Errorf("failed to create or get user: %w", err)
	}

	return user, nil
}

//nolint:funlen
func (db *DB) ImportRuns(ctx context.Context,
	user, profile, gameVersion string, schemaVersion int, runs []model.Run) error {

	if len(runs) == 0 {
		return nil
	}

	// Start transaction
	tx, err := db.Pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer func() {
		_ = tx.Rollback(ctx)
	}()

	// Create or ensure profile exists
	_, err = tx.Exec(ctx, `
		INSERT INTO profiles (username, profile_name)
		VALUES ($1, $2)
		ON CONFLICT (username, profile_name) DO NOTHING
	`, user, profile)
	if err != nil {
		return fmt.Errorf("failed to create profile: %w", err)
	}

	// Create a temporary table for the bulk data
	tempTableQuery := `
		CREATE TEMP TABLE temp_runs (
			username TEXT,
			profile_name TEXT,
			run_timestamp TIMESTAMP WITH TIME ZONE,
			character_name TEXT,
			victory BOOLEAN,
			abandoned BOOLEAN,
			score INTEGER,
			floor_reached INTEGER,
			playtime_minutes INTEGER,
			game_version TEXT,
			data_schema_version INTEGER,
			run_data JSONB
		)
	`

	_, err = tx.Exec(ctx, tempTableQuery)
	if err != nil {
		return fmt.Errorf("failed to create temp table: %w", err)
	}

	// Convert model.Run to RunRow structs
	runRows := db.convertRunsToRows(user, profile, gameVersion, schemaVersion, runs)
	rows := make([][]any, len(runRows))
	for i, runRow := range runRows {
		rows[i] = runRow.ToSlice()
	}

	// Use CopyFrom for efficient bulk insert into temp table
	copyCount, err := tx.CopyFrom(ctx,
		pgx.Identifier{"temp_runs"},
		[]string{
			"username", "profile_name", "run_timestamp", "character_name",
			"victory", "abandoned", "score", "floor_reached", "playtime_minutes",
			"game_version", "data_schema_version", "run_data",
		},
		pgx.CopyFromRows(rows),
	)
	if err != nil {
		return fmt.Errorf("failed to copy data to temp table: %w", err)
	}

	db.logger.Info("bulk inserted runs into temp table", "count", copyCount)

	// Insert from temp table to main table with conflict resolution
	upsertQuery := `
		INSERT INTO runs (
			username, profile_name, run_timestamp, character_name,
			victory, abandoned, score, floor_reached, playtime_minutes,
			game_version, data_schema_version, run_data
		)
		SELECT 
			username, profile_name, run_timestamp, character_name,
			victory, abandoned, score, floor_reached, playtime_minutes,
			game_version, data_schema_version, run_data::jsonb
		FROM temp_runs
		ON CONFLICT (username, profile_name, run_timestamp) DO UPDATE SET
			character_name = EXCLUDED.character_name,
			victory = EXCLUDED.victory,
			abandoned = EXCLUDED.abandoned,
			score = EXCLUDED.score,
			floor_reached = EXCLUDED.floor_reached,
			playtime_minutes = EXCLUDED.playtime_minutes,
			game_version = EXCLUDED.game_version,
			data_schema_version = EXCLUDED.data_schema_version,
			run_data = EXCLUDED.run_data,
			updated_at = CURRENT_TIMESTAMP
	`

	result, err := tx.Exec(ctx, upsertQuery)
	if err != nil {
		return fmt.Errorf("failed to upsert runs from temp table: %w", err)
	}

	rowsAffected := result.RowsAffected()
	db.logger.Info("upserted runs", "rows_affected", rowsAffected)

	// Commit the transaction
	if err := tx.Commit(ctx); err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}

	return nil
}

// convertRunsToRows converts model.Run slice to RunRow slice
func (db *DB) convertRunsToRows(user, profile, gameVersion string, schemaVersion int, runs []model.Run) []RunRow {
	runRows := make([]RunRow, 0, len(runs))

	for _, run := range runs {
		// Convert timestamp to time.Time
		runTime := time.Unix(int64(run.Timestamp), 0)

		// Determine character name from player class
		characterName, err := mapPlayerClassToCharacter(run.PlayerClass)
		if err != nil {
			db.logger.Warn("failed to map player class to character", "error", err, "player_class", run.PlayerClass)
			continue
		}

		// Marshal the entire run data to JSON
		runData, err := json.Marshal(run)
		if err != nil {
			db.logger.Warn("failed to marshal run data", "error", err, "run", run)
			continue
		}

		runRow := RunRow{
			Username:          user,
			ProfileName:       profile,
			RunTimestamp:      runTime,
			CharacterName:     characterName,
			Victory:           run.IsHeartKill,
			Abandoned:         run.Abandoned,
			Score:             run.Score,
			FloorReached:      run.FloorsReached,
			PlaytimeMinutes:   run.PlayTimeMinutes,
			GameVersion:       gameVersion,
			DataSchemaVersion: schemaVersion,
			RunData:           string(runData),
		}

		runRows = append(runRows, runRow)
	}

	return runRows
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
