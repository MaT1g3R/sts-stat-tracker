package app

import (
	"fmt"
	"net/http"
	"strconv"
	"time"

	"github.com/MaT1g3R/stats-tracker/internal/db"

	"github.com/MaT1g3R/stats-tracker/internal/model"
	"github.com/MaT1g3R/stats-tracker/internal/ui/pages"
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

func getCharacterOptions(k model.LeaderboardKind) []*pages.CharacterOption {
	if k.Value == "streak" {
		return defaultCharacterOptions("Rotating")
	}
	return defaultCharacterOptions("All Characters")
}

type leaderboardParams struct {
	selectedKind  model.LeaderboardKind
	selectedChar  string
	pageInt       int
	selectedMonth *time.Time

	characterOptions []*pages.CharacterOption
}

func parseLeaderboardParams(r *http.Request) (_ *leaderboardParams, err error) {
	p := &leaderboardParams{}

	p.selectedKind = model.LeaderboardKinds[0]
	if leaderboardType := r.FormValue("kind"); leaderboardType != "" {
		if p.selectedKind, err = model.GetLeaderboardKind(leaderboardType); err != nil {
			return nil, err
		}
	}
	p.characterOptions = getCharacterOptions(p.selectedKind)
	if p.selectedChar = r.FormValue("char"); p.selectedChar != "" {
		found := false
		for _, char := range p.characterOptions {
			if char.Value == p.selectedChar {
				found = true
				char.Selected = true
			}
		}
		if !found {
			return nil, fmt.Errorf("unknown leaderboard character: %s", p.selectedChar)
		}
	} else {
		p.characterOptions[0].Selected = true
		p.selectedChar = p.characterOptions[0].Value
	}

	page := r.FormValue("page")
	if page == "" {
		page = "1"
	}
	p.pageInt, err = strconv.Atoi(page)
	if err != nil {
		return nil, fmt.Errorf("invalid page: %s", page)
	}

	if p.selectedKind.Monthly {
		if m := r.FormValue("month"); m != "" {
			month, err := time.Parse("2006-01", m)
			if err != nil {
				return nil, fmt.Errorf("invalid month: %s", m)
			}
			p.selectedMonth = &month
		}
	}

	return p, nil
}

// handleLeaderboard handles the leaderboard page
func (app *App) handleLeaderboard(w http.ResponseWriter, r *http.Request) {
	// Get the selected leaderboard type from the query parameters
	p, err := parseLeaderboardParams(r)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	res, err := app.db.QueryLeaderboard(r.Context(), p.selectedKind, p.selectedChar, p.pageInt, p.selectedMonth)
	if err != nil {
		http.Error(w, "Failed to query leaderboards", http.StatusInternalServerError)
		return
	}
	monthOptions := make([]*pages.MonthOption, len(res.Months))
	for i, m := range res.Months {
		selected := false
		if p.selectedMonth != nil {
			selected = m.Equal(*p.selectedMonth)
		}
		monthOptions[i] = &pages.MonthOption{
			Value:    m.Format("2006-01"),
			Display:  m.Format("Jan 2006"),
			Selected: selected,
		}
	}

	if p.selectedMonth == nil && len(monthOptions) > 0 {
		monthOptions[0].Selected = true
	}

	// Render the leaderboard page
	_ = pages.Leaderboard(pages.LeaderboardProps{
		Kinds:            model.LeaderboardKinds,
		SelectedKind:     p.selectedKind,
		CharacterOptions: p.characterOptions,
		Entries:          res.Entries,
		MonthOptions:     monthOptions,
		TotalPages:       res.Pages,
		CurrentPage:      p.pageInt,
		PageSize:         db.PageSize,
	}).Render(r.Context(), w)
}
