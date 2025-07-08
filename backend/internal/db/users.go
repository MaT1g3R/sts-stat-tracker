package db

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

func (db *DB) GetUser(ctx context.Context, name string) (model.User, error) {
	var user model.User
	err := db.Pool.QueryRow(ctx, `
		SELECT username, created_at, last_seen, profile_picture_url
		FROM users
		WHERE username = $1
	`, name).Scan(&user.Username, &user.CreatedAt, &user.LastSeenAt, &user.ProfilePictureUrl)

	if errors.Is(err, pgx.ErrNoRows) {
		return model.User{}, fmt.Errorf("%w: user not found: %s", err, name)
	}
	if err != nil {
		return model.User{}, fmt.Errorf("failed to get user: %w", err)
	}

	return user, nil
}

func (db *DB) GetProfiles(ctx context.Context, user string) ([]string, error) {
	rows, err := db.Pool.Query(ctx, `
		SELECT profile_name
		FROM profiles
		WHERE username = $1
		ORDER BY profile_name ASC
	`, user)
	if err != nil {
		return nil, fmt.Errorf("failed to get profiles: %w", err)
	}
	defer rows.Close()

	var profiles []string
	for rows.Next() {
		var profile string
		if err := rows.Scan(&profile); err != nil {
			return nil, fmt.Errorf("failed to scan profile row: %w", err)
		}
		profiles = append(profiles, profile)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating profile rows: %w", err)
	}

	return profiles, nil
}

func (db *DB) SearchUsersByPrefix(ctx context.Context, prefix string, limit int) ([]model.User, error) {
	if limit <= 0 {
		limit = 10 // Default limit if not specified or invalid
	}

	rows, err := db.Pool.Query(ctx, `
		SELECT username, created_at, last_seen, profile_picture_url
		FROM users
		WHERE username ILIKE $1 || '%'
		ORDER BY last_seen DESC, username ASC
		LIMIT $2
	`, prefix, limit)
	if err != nil {
		return nil, fmt.Errorf("failed to search users by prefix: %w", err)
	}
	defer rows.Close()

	var users []model.User
	for rows.Next() {
		var user model.User
		if err := rows.Scan(&user.Username, &user.CreatedAt, &user.LastSeenAt, &user.ProfilePictureUrl); err != nil {
			return nil, fmt.Errorf("failed to scan user row: %w", err)
		}
		users = append(users, user)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("error iterating user rows: %w", err)
	}

	return users, nil
}

func (db *DB) CreateOrGetUser(ctx context.Context, username string) (model.User, error) {
	var user model.User
	now := time.Now()

	err := db.Pool.QueryRow(ctx, `
		INSERT INTO users (username, created_at, last_seen)
		VALUES ($1, $2, $2)
		ON CONFLICT (username) DO UPDATE
		SET last_seen = $2
		RETURNING username, created_at, last_seen, profile_picture_url
	`, username, now).Scan(&user.Username, &user.CreatedAt, &user.LastSeenAt, &user.ProfilePictureUrl)

	if err != nil {
		return model.User{}, fmt.Errorf("failed to create or get user: %w", err)
	}

	return user, nil
}

func (db *DB) SetUserProfilePicture(ctx context.Context, username string, profilePictureUrl string) error {
	_, err := db.Pool.Exec(ctx, `
		UPDATE users
		SET profile_picture_url = $1
		WHERE username = $2
	`, profilePictureUrl, username)

	if err != nil {
		return fmt.Errorf("failed to update user profile picture: %w", err)
	}

	return nil
}
