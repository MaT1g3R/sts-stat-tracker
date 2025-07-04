package app

import (
	"encoding/json"
	"net/http"
	"net/url"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

type UploadAllRequest struct {
	Runs          []model.Run `json:"runs"`
	Profile       string      `json:"profile"`
	GameVersion   string      `json:"gameVersion"`
	SchemaVersion int         `json:"schemaVersion"`
}

func (app *App) UploadAll(w http.ResponseWriter, r *http.Request) {
	user, err := app.Authenticate(w, r)
	if err != nil {
		return
	}

	// Parse request body
	var req UploadAllRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		app.logger.Warn("Failed to decode upload all request", "error", err)
		http.Error(w, "Invalid JSON in request body", http.StatusBadRequest)
		return
	}

	// Validate required fields
	if req.Profile == "" {
		http.Error(w, "Profile is required", http.StatusBadRequest)
		return
	}

	if len(req.Runs) == 0 {
		http.Error(w, "No runs provided", http.StatusBadRequest)
		return
	}

	// URL encode the profile
	encodedProfile := url.QueryEscape(req.Profile)

	// Import runs to database
	err = app.db.ImportRuns(r.Context(), user.Username, encodedProfile, req.GameVersion, req.SchemaVersion, req.Runs)
	if err != nil {
		app.logger.Error("failed to import runs", "error", err, "user", user.Username, "profile", req.Profile)
		http.Error(w, "Failed to import runs", http.StatusInternalServerError)
		return
	}

	// Return success response
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	err = json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": "Runs imported successfully",
		"count":   len(req.Runs),
	})
	if err != nil {
		app.logger.Error("failed to write response", "error", err)
	}
}
