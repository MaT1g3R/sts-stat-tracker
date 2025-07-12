package db

import (
	"context"
	"fmt"
	"log/slog"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
	"github.com/testcontainers/testcontainers-go/wait"

	"github.com/MaT1g3R/stats-tracker/internal/model"
)

type DBSuite struct {
	suite.Suite
	container *postgres.PostgresContainer
	db        *DB
}

func (suite *DBSuite) setDB(ctx context.Context) {
	t := suite.T()
	if suite.db != nil {
		suite.db.Close()
	}
	connStr, err := suite.container.ConnectionString(ctx, "sslmode=disable")
	assert.NoError(t, err)

	logger := slog.New(slog.NewTextHandler(os.Stdout, nil))
	slog.SetDefault(logger)

	db, err := New(ctx, connStr, logger)
	assert.NoError(t, err)

	suite.db = db
}

func (suite *DBSuite) SetupSuite() {
	ctx := context.Background()
	t := suite.T()

	pgContainer, err := postgres.Run(ctx,
		"postgres:17-alpine",
		postgres.WithDatabase("test-db"),
		postgres.WithUsername("postgres"),
		postgres.WithPassword("postgres"),
		testcontainers.WithWaitStrategy(
			wait.ForLog("database system is ready to accept connections").
				WithOccurrence(2).WithStartupTimeout(5*time.Second)),
	)
	assert.NoError(t, err)
	suite.container = pgContainer
	suite.setDB(ctx)
}

func (suite *DBSuite) TearDownSuite() {
	t := suite.T()
	suite.db.Close()
	err := suite.container.Terminate(context.Background())
	assert.NoError(t, err)
}

func (suite *DBSuite) SetupTest() {
	t := suite.T()
	ctx := context.Background()
	const query = `
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;
`
	_, err := suite.db.Pool.Exec(ctx, query)
	assert.NoError(t, err)

	suite.setDB(ctx)

	err = suite.db.RunMigrations()
	assert.NoError(t, err)
}

func (suite *DBSuite) TestUsers() {
	t := suite.T()
	ctx := context.Background()
	_, err := suite.db.CreateOrGetUser(ctx, "user1")
	assert.NoError(t, err)

	res, err := suite.db.SearchUsersByPrefix(ctx, "user1", 10)
	assert.NoError(t, err)
	assert.Len(t, res, 1)

	res, err = suite.db.SearchUsersByPrefix(ctx, "user2", 10)
	assert.NoError(t, err)
	assert.Len(t, res, 0)
}

