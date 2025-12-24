package app

import (
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

type postLeaderboardRequest struct {
	Score       float64 `json:"score"`
	PlayerClass string  `json:"playerClass"`
	Kind        string  `json:"kind"`
	Timestamp   int64   `json:"timestamp"`
}

func processRequests(
	username string,
	reqs []postLeaderboardRequest,
) ([]model.LeaderboardEntry, []model.LeaderboardKind, error) {
	entries := make([]model.LeaderboardEntry, len(reqs))
	kinds := make([]model.LeaderboardKind, len(reqs))

	for i, req := range reqs {
		kind, err := model.GetLeaderboardKind(req.Kind)
		if err != nil {
			return nil, nil, err
		}
		if req.Score < 0 {
			return nil, nil, fmt.Errorf("invalid %s score value: %f", kind.Value, req.Score)
		}
		kinds[i] = kind

		class := "all"
		if req.PlayerClass != "" {
			class, err = model.MapPlayerClassToCharacter(req.PlayerClass)
			if err != nil {
				return nil, nil, err
			}
		}

		timestamp := time.Unix(req.Timestamp, 0).UTC()
		date := time.Date(
			timestamp.Year(),
			timestamp.Month(),
			timestamp.Day(), 0, 0, 0, 0, time.UTC,
		)

		entries[i] = model.LeaderboardEntry{
			Score:      req.Score,
			PlayerName: username,
			Character:  class,
			Date:       date,
		}
	}
	return entries, kinds, nil
}

func (app *App) handlePostLeaderboard(w http.ResponseWriter, r *http.Request) {
	user, err := app.Authenticate(w, r)
	if err != nil {
		return
	}

	// Parse request body
	var request []postLeaderboardRequest

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		app.logger.Warn("upload leaderboard: invalid JSON", "error", err, "username", user.Username)
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}
	if len(request) == 0 {
		return
	}
	if len(request) > 10*len(model.LeaderboardKinds) {
		app.logger.Warn("upload leaderboard: list too long", "username", user.Username)
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	entries, kinds, err := processRequests(user.Username, request)
	if err != nil {
		app.logger.Warn("upload leaderboard: bad request", "error", err, "username", user.Username)
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	// Insert leaderboard entries
	if err := app.db.InsertLeaderboardEntries(r.Context(), user.Username, kinds, entries); err != nil {
		app.logger.Error("failed to insert leaderboard entries", "error", err)
		http.Error(w, "Failed to save leaderboard data", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(map[string]string{"status": "success"})
}
