DROP TRIGGER IF EXISTS update_user_last_seen_on_run_insert ON runs;
DROP TRIGGER IF EXISTS update_runs_updated_at ON runs;
DROP FUNCTION IF EXISTS update_user_last_seen();
DROP FUNCTION IF EXISTS update_updated_at_column();

DROP TABLE IF EXISTS backfill_notifications;
DROP TABLE IF EXISTS run_statistics;
DROP TABLE IF EXISTS runs;
DROP TABLE IF EXISTS characters;
DROP TABLE IF EXISTS profiles;
DROP TABLE IF EXISTS users;
DROP EXTENSION IF EXISTS "uuid-ossp";
