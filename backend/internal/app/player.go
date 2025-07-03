package app

import (
	"errors"
	"net/http"

	"github.com/jackc/pgx/v5"

	"github.com/MaT1g3R/stats-tracker/internal/ui/pages"
)

func (a *App) handlePlayer(w http.ResponseWriter, r *http.Request) {
	name := r.PathValue("name")
	if name == "" {
		w.WriteHeader(http.StatusNotFound)
		return
	}
	p, err := a.db.GetUser(r.Context(), name)
	if errors.Is(err, pgx.ErrNoRows) {
		w.WriteHeader(http.StatusNotFound)
	} else if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
	}
	_ = pages.Player(p.Username, p.LastSeenAt).Render(r.Context(), w)
}
