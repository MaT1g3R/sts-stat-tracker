package stats

import (
	"fmt"

	"github.com/a-h/templ"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

type RelicWinRate struct {
	WinRate           map[string]*Rate `json:"win_rate"`
	WinRateWhenBought map[string]*Rate `json:"win_rate_when_bought"`
}

type relicWinRateRow struct {
	Relic      string  `json:"relic"`
	WinRate    float64 `json:"win_rate"`
	WinLossStr string  `json:"win_loss_str"`
	TimesSeen  int64   `json:"times_seen"`
}

func NewRelicWinRate() *RelicWinRate {
	return &RelicWinRate{
		WinRate:           map[string]*Rate{},
		WinRateWhenBought: map[string]*Rate{},
	}
}

func (r *RelicWinRate) Name() string {
	return StatTypes[5]
}

func (r *RelicWinRate) CollectRun(run *model.Run) {
	// Track win rate for all relics
	for _, relicName := range run.Relics {
		rateObj := PutIfAbsent(r.WinRate, relicName, &Rate{})
		if run.IsHeartKill {
			rateObj.Yes++
		} else {
			rateObj.No++
		}
	}

	// Track win rate for purchased relics
	for _, relicName := range run.RelicsPurchased {
		rateObj := PutIfAbsent(r.WinRateWhenBought, relicName, &Rate{})
		if run.IsHeartKill {
			rateObj.Yes++
		} else {
			rateObj.No++
		}
	}
}

func (r *RelicWinRate) Finalize() {
}

func (r *RelicWinRate) Render() templ.Component {
	// Prepare the pre-computed rows for all relics
	allRelicsRows := make([]relicWinRateRow, 0)
	for relicName, rate := range r.WinRate {
		total := rate.Yes + rate.No
		if total > 0 {
			allRelicsRows = append(allRelicsRows, relicWinRateRow{
				Relic:      relicName,
				WinRate:    rate.GetRate() * 100,
				WinLossStr: fmt.Sprintf("(%d/%d)", rate.Yes, total),
				TimesSeen:  int64(total),
			})
		}
	}

	// Prepare the pre-computed rows for purchased relics
	purchasedRelicsRows := make([]relicWinRateRow, 0)
	for relicName, rate := range r.WinRateWhenBought {
		total := rate.Yes + rate.No
		if total > 0 {
			purchasedRelicsRows = append(purchasedRelicsRows, relicWinRateRow{
				Relic:      relicName,
				WinRate:    rate.GetRate() * 100,
				WinLossStr: fmt.Sprintf("(%d/%d)", rate.Yes, total),
				TimesSeen:  int64(total),
			})
		}
	}
	return DisplayRelicWinRate(allRelicsRows, purchasedRelicsRows)
}
