-- Index optimizations based on performance analysis

-- 1. Composite index for game_version + data_schema_version queries
-- This helps with common queries that filter by both game version and schema version
CREATE INDEX idx_runs_game_version_schema ON runs (game_version, data_schema_version);

-- 3. Partial index for non-abandoned runs
-- This reduces index size by only indexing non-abandoned runs, which are queried more frequently
CREATE INDEX idx_runs_abandoned_false ON runs (username, profile_name, character_name) 
WHERE abandoned = false;

-- 4. Additional composite index for common query patterns
-- This helps with queries that filter by user, profile, game version, and timestamp
CREATE INDEX idx_runs_user_profile_game_version_timestamp ON runs (username, profile_name, game_version, run_timestamp DESC);

-- 5. Index for increment queries (QueryIncrement method optimization)
-- This helps with queries that look for runs with specific schema versions
CREATE INDEX idx_runs_user_profile_game_schema_timestamp ON runs (username, profile_name, game_version, data_schema_version, run_timestamp DESC);

-- 6. Index for statistics cache by period boundaries
-- This helps with cache lookups that filter by time periods
CREATE INDEX idx_run_statistics_period_boundaries ON run_statistics (username, profile_name, character_name, game_version, period_start, period_end);