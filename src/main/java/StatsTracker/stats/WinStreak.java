package StatsTracker.stats;

import com.megacrit.cardcrawl.characters.AbstractPlayer;

import java.util.List;

public class WinStreak {
    int streak = 0;
    long dateAchieved = 0;
    AbstractPlayer.PlayerClass playerClass = null;

    private WinStreak(int streak, long dateAchieved, AbstractPlayer.PlayerClass playerClass) {
        this.streak = streak;
        this.dateAchieved = dateAchieved;
        this.playerClass = playerClass;
    }

    public Leaderboard.Entry toEntry() {
        return new Leaderboard.Entry(streak, playerClass, Leaderboard.STREAK_KIND, dateAchieved);
    }


    public static WinStreak getWinStreak(List<Run> runs, AbstractPlayer.PlayerClass playerClass) {
        if (playerClass == null) {
            return getHighestRotateWinStreak(runs);
        }
        return getHighestWinStreak(runs, playerClass);
    }

    private static WinStreak getHighestWinStreak(List<Run> runs, AbstractPlayer.PlayerClass playerClass) {
        if (runs.isEmpty()) {
            return null;
        }

        int max = 0;
        int current = 0;
        long currentTime = 0;

        for (Run run : runs) {
            if (run.playerClass != playerClass) {
                continue;
            }

            if (run.isHeartKill) {
                ++current;
            } else {
                if (current > max) {
                    currentTime = run.timestamp;
                    max = current;
                }
                current = 0;
            }
        }

        if (current > max) {
            currentTime = runs.get(runs.size() - 1).timestamp;
            max = current;
        }
        return new WinStreak(max, currentTime, playerClass);
    }


    private static WinStreak getHighestRotateWinStreak(List<Run> runs) {
        if (runs.isEmpty()) {
            return null;
        }

        int max = 0;
        int current = 0;
        long currentTime = 0;

        AbstractPlayer.PlayerClass currentClass;
        AbstractPlayer.PlayerClass previousClass = null;

        for (Run run : runs) {
            currentClass = run.playerClass;
            if (run.isHeartKill) {
                if (previousClass == null || classFollows(currentClass, previousClass)) {
                    ++current;
                } else {
                    if (current > max) {
                        max = current;
                        currentTime = run.timestamp;
                    }
                    current = 1;
                }
            } else {
                if (current > max) {
                    max = current;
                    currentTime = run.timestamp;
                }
                current = 0;
            }
            previousClass = currentClass;
        }
        if (current > max) {
            max = current;
            currentTime = runs.get(runs.size() - 1).timestamp;
        }
        return new WinStreak(max, currentTime, null);
    }

    private static boolean classFollows(AbstractPlayer.PlayerClass currentClass, AbstractPlayer.PlayerClass previousClass) {
        switch (currentClass) {
            case IRONCLAD:
                return previousClass == AbstractPlayer.PlayerClass.WATCHER;
            case THE_SILENT:
                return previousClass == AbstractPlayer.PlayerClass.IRONCLAD;
            case DEFECT:
                return previousClass == AbstractPlayer.PlayerClass.THE_SILENT;
            case WATCHER:
                return previousClass == AbstractPlayer.PlayerClass.DEFECT;
            default:
                return false;
        }
    }
}
