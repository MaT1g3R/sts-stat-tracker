package db

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"github.com/MaT1g3R/stats-tracker/internal/model"
	"github.com/jackc/pgx/v5"
)

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
		r.RunTimestamp.UTC(),
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

func (db *DB) GetFirstAndLastRun(ctx context.Context, user string, gameVersion string) (RunRow, RunRow, error) {
	var firstRun, lastRun RunRow

	// Get first run (earliest timestamp)
	err := db.Pool.QueryRow(ctx, `
		SELECT username, profile_name, run_timestamp, character_name,
			victory, abandoned, score, floor_reached, playtime_minutes,
			game_version, data_schema_version, run_data
		FROM runs
		WHERE username = $1 AND game_version = $2
		ORDER BY run_timestamp ASC
		LIMIT 1
	`, user, gameVersion).Scan(
		&firstRun.Username, &firstRun.ProfileName, &firstRun.RunTimestamp,
		&firstRun.CharacterName, &firstRun.Victory, &firstRun.Abandoned,
		&firstRun.Score, &firstRun.FloorReached, &firstRun.PlaytimeMinutes,
		&firstRun.GameVersion, &firstRun.DataSchemaVersion, &firstRun.RunData,
	)

	if errors.Is(err, pgx.ErrNoRows) {
		return RunRow{}, RunRow{}, fmt.Errorf("no runs found for user %s", user)
	}
	if err != nil {
		return RunRow{}, RunRow{}, fmt.Errorf("failed to get first run: %w", err)
	}

	// Get last run (latest timestamp)
	err = db.Pool.QueryRow(ctx, `
		SELECT username, profile_name, run_timestamp, character_name,
			victory, abandoned, score, floor_reached, playtime_minutes,
			game_version, data_schema_version, run_data
		FROM runs
		WHERE username = $1 AND game_version = $2
		ORDER BY run_timestamp DESC
		LIMIT 1
	`, user, gameVersion).Scan(
		&lastRun.Username, &lastRun.ProfileName, &lastRun.RunTimestamp,
		&lastRun.CharacterName, &lastRun.Victory, &lastRun.Abandoned,
		&lastRun.Score, &lastRun.FloorReached, &lastRun.PlaytimeMinutes,
		&lastRun.GameVersion, &lastRun.DataSchemaVersion, &lastRun.RunData,
	)

	if err != nil {
		return RunRow{}, RunRow{}, fmt.Errorf("failed to get last run: %w", err)
	}

	return firstRun, lastRun, nil
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
		if err := tx.Rollback(ctx); err != nil && !errors.Is(err, pgx.ErrTxClosed) {
			db.logger.Warn("failed to rollback transaction", "error", err)
		}
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

	// Delete cached run statistics for the user
	deleteResult, err := tx.Exec(ctx, `
	DELETE FROM run_statistics
	WHERE username = $1
	AND profile_name = $2
	AND game_version = $3
`, user, profile, gameVersion)
	if err != nil {
		return fmt.Errorf("failed to delete cached run statistics: %w", err)
	}
	deletedRows := deleteResult.RowsAffected()
	db.logger.Info("deleted cached run statistics", "rows_affected", deletedRows)

	// Commit the transaction
	if err := tx.Commit(ctx); err != nil {
		return fmt.Errorf("failed to commit transaction: %w", err)
	}

	return nil
}

