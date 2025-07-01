package db

import (
	"database/sql"
	"embed"
	"errors"
	"fmt"

	"github.com/golang-migrate/migrate/v4"
	pgxv5 "github.com/golang-migrate/migrate/v4/database/pgx/v5"
	"github.com/golang-migrate/migrate/v4/source/iofs"
)

//go:embed migrations/*.sql
var migrationFS embed.FS

func (db *DB) RunMigrations() error {
	return RunMigrationsWithDB(db.SQL)
}

func RunMigrationsWithDB(sqlDB *sql.DB) error {
	// Create pgx v5 driver instance
	driver, err := pgxv5.WithInstance(sqlDB, &pgxv5.Config{
		MigrationsTable: "schema_migrations", // Optional: customize table name
	})
	if err != nil {
		return fmt.Errorf("could not create migration driver: %w", err)
	}

	// Create migration source from embedded files
	source, err := iofs.New(migrationFS, "migrations")
	if err != nil {
		return fmt.Errorf("could not create migration source: %w", err)
	}

	// Create migration instance
	m, err := migrate.NewWithInstance("iofs", source, "pgx5", driver)
	if err != nil {
		return fmt.Errorf("could not create migration instance: %w", err)
	}
	defer func() {
		if sourceErr, dbErr := m.Close(); sourceErr != nil || dbErr != nil {
			fmt.Printf("failed to close migration: source=%v, db=%v\n", sourceErr, dbErr)
		}
	}()

	// Run migrations
	if err := m.Up(); err != nil && !errors.Is(err, migrate.ErrNoChange) {
		return fmt.Errorf("could not run migrations: %w", err)
	}

	return nil
}

func (db *DB) GetMigrationVersion() (uint, bool, error) {
	return GetMigrationVersionWithDB(db.SQL)
}

func GetMigrationVersionWithDB(sqlDB *sql.DB) (uint, bool, error) {
	driver, err := pgxv5.WithInstance(sqlDB, &pgxv5.Config{})
	if err != nil {
		return 0, false, fmt.Errorf("could not create migration driver: %w", err)
	}

	source, err := iofs.New(migrationFS, "migrations")
	if err != nil {
		return 0, false, fmt.Errorf("could not create migration source: %w", err)
	}

	m, err := migrate.NewWithInstance("iofs", source, "pgx5", driver)
	if err != nil {
		return 0, false, fmt.Errorf("could not create migration instance: %w", err)
	}
	defer func() {
		if sourceErr, dbErr := m.Close(); sourceErr != nil || dbErr != nil {
			fmt.Printf("failed to close migration: source=%v, db=%v\n", sourceErr, dbErr)
		}
	}()

	return m.Version()
}

func (db *DB) MigrateDown(steps int) error {
	driver, err := pgxv5.WithInstance(db.SQL, &pgxv5.Config{})
	if err != nil {
		return fmt.Errorf("could not create migration driver: %w", err)
	}

	source, err := iofs.New(migrationFS, "migrations")
	if err != nil {
		return fmt.Errorf("could not create migration source: %w", err)
	}

	m, err := migrate.NewWithInstance("iofs", source, "pgx5", driver)
	if err != nil {
		return fmt.Errorf("could not create migration instance: %w", err)
	}
	defer func() {
		if sourceErr, dbErr := m.Close(); sourceErr != nil || dbErr != nil {
			fmt.Printf("failed to close migration: source=%v, db=%v\n", sourceErr, dbErr)
		}
	}()

	return m.Steps(-steps)
}