func (suite *DBSuite) TestLeaderboardInsertAndQuery() {
	t := suite.T()
	ctx := context.Background()

	// Create test users
	_, err := suite.db.CreateOrGetUser(ctx, "player1")
	assert.NoError(t, err)
	_, err = suite.db.CreateOrGetUser(ctx, "player2")
	assert.NoError(t, err)
	_, err = suite.db.CreateOrGetUser(ctx, "player3")
	assert.NoError(t, err)

	// Test all leaderboard kinds and characters
	testCases := []struct {
		name      string
		kind      string
		character string
		entries   []struct {
			user  string
			score float64
			date  time.Time
		}
	}{
		{
			name:      "Speedrun Ironclad",
			kind:      "speedrun",
			character: "ironclad",
			entries: []struct {
				user  string
				score float64
				date  time.Time
			}{
				{"player1", 1200.5, time.Date(2024, 1, 15, 10, 0, 0, 0, time.UTC)},
				{"player2", 1100.3, time.Date(2024, 1, 16, 11, 0, 0, 0, time.UTC)},
				{"player3", 1300.7, time.Date(2024, 1, 17, 12, 0, 0, 0, time.UTC)},
			},
		},
		{
			name:      "Speedrun Silent",
			kind:      "speedrun",
			character: "silent",
			entries: []struct {
				user  string
				score float64
				date  time.Time
			}{
				{"player1", 1150.2, time.Date(2024, 1, 18, 13, 0, 0, 0, time.UTC)},
				{"player2", 1050.8, time.Date(2024, 1, 19, 14, 0, 0, 0, time.UTC)},
			},
		},
		{
			name:      "Speedrun Defect",
			kind:      "speedrun",
			character: "defect",
			entries: []struct {
				user  string
				score float64
				date  time.Time
			}{
				{"player1", 1250.4, time.Date(2024, 1, 20, 15, 0, 0, 0, time.UTC)},
			},
		},
		{
			name:      "Speedrun Watcher",
			kind:      "speedrun",
			character: "watcher",
			entries: []struct {
				user  string
				score float64
				date  time.Time
			}{
				{"player2", 1180.6, time.Date(2024, 1, 21, 16, 0, 0, 0, time.UTC)},
			},
		},
		{
			name:      "Streak Ironclad",
			kind:      "streak",
			character: "ironclad",
			entries: []struct {
				user  string
				score float64
				date  time.Time
			}{
				{"player1", 5.0, time.Date(2024, 1, 22, 17, 0, 0, 0, time.UTC)},
				{"player2", 8.0, time.Date(2024, 1, 23, 18, 0, 0, 0, time.UTC)},
				{"player3", 3.0, time.Date(2024, 1, 24, 19, 0, 0, 0, time.UTC)},
			},
		},
		{
			name:      "Streak Silent",
			kind:      "streak",
			character: "silent",
			entries: []struct {
				user  string
				score float64
				date  time.Time
			}{
				{"player1", 7.0, time.Date(2024, 1, 25, 20, 0, 0, 0, time.UTC)},
				{"player2", 4.0, time.Date(2024, 1, 26, 21, 0, 0, 0, time.UTC)},
			},
		},
		{
			name:      "Streak Defect",
			kind:      "streak",
			character: "defect",
			entries: []struct {
				user  string
				score float64
				date  time.Time
			}{
				{"player3", 6.0, time.Date(2024, 1, 27, 22, 0, 0, 0, time.UTC)},
			},
		},
		{
			name:      "Streak Watcher",
			kind:      "streak",
			character: "watcher",
			entries: []struct {
				user  string
				score float64
				date  time.Time
			}{
				{"player1", 9.0, time.Date(2024, 1, 28, 23, 0, 0, 0, time.UTC)},
			},
		},
	}

	// Insert all entries
	for _, tc := range testCases {
		t.Run("Insert "+tc.name, func(t *testing.T) {
			kind, err := model.GetLeaderboardKind(tc.kind)
			assert.NoError(t, err)

			for _, entry := range tc.entries {
				leaderboardEntry := model.LeaderboardEntry{
					PlayerName: entry.user,
					Character:  tc.character,
					Score:      entry.score,
					Date:       entry.date,
				}

				err := suite.db.InsertLeaderboardEntry(ctx, nil, entry.user, kind, leaderboardEntry)
				assert.NoError(t, err)
			}
		})
	}

	// Test querying each leaderboard
	for _, tc := range testCases {
		t.Run("Query "+tc.name, func(t *testing.T) {
			kind, err := model.GetLeaderboardKind(tc.kind)
			assert.NoError(t, err)

			result, err := suite.db.QueryLeaderboard(ctx, kind, tc.character, 1, nil)
			assert.NoError(t, err)
			assert.NotEmpty(t, result.Entries)
			assert.Len(t, result.Entries, len(tc.entries))

			// Verify entries are sorted correctly
			if tc.kind == "speedrun" {
				// Speedrun should be sorted by score ascending (fastest first)
				for i := 1; i < len(result.Entries); i++ {
					assert.LessOrEqual(t, result.Entries[i-1].Score, result.Entries[i].Score)
				}
			} else if tc.kind == "streak" {
				// Streak should be sorted by score descending (highest first)
				for i := 1; i < len(result.Entries); i++ {
					assert.GreaterOrEqual(t, result.Entries[i-1].Score, result.Entries[i].Score)
				}
			}
		})
	}

	// Test querying "all" characters for speedrun
	t.Run("Query Speedrun All Characters", func(t *testing.T) {
		kind, err := model.GetLeaderboardKind("speedrun")
		assert.NoError(t, err)

		result, err := suite.db.QueryLeaderboard(ctx, kind, "all", 1, nil)
		assert.NoError(t, err)
		assert.NotEmpty(t, result.Entries)
		// Should contain entries from all characters
		assert.GreaterOrEqual(t, len(result.Entries), 7) // We inserted 7 speedrun entries total
	})
}

