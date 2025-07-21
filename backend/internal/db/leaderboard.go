package db

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

const PageSize = 10

func (db *DB) InsertLeaderboardEntry(
	ctx context.Context,
	tx pgx.Tx,
	user string,
	kind model.LeaderboardKind,
	entry model.LeaderboardEntry,
) (err error) {
	month := time.Date(entry.Date.Year(), entry.Date.Month(), 1, 0, 0, 0, 0, time.UTC)
	exec := db.Pool.Exec
	if tx != nil {
		exec = tx.Exec
	}
	switch kind.Value {
	case "speedrun":
		const query = `
INSERT INTO leaderboard VALUES ($1, $2, $3, $4, $5) ON CONFLICT (username, character, kind) DO UPDATE SET
	date_achieved = CASE WHEN leaderboard.score > EXCLUDED.score
		THEN EXCLUDED.date_achieved
		ELSE leaderboard.date_achieved END,
    score = CASE WHEN leaderboard.score > EXCLUDED.score
		THEN EXCLUDED.score
		ELSE leaderboard.score END
`
		_, err = exec(ctx, query, user, entry.Character, kind.Value, entry.Date, entry.Score)
	case "streak":
		const query = `
INSERT INTO leaderboard VALUES ($1, $2, $3, $4, $5) ON CONFLICT (username, character, kind) DO UPDATE SET
    date_achieved = CASE WHEN leaderboard.score < EXCLUDED.score
		THEN EXCLUDED.date_achieved
		ELSE leaderboard.date_achieved END,
    score = CASE WHEN leaderboard.score < EXCLUDED.score
		THEN EXCLUDED.score
		ELSE leaderboard.score END
`
		_, err = exec(ctx, query, user, entry.Character, kind.Value, entry.Date, entry.Score)
	case "winrate-monthly":
		const query = `
INSERT INTO monthly_leaderboard VALUES ($1, $2, $3, $4, $5, $6)
ON CONFLICT (username, character, kind, month) DO UPDATE SET
    date_achieved = EXCLUDED.date_achieved,
    score = EXCLUDED.score
`
		_, err = exec(ctx, query, user, entry.Character, kind.Value, month, entry.Date, entry.Score)
	default:
		return fmt.Errorf("unknown leaderboard kind: %s", kind.Value)
	}
	if err != nil {
		return fmt.Errorf("failed to insert leaderboard entry: %w", err)
	}
	db.logger.Info("Inserted leaderboard entry", "user", user, "kind", kind.Value)
	return nil
}

func (db *DB) InsertLeaderboardEntries(ctx context.Context,
	user string, kinds []model.LeaderboardKind, entries []model.LeaderboardEntry) (err error) {
	if len(kinds) != len(entries) {
		return fmt.Errorf("number of leaderboard entries does not match number of kinds")
	}
	if len(entries) == 0 {
		return nil
	}
	tx, err := db.Pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("failed to start transaction: %w", err)
	}

	defer func() {
		if e := tx.Rollback(ctx); e != nil && !errors.Is(e, pgx.ErrTxClosed) {
			db.logger.Warn("failed to rollback transaction", "error", e)
			err = errors.Join(err, e)
		}
	}()

	for i, entry := range entries {
		kind := kinds[i]
		if err = db.InsertLeaderboardEntry(ctx, tx, user, kind, entry); err != nil {
			return fmt.Errorf("failed to insert leaderboard entry: %w", err)
		}
	}

	if e := tx.Commit(ctx); e != nil {
		db.logger.Error("failed to commit transaction", "error", e)
		err = errors.Join(err, fmt.Errorf("failed to commit transaction: %w", e))
	}

	return err
}

type LeaderboardQueryResults struct {
	Pages   int
	Months  []time.Time
	Entries []model.LeaderboardEntry
}

