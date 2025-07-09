package stats

import (
	"fmt"
	"slices"

	"github.com/a-h/templ"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

// EncounterStats contains statistics about encounters
type EncounterStats struct {
	// Average HP loss per encounter for each act and enemy
	AvgHPLossPerActAndEnemy map[int]map[string]*Mean `json:"avg_hp_loss"`

	// Mortality rate per encounter for each act
	MortalityRatePerAct map[int]map[string]*Rate `json:"mortality_rate"`

	// Average potions used per encounter for each act and enemy
	AvgPotionsPerActAndEnemy map[int]map[string]*Mean `json:"avg_potions"`

	// Top 10 most damage taken fights for each act
	TopDamageFightsPerAct map[int][]DamageFight `json:"top_damage"`
}

// DamageFight represents a fight with damage information
type DamageFight struct {
	Enemies string
	Damage  int
	Floor   int
}

// NewEncounterStats creates a new EncounterStats
func NewEncounterStats() *EncounterStats {
	return &EncounterStats{
		AvgHPLossPerActAndEnemy:  map[int]map[string]*Mean{1: {}, 2: {}, 3: {}, 4: {}},
		MortalityRatePerAct:      map[int]map[string]*Rate{1: {}, 2: {}, 3: {}, 4: {}},
		AvgPotionsPerActAndEnemy: map[int]map[string]*Mean{1: {}, 2: {}, 3: {}, 4: {}},
		TopDamageFightsPerAct:    map[int][]DamageFight{1: {}, 2: {}, 3: {}, 4: {}},
	}
}

// Name returns the name of the stat
func (e *EncounterStats) Name() string {
	return StatTypes[7]
}

// CollectRun processes a run to collect encounter statistics
func (e *EncounterStats) CollectRun(run *model.Run) {
	for _, encounter := range run.EncounterStats {
		act := run.GetAct(encounter.Floor)
		damage := encounter.Damage
		if damage >= 99999 {
			damage -= 99999
		}
		PutIfAbsent(e.AvgHPLossPerActAndEnemy[act], encounter.Enemies, &Mean{}).Add(float64(damage))
		PutIfAbsent(e.MortalityRatePerAct[act], encounter.Enemies, &Rate{}).No++
		if encounter.PotionsUsed >= 0 {
			PutIfAbsent(e.AvgPotionsPerActAndEnemy[act], encounter.Enemies, &Mean{}).Add(float64(encounter.PotionsUsed))
		}
		e.TopDamageFightsPerAct[act] = append(
			e.TopDamageFightsPerAct[act],
			DamageFight{Enemies: encounter.Enemies, Damage: damage, Floor: encounter.Floor},
		)
	}

	if run.KilledBy != "" {
		act := run.GetAct(run.FloorsReached)
		k := PutIfAbsent(e.MortalityRatePerAct[act], run.KilledBy, &Rate{})
		k.Yes++
		k.No--
	}
}

// Finalize sorts the top damage fights for each act
func (e *EncounterStats) Finalize() {
	for act, fights := range e.TopDamageFightsPerAct {
		slices.SortFunc(fights, func(a, b DamageFight) int {
			return b.Damage - a.Damage
		})
		if len(fights) > 10 {
			fights = fights[:10]
		}
		e.TopDamageFightsPerAct[act] = fights
	}
}

type ChartData struct {
	Labels []string
	Data   []float64
}

type pair struct {
	k string
	v float64
}

func MapToSortedChartData(input map[string]float64) ChartData {
	pairs := make([]pair, 0, len(input))
	for k, v := range input {
		pairs = append(pairs, pair{k, v})
	}
	slices.SortFunc(pairs, func(a, b pair) int {
		if a.v > b.v {
			return -1
		} else if a.v < b.v {
			return 1
		}
		return 0
	})

	d := ChartData{}
	for _, pair := range pairs {
		d.Labels = append(d.Labels, pair.k)
		d.Data = append(d.Data, pair.v)
	}
	return d
}

// Render returns a templ component to render the encounter stats
func (e *EncounterStats) Render() templ.Component {
	avgHPLoss := map[int]ChartData{1: {}, 2: {}, 3: {}, 4: {}}
	for act, data := range e.AvgHPLossPerActAndEnemy {
		input := map[string]float64{}
		for name, mean := range data {
			input[name] = mean.Mean()
		}
		avgHPLoss[act] = MapToSortedChartData(input)
	}

	mortalityRate := map[int]ChartData{1: {}, 2: {}, 3: {}, 4: {}}
	for act, data := range e.MortalityRatePerAct {
		input := map[string]float64{}
		for name, rate := range data {
			input[name] = rate.GetRate() * 100
		}
		mortalityRate[act] = MapToSortedChartData(input)
	}

	avgPotions := map[int]ChartData{1: {}, 2: {}, 3: {}, 4: {}}
	for act, data := range e.AvgPotionsPerActAndEnemy {
		input := map[string]float64{}
		for name, mean := range data {
			input[name] = mean.Mean()
		}
		avgPotions[act] = MapToSortedChartData(input)
	}

	topDamageFights := map[int]ChartData{1: {}, 2: {}, 3: {}, 4: {}}
	for act, data := range e.TopDamageFightsPerAct {
		d := ChartData{}
		for _, fight := range data {
			name := fmt.Sprintf("%s (Floor %d)", fight.Enemies, fight.Floor)
			dmg := fight.Damage
			d.Labels = append(d.Labels, name)
			d.Data = append(d.Data, float64(dmg))
		}
		topDamageFights[act] = d
	}
	return EncounterStatsTemplate(
		avgHPLoss,
		mortalityRate,
		avgPotions,
		topDamageFights,
	)
}
