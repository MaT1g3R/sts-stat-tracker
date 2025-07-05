package stats

import (
	"github.com/MaT1g3R/stats-tracker/internal/model"
	"github.com/a-h/templ"
)

type CardPicks struct {
	CardPickPerAct map[int]map[string]*Rate `json:"card_pick_per_act"`
}

func NewCardPicks() *CardPicks {
	return &CardPicks{
		CardPickPerAct: map[int]map[string]*Rate{
			1: {},
			2: {},
			3: {},
			4: {},
		},
	}
}

func (c *CardPicks) Name() string {
	return StatTypes[1]
}

func (c *CardPicks) CollectRun(run *model.Run) {
	for _, choice := range run.CardChoices {
		act := run.GetAct(choice.Floor)
		actMap := c.CardPickPerAct[act]
		pickedRate, ok := actMap[choice.Picked.Name]

		if ok {
			pickedRate.Yes++
		} else {
			actMap[choice.Picked.Name] = &Rate{Yes: 1, No: 0}
		}

		for _, skipped := range choice.Skipped {
			skippedRate, ok := actMap[skipped.Name]
			if ok {
				skippedRate.No++
			} else {
				actMap[skipped.Name] = &Rate{Yes: 0, No: 1}
			}
		}
	}
}

func (c *CardPicks) Finalize() {}

func (c *CardPicks) Render() templ.Component {
	return CardPicksDisplay(c)
}
