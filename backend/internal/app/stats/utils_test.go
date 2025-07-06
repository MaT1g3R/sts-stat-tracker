package stats

import (
	"math"
	"testing"
)

func TestFormatTime(t *testing.T) {
	tests := []struct {
		name     string
		seconds  int
		expected string
	}{
		{
			name:     "zero seconds",
			seconds:  0,
			expected: "00m 00s",
		},
		{
			name:     "only seconds",
			seconds:  45,
			expected: "00m 45s",
		},
		{
			name:     "minutes and seconds",
			seconds:  125, // 2m 5s
			expected: "02m 05s",
		},
		{
			name:     "exactly one hour",
			seconds:  3600,
			expected: "01h 00m 00s",
		},
		{
			name:     "hours, minutes, and seconds",
			seconds:  3725, // 1h 2m 5s
			expected: "01h 02m 05s",
		},
		{
			name:     "multiple hours",
			seconds:  7325, // 2h 2m 5s
			expected: "02h 02m 05s",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := FormatTime(tt.seconds)
			if result != tt.expected {
				t.Errorf("FormatTime(%d) = %q, want %q", tt.seconds, result, tt.expected)
			}
		})
	}
}

func TestFormatPercentage(t *testing.T) {
	tests := []struct {
		name     string
		value    float64
		expected string
	}{
		{
			name:     "zero percentage",
			value:    0.0,
			expected: "0.0%",
		},
		{
			name:     "normal percentage",
			value:    0.75,
			expected: "75.0%",
		},
		{
			name:     "small percentage",
			value:    0.123,
			expected: "12.3%",
		},
		{
			name:     "100 percent",
			value:    1.0,
			expected: "100.0%",
		},
		{
			name:     "over 100 percent",
			value:    1.25,
			expected: "125.0%",
		},
		{
			name:     "NaN value",
			value:    math.NaN(),
			expected: "0.0%",
		},
		{
			name:     "positive infinity",
			value:    math.Inf(1),
			expected: "0.0%",
		},
		{
			name:     "negative infinity",
			value:    math.Inf(-1),
			expected: "0.0%",
		},
		{
			name:     "negative percentage",
			value:    -0.25,
			expected: "-25.0%",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := FormatPercentage(tt.value)
			if result != tt.expected {
				t.Errorf("FormatPercentage(%f) = %q, want %q", tt.value, result, tt.expected)
			}
		})
	}
}

func TestPutIfAbsent(t *testing.T) {
	t.Run("key exists", func(t *testing.T) {
		m := map[string]int{"existing": 42}
		result := PutIfAbsent(m, "existing", 100)

		if result != 42 {
			t.Errorf("PutIfAbsent returned %d, want 42", result)
		}

		if m["existing"] != 42 {
			t.Errorf("Map value changed to %d, should remain 42", m["existing"])
		}
	})

	t.Run("key does not exist", func(t *testing.T) {
		m := map[string]int{}
		result := PutIfAbsent(m, "new", 100)

		if result != 100 {
			t.Errorf("PutIfAbsent returned %d, want 100", result)
		}

		if m["new"] != 100 {
			t.Errorf("Map value is %d, want 100", m["new"])
		}
	})

	t.Run("with different types", func(t *testing.T) {
		m := map[int]string{1: "one"}
		result := PutIfAbsent(m, 2, "two")

		if result != "two" {
			t.Errorf("PutIfAbsent returned %q, want %q", result, "two")
		}

		if m[2] != "two" {
			t.Errorf("Map value is %q, want %q", m[2], "two")
		}
	})

	t.Run("empty map", func(t *testing.T) {
		m := make(map[string]int)
		result := PutIfAbsent(m, "first", 1)

		if result != 1 {
			t.Errorf("PutIfAbsent returned %d, want 1", result)
		}

		if len(m) != 1 {
			t.Errorf("Map length is %d, want 1", len(m))
		}
	})
}
