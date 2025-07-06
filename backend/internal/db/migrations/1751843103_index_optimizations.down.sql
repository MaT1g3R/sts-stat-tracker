-- Rollback index optimizations

DROP INDEX IF EXISTS idx_runs_game_version_schema;
DROP INDEX IF EXISTS idx_runs_abandoned_false;
DROP INDEX IF EXISTS idx_runs_user_profile_game_version_timestamp;
DROP INDEX IF EXISTS idx_runs_user_profile_game_schema_timestamp;
DROP INDEX IF EXISTS idx_run_statistics_period_boundaries;