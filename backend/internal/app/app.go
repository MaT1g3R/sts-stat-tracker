package app

import (
	"fmt"
	"log/slog"
	"net/http"

	"github.com/MaT1g3R/stats-tracker/internal/clients"

	"github.com/MaT1g3R/stats-tracker/internal/config"
	"github.com/MaT1g3R/stats-tracker/internal/db"
)

type App struct {
	Server *http.Server

	cfg    *config.Config
	logger *slog.Logger

	db           *db.DB
	authClient   *clients.AuthClient
	twitchClient *clients.TwitchClient
}

func NewApp(
	cfg *config.Config,
	logger *slog.Logger,
	db *db.DB,
	authClient *clients.AuthClient,
	twitchClient *clients.TwitchClient) *App {
	app := &App{
		cfg:          cfg,
		logger:       logger,
		db:           db,
		authClient:   authClient,
		twitchClient: twitchClient,
	}

	// Setup HTTP server
	mux := http.NewServeMux()
	// Healthcheck
	mux.HandleFunc("GET /api/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_, err := fmt.Fprint(w, `{"status":"ok"}`)
		if err != nil {
			slog.Error("Failed to write health check response", "error", err)
		}
	})
	// Setup static file server
	fs := http.FileServer(http.Dir(cfg.StaticFilesDir))
	mux.Handle("GET /assets/", http.StripPrefix("/assets/", fs))

	// API routes
	mux.HandleFunc("POST /api/v1/upload-all", app.UploadAll)
	mux.HandleFunc("GET /api/v1/players/search", app.handlePlayerSearch)
	mux.HandleFunc("GET /api/v1/players/{name}/stats", app.handlePlayerStats)

	// App routes
	mux.HandleFunc("GET /app/players/{name}", app.handlePlayer)
	mux.HandleFunc("GET /app/players", app.handlePlayers)

	// Redirect
	mux.HandleFunc("GET /", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			http.NotFound(w, r)
			return
		}
		http.Redirect(w, r, "/app/players", http.StatusFound)
	})

	// Create server with timeouts
	server := &http.Server{
		Addr:         fmt.Sprintf("%s:%d", cfg.Host, cfg.Port),
		Handler:      mux,
		ReadTimeout:  cfg.ReadTimeout,
		WriteTimeout: cfg.WriteTimeout,
	}
	app.Server = server
	return app
}
