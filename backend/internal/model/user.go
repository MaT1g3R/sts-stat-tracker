package model

import "time"

type User struct {
	Username          string    `json:"username" db:"username"`
	CreatedAt         time.Time `json:"created_at" db:"created_at"`
	LastSeenAt        time.Time `json:"last_seen" db:"last_seen"`
	ProfilePictureUrl *string   `json:"profile_picture_url" db:"profile_picture_url"`
}

func (u User) GetProfilePictureUrl() string {
	if u.ProfilePictureUrl == nil {
		return "/assets/img/ayaya.png"
	}
	return *u.ProfilePictureUrl
}
