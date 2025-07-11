package model

import (
	"fmt"
	"time"
)

type LeaderboardKind struct {
	Value   string
	Display string

	Monthly bool
}

var LeaderboardKinds = []LeaderboardKind{
	{
		Value:   "streak",
		Display: "Streak",
	},
	{
		Value:   "winrate-monthly",
		Display: "Monthly Win Rate",
		Monthly: true,
	},
	{
		Value:   "speedrun",
		Display: "Fastest Win",
	},
}

func GetLeaderboardKind(k string) (LeaderboardKind, error) {
	for _, kind := range LeaderboardKinds {
		if kind.Value == k {
			return kind, nil
		}
	}
	return LeaderboardKind{}, fmt.Errorf("unknown leaderboard kind: %s", k)
}

type LeaderboardEntry struct {
	Rank       int
	PlayerName string
	Score      float64
	Character  string
	Date       time.Time
}
