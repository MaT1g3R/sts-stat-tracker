package stats

import (
	"fmt"
	"slices"
	"strconv"
	"time"

	"github.com/a-h/templ"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

// TimeStats contains time-related statistics
type TimeStats struct {
	TotalPlayTime  string
	FastestWin     string
	AverageWinTime string
}

// WinStats contains win/loss statistics
type WinStats struct {
	Wins          string
	Losses        string
	WinRate       string
	BestWinStreak string
}

// GameStats contains general game statistics
type GameStats struct {
	HighestScore  string
	TotalFloors   string
	BossesKilled  string
	EnemiesKilled string
}

// SurvivalStats contains survival rate statistics
type SurvivalStats struct {
	Act1Rate string
	Act2Rate string
	Act3Rate string
	Act4Rate string
	NobRate  string
}

// ScalingStats contains meta scaling statistics
type ScalingStats struct {
	MaxRelics       string
	MaxGold         string
	MaxRemoves      string
	MaxRitualDagger string
	MaxHP           string
	MaxSearingBlow  string
	MaxPotions      string
	MaxAlgorithm    string
	MaxLessons      string
}

type MonthlyWinRateData struct {
	Labels  []string
	Dataset []float64
}

type runForStreak struct {
	char string
	win  bool
}

type Overview struct {
	character     string
	runsForStreak []runForStreak

	TotalPlayTimeMins int `json:"total_play_time_mins"`
	FastestWinMins    int `json:"fastest_win_mins"`

	AverageWinTimeMins Mean `json:"average_win_time_mins"`

	HighestScore  int `json:"highest_score"`
	BestWinStreak int `json:"best_win_streak"`

	Wins       int     `json:"wins"`
	Losses     int     `json:"losses"`
	WinPercent float64 `json:"win_percent"`

	SurvivalRatePerAct map[int]*Rate `json:"survival_rate_per_act"`

	TotalFloorsClimbed int  `json:"total_floors_climbed"`
	BossesKilled       int  `json:"bosses_killed"`
	EnemyKilled        int  `json:"enemy_killed"`
	NobSurvivalRate    Rate `json:"nob_survival_rate"`

	// Meta scaling stats
	MaxRelics       int `json:"max_relics"`
	MaxGold         int `json:"max_gold"`
	MaxRemoves      int `json:"max_removes"`
	MaxRitualDagger int `json:"max_ritual_dagger"`
	MaxHP           int `json:"max_hp"`
	MaxSearingBlow  int `json:"max_searing_blow"`
	MaxPotions      int `json:"max_potions"`
	MaxAlgorithm    int `json:"max_algorithm"`
	MaxLessons      int `json:"max_lessons"`

	MonthlyWinRate map[string]*Rate `json:"monthly_win_rate"`
}

func NewOverview(character string) *Overview {
	return &Overview{
		character: character,
		SurvivalRatePerAct: map[int]*Rate{
			1: {},
			2: {},
			3: {},
			4: {},
		},
		MonthlyWinRate: map[string]*Rate{},
	}
}

func (o *Overview) Name() string {
	return StatTypes[0]
}

func (o *Overview) metaScaling(run *model.Run) {
	// Update max relics
	if len(run.Relics) > o.MaxRelics {
		o.MaxRelics = len(run.Relics)
	}

	// Update max gold
	if run.Gold > o.MaxGold {
		o.MaxGold = run.Gold
	}

	// Update max removes
	if len(run.ItemsPurged) > o.MaxRemoves {
		o.MaxRemoves = len(run.ItemsPurged)
	}

	// Update max ritual dagger
	if run.MaxDagger > o.MaxRitualDagger {
		o.MaxRitualDagger = run.MaxDagger
	}

	// Update max HP
	if run.MaxHP > o.MaxHP {
		o.MaxHP = run.MaxHP
	}

	// Update max searing blow
	if run.MaxSearingBlow > o.MaxSearingBlow {
		o.MaxSearingBlow = run.MaxSearingBlow
	}

	// Update max potions created
	if run.PotionsCreated > o.MaxPotions {
		o.MaxPotions = run.PotionsCreated
	}

	// Update max genetic algorithm
	if run.MaxAlgo > o.MaxAlgorithm {
		o.MaxAlgorithm = run.MaxAlgo
	}

	// Update max lessons learned
	if run.LessonsLearned > o.MaxLessons {
		o.MaxLessons = run.LessonsLearned
	}

}

func (o *Overview) monthlyWinRate(run *model.Run) {
	t := time.Unix(int64(run.Timestamp), 0).UTC()
	monthStr := fmt.Sprintf("%d", t.Month())
	if t.Month() < 10 {
		monthStr = "0" + monthStr
	}
	month := fmt.Sprintf("%d-%s", t.Year(), monthStr)

	rate := PutIfAbsent(o.MonthlyWinRate, month, &Rate{})
	if run.IsHeartKill {
		rate.Yes++
	} else {
		rate.No++
	}
}

func (o *Overview) CollectRun(run *model.Run) {
	o.TotalPlayTimeMins += run.PlayTimeMinutes
	o.TotalFloorsClimbed += run.FloorsReached
	o.BossesKilled += run.BossesKilled
	o.EnemyKilled += run.EnemiesKilled

	act1 := o.SurvivalRatePerAct[1]
	act2 := o.SurvivalRatePerAct[2]
	act3 := o.SurvivalRatePerAct[3]
	act4 := o.SurvivalRatePerAct[4]

	if run.IsHeartKill {
		o.Wins++
		if o.FastestWinMins == 0 || run.PlayTimeMinutes < o.FastestWinMins {
			o.FastestWinMins = run.PlayTimeMinutes
		}
		o.AverageWinTimeMins.Add(float64(run.PlayTimeMinutes))
		for _, i := range []int{1, 2, 3, 4} {
			o.SurvivalRatePerAct[i].Yes++
		}
	} else {
		o.Losses++
		switch run.GetAct(run.FloorsReached) {
		case 1:
			act1.No++
		case 2:
			act1.Yes++
			act2.No++
		case 3:
			act1.Yes++
			act2.Yes++
			act3.No++
		case 4:
			act1.Yes++
			act2.Yes++
			act3.Yes++
			act4.No++
		}
	}

	if o.HighestScore == 0 || run.Score > o.HighestScore {
		o.HighestScore = run.Score
	}

	// Nob survival rate
	for _, e := range run.EncounterStats {
		if e.Enemies == "Gremlin Nob" {
			o.NobSurvivalRate.Yes++
		}
	}
	if run.KilledBy == "Gremlin Nob" {
		o.NobSurvivalRate.Yes--
		o.NobSurvivalRate.No++
	}
	// Streak
	o.runsForStreak = append(o.runsForStreak, runForStreak{
		char: run.PlayerClass,
		win:  run.IsHeartKill,
	})

	o.metaScaling(run)
	o.monthlyWinRate(run)
}

func (o *Overview) Finalize() {
	if o.character == "all" {
		o.BestWinStreak = o.getHighestRotateStreak()
	} else {
		o.BestWinStreak = o.getHighestStreak()
	}
	if o.Wins+o.Losses == 0 {
		o.WinPercent = 0
	} else {
		o.WinPercent = float64(o.Wins) / float64(o.Wins+o.Losses)
	}
}

func (o *Overview) getHighestStreak() int {
	current := 0
	max_ := 0
	for _, r := range o.runsForStreak {
		if r.win {
			current++
		} else {
			max_ = max(max_, current)
			current = 0
		}
	}
	max_ = max(max_, current)
	return max_
}

func (o *Overview) getHighestRotateStreak() int {
	max_ := 0
	current := 0
	currClass := ""
	prevClass := ""

	for _, r := range o.runsForStreak {
		currClass = r.char
		if r.win {
			if prevClass == "" || classFollows(currClass, prevClass) {
				current++
			} else {
				max_ = max(max_, current)
				current = 1
			}
		} else {
			max_ = max(max_, current)
			current = 0
		}
		prevClass = r.char
	}
	max_ = max(max_, current)
	return max_
}

func classFollows(curr string, prev string) bool {
	switch curr {
	case "IRONCLAD":
		return prev == "WATCHER"
	case "THE_SILENT":
		return prev == "IRONCLAD"
	case "DEFECT":
		return prev == "THE_SILENT"
	case "WATCHER":
		return prev == "DEFECT"
	default:
		return false
	}
}

//nolint:funlen
func (o *Overview) Render() templ.Component {
	// Create TimeStats struct
	timeStats := TimeStats{
		TotalPlayTime:  FormatTime(o.TotalPlayTimeMins),
		FastestWin:     FormatTime(o.FastestWinMins),
		AverageWinTime: FormatTime(int(o.AverageWinTimeMins.Mean())),
	}

	// Create WinStats struct
	winStats := WinStats{
		Wins:          strconv.Itoa(o.Wins),
		Losses:        strconv.Itoa(o.Losses),
		WinRate:       FormatPercentageFromRate(o.WinPercent),
		BestWinStreak: strconv.Itoa(o.BestWinStreak),
	}

	// Create GameStats struct
	gameStats := GameStats{
		HighestScore:  fmt.Sprintf("%d", o.HighestScore),
		TotalFloors:   fmt.Sprintf("%d", o.TotalFloorsClimbed),
		BossesKilled:  fmt.Sprintf("%d", o.BossesKilled),
		EnemiesKilled: fmt.Sprintf("%d", o.EnemyKilled),
	}

	// Create SurvivalStats struct
	survivalStats := SurvivalStats{
		Act1Rate: FormatPercentageFromRate(o.SurvivalRatePerAct[1].GetRate()),
		Act2Rate: FormatPercentageFromRate(o.SurvivalRatePerAct[2].GetRate()),
		Act3Rate: FormatPercentageFromRate(o.SurvivalRatePerAct[3].GetRate()),
		Act4Rate: FormatPercentageFromRate(o.SurvivalRatePerAct[4].GetRate()),
		NobRate:  FormatPercentageFromRate(o.NobSurvivalRate.GetRate()),
	}

	// Create ScalingStats struct
	scalingStats := ScalingStats{
		MaxRelics:       fmt.Sprintf("%d", o.MaxRelics),
		MaxGold:         fmt.Sprintf("%d", o.MaxGold),
		MaxRemoves:      fmt.Sprintf("%d", o.MaxRemoves),
		MaxRitualDagger: fmt.Sprintf("%d", o.MaxRitualDagger),
		MaxHP:           fmt.Sprintf("%d", o.MaxHP),
		MaxSearingBlow:  fmt.Sprintf("%d", o.MaxSearingBlow),
		MaxPotions:      fmt.Sprintf("%d", o.MaxPotions),
		MaxAlgorithm:    fmt.Sprintf("%d", o.MaxAlgorithm),
		MaxLessons:      fmt.Sprintf("%d", o.MaxLessons),
	}

	monthlyWinRateData := MonthlyWinRateData{}

	for key := range o.MonthlyWinRate {
		monthlyWinRateData.Labels = append(monthlyWinRateData.Labels, key)
	}
	slices.Sort(monthlyWinRateData.Labels)
	for _, label := range monthlyWinRateData.Labels {
		rate := o.MonthlyWinRate[label]
		if rate == nil {
			rate = &Rate{}
		}
		monthlyWinRateData.Dataset = append(monthlyWinRateData.Dataset, rate.GetRate()*100)
	}

	// Render the template with the structs
	return PlayerOverview(
		timeStats,
		winStats,
		gameStats,
		survivalStats,
		scalingStats,
		monthlyWinRateData,
	)
}