func (suite *DBSuite) TestLeaderboardMonthlyWinRate() {
	t := suite.T()
	ctx := context.Background()

	// Create multiple test users
	testUsers := []string{"monthly_player1", "monthly_player2", "monthly_player3", "monthly_player4", "monthly_player5"}
	for _, user := range testUsers {
		_, err := suite.db.CreateOrGetUser(ctx, user)
		assert.NoError(t, err)
	}

	kind, err := model.GetLeaderboardKind("winrate-monthly")
	assert.NoError(t, err)

	// Test monthly winrate for all characters including "all"
	characters := []string{"ironclad", "silent", "defect", "watcher", "all"}

	for _, character := range characters {
		t.Run("Monthly WinRate "+character, func(t *testing.T) {
			// Insert entries for various players across different months
			entries := []struct {
				user  string
				score float64
				date  time.Time
			}{
				// January 2024 entries
				{"monthly_player1", 0.75, time.Date(2024, 1, 15, 10, 0, 0, 0, time.UTC)},
				{"monthly_player2", 0.85, time.Date(2024, 1, 20, 11, 0, 0, 0, time.UTC)},
				{"monthly_player3", 0.60, time.Date(2024, 1, 25, 12, 0, 0, 0, time.UTC)},

				// February 2024 entries
				{"monthly_player1", 0.65, time.Date(2024, 2, 10, 12, 0, 0, 0, time.UTC)},
				{"monthly_player2", 0.90, time.Date(2024, 2, 25, 13, 0, 0, 0, time.UTC)},
				{"monthly_player4", 0.70, time.Date(2024, 2, 15, 14, 0, 0, 0, time.UTC)},

				// March 2024 entries
				{"monthly_player1", 0.80, time.Date(2024, 3, 5, 15, 0, 0, 0, time.UTC)},
				{"monthly_player3", 0.95, time.Date(2024, 3, 12, 16, 0, 0, 0, time.UTC)},
				{"monthly_player5", 0.55, time.Date(2024, 3, 20, 17, 0, 0, 0, time.UTC)},

				// April 2024 entries
				{"monthly_player2", 0.88, time.Date(2024, 4, 8, 18, 0, 0, 0, time.UTC)},
				{"monthly_player4", 0.72, time.Date(2024, 4, 18, 19, 0, 0, 0, time.UTC)},

				// May 2024 entries
				{"monthly_player1", 0.92, time.Date(2024, 5, 3, 20, 0, 0, 0, time.UTC)},
				{"monthly_player5", 0.68, time.Date(2024, 5, 28, 21, 0, 0, 0, time.UTC)},
			}

			for _, entry := range entries {
				leaderboardEntry := model.LeaderboardEntry{
					PlayerName: entry.user,
					Character:  character,
					Score:      entry.score,
					Date:       entry.date,
				}

				err := suite.db.InsertLeaderboardEntry(ctx, nil, entry.user, kind, leaderboardEntry)
				assert.NoError(t, err)
			}

			// Query without specifying month (should get latest month - May 2024)
			result, err := suite.db.QueryLeaderboard(ctx, kind, character, 1, nil)
			assert.NoError(t, err)
			assert.NotEmpty(t, result.Entries)
			assert.NotEmpty(t, result.Months)

			// Should have 5 months available (Jan, Feb, Mar, Apr, May 2024)
			assert.Len(t, result.Months, 5)

			// Months should be sorted descending (latest first)
			expectedMonths := []time.Time{
				time.Date(2024, 5, 1, 0, 0, 0, 0, time.UTC), // May
				time.Date(2024, 4, 1, 0, 0, 0, 0, time.UTC), // April
				time.Date(2024, 3, 1, 0, 0, 0, 0, time.UTC), // March
				time.Date(2024, 2, 1, 0, 0, 0, 0, time.UTC), // February
				time.Date(2024, 1, 1, 0, 0, 0, 0, time.UTC), // January
			}

			for i, expectedMonth := range expectedMonths {
				assert.Equal(t, expectedMonth, result.Months[i],
					"Month %d should be %v but got %v", i, expectedMonth, result.Months[i])
			}

			// Query latest month (May 2024) - should have 2 entries
			mayResult := result                 // Already queried above
			assert.Len(t, mayResult.Entries, 2) // monthly_player1 and monthly_player5

			// Verify entries are sorted by score descending (highest winrate first)
			for i := 1; i < len(mayResult.Entries); i++ {
				assert.GreaterOrEqual(t, mayResult.Entries[i-1].Score, mayResult.Entries[i].Score)
			}

			// Verify the specific entries for May
			assert.Equal(t, "monthly_player1", mayResult.Entries[0].PlayerName)
			assert.Equal(t, 0.92, mayResult.Entries[0].Score)
			assert.Equal(t, "monthly_player5", mayResult.Entries[1].PlayerName)
			assert.Equal(t, 0.68, mayResult.Entries[1].Score)

			// Query specific month - March 2024 (should have 3 entries)
			mar2024 := time.Date(2024, 3, 1, 0, 0, 0, 0, time.UTC)
			marResult, err := suite.db.QueryLeaderboard(ctx, kind, character, 1, &mar2024)
			assert.NoError(t, err)
			assert.NotEmpty(t, marResult.Entries)
			assert.Len(t, marResult.Entries, 3) // monthly_player1, monthly_player3, monthly_player5

			// Verify March entries are sorted by score descending
			for i := 1; i < len(marResult.Entries); i++ {
				assert.GreaterOrEqual(t, marResult.Entries[i-1].Score, marResult.Entries[i].Score)
			}

			// Verify the specific entries for March
			assert.Equal(t, "monthly_player3", marResult.Entries[0].PlayerName)
			assert.Equal(t, 0.95, marResult.Entries[0].Score)
			assert.Equal(t, "monthly_player1", marResult.Entries[1].PlayerName)
			assert.Equal(t, 0.80, marResult.Entries[1].Score)
			assert.Equal(t, "monthly_player5", marResult.Entries[2].PlayerName)
			assert.Equal(t, 0.55, marResult.Entries[2].Score)

			// Query specific month - February 2024 (should have 3 entries)
			feb2024 := time.Date(2024, 2, 1, 0, 0, 0, 0, time.UTC)
			febResult, err := suite.db.QueryLeaderboard(ctx, kind, character, 1, &feb2024)
			assert.NoError(t, err)
			assert.NotEmpty(t, febResult.Entries)
			assert.Len(t, febResult.Entries, 3) // monthly_player1, monthly_player2, monthly_player4

			// Verify February entries are sorted by score descending
			for i := 1; i < len(febResult.Entries); i++ {
				assert.GreaterOrEqual(t, febResult.Entries[i-1].Score, febResult.Entries[i].Score)
			}

			// Verify the specific entries for February
			assert.Equal(t, "monthly_player2", febResult.Entries[0].PlayerName)
			assert.Equal(t, 0.90, febResult.Entries[0].Score)
			assert.Equal(t, "monthly_player4", febResult.Entries[1].PlayerName)
			assert.Equal(t, 0.70, febResult.Entries[1].Score)
			assert.Equal(t, "monthly_player1", febResult.Entries[2].PlayerName)
			assert.Equal(t, 0.65, febResult.Entries[2].Score)

			// Query specific month - January 2024 (should have 3 entries)
			jan2024 := time.Date(2024, 1, 1, 0, 0, 0, 0, time.UTC)
			janResult, err := suite.db.QueryLeaderboard(ctx, kind, character, 1, &jan2024)
			assert.NoError(t, err)
			assert.NotEmpty(t, janResult.Entries)
			assert.Len(t, janResult.Entries, 3) // monthly_player1, monthly_player2, monthly_player3

			// Verify January entries are sorted by score descending
			for i := 1; i < len(janResult.Entries); i++ {
				assert.GreaterOrEqual(t, janResult.Entries[i-1].Score, janResult.Entries[i].Score)
			}

			// Verify the specific entries for January
			assert.Equal(t, "monthly_player2", janResult.Entries[0].PlayerName)
			assert.Equal(t, 0.85, janResult.Entries[0].Score)
			assert.Equal(t, "monthly_player1", janResult.Entries[1].PlayerName)
			assert.Equal(t, 0.75, janResult.Entries[1].Score)
			assert.Equal(t, "monthly_player3", janResult.Entries[2].PlayerName)
			assert.Equal(t, 0.60, janResult.Entries[2].Score)

			// Query specific month - April 2024 (should have 2 entries)
			apr2024 := time.Date(2024, 4, 1, 0, 0, 0, 0, time.UTC)
			aprResult, err := suite.db.QueryLeaderboard(ctx, kind, character, 1, &apr2024)
			assert.NoError(t, err)
			assert.NotEmpty(t, aprResult.Entries)
			assert.Len(t, aprResult.Entries, 2) // monthly_player2, monthly_player4

			// Verify April entries are sorted by score descending
			for i := 1; i < len(aprResult.Entries); i++ {
				assert.GreaterOrEqual(t, aprResult.Entries[i-1].Score, aprResult.Entries[i].Score)
			}

			// Verify the specific entries for April
			assert.Equal(t, "monthly_player2", aprResult.Entries[0].PlayerName)
			assert.Equal(t, 0.88, aprResult.Entries[0].Score)
			assert.Equal(t, "monthly_player4", aprResult.Entries[1].PlayerName)
			assert.Equal(t, 0.72, aprResult.Entries[1].Score)
		})
	}
}

