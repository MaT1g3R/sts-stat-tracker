package config

import (
	"log/slog"
	"os"
	"testing"
	"time"
)

func TestConfig_IsDevelopment(t *testing.T) {
	tests := []struct {
		name        string
		environment string
		expected    bool
	}{
		{
			name:        "development environment",
			environment: "development",
			expected:    true,
		},
		{
			name:        "production environment",
			environment: "production",
			expected:    false,
		},
		{
			name:        "test environment",
			environment: "test",
			expected:    false,
		},
		{
			name:        "empty environment",
			environment: "",
			expected:    false,
		},
		{
			name:        "custom environment",
			environment: "staging",
			expected:    false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			config := &Config{Environment: tt.environment}
			result := config.IsDevelopment()
			if result != tt.expected {
				t.Errorf("IsDevelopment() = %t, want %t", result, tt.expected)
			}
		})
	}
}

func TestConfig_IsProduction(t *testing.T) {
	tests := []struct {
		name        string
		environment string
		expected    bool
	}{
		{
			name:        "production environment",
			environment: "production",
			expected:    true,
		},
		{
			name:        "development environment",
			environment: "development",
			expected:    false,
		},
		{
			name:        "test environment",
			environment: "test",
			expected:    false,
		},
		{
			name:        "empty environment",
			environment: "",
			expected:    false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			config := &Config{Environment: tt.environment}
			result := config.IsProduction()
			if result != tt.expected {
				t.Errorf("IsProduction() = %t, want %t", result, tt.expected)
			}
		})
	}
}

func TestConfig_IsTest(t *testing.T) {
	tests := []struct {
		name        string
		environment string
		expected    bool
	}{
		{
			name:        "test environment",
			environment: "test",
			expected:    true,
		},
		{
			name:        "development environment",
			environment: "development",
			expected:    false,
		},
		{
			name:        "production environment",
			environment: "production",
			expected:    false,
		},
		{
			name:        "empty environment",
			environment: "",
			expected:    false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			config := &Config{Environment: tt.environment}
			result := config.IsTest()
			if result != tt.expected {
				t.Errorf("IsTest() = %t, want %t", result, tt.expected)
			}
		})
	}
}

func TestGetEnvAsString(t *testing.T) {
	tests := []struct {
		name     string
		key      string
		value    string
		fallback string
		expected string
	}{
		{
			name:     "existing environment variable",
			key:      "TEST_STRING_VAR",
			value:    "test_value",
			fallback: "fallback_value",
			expected: "test_value",
		},
		{
			name:     "non-existing environment variable",
			key:      "NON_EXISTING_VAR",
			value:    "",
			fallback: "fallback_value",
			expected: "fallback_value",
		},
		{
			name:     "empty environment variable",
			key:      "EMPTY_VAR",
			value:    "",
			fallback: "fallback_value",
			expected: "fallback_value",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Clean up environment
			_ = os.Unsetenv(tt.key)

			// Set environment variable if value is provided
			if tt.value != "" {
				_ = os.Setenv(tt.key, tt.value)
				defer func(key string) {
					_ = os.Unsetenv(key)
				}(tt.key)
			}

			result := getEnvAsString(tt.key, tt.fallback)
			if result != tt.expected {
				t.Errorf("getEnvAsString(%q, %q) = %q, want %q", tt.key, tt.fallback, result, tt.expected)
			}
		})
	}
}

func TestGetEnvAsInt(t *testing.T) {
	tests := []struct {
		name     string
		key      string
		value    string
		fallback int
		expected int
	}{
		{
			name:     "valid integer",
			key:      "TEST_INT_VAR",
			value:    "42",
			fallback: 10,
			expected: 42,
		},
		{
			name:     "invalid integer",
			key:      "TEST_INVALID_INT",
			value:    "not_a_number",
			fallback: 10,
			expected: 10,
		},
		{
			name:     "empty value",
			key:      "TEST_EMPTY_INT",
			value:    "",
			fallback: 10,
			expected: 10,
		},
		{
			name:     "negative integer",
			key:      "TEST_NEGATIVE_INT",
			value:    "-5",
			fallback: 10,
			expected: -5,
		},
		{
			name:     "zero integer",
			key:      "TEST_ZERO_INT",
			value:    "0",
			fallback: 10,
			expected: 0,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Clean up environment
			_ = os.Unsetenv(tt.key)

			// Set environment variable if value is provided
			if tt.value != "" {
				_ = os.Setenv(tt.key, tt.value)
				defer func(key string) {
					_ = os.Unsetenv(key)
				}(tt.key)
			}

			result := getEnvAsInt(tt.key, tt.fallback)
			if result != tt.expected {
				t.Errorf("getEnvAsInt(%q, %d) = %d, want %d", tt.key, tt.fallback, result, tt.expected)
			}
		})
	}
}