//nolint:funlen
func (db *DB) BatchInsertRuns(ctx context.Context,
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
		if err := tx.Rollback(ctx); err != nil && !errors.Is(err, pgx.ErrTxClosed) {
			db.logger.Warn("failed to rollback transaction", "error", err)
		}
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

	// Calculate the date range of all runs
	runRows := db.convertRunsToRows(user, profile, gameVersion, schemaVersion, runs)
	if len(runRows) == 0 {
		return nil // No valid runs to insert
	}

	var minDate, maxDate time.Time
	for i, row := range runRows {
		if i == 0 {
			minDate = row.RunTimestamp
			maxDate = row.RunTimestamp
		} else {
			if row.RunTimestamp.Before(minDate) {
				minDate = row.RunTimestamp
			}
			if row.RunTimestamp.After(maxDate) {
				maxDate = row.RunTimestamp
			}
		}
	}

	batch := &pgx.Batch{}
	const insertSQL = `
INSERT INTO runs (
	username, profile_name, run_timestamp, character_name,
	victory, abandoned, score, floor_reached, playtime_minutes,
	game_version, data_schema_version, run_data
) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
ON CONFLICT (username, profile_name, run_timestamp) DO UPDATE SET
character_name = EXCLUDED.character_name,
victory = EXCLUDED.victory,
abandoned = EXCLUDED.abandoned,
score = EXCLUDED.score,
floor_reached = EXCLUDED.floor_reached,
playtime_minutes = EXCLUDED.playtime_minutes,
game_version = EXCLUDED.game_version,
data_schema_version = EXCLUDED.data_schema_version,
run_data = EXCLUDED.run_data
`

	// Queue all inserts
	for _, row := range runRows {
		batch.Queue(insertSQL, row.ToSlice()...)
	}

	// Single targeted cache deletion for the date range
	const deleteCacheSQL = `
DELETE FROM run_statistics
WHERE username = $1
AND profile_name = $2
AND game_version = $3
AND (period_start <= $4 AND period_end >= $5)
`
	batch.Queue(deleteCacheSQL, user, profile, gameVersion, maxDate, minDate)

	br := tx.SendBatch(ctx, batch)
	ct, err := br.Exec()
	if err != nil {
		return fmt.Errorf("failed to insert runs: %w", err)
	}
	db.logger.Info("inserted runs", "runs", len(runs), "rows_affected", ct.RowsAffected())

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
		runTime := time.Unix(int64(run.Timestamp), 0).UTC()

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

//nolint:funlen
func (db *DB) QueryRuns(ctx context.Context, tx pgx.Tx,
	user, gameVersion, character, profile string,
	startDate, endDate time.Time, includeAbandoned bool) (_ []model.Run, err error) {
	query := `
		SELECT run_data
		FROM runs
		WHERE username = $1
		AND game_version = $2
		AND run_timestamp BETWEEN $3 AND $4
	`

	startDate = time.Date(startDate.Year(), startDate.Month(), startDate.Day(), 0, 0, 0, 0, time.UTC)
	endDate = time.Date(endDate.Year(), endDate.Month(), endDate.Day(), 23, 59, 59, 0, time.UTC)

	args := []interface{}{user, gameVersion, startDate, endDate}
	argCount := 4

	if character != "all" {
		argCount++
		query += fmt.Sprintf(" AND character_name = $%d", argCount)
		args = append(args, character)
	}

	if profile != "" {
		argCount++
		query += fmt.Sprintf(" AND profile_name = $%d", argCount)
		args = append(args, profile)
	}

	if !includeAbandoned {
		query += " AND NOT abandoned"
	}

	query += " ORDER BY run_timestamp ASC"

	var rows pgx.Rows
	if tx != nil {
		rows, err = tx.Query(ctx, query, args...)
	} else {
		rows, err = db.Pool.Query(ctx, query, args...)
	}
	if err != nil {
		return nil, fmt.Errorf("failed to query runs: %w", err)
	}
	defer rows.Close()

	var runs []model.Run
	for rows.Next() {
		var runData string
		if err := rows.Scan(&runData); err != nil {
			return nil, fmt.Errorf("failed to scan run row: %w", err)
		}

		var run model.Run
		if err := json.Unmarshal([]byte(runData), &run); err != nil {
			return nil, fmt.Errorf("failed to unmarshal run data: %w", err)
		}

		runs = append(runs, run)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating run rows: %w", err)
	}

	return runs, nil
}

func (db *DB) QueryIncrement(ctx context.Context,
	user, profile, gameVersion string, schemaVersion int) (*time.Time, []time.Time, error) {
	// Start transaction
	tx, err := db.Pool.Begin(ctx)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer func() {
		if err := tx.Rollback(ctx); err != nil && !errors.Is(err, pgx.ErrTxClosed) {
			db.logger.Warn("failed to rollback transaction", "error", err)
		}
	}()

	// Query for the timestamp of the last run matching the exact schemaVersion
	var lastMatchingTimestamp *time.Time
	err = tx.QueryRow(ctx, `
		SELECT run_timestamp
		FROM runs
		WHERE username = $1 
		AND profile_name = $2 
		AND game_version = $3 
		AND data_schema_version = $4
		ORDER BY run_timestamp DESC
		LIMIT 1
	`, user, profile, gameVersion, schemaVersion).Scan(&lastMatchingTimestamp)

	if err != nil && !errors.Is(err, pgx.ErrNoRows) {
		return nil, nil, fmt.Errorf("failed to query last matching run: %w", err)
	}

	// Query for all timestamps of runs with schemaVersion less than the input
	rows, err := tx.Query(ctx, `
		SELECT run_timestamp
		FROM runs
		WHERE username = $1 
		AND profile_name = $2 
		AND game_version = $3 
		AND data_schema_version < $4
		ORDER BY run_timestamp ASC
	`, user, profile, gameVersion, schemaVersion)

	if err != nil {
		return nil, nil, fmt.Errorf("failed to query runs with lesser schema version: %w", err)
	}
	defer rows.Close()

	var lesserTimestamps []time.Time
	for rows.Next() {
		var timestamp time.Time
		if err := rows.Scan(&timestamp); err != nil {
			return nil, nil, fmt.Errorf("failed to scan timestamp: %w", err)
		}
		lesserTimestamps = append(lesserTimestamps, timestamp)
	}

	if err := rows.Err(); err != nil {
		return nil, nil, fmt.Errorf("error iterating timestamp rows: %w", err)
	}

	// Commit the transaction
	if err := tx.Commit(ctx); err != nil {
		return nil, nil, fmt.Errorf("failed to commit transaction: %w", err)
	}

	return lastMatchingTimestamp, lesserTimestamps, nil
}
