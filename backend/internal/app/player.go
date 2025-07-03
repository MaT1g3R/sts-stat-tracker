package app

import (
	"net/http"

	"github.com/MaT1g3R/stats-tracker/internal/ui/pages"
)

var (
	Characters       = []string{"all", "ironclad", "silent", "defect", "watcher"}
	DefaultCharacter = Characters[0]

	GameVersions       = []string{"sts1"}
	DefaultGameVersion = GameVersions[0]

	StatTypes       = []string{"Overview"}
	DefaultStatType = StatTypes[0]
)

func (app *App) handlePlayer(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	name := r.PathValue("name")
	if name == "" {
		w.WriteHeader(http.StatusBadRequest)
	}

	user, err := app.db.GetUser(ctx, name)
	if err != nil {
		app.logger.Error("Failed to get user", "error", err)
		w.WriteHeader(http.StatusNotFound)
		return
	}

	profiles, err := app.db.GetProfiles(ctx, user.Username)
	if err != nil {
		app.logger.Error("Failed to get profiles", "error", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	if len(profiles) == 0 {
		w.WriteHeader(http.StatusNotFound)
		return
	}
	firstRun, lastRun, err := app.db.GetFirstAndLastRun(ctx, user.Username, DefaultGameVersion)
	if err != nil {
		app.logger.Error("Failed to get first and last run", "error", err)
		w.WriteHeader(http.StatusInternalServerError)
	}

	// Create player page props
	props := pages.PlayerPageProps{
		Name:             name,
		AvatarURL:        user.GetProfilePictureUrl(),
		LastSeen:         user.LastSeenAt,
		StartDate:        firstRun.RunTimestamp,
		EndDate:          lastRun.RunTimestamp,
		IncludeAbandoned: false,
		Characters:       Characters,
		Character:        DefaultCharacter,
		GameVersions:     GameVersions,
		GameVersion:      DefaultGameVersion,
		StatTypeOptions:  StatTypes,
		StatType:         DefaultStatType,
		Profiles:         profiles,
		SelectedProfile:  lastRun.ProfileName,
	}

	// Render the player page
	_ = pages.PlayerPage(props).Render(r.Context(), w)
}
