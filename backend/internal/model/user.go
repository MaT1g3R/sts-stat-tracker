package model

import "time"

type User struct {
	Username   string    `json:"username" db:"username"`
	CreatedAt  time.Time `json:"created_at" db:"created_at"`
	LastSeenAt time.Time `json:"last_seen" db:"last_seen"`
}
