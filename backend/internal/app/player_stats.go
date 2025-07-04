package app

import (
	"net/http"

	"github.com/MaT1g3R/stats-tracker/internal/db"

	"github.com/MaT1g3R/stats-tracker/internal/ui/pages"
)

// handlePlayerStats handles HTMX requests for player statistics with filters
//
//nolint:funlen
func (app *App) handlePlayerStats(w http.ResponseWriter, r *http.Request) {
	name := r.PathValue("name")
	if name == "" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}

	// Parse form values to get filter parameters first so we can use them for profile fetching
	if err := r.ParseForm(); err != nil {
		app.logger.Error("Failed to parse form", "error", err)
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	// Get filter values
	gameVersion := r.FormValue("game-version")
	character := r.FormValue("character")
	startDateStr := r.FormValue("start-date")
	endDateStr := r.FormValue("end-date")
	profile := r.FormValue("profile-name")

	startDate, err := pages.StringToDate(startDateStr)
	if err != nil {
		app.logger.Error("Failed to parse start date", "error", err)
		w.WriteHeader(http.StatusBadRequest)
	}
	endDate, err := pages.StringToDate(endDateStr)
	if err != nil {
		app.logger.Error("Failed to parse end date", "error", err)
		w.WriteHeader(http.StatusBadRequest)
	}

	// Parse include abandoned runs checkbox
	includeAbandoned := r.FormValue("include-abandoned") == "on"
	statType := r.FormValue("stat-type")

	// Create player page props with filter values
	props := pages.PlayerPageProps{
		Name:             name,
		GameVersion:      gameVersion,
		Character:        character,
		StartDate:        startDate,
		EndDate:          endDate,
		IncludeAbandoned: includeAbandoned,
		StatType:         statType,
		SelectedProfile:  profile,
	}
	stat, err := app.db.QueryStats(r.Context(), statType, db.StatQuery{
		Player:           name,
		GameVersion:      gameVersion,
		Character:        character,
		Profile:          profile,
		StartDate:        startDate,
		EndDate:          endDate,
		IncludeAbandoned: includeAbandoned,
	})
	if err != nil {
		app.logger.Error("Failed to query stats", "error", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	_ = pages.PlayerStats(props, stat.Render()).Render(r.Context(), w)
}
