ALTER TABLE run_statistics
    ADD COLUMN include_abandoned BOOLEAN NOT NULL DEFAULT FALSE;