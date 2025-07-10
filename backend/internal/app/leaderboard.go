package app

import (
	"fmt"
	"github.com/MaT1g3R/stats-tracker/internal/ui/pages"
	"net/http"
	"time"
)

func defaultCharacterOptions(allDisplay string) []*pages.CharacterOption {
	return []*pages.CharacterOption{
		{
			Value:   "all",
			Display: allDisplay,
		},
		{
			Value:   "ironclad",
			Display: "Ironclad",
		},
		{
			Value:   "silent",
			Display: "Silent",
		},
		{
			Value:   "defect",
			Display: "Defect",
		},
		{
			Value:   "watcher",
			Display: "Watcher",
		},
	}
}

var LeaderboardKinds = []pages.LeaderboardKind{
	{
		Value:   "streak",
		Display: "Streak",
	},
	{
		Value:   "winrate",
		Display: "Win Rate",
	},
	{
		Value:   "winrate-monthly",
		Display: "Monthly Win Rate",
	},
	{
		Value:   "speedrun",
		Display: "Fastest Win",
	},
}

func getLeaderboardKind(k string) (pages.LeaderboardKind, error) {
	for _, kind := range LeaderboardKinds {
		if kind.Value == k {
			return kind, nil
		}
	}
	return pages.LeaderboardKind{}, fmt.Errorf("unknown leaderboard kind: %s", k)
}

func hasMonthOptions(k pages.LeaderboardKind) bool {
	switch k.Value {
	case "winrate-monthly":
		return true
	default:
		return false
	}
}

func getCharacterOptions(k pages.LeaderboardKind) []*pages.CharacterOption {
	if k.Value == "streak" {
		return defaultCharacterOptions("Rotating")
	}
	return defaultCharacterOptions("All")
}

// handleLeaderboard handles the leaderboard page
func (app *App) handleLeaderboard(w http.ResponseWriter, r *http.Request) {
	// Get the selected leaderboard type from the query parameters
	var err error
	selectedKind := LeaderboardKinds[0]
	if leaderboardType := r.FormValue("kind"); leaderboardType != "" {
		if selectedKind, err = getLeaderboardKind(leaderboardType); err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
	}

	var monthOptions []*pages.MonthOption
	if hasMonthOptions(selectedKind) {
		selectedMonth := r.FormValue("month")
		monthOptions = getMockMonths()
		if selectedMonth == "" {
			monthOptions[0].Selected = true
			selectedMonth = monthOptions[0].Value
		} else {
			found := false
			for _, monthOption := range monthOptions {
				if monthOption.Value == selectedMonth {
					found = true
					monthOption.Selected = true
					break
				}
			}
			if !found {
				http.Error(
					w,
					fmt.Sprintf("unknown leaderboard month: %s", selectedMonth),
					http.StatusBadRequest,
				)
				return
			}
		}
	}

	entries := getMockEntries(selectedKind)
	characterOptions := getCharacterOptions(selectedKind)
	if selectedChar := r.FormValue("char"); selectedChar != "" {
		found := false
		for _, char := range characterOptions {
			if char.Value == selectedChar {
				found = true
				char.Selected = true
			}
		}
		if !found {
			http.Error(
				w,
				fmt.Sprintf("unknown leaderboard character: %s", selectedChar),
				http.StatusBadRequest,
			)
			return
		}
	} else {
		characterOptions[0].Selected = true
	}

	// Render the leaderboard page
	_ = pages.Leaderboard(pages.LeaderboardProps{
		Kinds:            LeaderboardKinds,
		SelectedKind:     selectedKind,
		CharacterOptions: characterOptions,
		Entries:          entries,
		MonthOptions:     monthOptions,
	}).Render(r.Context(), w)
}

func getMockMonths() []*pages.MonthOption {
	return []*pages.MonthOption{
		{
			Value:    "2025-07",
			Display:  "July 2025",
			Selected: false,
		},
		{
			Value:    "2025-06",
			Display:  "June 2025",
			Selected: false,
		},
		{
			Value:    "2025-05",
			Display:  "May 2025",
			Selected: false,
		},
		{
			Value:    "2025-04",
			Display:  "April 2025",
			Selected: false,
		},
		{
			Value:    "2025-03",
			Display:  "March 2025",
			Selected: false,
		},
		{
			Value:    "2025-02",
			Display:  "February 2025",
			Selected: false,
		},
		{
			Value:    "2025-01",
			Display:  "January 2025",
			Selected: false,
		},
		{
			Value:    "2024-12",
			Display:  "December 2024",
			Selected: false,
		},
		{
			Value:    "2024-11",
			Display:  "November 2024",
			Selected: false,
		},
		{
			Value:    "2024-10",
			Display:  "October 2024",
			Selected: false,
		},
		{
			Value:    "2024-09",
			Display:  "September 2024",
			Selected: false,
		},
		{
			Value:    "2024-08",
			Display:  "August 2024",
			Selected: false,
		},
	}
}

func getMockEntries(k pages.LeaderboardKind) []pages.LeaderboardEntry {
	entries := make([]pages.LeaderboardEntry, 0)
	characters := []string{"ironclad", "silent", "defect", "watcher"}
	now := time.Now()

	for i := 0; i < 10; i++ {
		entry := pages.LeaderboardEntry{
			Rank:       i + 1,
			PlayerName: fmt.Sprintf("Player%d", i+1),
			Character:  characters[i%len(characters)],
			Date:       now.AddDate(0, 0, -i),
		}

		switch k.Value {
		case "streak":
			entry.Score = float64(20 - i)
		case "winrate":
			entry.Score = float64(100 - i*5)
		case "winrate-monthly":
			entry.Score = float64(95 - i*4)
		case "speedrun":
			entry.Score = float64(600 + i*30)
		}

		entries = append(entries, entry)
	}
	return entries
}
