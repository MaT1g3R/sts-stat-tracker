package config

import (
	"fmt"
	"log/slog"
	"os"
	"strconv"
	"strings"
	"time"
)

// Config holds all application configuration values
type Config struct {
	// Database settings
	DatabaseURL          string
	DatabaseMaxOpenConns int
	DatabaseMaxIdleConns int
	DatabaseConnLifetime time.Duration

	// Server settings
	Port            int
	Host            string
	ReadTimeout     time.Duration
	WriteTimeout    time.Duration
	ShutdownTimeout time.Duration

	// Application settings
	Environment       string
	LogLevel          slog.Level
	EnableProfiler    bool
	EnableCORS        bool
	AllowedOrigins    []string
	MaxRequestSize    int64
	StaticFilesDir    string
	TemplatesCacheDir string
}

// Load reads configuration from environment variables
func Load() (*Config, error) {
	var config Config

	// Database settings
	config.DatabaseURL = os.Getenv("DATABASE_URL")
	if config.DatabaseURL == "" {
		return nil, fmt.Errorf("DATABASE_URL environment variable is required")
	}

	config.DatabaseMaxOpenConns = getEnvAsInt("DATABASE_MAX_OPEN_CONNS", 25)
	config.DatabaseMaxIdleConns = getEnvAsInt("DATABASE_MAX_IDLE_CONNS", 25)
	config.DatabaseConnLifetime = getEnvAsDuration("DATABASE_CONN_LIFETIME", 5*time.Minute)

	// Server settings
	config.Port = getEnvAsInt("PORT", 8090)
	config.Host = getEnvAsString("HOST", "")
	config.ReadTimeout = getEnvAsDuration("READ_TIMEOUT", 5*time.Second)
	config.WriteTimeout = getEnvAsDuration("WRITE_TIMEOUT", 10*time.Second)
	config.ShutdownTimeout = getEnvAsDuration("SHUTDOWN_TIMEOUT", 30*time.Second)

	// Application settings
	config.Environment = getEnvAsString("ENVIRONMENT", "development")
	config.LogLevel = getEnvAsLogLevel("LOG_LEVEL", slog.LevelInfo)
	config.EnableProfiler = getEnvAsBool("ENABLE_PROFILER", false)
	config.EnableCORS = getEnvAsBool("ENABLE_CORS", true)
	config.AllowedOrigins = getEnvAsStringSlice("ALLOWED_ORIGINS", []string{"http://localhost:8090"})
	config.MaxRequestSize = getEnvAsInt64("MAX_REQUEST_SIZE", 10<<20) // 10 MB
	config.StaticFilesDir = getEnvAsString("STATIC_FILES_DIR", "./assets")
	config.TemplatesCacheDir = getEnvAsString("TEMPLATES_CACHE_DIR", "./tmp/templates")

	return &config, nil
}

// IsDevelopment returns true if the application is running in development mode
func (c *Config) IsDevelopment() bool {
	return c.Environment == "development"
}

// IsProduction returns true if the application is running in production mode
func (c *Config) IsProduction() bool {
	return c.Environment == "production"
}

// IsTest returns true if the application is running in test mode
func (c *Config) IsTest() bool {
	return c.Environment == "test"
}

// Helper functions to get environment variables with fallbacks

func getEnvAsString(key, fallback string) string {
	value, exists := os.LookupEnv(key)
	if !exists {
		return fallback
	}
	return value
}

func getEnvAsInt(key string, fallback int) int {
	valueStr := getEnvAsString(key, "")
	if valueStr == "" {
		return fallback
	}
	value, err := strconv.Atoi(valueStr)
	if err != nil {
		slog.Warn("Invalid integer value for environment variable", "key", key, "value", valueStr, "error", err)
		return fallback
	}
	return value
}

func getEnvAsInt64(key string, fallback int64) int64 {
	valueStr := getEnvAsString(key, "")
	if valueStr == "" {
		return fallback
	}
	value, err := strconv.ParseInt(valueStr, 10, 64)
	if err != nil {
		slog.Warn("Invalid int64 value for environment variable", "key", key, "value", valueStr, "error", err)
		return fallback
	}
	return value
}

func getEnvAsBool(key string, fallback bool) bool {
	valueStr := getEnvAsString(key, "")
	if valueStr == "" {
		return fallback
	}
	value, err := strconv.ParseBool(valueStr)
	if err != nil {
		slog.Warn("Invalid boolean value for environment variable", "key", key, "value", valueStr, "error", err)
		return fallback
	}
	return value
}

func getEnvAsDuration(key string, fallback time.Duration) time.Duration {
	valueStr := getEnvAsString(key, "")
	if valueStr == "" {
		return fallback
	}
	value, err := time.ParseDuration(valueStr)
	if err != nil {
		slog.Warn("Invalid duration value for environment variable", "key", key, "value", valueStr, "error", err)
		return fallback
	}
	return value
}

func getEnvAsStringSlice(key string, fallback []string) []string {
	valueStr := getEnvAsString(key, "")
	if valueStr == "" {
		return fallback
	}
	values := strings.Split(valueStr, ",")
	for i, v := range values {
		values[i] = strings.TrimSpace(v)
	}
	return values
}

func getEnvAsLogLevel(key string, fallback slog.Level) slog.Level {
	valueStr := getEnvAsString(key, "")
	if valueStr == "" {
		return fallback
	}

	switch strings.ToLower(valueStr) {
	case "debug":
		return slog.LevelDebug
	case "info":
		return slog.LevelInfo
	case "warn", "warning":
		return slog.LevelWarn
	case "error":
		return slog.LevelError
	default:
		slog.Warn("Invalid log level for environment variable, using fallback",
			"key", key, "value", valueStr, "fallback", fallback)
		return fallback
	}
}
