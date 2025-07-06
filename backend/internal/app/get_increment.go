package app

import (
	"encoding/json"
	"net/http"
	"net/url"
	"time"
)

type GetIncrementRequest struct {
	Profile       string `json:"profile"`
	GameVersion   string `json:"gameVersion"`
	SchemaVersion int    `json:"schemaVersion"`
}

type GetIncrementResponse struct {
	LastRunTime *time.Time  `json:"lastRunTime"`
	Outdated    []time.Time `json:"outdated"`
}

func (app *App) handleGetIncrement(w http.ResponseWriter, r *http.Request) {
	user, err := app.Authenticate(w, r)
	if err != nil {
		return
	}
	var req GetIncrementRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		app.logger.Warn("Failed to decode get increment request", "error", err)
		http.Error(w, "Invalid JSON in request body", http.StatusBadRequest)
		return
	}

	if req.Profile == "" {
		http.Error(w, "Profile is required", http.StatusBadRequest)
		return
	}
	if req.GameVersion == "" {
		http.Error(w, "GameVersion is required", http.StatusBadRequest)
		return
	}
	if req.SchemaVersion == 0 {
		http.Error(w, "SchemaVersion is required", http.StatusBadRequest)
		return
	}

	// URL encode the profile
	encodedProfile := url.QueryEscape(req.Profile)

	last, outdated, err := app.db.QueryIncrement(r.Context(),
		user.Username, encodedProfile, req.GameVersion, req.SchemaVersion)
	if err != nil {
		app.logger.Warn("Failed to get increment", "error", err)
		http.Error(w, "Failed to get increment", http.StatusInternalServerError)
		return
	}
	response := GetIncrementResponse{
		LastRunTime: last,
		Outdated:    outdated,
	}
	w.Header().Set("Content-Type", "application/json")
	err = json.NewEncoder(w).Encode(response)
	if err != nil {
		app.logger.Warn("Failed to encode get increment response", "error", err)
		http.Error(w, "Failed to encode get increment response", http.StatusInternalServerError)
	}
	w.WriteHeader(http.StatusOK)
}