func (suite *DBSuite) TestLeaderboardBatchInsert() {
	t := suite.T()
	ctx := context.Background()

	// Create test user
	_, err := suite.db.CreateOrGetUser(ctx, "batch_player")
	assert.NoError(t, err)

	// Test batch insert with multiple kinds and entries
	kinds := []model.LeaderboardKind{}
	entries := []model.LeaderboardEntry{}

	// Add speedrun entries
	speedrunKind, err := model.GetLeaderboardKind("speedrun")
	assert.NoError(t, err)
	kinds = append(kinds, speedrunKind)
	entries = append(entries, model.LeaderboardEntry{
		PlayerName: "batch_player",
		Character:  "ironclad",
		Score:      1000.5,
		Date:       time.Date(2024, 1, 10, 10, 0, 0, 0, time.UTC),
	})

	// Add streak entries
	streakKind, err := model.GetLeaderboardKind("streak")
	assert.NoError(t, err)
	kinds = append(kinds, streakKind)
	entries = append(entries, model.LeaderboardEntry{
		PlayerName: "batch_player",
		Character:  "silent",
		Score:      12.0,
		Date:       time.Date(2024, 1, 11, 11, 0, 0, 0, time.UTC),
	})

	// Add monthly winrate entries
	winrateKind, err := model.GetLeaderboardKind("winrate-monthly")
	assert.NoError(t, err)
	kinds = append(kinds, winrateKind)
	entries = append(entries, model.LeaderboardEntry{
		PlayerName: "batch_player",
		Character:  "defect",
		Score:      0.80,
		Date:       time.Date(2024, 1, 12, 12, 0, 0, 0, time.UTC),
	})

	// Insert all entries in batch
	err = suite.db.InsertLeaderboardEntries(ctx, "batch_player", kinds, entries)
	assert.NoError(t, err)

	// Verify all entries were inserted correctly
	speedrunResult, err := suite.db.QueryLeaderboard(ctx, speedrunKind, "ironclad", 1, nil)
	assert.NoError(t, err)
	assert.NotEmpty(t, speedrunResult.Entries)

	streakResult, err := suite.db.QueryLeaderboard(ctx, streakKind, "silent", 1, nil)
	assert.NoError(t, err)
	assert.NotEmpty(t, streakResult.Entries)

	winrateResult, err := suite.db.QueryLeaderboard(ctx, winrateKind, "defect", 1, nil)
	assert.NoError(t, err)
	assert.NotEmpty(t, winrateResult.Entries)
}

