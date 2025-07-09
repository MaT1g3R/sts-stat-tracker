package stats

import (
	"context"
	"fmt"
	"io"
	"strconv"

	"github.com/a-h/templ"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

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
	}
}

func (o *Overview) Name() string {
	return StatTypes[0]
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

	// Meta scaling stats
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

func (o *Overview) Finalize() {
	if o.character == "all" {
		o.BestWinStreak = o.getHighestRotateStreak()
	} else {
		o.BestWinStreak = o.getHighestStreak()
	}
	o.WinPercent = float64(o.Wins) / float64(o.Wins+o.Losses)
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

func (o *Overview) Render() templ.Component {
	return templ.ComponentFunc(func(ctx context.Context, w io.Writer) error {
		// Get formatted values
		totalPlayTime := FormatTime(o.TotalPlayTimeMins)
		fastestWin := FormatTime(o.FastestWinMins)
		averageWinTime := FormatTime(int(o.AverageWinTimeMins.Mean()))
		winRate := FormatPercentage(o.WinPercent)

		// Format numbers with commas for readability
		highestScore := fmt.Sprintf("%d", o.HighestScore)
		totalFloors := fmt.Sprintf("%d", o.TotalFloorsClimbed)
		bossesKilled := fmt.Sprintf("%d", o.BossesKilled)
		enemiesKilled := fmt.Sprintf("%d", o.EnemyKilled)

		// Survival rates
		act1Rate := FormatPercentage(o.SurvivalRatePerAct[1].GetRate())
		act2Rate := FormatPercentage(o.SurvivalRatePerAct[2].GetRate())
		act3Rate := FormatPercentage(o.SurvivalRatePerAct[3].GetRate())
		act4Rate := FormatPercentage(o.SurvivalRatePerAct[4].GetRate())
		nobRate := FormatPercentage(o.NobSurvivalRate.GetRate())

		// Meta scaling stats
		maxRelics := fmt.Sprintf("%d", o.MaxRelics)
		maxGold := fmt.Sprintf("%d", o.MaxGold)
		maxRemoves := fmt.Sprintf("%d", o.MaxRemoves)
		maxRitualDagger := fmt.Sprintf("%d", o.MaxRitualDagger)
		maxHP := fmt.Sprintf("%d", o.MaxHP)
		maxSearingBlow := fmt.Sprintf("%d", o.MaxSearingBlow)
		maxPotions := fmt.Sprintf("%d", o.MaxPotions)
		maxAlgorithm := fmt.Sprintf("%d", o.MaxAlgorithm)
		maxLessons := fmt.Sprintf("%d", o.MaxLessons)

		// Render the template
		return PlayerOverview(
			totalPlayTime,
			fastestWin,
			averageWinTime,
			highestScore,
			strconv.Itoa(o.BestWinStreak),
			strconv.Itoa(o.Wins),
			strconv.Itoa(o.Losses),
			winRate,
			act1Rate,
			act2Rate,
			act3Rate,
			act4Rate,
			totalFloors,
			bossesKilled,
			enemiesKilled,
			nobRate,
			maxRelics,
			maxGold,
			maxRemoves,
			maxRitualDagger,
			maxHP,
			maxSearingBlow,
			maxPotions,
			maxAlgorithm,
			maxLessons,
		).Render(ctx, w)
	})
}
