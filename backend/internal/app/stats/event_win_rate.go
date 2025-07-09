package stats

import (
	"github.com/a-h/templ"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

type EventWinRate struct {
	WinRatePerAct map[int]map[string]*Rate `json:"win_rate_per_act"`
}

func NewEventWinRate() *EventWinRate {
	return &EventWinRate{
		WinRatePerAct: map[int]map[string]*Rate{
			1: {},
			2: {},
			3: {},
		},
	}
}

func (e *EventWinRate) Name() string {
	return StatTypes[6]
}

func (e *EventWinRate) CollectRun(run *model.Run) {
	// Track win rate for events by act
	for _, event := range run.EventStats {
		act := run.GetAct(event.Floor)
		if act > 3 {
			// Skip act 4 events
			continue
		}

		rateObj := PutIfAbsent(e.WinRatePerAct[act], event.EventName, &Rate{})
		if run.IsHeartKill {
			rateObj.Yes++
		} else {
			rateObj.No++
		}
	}
}

func (e *EventWinRate) Finalize() {
	// No pre-computation needed, all calculations will be done in JavaScript
}

func (e *EventWinRate) Render() templ.Component {
	return DisplayEventWinRate(e)
}
