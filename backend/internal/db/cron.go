package db

import (
	"context"

	"github.com/go-co-op/gocron/v2"
)

func (db *DB) RemoveOldStatsCache(s gocron.Scheduler) (gocron.Job, error) {
	task := gocron.NewTask(db.removeOldStatsCache)
	jobDefinition := gocron.DailyJob(1, gocron.NewAtTimes(
		gocron.NewAtTime(
			0, 0, 0,
		),
		gocron.NewAtTime(
			12, 0, 0,
		),
	))
	return s.NewJob(jobDefinition, task, gocron.WithStartAt(gocron.WithStartImmediately()))
}

func (db *DB) removeOldStatsCache() {
	query := `DELETE FROM run_statistics WHERE last_accessed < now() - interval '1 day'
`
	res, err := db.Pool.Exec(context.Background(), query)
	if err != nil {
		db.logger.Warn("failed to remove old stats cache", "err", err)
	}
	db.logger.Debug("removed old stats cache", "rows", res.RowsAffected())
}
