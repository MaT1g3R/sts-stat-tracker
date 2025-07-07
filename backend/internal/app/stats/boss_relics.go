package stats

import (
	"github.com/MaT1g3R/stats-tracker/internal/model"
	"github.com/a-h/templ"
)

var sentinel = struct{}{}

type BossRelics struct {
	WinRatePerAct  map[int]map[string]*Rate `json:"win_rate_per_act"`
	PickRatePerAct map[int]map[string]*Rate `json:"pick_rate_per_act"`
}

func NewBossRelics() *BossRelics {
	return &BossRelics{
		WinRatePerAct: map[int]map[string]*Rate{
			0: {},
			1: {},
			2: {},
		},
		PickRatePerAct: map[int]map[string]*Rate{
			1: {},
			2: {},
		},
	}
}

func (b *BossRelics) Name() string {
	return StatTypes[4]
}

func (b *BossRelics) CollectRun(run *model.Run) {
	if run.BossSwapRelic.Name != "" {
		r := PutIfAbsent(b.WinRatePerAct[0], run.BossSwapRelic.Name, &Rate{})
		if run.IsHeartKill {
			r.Yes++
		} else {
			r.No++
		}
	}

	for i, choice := range run.BossRelicChoiceStats {
		act := 2
		if i == 0 {
			act = 1
		}

		winRateMap := b.WinRatePerAct[act]
		pickRateMap := b.PickRatePerAct[act]

		picked := choice.Picked
		skipped := choice.NotPicked
		if picked == "" || picked == "SKIP" {
			picked = "SKIP"
		} else {
			skipped = append(skipped, "SKIP")
		}

		PutIfAbsent(pickRateMap, picked, &Rate{}).Yes++
		PutIfAbsent(winRateMap, picked, &Rate{})
		if run.IsHeartKill {
			winRateMap[picked].Yes++
		} else {
			winRateMap[picked].No++
		}

		for _, s := range skipped {
			PutIfAbsent(pickRateMap, s, &Rate{}).No++
		}
	}
}

func (b *BossRelics) Finalize() {}

func (b *BossRelics) Render() templ.Component {
	bossRelicKeySet := make(map[string]any)

	for _, m := range b.WinRatePerAct {
		for k := range m {
			bossRelicKeySet[k] = sentinel
		}
	}
	for _, m := range b.PickRatePerAct {
		for k := range m {
			bossRelicKeySet[k] = sentinel
		}
	}

	bossRelicKeys := make([]string, 0, len(bossRelicKeySet))
	for k := range bossRelicKeySet {
		bossRelicKeys = append(bossRelicKeys, k)
	}

	return DisplayBossRelics(b, bossRelicKeys)
}
