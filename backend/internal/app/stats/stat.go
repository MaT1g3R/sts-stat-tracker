package stats

import (
	"fmt"

	"github.com/MaT1g3R/stats-tracker/internal/model"
	"github.com/a-h/templ"
)

var StatTypes = []string{
	"Overview",
	"Card Picks",
	"Card Win Rate",
	"Neow Bonus",
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
	values []float64
}

func NewMean() *Mean {
	return &Mean{values: []float64{}}
}

func (m *Mean) Add(v float64) {
	m.values = append(m.values, v)
}

func (m *Mean) SampleSize() int {
	return len(m.values)
}

func (m *Mean) Mean() float64 {
	if len(m.values) == 0 {
		return 0
	}
	sum := 0.0
	for _, v := range m.values {
		sum += v
	}
	return sum / float64(len(m.values))
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