func TestGetEnvAsInt64(t *testing.T) {
	tests := []struct {
		name     string
		key      string
		value    string
		fallback int64
		expected int64
	}{
		{
			name:     "valid int64",
			key:      "TEST_INT64_VAR",
			value:    "9223372036854775807", // max int64
			fallback: 100,
			expected: 9223372036854775807,
		},
		{
			name:     "invalid int64",
			key:      "TEST_INVALID_INT64",
			value:    "not_a_number",
			fallback: 100,
			expected: 100,
		},
		{
			name:     "empty value",
			key:      "TEST_EMPTY_INT64",
			value:    "",
			fallback: 100,
			expected: 100,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Clean up environment
			_ = os.Unsetenv(tt.key)

			// Set environment variable if value is provided
			if tt.value != "" {
				_ = os.Setenv(tt.key, tt.value)
				defer func(key string) {
					_ = os.Unsetenv(key)
				}(tt.key)
			}

			result := getEnvAsInt64(tt.key, tt.fallback)
			if result != tt.expected {
				t.Errorf("getEnvAsInt64(%q, %d) = %d, want %d", tt.key, tt.fallback, result, tt.expected)
			}
		})
	}
}

func TestGetEnvAsBool(t *testing.T) {
	tests := []struct {
		name     string
		key      string
		value    string
		fallback bool
		expected bool
	}{
		{
			name:     "true value",
			key:      "TEST_BOOL_TRUE",
			value:    "true",
			fallback: false,
			expected: true,
		},
		{
			name:     "false value",
			key:      "TEST_BOOL_FALSE",
			value:    "false",
			fallback: true,
			expected: false,
		},
		{
			name:     "1 value",
			key:      "TEST_BOOL_1",
			value:    "1",
			fallback: false,
			expected: true,
		},
		{
			name:     "0 value",
			key:      "TEST_BOOL_0",
			value:    "0",
			fallback: true,
			expected: false,
		},
		{
			name:     "invalid boolean",
			key:      "TEST_INVALID_BOOL",
			value:    "maybe",
			fallback: true,
			expected: true,
		},
		{
			name:     "empty value",
			key:      "TEST_EMPTY_BOOL",
			value:    "",
			fallback: true,
			expected: true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Clean up environment
			_ = os.Unsetenv(tt.key)

			// Set environment variable if value is provided
			if tt.value != "" {
				_ = os.Setenv(tt.key, tt.value)
				defer func(key string) {
					_ = os.Unsetenv(key)
				}(tt.key)
			}

			result := getEnvAsBool(tt.key, tt.fallback)
			if result != tt.expected {
				t.Errorf("getEnvAsBool(%q, %t) = %t, want %t", tt.key, tt.fallback, result, tt.expected)
			}
		})
	}
}

func TestGetEnvAsDuration(t *testing.T) {
	tests := []struct {
		name     string
		key      string
		value    string
		fallback time.Duration
		expected time.Duration
	}{
		{
			name:     "valid duration seconds",
			key:      "TEST_DURATION_S",
			value:    "30s",
			fallback: 10 * time.Second,
			expected: 30 * time.Second,
		},
		{
			name:     "valid duration minutes",
			key:      "TEST_DURATION_M",
			value:    "5m",
			fallback: 1 * time.Minute,
			expected: 5 * time.Minute,
		},
		{
			name:     "invalid duration",
			key:      "TEST_INVALID_DURATION",
			value:    "not_a_duration",
			fallback: 10 * time.Second,
			expected: 10 * time.Second,
		},
		{
			name:     "empty value",
			key:      "TEST_EMPTY_DURATION",
			value:    "",
			fallback: 10 * time.Second,
			expected: 10 * time.Second,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Clean up environment
			_ = os.Unsetenv(tt.key)

			// Set environment variable if value is provided
			if tt.value != "" {
				_ = os.Setenv(tt.key, tt.value)
				defer func(key string) {
					_ = os.Unsetenv(key)
				}(tt.key)
			}

			result := getEnvAsDuration(tt.key, tt.fallback)
			if result != tt.expected {
				t.Errorf("getEnvAsDuration(%q, %v) = %v, want %v", tt.key, tt.fallback, result, tt.expected)
			}
		})
	}
}