func pagesQuery(ctx context.Context, tx pgx.Tx, kind, character string, allIsCharacter bool) (res int, err error) {
	if allIsCharacter || character != "all" {
		const query = `SELECT 1 + (count(*) / $1) FROM leaderboard WHERE kind = $2 AND character = $3`
		row := tx.QueryRow(ctx, query, PageSize, kind, character)
		if err = row.Scan(&res); err != nil {
			return 0, err
		}
	} else {
		const query = `SELECT 1 + (count(*) / $1) FROM leaderboard WHERE kind = $2`
		row := tx.QueryRow(ctx, query, PageSize, kind)
		if err = row.Scan(&res); err != nil {
			return 0, err
		}
	}
	return res, nil
}

func monthlyPagesQuery(ctx context.Context,
	tx pgx.Tx, kind, character string, month time.Time, allIsCharacter bool) (res int, err error) {
	if allIsCharacter || character != "all" {
		const query = `SELECT 1 + (count(*) / $1) FROM monthly_leaderboard WHERE kind = $2 AND character = $3 AND month = $4`
		row := tx.QueryRow(ctx, query, PageSize, kind, character, month)
		if err = row.Scan(&res); err != nil {
			return 0, err
		}
	} else {
		const query = `SELECT 1 + (count(*) / $1) FROM monthly_leaderboard WHERE kind = $2 AND month = $3`
		row := tx.QueryRow(ctx, query, PageSize, kind, month)
		if err = row.Scan(&res); err != nil {
			return 0, err
		}
	}
	return res, nil
}

func monthsQuery(ctx context.Context,
	tx pgx.Tx, kind, character string, allIsCharacter bool) (res []time.Time, err error) {
	var rows pgx.Rows
	if allIsCharacter || character != "all" {
		const query = `SELECT DISTINCT month from monthly_leaderboard WHERE kind = $1 AND character = $2 ORDER BY month DESC`
		rows, err = tx.Query(ctx, query, kind, character)
		defer rows.Close()
	} else {
		const query = `SELECT DISTINCT month from monthly_leaderboard WHERE kind = $1 ORDER BY month DESC`
		rows, err = tx.Query(ctx, query, kind)
		defer rows.Close()
	}
	if errors.Is(err, pgx.ErrNoRows) {
		return nil, nil
	}
	if err != nil {
		return nil, fmt.Errorf("failed to query leaderboard months: %w", err)
	}
	for rows.Next() {
		var month time.Time
		if err = rows.Scan(&month); err != nil {
			return nil, fmt.Errorf("failed to scan leaderboard months: %w", err)
		}
		res = append(res, month)
	}
	return res, err
}

func queryStreak(ctx context.Context, tx pgx.Tx, character string, page int) (int, []model.LeaderboardEntry, error) {
	pages, err := pagesQuery(ctx, tx, "streak", character, true)
	if err != nil {
		return 0, nil, err
	}

	const query = `
SELECT username, character, date_achieved, score FROM leaderboard
WHERE character = $1 AND kind = 'streak'
ORDER BY score DESC, date_achieved, username LIMIT $2 OFFSET $3
`
	rows, err := tx.Query(ctx, query, character, PageSize, (page-1)*PageSize)
	defer rows.Close()
	if err != nil {
		return 0, nil, fmt.Errorf("failed to query streak leaderboard: %w", err)
	}
	var entries []model.LeaderboardEntry
	for rows.Next() {
		entry := model.LeaderboardEntry{}
		err := rows.Scan(&entry.PlayerName, &entry.Character, &entry.Date, &entry.Score)
		if err != nil {
			return 0, nil, fmt.Errorf("failed to scan leaderboard entry: %w", err)
		}
		entries = append(entries, entry)
	}
	return pages, entries, nil
}