func (suite *DBSuite) TestLeaderboardEdgeCases() {
	t := suite.T()
	ctx := context.Background()

	// Create test users
	_, err := suite.db.CreateOrGetUser(ctx, "edge_player1")
	assert.NoError(t, err)
	_, err = suite.db.CreateOrGetUser(ctx, "edge_player2")
	assert.NoError(t, err)

	t.Run("Duplicate Entry Updates", func(t *testing.T) {
		kind, err := model.GetLeaderboardKind("speedrun")
		assert.NoError(t, err)

		// Insert initial entry
		entry1 := model.LeaderboardEntry{
			PlayerName: "edge_player1",
			Character:  "ironclad",
			Score:      1200.0,
			Date:       time.Date(2024, 1, 10, 10, 0, 0, 0, time.UTC),
		}
		err = suite.db.InsertLeaderboardEntry(ctx, nil, "edge_player1", kind, entry1)
		assert.NoError(t, err)

		// Insert better score (lower for speedrun)
		entry2 := model.LeaderboardEntry{
			PlayerName: "edge_player1",
			Character:  "ironclad",
			Score:      1100.0,
			Date:       time.Date(2024, 1, 11, 11, 0, 0, 0, time.UTC),
		}
		err = suite.db.InsertLeaderboardEntry(ctx, nil, "edge_player1", kind, entry2)
		assert.NoError(t, err)

		// Query and verify the better score is kept
		result, err := suite.db.QueryLeaderboard(ctx, kind, "ironclad", 1, nil)
		assert.NoError(t, err)
		assert.NotEmpty(t, result.Entries)

		// Find our player's entry
		var playerEntry model.LeaderboardEntry
		for _, e := range result.Entries {
			if e.PlayerName == "edge_player1" {
				playerEntry = e
				break
			}
		}
		assert.NotNil(t, playerEntry)
		assert.Equal(t, 1100.0, playerEntry.Score) // Better (lower) score should be kept
	})

	t.Run("Streak Duplicate Entry Updates", func(t *testing.T) {
		kind, err := model.GetLeaderboardKind("streak")
		assert.NoError(t, err)

		// Insert initial entry
		entry1 := model.LeaderboardEntry{
			PlayerName: "edge_player2",
			Character:  "silent",
			Score:      5.0,
			Date:       time.Date(2024, 1, 10, 10, 0, 0, 0, time.UTC),
		}
		err = suite.db.InsertLeaderboardEntry(ctx, nil, "edge_player2", kind, entry1)
		assert.NoError(t, err)

		// Insert better score (higher for streak)
		entry2 := model.LeaderboardEntry{
			PlayerName: "edge_player2",
			Character:  "silent",
			Score:      8.0,
			Date:       time.Date(2024, 1, 11, 11, 0, 0, 0, time.UTC),
		}
		err = suite.db.InsertLeaderboardEntry(ctx, nil, "edge_player2", kind, entry2)
		assert.NoError(t, err)

		// Query and verify the better score is kept
		result, err := suite.db.QueryLeaderboard(ctx, kind, "silent", 1, nil)
		assert.NoError(t, err)
		assert.NotEmpty(t, result.Entries)

		// Find our player's entry
		var playerEntry model.LeaderboardEntry
		for _, e := range result.Entries {
			if e.PlayerName == "edge_player2" {
				playerEntry = e
				break
			}
		}
		assert.NotNil(t, playerEntry)
		assert.Equal(t, 8.0, playerEntry.Score) // Better (higher) score should be kept
	})

	t.Run("Empty Results", func(t *testing.T) {
		kind, err := model.GetLeaderboardKind("speedrun")
		assert.NoError(t, err)

		// Query non-existent character combination
		result, err := suite.db.QueryLeaderboard(ctx, kind, "watcher", 1, nil)
		assert.NoError(t, err)
		assert.Empty(t, result.Entries)
		assert.Equal(t, 1, result.Pages) // Should still have 1 page even if empty
	})

	t.Run("Invalid Leaderboard Kind", func(t *testing.T) {
		_, err := model.GetLeaderboardKind("invalid-kind")
		assert.Error(t, err)
		assert.Contains(t, err.Error(), "unknown leaderboard kind")
	})

	t.Run("Batch Insert Mismatch", func(t *testing.T) {
		kinds := []model.LeaderboardKind{}
		entries := []model.LeaderboardEntry{{}}

		speedrunKind, err := model.GetLeaderboardKind("speedrun")
		assert.NoError(t, err)
		kinds = append(kinds, speedrunKind)
		kinds = append(kinds, speedrunKind) // 2 kinds, 1 entry

		err = suite.db.InsertLeaderboardEntries(ctx, "edge_player1", kinds, entries)
		assert.Error(t, err)
		assert.Contains(t, err.Error(), "number of leaderboard entries does not match")
	})

	t.Run("Empty Batch Insert", func(t *testing.T) {
		err := suite.db.InsertLeaderboardEntries(ctx, "edge_player1", []model.LeaderboardKind{}, []model.LeaderboardEntry{})
		assert.NoError(t, err) // Should handle empty batch gracefully
	})
}