func TestGetEnvAsStringSlice(t *testing.T) {
	tests := []struct {
		name     string
		key      string
		value    string
		fallback []string
		expected []string
	}{
		{
			name:     "comma separated values",
			key:      "TEST_STRING_SLICE",
			value:    "value1,value2,value3",
			fallback: []string{"default"},
			expected: []string{"value1", "value2", "value3"},
		},
		{
			name:     "single value",
			key:      "TEST_SINGLE_VALUE",
			value:    "single",
			fallback: []string{"default"},
			expected: []string{"single"},
		},
		{
			name:     "values with spaces",
			key:      "TEST_SPACED_VALUES",
			value:    "value1, value2 , value3",
			fallback: []string{"default"},
			expected: []string{"value1", "value2", "value3"},
		},
		{
			name:     "empty value",
			key:      "TEST_EMPTY_SLICE",
			value:    "",
			fallback: []string{"default1", "default2"},
			expected: []string{"default1", "default2"},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Clean up environment
			_ = os.Unsetenv(tt.key)

			// Set environment variable if value is provided
			if tt.value != "" {
				_ = os.Setenv(tt.key, tt.value)
				defer func(key string) {
					_ = os.Unsetenv(key)
				}(tt.key)
			}

			result := getEnvAsStringSlice(tt.key, tt.fallback)
			if len(result) != len(tt.expected) {
				t.Errorf("getEnvAsStringSlice(%q, %v) length = %d, want %d", tt.key, tt.fallback, len(result), len(tt.expected))
				return
			}

			for i, v := range result {
				if v != tt.expected[i] {
					t.Errorf("getEnvAsStringSlice(%q, %v)[%d] = %q, want %q", tt.key, tt.fallback, i, v, tt.expected[i])
				}
			}
		})
	}
}

func TestGetEnvAsLogLevel(t *testing.T) {
	tests := []struct {
		name     string
		key      string
		value    string
		fallback slog.Level
		expected slog.Level
	}{
		{
			name:     "debug level",
			key:      "TEST_LOG_DEBUG",
			value:    "debug",
			fallback: slog.LevelInfo,
			expected: slog.LevelDebug,
		},
		{
			name:     "info level",
			key:      "TEST_LOG_INFO",
			value:    "info",
			fallback: slog.LevelDebug,
			expected: slog.LevelInfo,
		},
		{
			name:     "warn level",
			key:      "TEST_LOG_WARN",
			value:    "warn",
			fallback: slog.LevelInfo,
			expected: slog.LevelWarn,
		},
		{
			name:     "warning level",
			key:      "TEST_LOG_WARNING",
			value:    "warning",
			fallback: slog.LevelInfo,
			expected: slog.LevelWarn,
		},
		{
			name:     "error level",
			key:      "TEST_LOG_ERROR",
			value:    "error",
			fallback: slog.LevelInfo,
			expected: slog.LevelError,
		},
		{
			name:     "invalid level",
			key:      "TEST_LOG_INVALID",
			value:    "invalid",
			fallback: slog.LevelInfo,
			expected: slog.LevelInfo,
		},
		{
			name:     "empty value",
			key:      "TEST_LOG_EMPTY",
			value:    "",
			fallback: slog.LevelWarn,
			expected: slog.LevelWarn,
		},
		{
			name:     "case insensitive",
			key:      "TEST_LOG_CASE",
			value:    "DEBUG",
			fallback: slog.LevelInfo,
			expected: slog.LevelDebug,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// Clean up environment
			_ = os.Unsetenv(tt.key)

			// Set environment variable if value is provided
			if tt.value != "" {
				_ = os.Setenv(tt.key, tt.value)
				defer func(key string) {
					_ = os.Unsetenv(key)
				}(tt.key)
			}

			result := getEnvAsLogLevel(tt.key, tt.fallback)
			if result != tt.expected {
				t.Errorf("getEnvAsLogLevel(%q, %v) = %v, want %v", tt.key, tt.fallback, result, tt.expected)
			}
		})
	}
}

func TestLoad_MissingDatabaseURL(t *testing.T) {
	// Ensure DATABASE_URL is not set
	_ = os.Unsetenv("DATABASE_URL")

	config, err := Load()
	if err == nil {
		t.Error("Load() expected error for missing DATABASE_URL, got nil")
	}
	if config != nil {
		t.Errorf("Load() expected nil config for missing DATABASE_URL, got %v", config)
	}
}

func TestLoad_WithDatabaseURL(t *testing.T) {
	// Set required DATABASE_URL
	_ = os.Setenv("DATABASE_URL", "postgres://test:test@localhost:5432/test")
	defer func() {
		_ = os.Unsetenv("DATABASE_URL")
	}()

	config, err := Load()
	if err != nil {
		t.Errorf("Load() unexpected error: %v", err)
	}
	if config == nil {
		t.Fatal("Load() returned nil config")
	}

	// Test that defaults are set correctly
	if config.Port != 8090 {
		t.Errorf("Default Port = %d, want 8090", config.Port)
	}

	if config.Environment != "development" {
		t.Errorf("Default Environment = %q, want %q", config.Environment, "development")
	}

	if config.LogLevel != slog.LevelInfo {
		t.Errorf("Default LogLevel = %v, want %v", config.LogLevel, slog.LevelInfo)
	}
}
