package stats

import (
	"testing"
)

func TestGetStatByKind(t *testing.T) {
	tests := []struct {
		name      string
		kind      string
		character string
		wantErr   bool
		wantType  string
	}{
		{
			name:      "Overview stat",
			kind:      "Overview",
			character: "IRONCLAD",
			wantErr:   false,
			wantType:  "Overview",
		},
		{
			name:      "Card Picks stat",
			kind:      "Card Picks",
			character: "SILENT",
			wantErr:   false,
			wantType:  "Card Picks",
		},
		{
			name:      "Card Win Rate stat",
			kind:      "Card Win Rate",
			character: "DEFECT",
			wantErr:   false,
			wantType:  "Card Win Rate",
		},
		{
			name:      "Neow Bonus stat",
			kind:      "Neow Bonus",
			character: "WATCHER",
			wantErr:   false,
			wantType:  "Neow Bonus",
		},
		{
			name:      "Unknown stat type",
			kind:      "Unknown",
			character: "IRONCLAD",
			wantErr:   true,
			wantType:  "",
		},
		{
			name:      "Empty stat type",
			kind:      "",
			character: "IRONCLAD",
			wantErr:   true,
			wantType:  "",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			stat, err := GetStatByKind(tt.kind, tt.character)

			if tt.wantErr {
				if err == nil {
					t.Errorf("GetStatByKind() expected error, got nil")
				}
				if stat != nil {
					t.Errorf("GetStatByKind() expected nil stat, got %v", stat)
				}
			} else {
				if err != nil {
					t.Errorf("GetStatByKind() unexpected error: %v", err)
				}
				if stat == nil {
					t.Errorf("GetStatByKind() expected stat, got nil")
				} else {
					if stat.Name() != tt.wantType {
						t.Errorf("GetStatByKind() stat name = %q, want %q", stat.Name(), tt.wantType)
					}
				}
			}
		})
	}
}

func TestStatTypes(t *testing.T) {
	expectedTypes := []string{
		"Overview",
		"Card Picks",
		"Card Win Rate",
		"Neow Bonus",
		"Boss Relics",
		"Relic Win Rate",
		"Event Win Rate",
		"Encounter Stats",
	}

	if len(StatTypes) != len(expectedTypes) {
		t.Errorf("StatTypes length = %d, want %d", len(StatTypes), len(expectedTypes))
	}

	for i, expected := range expectedTypes {
		if i >= len(StatTypes) {
			t.Errorf("StatTypes[%d] missing, want %q", i, expected)
			continue
		}
		if StatTypes[i] != expected {
			t.Errorf("StatTypes[%d] = %q, want %q", i, StatTypes[i], expected)
		}
	}
}

func TestMean(t *testing.T) {
	t.Run("new mean", func(t *testing.T) {
		m := NewMean()
		if m == nil {
			t.Fatal("NewMean() returned nil")
		}
		if m.SampleSize() != 0 {
			t.Errorf("NewMean() sample size = %d, want 0", m.SampleSize())
		}
		if m.Mean() != 0 {
			t.Errorf("NewMean() mean = %f, want 0", m.Mean())
		}
	})

	t.Run("add single value", func(t *testing.T) {
		m := NewMean()
		m.Add(5.0)

		if m.SampleSize() != 1 {
			t.Errorf("SampleSize() = %d, want 1", m.SampleSize())
		}
		if m.Mean() != 5.0 {
			t.Errorf("Mean() = %f, want 5.0", m.Mean())
		}
	})

	t.Run("add multiple values", func(t *testing.T) {
		m := NewMean()
		values := []float64{1.0, 2.0, 3.0, 4.0, 5.0}

		for _, v := range values {
			m.Add(v)
		}

		if m.SampleSize() != 5 {
			t.Errorf("SampleSize() = %d, want 5", m.SampleSize())
		}

		expectedMean := 3.0 // (1+2+3+4+5)/5
		if m.Mean() != expectedMean {
			t.Errorf("Mean() = %f, want %f", m.Mean(), expectedMean)
		}
	})

	t.Run("add negative values", func(t *testing.T) {
		m := NewMean()
		m.Add(-2.0)
		m.Add(2.0)

		if m.SampleSize() != 2 {
			t.Errorf("SampleSize() = %d, want 2", m.SampleSize())
		}
		if m.Mean() != 0.0 {
			t.Errorf("Mean() = %f, want 0.0", m.Mean())
		}
	})

	t.Run("add zero values", func(t *testing.T) {
		m := NewMean()
		m.Add(0.0)
		m.Add(0.0)
		m.Add(0.0)

		if m.SampleSize() != 3 {
			t.Errorf("SampleSize() = %d, want 3", m.SampleSize())
		}
		if m.Mean() != 0.0 {
			t.Errorf("Mean() = %f, want 0.0", m.Mean())
		}
	})

	t.Run("add decimal values", func(t *testing.T) {
		m := NewMean()
		m.Add(1.5)
		m.Add(2.5)

		if m.SampleSize() != 2 {
			t.Errorf("SampleSize() = %d, want 2", m.SampleSize())
		}
		if m.Mean() != 2.0 {
			t.Errorf("Mean() = %f, want 2.0", m.Mean())
		}
	})
}

func TestRate(t *testing.T) {
	t.Run("zero rate", func(t *testing.T) {
		r := Rate{Yes: 0, No: 0}
		if r.GetRate() != 0.0 {
			t.Errorf("GetRate() = %f, want 0.0", r.GetRate())
		}
	})

	t.Run("100% rate", func(t *testing.T) {
		r := Rate{Yes: 10, No: 0}
		if r.GetRate() != 1.0 {
			t.Errorf("GetRate() = %f, want 1.0", r.GetRate())
		}
	})

	t.Run("0% rate", func(t *testing.T) {
		r := Rate{Yes: 0, No: 10}
		if r.GetRate() != 0.0 {
			t.Errorf("GetRate() = %f, want 0.0", r.GetRate())
		}
	})

	t.Run("50% rate", func(t *testing.T) {
		r := Rate{Yes: 5, No: 5}
		if r.GetRate() != 0.5 {
			t.Errorf("GetRate() = %f, want 0.5", r.GetRate())
		}
	})

	t.Run("75% rate", func(t *testing.T) {
		r := Rate{Yes: 3, No: 1}
		if r.GetRate() != 0.75 {
			t.Errorf("GetRate() = %f, want 0.75", r.GetRate())
		}
	})

	t.Run("25% rate", func(t *testing.T) {
		r := Rate{Yes: 1, No: 3}
		if r.GetRate() != 0.25 {
			t.Errorf("GetRate() = %f, want 0.25", r.GetRate())
		}
	})

	t.Run("rate with large numbers", func(t *testing.T) {
		r := Rate{Yes: 1000, No: 3000}
		if r.GetRate() != 0.25 {
			t.Errorf("GetRate() = %f, want 0.25", r.GetRate())
		}
	})
}