func (suite *DBSuite) TestLeaderboardPagination() {
	t := suite.T()
	ctx := context.Background()

	// Create many test users
	for i := 1; i <= 25; i++ {
		_, err := suite.db.CreateOrGetUser(ctx, fmt.Sprintf("page_player%d", i))
		assert.NoError(t, err)
	}

	kind, err := model.GetLeaderboardKind("speedrun")
	assert.NoError(t, err)

	// Insert 25 entries for speedrun ironclad
	for i := 1; i <= 25; i++ {
		entry := model.LeaderboardEntry{
			PlayerName: fmt.Sprintf("page_player%d", i),
			Character:  "ironclad",
			Score:      float64(1000 + i*10), // Scores from 1010 to 1250
			Date:       time.Date(2024, 1, i, 10, 0, 0, 0, time.UTC),
		}
		err = suite.db.InsertLeaderboardEntry(ctx, nil, fmt.Sprintf("page_player%d", i), kind, entry)
		assert.NoError(t, err)
	}

	// Test first page
	result, err := suite.db.QueryLeaderboard(ctx, kind, "ironclad", 1, nil)
	assert.NoError(t, err)
	assert.Len(t, result.Entries, 10) // PageSize is 10
	assert.Equal(t, 3, result.Pages)  // 25 entries = 3 pages (10+10+5)

	// Verify first page has lowest scores (best speedrun times)
	assert.Equal(t, 1010.0, result.Entries[0].Score)
	assert.Equal(t, 1100.0, result.Entries[9].Score)

	// Test second page
	result, err = suite.db.QueryLeaderboard(ctx, kind, "ironclad", 2, nil)
	assert.NoError(t, err)
	assert.Len(t, result.Entries, 10)
	assert.Equal(t, 1110.0, result.Entries[0].Score)
	assert.Equal(t, 1200.0, result.Entries[9].Score)

	// Test third page
	result, err = suite.db.QueryLeaderboard(ctx, kind, "ironclad", 3, nil)
	assert.NoError(t, err)
	assert.Len(t, result.Entries, 5) // Last page has remaining 5 entries
	assert.Equal(t, 1210.0, result.Entries[0].Score)
	assert.Equal(t, 1250.0, result.Entries[4].Score)

	// Test page beyond available data
	result, err = suite.db.QueryLeaderboard(ctx, kind, "ironclad", 4, nil)
	assert.NoError(t, err)
	assert.Empty(t, result.Entries)
}

func TestDBSuite(t *testing.T) {
	suite.Run(t, new(DBSuite))
}
