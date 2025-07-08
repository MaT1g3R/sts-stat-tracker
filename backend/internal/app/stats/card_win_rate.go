package stats

import (
	"github.com/a-h/templ"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

type CardWinRate struct {
	WinRates map[string]*Rate `json:"win_rates"`
}

func NewCardWinRate() *CardWinRate {
	return &CardWinRate{
		WinRates: make(map[string]*Rate),
	}
}

func (c *CardWinRate) Name() string {
	return StatTypes[2]
}

func (c *CardWinRate) CollectRun(run *model.Run) {
	distinctCards := make(map[string]any)
	for _, card := range run.MasterDeck {
		distinctCards[card.Name] = struct{}{}
	}
	for card := range distinctCards {
		_, ok := c.WinRates[card]
		if !ok {
			c.WinRates[card] = &Rate{}
		}
		if run.IsHeartKill {
			c.WinRates[card].Yes++
		} else {
			c.WinRates[card].No++
		}
	}
}

func (c *CardWinRate) Finalize() {}

func (c *CardWinRate) Render() templ.Component {
	return CardWinRateDisplay(c)
}
