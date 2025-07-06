package model

import (
	"testing"
	"time"
)

func TestUser_GetProfilePictureUrl(t *testing.T) {
	tests := []struct {
		name     string
		user     User
		expected string
	}{
		{
			name: "user with profile picture URL",
			user: User{
				Username:          "testuser",
				CreatedAt:         time.Now(),
				LastSeenAt:        time.Now(),
				ProfilePictureUrl: stringPtr("https://example.com/profile.jpg"),
			},
			expected: "https://example.com/profile.jpg",
		},
		{
			name: "user without profile picture URL (nil)",
			user: User{
				Username:          "testuser",
				CreatedAt:         time.Now(),
				LastSeenAt:        time.Now(),
				ProfilePictureUrl: nil,
			},
			expected: "/assets/img/ayaya.png",
		},
		{
			name: "user with empty profile picture URL",
			user: User{
				Username:          "testuser",
				CreatedAt:         time.Now(),
				LastSeenAt:        time.Now(),
				ProfilePictureUrl: stringPtr(""),
			},
			expected: "",
		},
		{
			name: "user with custom profile picture URL",
			user: User{
				Username:          "anotheruser",
				CreatedAt:         time.Now(),
				LastSeenAt:        time.Now(),
				ProfilePictureUrl: stringPtr("https://cdn.example.com/avatars/user123.png"),
			},
			expected: "https://cdn.example.com/avatars/user123.png",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := tt.user.GetProfilePictureUrl()
			if result != tt.expected {
				t.Errorf("GetProfilePictureUrl() = %q, want %q", result, tt.expected)
			}
		})
	}
}

func TestUser_Fields(t *testing.T) {
	now := time.Now()
	profileUrl := "https://example.com/profile.jpg"

	user := User{
		Username:          "testuser",
		CreatedAt:         now,
		LastSeenAt:        now,
		ProfilePictureUrl: &profileUrl,
	}

	// Test that all fields are properly set
	if user.Username != "testuser" {
		t.Errorf("Username = %q, want %q", user.Username, "testuser")
	}

	if !user.CreatedAt.Equal(now) {
		t.Errorf("CreatedAt = %v, want %v", user.CreatedAt, now)
	}

	if !user.LastSeenAt.Equal(now) {
		t.Errorf("LastSeenAt = %v, want %v", user.LastSeenAt, now)
	}

	if user.ProfilePictureUrl == nil {
		t.Error("ProfilePictureUrl is nil, want non-nil")
	} else if *user.ProfilePictureUrl != profileUrl {
		t.Errorf("ProfilePictureUrl = %q, want %q", *user.ProfilePictureUrl, profileUrl)
	}
}

func TestUser_ZeroValue(t *testing.T) {
	var user User

	// Test zero values
	if user.Username != "" {
		t.Errorf("Zero value Username = %q, want empty string", user.Username)
	}

	if !user.CreatedAt.IsZero() {
		t.Errorf("Zero value CreatedAt = %v, want zero time", user.CreatedAt)
	}

	if !user.LastSeenAt.IsZero() {
		t.Errorf("Zero value LastSeenAt = %v, want zero time", user.LastSeenAt)
	}

	if user.ProfilePictureUrl != nil {
		t.Errorf("Zero value ProfilePictureUrl = %v, want nil", user.ProfilePictureUrl)
	}

	// Test GetProfilePictureUrl with zero value
	defaultUrl := user.GetProfilePictureUrl()
	if defaultUrl != "/assets/img/ayaya.png" {
		t.Errorf("Zero value GetProfilePictureUrl() = %q, want %q", defaultUrl, "/assets/img/ayaya.png")
	}
}

// Helper function to create a string pointer
func stringPtr(s string) *string {
	return &s
}
