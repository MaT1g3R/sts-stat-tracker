package app

import (
	"encoding/json"
	"net/http"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

func (app *App) handlePostLeaderboard(w http.ResponseWriter, r *http.Request) {
	user, err := app.Authenticate(w, r)
	if err != nil {
		return
	}

	// Parse request body
	var request struct {
		Kinds   []model.LeaderboardKind  `json:"kinds"`
		Entries []model.LeaderboardEntry `json:"entries"`
	}

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Insert leaderboard entries
	if err := app.db.InsertLeaderboardEntries(r.Context(), user.Username, request.Kinds, request.Entries); err != nil {
		app.logger.Error("failed to insert leaderboard entries", "error", err)
		http.Error(w, "Failed to save leaderboard data", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(map[string]string{"status": "success"})
}
