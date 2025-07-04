package app

import (
	"fmt"
	"net/http"
	"strings"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

const bearerPrefix = "Bearer "

func extractBearerToken(header string) (string, error) {
	if header == "" {
		return "", fmt.Errorf("authorization header is missing")
	}
	if !strings.HasPrefix(header, bearerPrefix) {
		return "", fmt.Errorf("invalid authorization header format")
	}
	return strings.TrimPrefix(header, bearerPrefix), nil
}

func (app *App) Authenticate(w http.ResponseWriter, r *http.Request) (model.User, error) {
	token, err := extractBearerToken(r.Header.Get("Authorization"))
	if err != nil {
		err := fmt.Errorf("cannot extract bearer token: %w", err)
		http.Error(w, err.Error(), http.StatusBadRequest)
		return model.User{}, err
	}

	if token == "" {
		err := fmt.Errorf("bearer token is missing")
		http.Error(w, err.Error(), http.StatusBadRequest)
		return model.User{}, err
	}

	userID := r.Header.Get("User-ID")
	if userID == "" {
		err := fmt.Errorf("user ID is missing")
		http.Error(w, err.Error(), http.StatusBadRequest)
		return model.User{}, err
	}

	user, err := app.authClient.Authenticate(r.Context(), userID, token)
	if err != nil {
		err := fmt.Errorf("failed to authenticate user: %w", err)
		http.Error(w, err.Error(), http.StatusUnauthorized)
		return model.User{}, err
	}

	dbUser, err := app.db.CreateOrGetUser(r.Context(), user)
	if err != nil {
		err := fmt.Errorf("failed to get or create user in database: %w", err)
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return model.User{}, err
	}

	if dbUser.ProfilePictureUrl == nil {
		pic, err := app.twitchClient.GetUserProfileImage(r.Context(), userID)
		if err != nil {
			app.logger.Warn("failed to get user profile image from twitch client", "error", err)
		} else {
			err = app.db.SetUserProfilePicture(r.Context(), dbUser.Username, pic)
			if err != nil {
				app.logger.Warn("failed to set user profile picture", "error", err)
			} else {
				dbUser.ProfilePictureUrl = &pic
			}
		}
	}

	return dbUser, nil
}
