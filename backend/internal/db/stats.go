package db

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"github.com/MaT1g3R/stats-tracker/internal/app/stats"
	"github.com/jackc/pgx/v5"
)

type StatQuery struct {
	Player           string
	GameVersion      string
	Character        string
	Profile          string
	StartDate        time.Time
	EndDate          time.Time
	IncludeAbandoned bool
}

func (db *DB) QueryStats(ctx context.Context, kind string, query StatQuery) (stats.Stat, error) {
	var stat stats.Stat
	switch kind {
	case stats.StatTypes[0]:
		stat = stats.NewOverview(query.Character)
	case stats.StatTypes[1]:
		stat = stats.NewCardPicks()
	case stats.StatTypes[2]:
		stat = stats.NewCardWinRate()
	default:
		return stat, fmt.Errorf("unknown stat kind %s", kind)
	}

	// Using a constant query for better performance with prepared statements
	const statQuery = `
UPDATE run_statistics SET
last_accessed = now()
WHERE username = $1
AND profile_name = $2
AND character_name = $3
AND game_version = $4
AND include_abandoned = $5
AND stat_kind = $6
AND period_start = $7
AND period_end = $8
RETURNING stats;
`
	// Create a timeout context for the database query
	queryCtx, cancel := context.WithTimeout(ctx, 5*time.Second)
	defer cancel()

	var bs []byte
	err := db.Pool.QueryRow(
		queryCtx,
		statQuery,
		query.Player,
		query.Profile,
		query.Character,
		query.GameVersion,
		query.IncludeAbandoned,
		kind,
		query.StartDate,
		query.EndDate,
	).Scan(&bs)
	if errors.Is(err, pgx.ErrNoRows) {
		return db.calculateStats(ctx, query, stat)
	}
	if err != nil {
		return stat, fmt.Errorf("failed to query stats: %w", err)
	}
	if err := json.Unmarshal(bs, &stat); err != nil {
		return stat, fmt.Errorf("failed to unmarshal stats: %w", err)
	}
	db.logger.Info("querying stats", "query", query, "cache_hit", true)
	return stat, nil
}

//nolint:funlen
func (db *DB) calculateStats(ctx context.Context, query StatQuery, stat stats.Stat) (stats.Stat, error) {
	// Create a timeout context for the database operation
	txCtx, cancel := context.WithTimeout(ctx, 30*time.Second)
	defer cancel()

	tx, err := db.Pool.Begin(txCtx)
	if err != nil {
		return nil, fmt.Errorf("failed to start transaction: %w", err)
	}
	defer func(tx pgx.Tx, ctx context.Context) {
		_ = tx.Rollback(ctx)
	}(tx, ctx)
	runs, err := db.QueryRuns(ctx, tx,
		query.Player,
		query.GameVersion,
		query.Character,
		query.Profile,
		query.StartDate,
		query.EndDate,
		query.IncludeAbandoned,
	)
	if err != nil {
		return nil, fmt.Errorf("failed to query runs: %w", err)
	}
	for _, run := range runs {
		stat.CollectRun(&run)
	}
	stat.Finalize()

	const insertQuery = `
INSERT INTO run_statistics (
	username,
	profile_name,
	character_name,
	game_version,
	include_abandoned,
	stat_kind,
	period_start,
	period_end,
	stats,
	last_calculated,
	last_accessed
) VALUES (
	$1::text,
	$2::text,
	$3::text,
	$4::text,
	$5::boolean,
	$6::text,
	$7::timestamp,
	$8::timestamp,
	$9::jsonb,
	now(),
	now()
) ON CONFLICT (
	username,
	profile_name,
	character_name,
	game_version,
	include_abandoned,
	stat_kind,
	period_start,
	period_end
) DO UPDATE SET
	stats = EXCLUDED.stats,
	last_calculated = EXCLUDED.last_calculated,
	last_accessed = EXCLUDED.last_accessed
`
	// Use prepared statement with timeout context
	execCtx, execCancel := context.WithTimeout(ctx, 10*time.Second)
	defer execCancel()

	_, err = tx.Exec(
		execCtx,
		insertQuery,
		query.Player,
		query.Profile,
		query.Character,
		query.GameVersion,
		query.IncludeAbandoned,
		stat.Name(),
		query.StartDate,
		query.EndDate,
		stat,
	)
	if err != nil {
		return nil, fmt.Errorf("failed to insert stats: %w", err)
	}
	err = tx.Commit(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to commit transaction: %w", err)
	}

	db.logger.Info("querying stats", "query", query, "cache_hit", false)
	return stat, nil
}
