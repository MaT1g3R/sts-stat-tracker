package app

import (
	"encoding/json"
	"net/http"
	"strconv"
)

type GetIncrementResponse struct {
	LastRunTime *int64  `json:"lastRunTime"`
	Outdated    []int64 `json:"outdated"`
}

func (app *App) handleGetIncrement(w http.ResponseWriter, r *http.Request) {
	user, err := app.Authenticate(w, r)
	if err != nil {
		return
	}

	profile := r.FormValue("profile")
	if profile == "" {
		http.Error(w, "Missing profile", http.StatusBadRequest)
		return
	}
	gameVersion := r.FormValue("game-version")
	if gameVersion == "" {
		http.Error(w, "Missing game-version", http.StatusBadRequest)
		return
	}
	schemaVersionStr := r.FormValue("schema-version")
	if schemaVersionStr == "" {
		http.Error(w, "Missing schema-version", http.StatusBadRequest)
		return
	}
	schemaVersion, err := strconv.Atoi(schemaVersionStr)
	if err != nil || schemaVersion < 1 {
		http.Error(w, "Invalid schema version", http.StatusBadRequest)
		return
	}

	last, outdated, err := app.db.QueryIncrement(r.Context(),
		user.Username, profile, gameVersion, schemaVersion)
	if err != nil {
		app.logger.Warn("Failed to get increment", "error", err)
		http.Error(w, "Failed to get increment", http.StatusInternalServerError)
		return
	}

	var lastTimeStamp *int64
	if last != nil {
		t := last.UTC().Unix()
		lastTimeStamp = &t
	}

	outdatedTimeStamps := make([]int64, 0, len(outdated))
	for _, t := range outdated {
		outdatedTimeStamps = append(outdatedTimeStamps, t.UTC().Unix())
	}

	response := GetIncrementResponse{
		LastRunTime: lastTimeStamp,
		Outdated:    outdatedTimeStamps,
	}
	w.Header().Set("Content-Type", "application/json")
	err = json.NewEncoder(w).Encode(response)
	if err != nil {
		app.logger.Warn("Failed to encode get increment response", "error", err)
		http.Error(w, "Failed to encode get increment response", http.StatusInternalServerError)
	}
}
