package pages

import "time"

type PlayerPageProps struct {
	Name             string
	LastSeen         time.Time
	StartDate        time.Time
	EndDate          time.Time
	IncludeAbandoned bool

	Characters []string
	Character  string

	GameVersions []string
	GameVersion  string

	StatTypeOptions []string
	StatType        string

	Profiles        []string
	SelectedProfile string
}

func GameVersionDisplay(v string) string {
	switch v {
	case "sts1":
		return "Slay the Spire"
	default:
		return v
	}
}

func CharacterDisplay(v string) string {
	switch v {
	case "all":
		return "All Characters"
	case "ironclad":
		return "Ironclad"
	case "silent":
		return "The Silent"
	case "defect":
		return "Defect"
	case "watcher":
		return "Watcher"
	default:
		return v
	}
}

func DateToString(date time.Time) string {
	return date.Format("2006-01-02")
}

func StringToDate(date string) (time.Time, error) {
	return time.Parse("2006-01-02", date)
}
