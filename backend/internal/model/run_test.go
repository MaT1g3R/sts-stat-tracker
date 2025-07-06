package model

import (
	"testing"
)

func TestRun_GetAct(t *testing.T) {
	tests := []struct {
		name        string
		run         Run
		floor       int
		expectedAct int
	}{
		// Act 1 tests (floors 1-17)
		{
			name:        "floor 1 is act 1",
			run:         Run{PortalFloor: 0},
			floor:       1,
			expectedAct: 1,
		},
		{
			name:        "floor 10 is act 1",
			run:         Run{PortalFloor: 0},
			floor:       10,
			expectedAct: 1,
		},
		{
			name:        "floor 17 is act 1",
			run:         Run{PortalFloor: 0},
			floor:       17,
			expectedAct: 1,
		},

		// Act 2 tests (floors 18-34)
		{
			name:        "floor 18 is act 2",
			run:         Run{PortalFloor: 0},
			floor:       18,
			expectedAct: 2,
		},
		{
			name:        "floor 25 is act 2",
			run:         Run{PortalFloor: 0},
			floor:       25,
			expectedAct: 2,
		},
		{
			name:        "floor 34 is act 2",
			run:         Run{PortalFloor: 0},
			floor:       34,
			expectedAct: 2,
		},

		// Act 3 tests (floors 35-52, without portal)
		{
			name:        "floor 35 is act 3 without portal",
			run:         Run{PortalFloor: 0},
			floor:       35,
			expectedAct: 3,
		},
		{
			name:        "floor 45 is act 3 without portal",
			run:         Run{PortalFloor: 0},
			floor:       45,
			expectedAct: 3,
		},
		{
			name:        "floor 52 is act 3 without portal",
			run:         Run{PortalFloor: 0},
			floor:       52,
			expectedAct: 3,
		},

		// Act 4 tests (floors 53+, without portal)
		{
			name:        "floor 53 is act 4 without portal",
			run:         Run{PortalFloor: 0},
			floor:       53,
			expectedAct: 4,
		},
		{
			name:        "floor 60 is act 4 without portal",
			run:         Run{PortalFloor: 0},
			floor:       60,
			expectedAct: 4,
		},

		// Portal tests - Act 3 with portal (floors 35 to PortalFloor+3)
		{
			name:        "floor 35 is act 3 with portal at 40",
			run:         Run{PortalFloor: 40},
			floor:       35,
			expectedAct: 3,
		},
		{
			name:        "floor at portal is act 3",
			run:         Run{PortalFloor: 40},
			floor:       40,
			expectedAct: 3,
		},
		{
			name:        "floor at portal+1 is act 3",
			run:         Run{PortalFloor: 40},
			floor:       41,
			expectedAct: 3,
		},
		{
			name:        "floor at portal+3 is act 3",
			run:         Run{PortalFloor: 40},
			floor:       43,
			expectedAct: 3,
		},

		// Portal tests - Act 4 with portal (floors PortalFloor+4 and beyond)
		{
			name:        "floor at portal+4 is act 4",
			run:         Run{PortalFloor: 40},
			floor:       44,
			expectedAct: 4,
		},
		{
			name:        "floor at portal+10 is act 4",
			run:         Run{PortalFloor: 40},
			floor:       50,
			expectedAct: 4,
		},

		// Edge cases with different portal floors
		{
			name:        "early portal at floor 35",
			run:         Run{PortalFloor: 35},
			floor:       35,
			expectedAct: 3,
		},
		{
			name:        "early portal at floor 35, floor 38 is act 3",
			run:         Run{PortalFloor: 35},
			floor:       38,
			expectedAct: 3,
		},
		{
			name:        "early portal at floor 35, floor 39 is act 4",
			run:         Run{PortalFloor: 35},
			floor:       39,
			expectedAct: 4,
		},

		{
			name:        "late portal at floor 50",
			run:         Run{PortalFloor: 50},
			floor:       50,
			expectedAct: 3,
		},
		{
			name:        "late portal at floor 50, floor 53 is act 3",
			run:         Run{PortalFloor: 50},
			floor:       53,
			expectedAct: 3,
		},
		{
			name:        "late portal at floor 50, floor 54 is act 4",
			run:         Run{PortalFloor: 50},
			floor:       54,
			expectedAct: 4,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := tt.run.GetAct(tt.floor)
			if result != tt.expectedAct {
				t.Errorf("GetAct(%d) = %d, want %d (PortalFloor: %d)",
					tt.floor, result, tt.expectedAct, tt.run.PortalFloor)
			}
		})
	}
}

