package app

import (
	"net/http"
	"time"

	"github.com/MaT1g3R/stats-tracker/internal/app/stats"
	"github.com/MaT1g3R/stats-tracker/internal/ui/pages"
)

var (
	Characters       = []string{"all", "ironclad", "silent", "defect", "watcher"}
	DefaultCharacter = Characters[0]

	GameVersions       = []string{"sts1"}
	DefaultGameVersion = GameVersions[0]

	DefaultStatType = stats.StatTypes[0]
)

//nolint:funlen
func (app *App) handlePlayer(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()
	name := r.PathValue("name")
	if name == "" {
		w.WriteHeader(http.StatusBadRequest)
	}

	// Get filter values
	gameVersion := r.FormValue("game-version")
	if gameVersion == "" {
		gameVersion = DefaultGameVersion
	}

	character := r.FormValue("character")
	if character == "" {
		character = DefaultCharacter
	}
	statType := r.FormValue("stat-type")
	if statType == "" {
		statType = DefaultStatType
	}
	includeAbandoned := r.FormValue("include-abandoned") == "on"

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
	firstRun, lastRun, err := app.db.GetFirstAndLastRun(ctx, user.Username, gameVersion)
	if err != nil {
		app.logger.Error("Failed to get first and last run", "error", err)
		w.WriteHeader(http.StatusInternalServerError)
	}

	profile := r.FormValue("profile-name")
	if profile == "" {
		profile = lastRun.ProfileName
	}

	startDate := firstRun.RunTimestamp
	if startDateStr := r.FormValue("start-date"); startDateStr != "" {
		d, err := time.Parse(time.DateOnly, startDateStr)
		if err != nil {
			app.logger.Warn("Failed to parse start date", "error", err)
		} else {
			startDate = d
		}
	}
	endDate := lastRun.RunTimestamp
	if endDateStr := r.FormValue("end-date"); endDateStr != "" {
		d, err := time.Parse(time.DateOnly, endDateStr)
		if err != nil {
			app.logger.Warn("Failed to parse end date", "error", err)
		} else {
			endDate = d
		}
	}

	// Create player page props
	props := pages.PlayerPageProps{
		Name:             name,
		AvatarURL:        user.GetProfilePictureUrl(),
		LastSeen:         user.LastSeenAt,
		MinDate:          firstRun.RunTimestamp,
		MaxDate:          lastRun.RunTimestamp,
		StartDate:        startDate,
		EndDate:          endDate,
		IncludeAbandoned: includeAbandoned,
		Characters:       Characters,
		Character:        character,
		GameVersions:     GameVersions,
		GameVersion:      gameVersion,
		StatTypeOptions:  stats.StatTypes,
		StatType:         statType,
		Profiles:         profiles,
		SelectedProfile:  profile,
	}

	// Render the player page
	_ = pages.PlayerPage(props).Render(r.Context(), w)
}
