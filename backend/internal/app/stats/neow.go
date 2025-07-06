package stats

import (
	"github.com/MaT1g3R/stats-tracker/internal/model"
	"github.com/a-h/templ"
)

var bonusStrings = map[string]string{
	"THREE_CARDS":             "Choose one of three cards to obtain.",
	"RANDOM_COLORLESS":        "Choose an uncommon Colorless card to obtain.",
	"RANDOM_COMMON_RELIC":     "Obtain a random common relic.",
	"REMOVE_CARD":             "Remove a card.",
	"TRANSFORM_CARD":          "Transform a card.",
	"UPGRADE_CARD":            "Upgrade a card.",
	"THREE_ENEMY_KILL":        "Enemies in the first three combats have 1 HP.",
	"THREE_SMALL_POTIONS":     "Gain 3 random potions.",
	"TEN_PERCENT_HP_BONUS":    "Gain 10% Max HP.",
	"ONE_RANDOM_RARE_CARD":    "Gain a random Rare card.",
	"HUNDRED_GOLD":            "Gain 100 gold.",
	"TWO_FIFTY_GOLD":          "Gain 250 gold.",
	"TWENTY_PERCENT_HP_BONUS": "Gain 20% Max HP.",
	"RANDOM_COLORLESS_2":      "Choose a Rare colorless card to obtain.",
	"THREE_RARE_CARDS":        "Choose a Rare card to obtain.",
	"REMOVE_TWO":              "Remove two cards.",
	"TRANSFORM_TWO_CARDS":     "Transform two cards.",
	"ONE_RARE_RELIC":          "Obtain a random Rare relic.",
	"BOSS_RELIC":              "Lose your starter relic. Obtain a random Boss relic.",
}

var costStrings = map[string]string{
	"CURSE":               "Gain a curse.",
	"NO_GOLD":             "Lose all gold.",
	"TEN_PERCENT_HP_LOSS": "Lose 10% Max HP.",
	"PERCENT_DAMAGE":      "Take damage.",
	"TWO_STARTER_CARDS":   "Add two starter cards.",
	"ONE_STARTER_CARD":    "Add a starter card.",
}

type NeowRow struct {
	Bonus    string `json:"bonus"`
	Cost     string `json:"cost"`
	WinRate  Rate   `json:"win_rate"`
	PickRate Rate   `json:"pick_rate"`
}

type NeowStats struct {
	winRate  map[model.Neow]*Rate
	pickRate map[model.Neow]*Rate
	keySet   map[model.Neow]any

	NeowRows []NeowRow `json:"neow_rows"`
}

func NewNeowStats() *NeowStats {
	return &NeowStats{
		winRate:  make(map[model.Neow]*Rate),
		pickRate: make(map[model.Neow]*Rate),
		keySet:   make(map[model.Neow]any),
	}
}

func (n *NeowStats) Name() string {
	return StatTypes[3]
}

func (n *NeowStats) CollectRun(run *model.Run) {
	winRate := PutIfAbsent(n.winRate, run.NeowPicked, &Rate{})
	n.keySet[run.NeowPicked] = struct{}{}
	if run.IsHeartKill {
		winRate.Yes++
	} else {
		winRate.No++
	}

	if len(run.NeowSkipped) > 0 {
		PutIfAbsent(n.pickRate, run.NeowPicked, &Rate{}).Yes++
		for _, skipped := range run.NeowSkipped {
			n.keySet[skipped] = struct{}{}
			PutIfAbsent(n.pickRate, skipped, &Rate{}).No++
		}
	}
}

func (n *NeowStats) Finalize() {
	for neow := range n.keySet {
		cost := neow.Cost
		if cost == "" || cost == "NONE" {
			cost = "-"
		}
		winRate, ok := n.winRate[neow]
		if !ok {
			winRate = &Rate{}
		}
		pickRate, ok := n.pickRate[neow]
		if !ok {
			pickRate = &Rate{}
		}

		row := NeowRow{
			Bonus:    neow.Bonus,
			Cost:     cost,
			WinRate:  *winRate,
			PickRate: *pickRate,
		}
		n.NeowRows = append(n.NeowRows, row)
	}
}

func (n *NeowStats) Render() templ.Component {
	return NeowDisplay(n.NeowRows)
}
