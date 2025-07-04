-- Users table (username as primary key)
CREATE TABLE users
(
    username            VARCHAR(50) PRIMARY KEY,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_seen           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    profile_picture_url TEXT
);

-- Profiles table (composite primary key of username + profile_name)
CREATE TABLE profiles
(
    username     VARCHAR(50)  NOT NULL REFERENCES users (username) ON DELETE CASCADE,
    profile_name VARCHAR(100) NOT NULL,

    -- Primary key is combination of username and profile name
    PRIMARY KEY (username, profile_name)
);

-- Characters table (character name as primary key)
CREATE TABLE characters
(
    name         VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(50) NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insert default characters
INSERT INTO characters (name, display_name)
VALUES ('ironclad', 'The Ironclad'),
       ('silent', 'The Silent'),
       ('defect', 'The Defect'),
       ('watcher', 'The Watcher');

-- Main runs table (using timestamp as part of primary key)
CREATE TABLE runs
(
    username            VARCHAR(50)              NOT NULL,
    profile_name        VARCHAR(100)             NOT NULL,
    run_timestamp       TIMESTAMP WITH TIME ZONE NOT NULL,
    character_name      VARCHAR(50)              NOT NULL REFERENCES characters (name),

    -- Core run metrics
    victory             BOOLEAN                  NOT NULL,
    abandoned           BOOLEAN                  NOT NULL DEFAULT false,
    score               INTEGER                           DEFAULT 0,
    floor_reached       INTEGER                  NOT NULL,
    playtime_minutes    INTEGER,

    -- Game context
    game_version        VARCHAR(20),

    -- Metadata timestamps
    created_at          TIMESTAMP WITH TIME ZONE          DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE          DEFAULT CURRENT_TIMESTAMP,

    -- Schema versioning for backfill management
    data_schema_version INTEGER                           DEFAULT 1,

    -- Flexible JSONB storage for detailed run data
    run_data            JSONB                    NOT NULL,

    -- Primary key is combination of username, profile_name and timestamp
    PRIMARY KEY (username, profile_name, run_timestamp),

    -- Foreign key constraint to profiles
    FOREIGN KEY (username, profile_name) REFERENCES profiles (username, profile_name) ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT valid_floor CHECK (floor_reached >= 0 AND floor_reached <= 60),
    CONSTRAINT valid_playtime CHECK (playtime_minutes IS NULL OR playtime_minutes >= 0),
    CONSTRAINT valid_schema_version CHECK (data_schema_version > 0)
);

-- Run statistics cache table (for performance) - with period boundaries
CREATE TABLE run_statistics
(
    username          VARCHAR(50)  NOT NULL,
    profile_name      VARCHAR(100) NOT NULL,
    character_name    VARCHAR(50)  NOT NULL,
    game_version      VARCHAR(50)  NOT NULL,
    include_abandoned BOOLEAN      NOT NULL,
    stat_kind         TEXT         NOT NULL,

    -- Period boundaries
    period_start      DATE         NOT NULL,
    period_end        DATE         NOT NULL,

    -- All cached statistics in JSONB
    stats             JSONB        NOT NULL,

    -- Metadata
    last_calculated   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_accessed     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Primary key includes period boundaries
    PRIMARY KEY (username, profile_name, character_name, game_version, stat_kind, include_abandoned, period_start,
                 period_end),

    -- Foreign key constraints
    FOREIGN KEY (username) REFERENCES users (username) ON DELETE CASCADE,
    FOREIGN KEY (username, profile_name) REFERENCES profiles (username, profile_name) ON DELETE CASCADE,
    FOREIGN KEY (character_name) REFERENCES characters (name),

    -- Constraint to ensure period_start < period_end
    CONSTRAINT valid_period CHECK (period_start <= period_end)
);

-- Backfill notifications table
CREATE TABLE backfill_notifications
(
    id                      SERIAL PRIMARY KEY,
    username                VARCHAR(50) NOT NULL REFERENCES users (username) ON DELETE CASCADE,
    profile_name            VARCHAR(100),
    current_schema_version  INTEGER     NOT NULL,
    user_min_schema_version INTEGER     NOT NULL,
    outdated_runs_count     INTEGER     NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    acknowledged_at         TIMESTAMP WITH TIME ZONE,
    is_acknowledged         BOOLEAN                  DEFAULT false,

    -- Foreign key to profile (if specified)
    FOREIGN KEY (username, profile_name) REFERENCES profiles (username, profile_name) ON DELETE CASCADE
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Users indexes (primary key already creates index on username)
CREATE INDEX idx_users_last_seen ON users (last_seen);

-- Profiles indexes (primary key already creates index on username, profile_name)
CREATE INDEX idx_profiles_username ON profiles (username);

-- Runs indexes (primary key already creates index on username, profile_name, run_timestamp)
CREATE INDEX idx_runs_timestamp ON runs (run_timestamp DESC);
CREATE INDEX idx_runs_username ON runs (username);
CREATE INDEX idx_runs_profile ON runs (username, profile_name);
CREATE INDEX idx_runs_character ON runs (character_name);
CREATE INDEX idx_runs_victory ON runs (victory);
CREATE INDEX idx_runs_abandoned ON runs (abandoned);
CREATE INDEX idx_runs_schema_version ON runs (data_schema_version);

-- Runs indexes - Composite queries
CREATE INDEX idx_runs_profile_character ON runs (username, profile_name, character_name);
CREATE INDEX idx_runs_profile_timestamp ON runs (username, profile_name, run_timestamp DESC);
CREATE INDEX idx_runs_profile_character_timestamp ON runs (username, profile_name, character_name, run_timestamp DESC);
CREATE INDEX idx_runs_profile_victory ON runs (username, profile_name, victory);
CREATE INDEX idx_runs_profile_abandoned ON runs (username, profile_name, abandoned);
CREATE INDEX idx_runs_profile_character_victory ON runs (username, profile_name, character_name, victory);
CREATE INDEX idx_runs_profile_character_abandoned ON runs (username, profile_name, character_name, abandoned);

-- Runs indexes - Time-based queries
CREATE INDEX idx_runs_profile_daily ON runs (username, profile_name, DATE(run_timestamp AT TIME ZONE 'UTC'));
CREATE INDEX idx_runs_profile_monthly ON runs (username, profile_name,
                                               DATE_TRUNC('month', run_timestamp AT TIME ZONE 'UTC'));
-- JSONB indexes for flexible queries
CREATE INDEX idx_runs_data_gin ON runs USING GIN (run_data);

-- Run statistics indexes (primary key already creates main index)
CREATE INDEX idx_run_statistics_username ON run_statistics (username);
CREATE INDEX idx_run_statistics_profile ON run_statistics (username, profile_name);
CREATE INDEX idx_run_statistics_character ON run_statistics (character_name);
CREATE INDEX idx_run_statistics_calculated ON run_statistics (last_calculated);
CREATE INDEX idx_run_statistics_period ON run_statistics (period_start, period_end);

-- JSONB index for flexible stats queries
CREATE INDEX idx_run_statistics_stats_gin ON run_statistics USING GIN (stats);

-- Backfill notifications indexes
CREATE INDEX idx_backfill_username ON backfill_notifications (username);
CREATE INDEX idx_backfill_profile ON backfill_notifications (username, profile_name);
CREATE INDEX idx_backfill_acknowledged ON backfill_notifications (is_acknowledged) WHERE is_acknowledged = false;

-- ============================================================================
-- BASIC TRIGGERS
-- ============================================================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Function to update last_seen for users
CREATE OR REPLACE FUNCTION update_user_last_seen()
    RETURNS TRIGGER AS
$$
BEGIN
    UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE username = NEW.username;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for updated_at on runs only
CREATE TRIGGER update_runs_updated_at
    BEFORE UPDATE
    ON runs
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Trigger to update user last_seen when they upload runs
CREATE TRIGGER update_user_last_seen_on_run_insert
    AFTER INSERT
    ON runs
    FOR EACH ROW
EXECUTE FUNCTION update_user_last_seen();