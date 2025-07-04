package model

type BossRelic struct {
	Name string `json:"name"`
	Act  int    `json:"act"`
}

type BossRelicChoice struct {
	Picked    string   `json:"picked"`
	NotPicked []string `json:"not_picked"`
}

type Card struct {
	Name     string `json:"name"`
	Upgrades int    `json:"upgrades"`
}

type CardChoice struct {
	Floor   int    `json:"floor"`
	Picked  Card   `json:"picked"`
	Skipped []Card `json:"skipped"`
}

type Encounter struct {
	Damage      int    `json:"damage"`
	Enemies     string `json:"enemies"`
	Floor       int    `json:"floor"`
	PotionsUsed int    `json:"potionsUsed"`
	Turns       int    `json:"turns"`
}

type Event struct {
	EventName        string   `json:"event_name"`
	PlayerChoice     string   `json:"player_choice"`
	Floor            int      `json:"floor"`
	CardsObtained    []string `json:"cards_obtained"`
	CardsRemoved     []string `json:"cards_removed"`
	CardsTransformed []string `json:"cards_transformed"`
	CardsUpgraded    []string `json:"cards_upgraded"`
	RelicsObtained   []string `json:"relics_obtained"`
	RelcisLost       []string `json:"relcis_lost"`
	PotionsObtained  []string `json:"potions_obtained"`
	DamageTaken      int      `json:"damage_taken"`
	DamageHealed     int      `json:"damage_healed"`
	MaxHPLoss        int      `json:"max_hp_loss"`
	MaxHPGain        int      `json:"max_hp_gain"`
	GoldGain         int      `json:"gold_gain"`
	GoldLoss         int      `json:"gold_loss"`
}

type Neow struct {
	Bonus string `json:"bonus"`
	Cost  string `json:"cost"`
}

type Run struct {
	// Core run stats
	Timestamp       int    `json:"timestamp"`
	Score           int    `json:"score"`
	Abandoned       bool   `json:"abandoned"`
	IsHeartKill     bool   `json:"isHeartKill"`
	PlayerClass     string `json:"playerClass"`
	PlayTimeMinutes int    `json:"playtime"`
	FloorsReached   int    `json:"floorsReached"`
	PortalFloor     int    `json:"portalFloor"`
	KilledBy        string `json:"killedBy"`

	// Neow
	NeowPicked    Neow      `json:"neowPicked"`
	NeowSkipped   []Neow    `json:"neowSkipped"`
	BossSwapRelic BossRelic `json:"bossSwapRelic"`

	// Deck
	MasterDeck      []Card   `json:"masterDeck"`
	Relics          []string `json:"relics"`
	RelicsPurchased []string `json:"relicsPurchased"`
	ItemsPurged     []string `json:"itemsPurged"`

	// Encounters
	BossesKilled  int `json:"bossesKilled"`
	EnemiesKilled int `json:"enemiesKilled"`

	// Choices
	BossRelicChoiceStats []BossRelicChoice `json:"bossRelicChoiceStats"`
	CardChoices          []CardChoice      `json:"cardChoices"`
	EncounterStats       []Encounter       `json:"encounterStats"`
	EventStats           []Event           `json:"eventStats"`

	// Meta scaling
	Gold           int `json:"gold"`
	LessonsLearned int `json:"lessonsLearned"`
	MaxAlgo        int `json:"maxAlgo"`
	MaxDagger      int `json:"maxDagger"`
	MaxHP          int `json:"maxHP"`
	MaxSearingBlow int `json:"maxSearingBlow"`
	PotionsCreated int `json:"potionsCreated"`
}

func (r *Run) GetAct(floor int) int {
	if floor <= 17 {
		return 1
	}
	if floor <= 34 {
		return 2
	}
	if r.PortalFloor > 0 {
		if floor <= r.PortalFloor+3 {
			return 3
		} else {
			return 4
		}
	}
	if floor <= 52 {
		return 3
	}
	return 4
}
