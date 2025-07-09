package stats

import (
	"fmt"

	"github.com/a-h/templ"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

var StatTypes = []string{
	"Overview",
	"Card Picks",
	"Card Win Rate",
	"Neow Bonus",
	"Boss Relics",
	"Relic Win Rate",
	"Event Win Rate",
	"Encounter Stats",
}

func GetStatByKind(kind, character string) (Stat, error) {
	switch kind {
	case StatTypes[0]:
		return NewOverview(character), nil
	case StatTypes[1]:
		return NewCardPicks(), nil
	case StatTypes[2]:
		return NewCardWinRate(), nil
	case StatTypes[3]:
		return NewNeowStats(), nil
	case StatTypes[4]:
		return NewBossRelics(), nil
	case StatTypes[5]:
		return NewRelicWinRate(), nil
	case StatTypes[6]:
		return NewEventWinRate(), nil
	case StatTypes[7]:
		return NewEncounterStats(), nil
	default:
		return nil, fmt.Errorf("unknown stat type %s", kind)
	}
}

type Stat interface {
	Name() string
	CollectRun(run *model.Run)
	Finalize()
	Render() templ.Component
}

type Mean struct {
	Values []float64 `json:"values"`
}

func NewMean() *Mean {
	return &Mean{Values: []float64{}}
}

func (m *Mean) Add(v float64) {
	m.Values = append(m.Values, v)
}

func (m *Mean) SampleSize() int {
	return len(m.Values)
}

func (m *Mean) Mean() float64 {
	if len(m.Values) == 0 {
		return 0
	}
	sum := 0.0
	for _, v := range m.Values {
		sum += v
	}
	return sum / float64(len(m.Values))
}

type Rate struct {
	Yes int `json:"yes"`
	No  int `json:"no"`
}

func (r *Rate) GetRate() float64 {
	if (r.Yes + r.No) == 0 {
		return 0
	}
	return float64(r.Yes) / float64(r.Yes+r.No)
}
