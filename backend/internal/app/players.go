package app

import (
	"context"
	"fmt"
	"net/http"
	"time"

	"github.com/MaT1g3R/stats-tracker/internal/ui/components/search"
	"github.com/MaT1g3R/stats-tracker/internal/ui/pages"
)

func (app *App) handlePlayers(w http.ResponseWriter, r *http.Request) {
	_ = pages.Players().Render(r.Context(), w)
}

func (app *App) handlePlayerSearch(w http.ResponseWriter, r *http.Request) {
	query := r.URL.Query().Get("player-search")
	if query == "" {
		// Return empty results
		_ = search.PlayerResults().Render(r.Context(), w)
		return
	}

	// Search for players in the database by prefix
	players, err := app.searchPlayers(r.Context(), query)
	if err != nil {
		app.logger.Error("failed to search players", "error", err)
		w.WriteHeader(http.StatusInternalServerError)
		return
	}

	// Convert to search results
	_ = search.PlayerResults(search.ResultsProps{
		Players: players,
		Query:   query,
	}).Render(r.Context(), w)
}

// searchPlayers searches for players in the database by prefix
func (app *App) searchPlayers(ctx context.Context, query string) ([]search.PlayerResult, error) {
	// Search for users in the database
	users, err := app.db.SearchUsersByPrefix(ctx, query, 10)
	if err != nil {
		return nil, fmt.Errorf("failed to search users: %w", err)
	}

	// Convert users to search results
	results := make([]search.PlayerResult, 0, len(users))
	for _, user := range users {
		var lastSeen string
		if !user.LastSeenAt.IsZero() {
			// Format relative time
			duration := time.Since(user.LastSeenAt)
			lastSeen = formatRelativeTime(duration)
		}

		results = append(results, search.PlayerResult{
			Name:     user.Username,
			LastSeen: lastSeen,
			Avatar:   user.GetProfilePictureUrl(),
		})
	}

	return results, nil
}

// formatRelativeTime converts a duration to a human-readable relative time
func formatRelativeTime(duration time.Duration) string {
	switch {
	case duration < time.Minute:
		return "just now"
	case duration < time.Hour:
		mins := int(duration.Minutes())
		if mins == 1 {
			return "1 minute ago"
		}
		return fmt.Sprintf("%d minutes ago", mins)
	case duration < 24*time.Hour:
		hours := int(duration.Hours())
		if hours == 1 {
			return "1 hour ago"
		}
		return fmt.Sprintf("%d hours ago", hours)
	case duration < 7*24*time.Hour:
		days := int(duration.Hours() / 24)
		if days == 1 {
			return "1 day ago"
		}
		return fmt.Sprintf("%d days ago", days)
	case duration < 30*24*time.Hour:
		weeks := int(duration.Hours() / 24 / 7)
		if weeks == 1 {
			return "1 week ago"
		}
		return fmt.Sprintf("%d weeks ago", weeks)
	default:
		months := int(duration.Hours() / 24 / 30)
		if months == 1 {
			return "1 month ago"
		}
		return fmt.Sprintf("%d months ago", months)
	}
}