func TestRun_GetAct_EdgeCases(t *testing.T) {
	t.Run("floor 0", func(t *testing.T) {
		run := Run{PortalFloor: 0}
		result := run.GetAct(0)
		if result != 1 {
			t.Errorf("GetAct(0) = %d, want 1", result)
		}
	})

	t.Run("negative floor", func(t *testing.T) {
		run := Run{PortalFloor: 0}
		result := run.GetAct(-5)
		if result != 1 {
			t.Errorf("GetAct(-5) = %d, want 1", result)
		}
	})

	t.Run("very high floor without portal", func(t *testing.T) {
		run := Run{PortalFloor: 0}
		result := run.GetAct(1000)
		if result != 4 {
			t.Errorf("GetAct(1000) = %d, want 4", result)
		}
	})

	t.Run("very high floor with portal", func(t *testing.T) {
		run := Run{PortalFloor: 40}
		result := run.GetAct(1000)
		if result != 4 {
			t.Errorf("GetAct(1000) = %d, want 4", result)
		}
	})
}

func TestRun_Fields(t *testing.T) {
	// Test that Run struct can be created with all fields
	run := Run{
		Timestamp:       1234567890,
		Score:           1000,
		Abandoned:       false,
		IsHeartKill:     true,
		PlayerClass:     "IRONCLAD",
		PlayTimeMinutes: 45,
		FloorsReached:   52,
		PortalFloor:     40,
		KilledBy:        "Heart of the Spire",
		NeowPicked: Neow{
			Bonus: "Max HP +8",
			Cost:  "None",
		},
		NeowSkipped: []Neow{
			{Bonus: "Gain 100 Gold", Cost: "Lose 7 Max HP"},
		},
		BossSwapRelic: BossRelic{
			Name: "Burning Blood",
			Act:  1,
		},
		MasterDeck: []Card{
			{Name: "Strike", Upgrades: 0},
			{Name: "Defend", Upgrades: 1},
		},
		Relics:          []string{"Burning Blood", "Bag of Marbles"},
		RelicsPurchased: []string{"Bag of Marbles"},
		ItemsPurged:     []string{"Curse of the Bell"},
		BossesKilled:    3,
		EnemiesKilled:   45,
		BossRelicChoiceStats: []BossRelicChoice{
			{
				Picked:    "Runic Pyramid",
				NotPicked: []string{"Philosopher's Stone", "Snecko Eye"},
			},
		},
		CardChoices: []CardChoice{
			{
				Floor:   5,
				Picked:  Card{Name: "Anger", Upgrades: 0},
				Skipped: []Card{{Name: "Bash", Upgrades: 0}},
			},
		},
		EncounterStats: []Encounter{
			{
				Damage:      10,
				Enemies:     "Jaw Worm",
				Floor:       2,
				PotionsUsed: 0,
				Turns:       3,
			},
		},
		EventStats: []Event{
			{
				EventName:        "Dead Adventurer",
				PlayerChoice:     "Fight",
				Floor:            8,
				CardsObtained:    []string{"Strike"},
				CardsRemoved:     []string{},
				CardsTransformed: []string{},
				CardsUpgraded:    []string{},
				RelicsObtained:   []string{},
				RelcisLost:       []string{},
				PotionsObtained:  []string{},
				DamageTaken:      5,
				DamageHealed:     0,
				MaxHPLoss:        0,
				MaxHPGain:        0,
				GoldGain:         25,
				GoldLoss:         0,
			},
		},
		Gold:           150,
		LessonsLearned: 0,
		MaxAlgo:        0,
		MaxDagger:      0,
		MaxHP:          80,
		MaxSearingBlow: 0,
		PotionsCreated: 2,
	}

	// Test some key fields
	if run.PlayerClass != "IRONCLAD" {
		t.Errorf("PlayerClass = %q, want %q", run.PlayerClass, "IRONCLAD")
	}

	if run.PortalFloor != 40 {
		t.Errorf("PortalFloor = %d, want 40", run.PortalFloor)
	}

	if !run.IsHeartKill {
		t.Error("IsHeartKill = false, want true")
	}

	if len(run.MasterDeck) != 2 {
		t.Errorf("MasterDeck length = %d, want 2", len(run.MasterDeck))
	}
}

func TestRun_ZeroValue(t *testing.T) {
	var run Run

	// Test zero values
	if run.Timestamp != 0 {
		t.Errorf("Zero value Timestamp = %d, want 0", run.Timestamp)
	}

	if run.Abandoned != false {
		t.Errorf("Zero value Abandoned = %t, want false", run.Abandoned)
	}

	if run.PortalFloor != 0 {
		t.Errorf("Zero value PortalFloor = %d, want 0", run.PortalFloor)
	}

	// Test GetAct with zero value
	act := run.GetAct(25)
	if act != 2 {
		t.Errorf("Zero value GetAct(25) = %d, want 2", act)
	}
}