func querySpeedrun(ctx context.Context,
	tx pgx.Tx, character string, page int) (_ int, _ []model.LeaderboardEntry, err error) {
	pages, err := pagesQuery(ctx, tx, "speedrun", character, false)
	if err != nil {
		return 0, nil, err
	}
	var rows pgx.Rows
	if character == "all" {
		const query = `
SELECT username, character, date_achieved, score FROM leaderboard
WHERE kind = 'speedrun'
ORDER BY score,	date_achieved, username
LIMIT $1 OFFSET $2
`
		rows, err = tx.Query(ctx, query, PageSize, (page-1)*PageSize)
	} else {
		const query = `
SELECT username, character, date_achieved, score FROM leaderboard
WHERE character = $1 AND kind = 'speedrun'
ORDER BY score, date_achieved, username LIMIT $2 OFFSET $3
`
		rows, err = tx.Query(ctx, query, character, PageSize, (page-1)*PageSize)
	}
	defer rows.Close()
	if err != nil {
		return 0, nil, fmt.Errorf("failed to query speedrun leaderboard: %w", err)
	}
	var entries []model.LeaderboardEntry
	for rows.Next() {
		entry := model.LeaderboardEntry{}
		err := rows.Scan(&entry.PlayerName, &entry.Character, &entry.Date, &entry.Score)
		if err != nil {
			return 0, nil, fmt.Errorf("failed to scan leaderboard entry: %w", err)
		}
		entries = append(entries, entry)
	}
	return pages, entries, nil
}

func queryMonthlyWinRate(ctx context.Context,
	tx pgx.Tx, character string, page int, month time.Time) (int, []model.LeaderboardEntry, error) {
	pages, err := monthlyPagesQuery(ctx, tx, "winrate-monthly", character, month, true)
	if err != nil {
		return 0, nil, err
	}
	const query = `
SELECT username, character, date_achieved, score FROM monthly_leaderboard
WHERE character = $1 AND kind = 'winrate-monthly' AND month = $2
ORDER BY score DESC, date_achieved, username LIMIT $3 OFFSET $4
`
	rows, err := tx.Query(ctx, query, character, month, PageSize, (page-1)*PageSize)
	defer rows.Close()
	if err != nil {
		return 0, nil, fmt.Errorf("failed to query monthly winrate leaderboard: %w", err)
	}
	var entries []model.LeaderboardEntry
	for rows.Next() {
		entry := model.LeaderboardEntry{}
		err := rows.Scan(&entry.PlayerName, &entry.Character, &entry.Date, &entry.Score)
		if err != nil {
			return 0, nil, fmt.Errorf("failed to scan leaderboard entry: %w", err)
		}
		entries = append(entries, entry)
	}
	return pages, entries, nil
}

func (db *DB) QueryLeaderboard(ctx context.Context,
	kind model.LeaderboardKind, character string, page int, month *time.Time) (res LeaderboardQueryResults, err error) {
	tx, err := db.Pool.Begin(ctx)
	if err != nil {
		return res, fmt.Errorf("failed to start transaction: %w", err)
	}
	selectedMonth := month

	defer func() {
		if e := tx.Rollback(ctx); e != nil && !errors.Is(e, pgx.ErrTxClosed) {
			db.logger.Warn("failed to rollback transaction", "error", e)
			err = errors.Join(err, e)
		}
	}()

	res = LeaderboardQueryResults{}
	switch kind.Value {
	case "speedrun":
		res.Pages, res.Entries, err = querySpeedrun(ctx, tx, character, page)
	case "streak":
		res.Pages, res.Entries, err = queryStreak(ctx, tx, character, page)
	case "winrate-monthly":
		res.Months, err = monthsQuery(ctx, tx, kind.Value, character, true)
		if err != nil {
			return res, fmt.Errorf("failed to query monthly winrate leaderboard: %w", err)
		}
		if len(res.Months) == 0 {
			return LeaderboardQueryResults{}, nil
		}
		if selectedMonth == nil {
			selectedMonth = &res.Months[0]
		}
		res.Pages, res.Entries, err = queryMonthlyWinRate(ctx, tx, character, page, *selectedMonth)
	}
	if errors.Is(err, pgx.ErrNoRows) {
		return res, nil
	}
	if err != nil {
		return res, fmt.Errorf("failed to query leaderboard entries: %w", err)
	}

	if e := tx.Commit(ctx); e != nil {
		db.logger.Error("failed to commit transaction", "error", e)
		err = errors.Join(err, fmt.Errorf("failed to commit transaction: %w", e))
	}
	return res, err
}
