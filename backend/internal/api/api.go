package api

import (
	"fmt"
	"log/slog"
	"net/http"

	"github.com/MaT1g3R/stats-tracker/internal/clients"

	"github.com/MaT1g3R/stats-tracker/internal/config"
	"github.com/MaT1g3R/stats-tracker/internal/db"
)

type API struct {
	Server *http.Server

	cfg    *config.Config
	logger *slog.Logger

	db         *db.DB
	authClient *clients.AuthClient
}

func NewAPI(cfg *config.Config, logger *slog.Logger, db *db.DB, authClient *clients.AuthClient) *API {
	api := &API{
		cfg:        cfg,
		logger:     logger,
		db:         db,
		authClient: authClient,
	}

	// Setup HTTP server
	mux := http.NewServeMux()
	// Register API routes
	mux.HandleFunc("GET /api/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_, err := fmt.Fprint(w, `{"status":"ok"}`)
		if err != nil {
			slog.Error("Failed to write health check response", "error", err)
		}
	})
	mux.HandleFunc("/api/v1/upload-all", api.UploadAll)

	// Setup static file server
	fs := http.FileServer(http.Dir(cfg.StaticFilesDir))
	mux.Handle("GET /assets/", http.StripPrefix("/assets/", fs))

	// Create server with timeouts
	server := &http.Server{
		Addr:         fmt.Sprintf("%s:%d", cfg.Host, cfg.Port),
		Handler:      mux,
		ReadTimeout:  cfg.ReadTimeout,
		WriteTimeout: cfg.WriteTimeout,
	}
	api.Server = server
	return api
}
