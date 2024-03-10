package StatsTracker;

import com.megacrit.cardcrawl.characters.AbstractPlayer;

import java.util.List;

public class ClassStat {
    public final boolean rotate;
    public long playTime = 0;
    public long fastestTime = Integer.MAX_VALUE;
    public int numVictory = 0;
    public int numDeath = 0;
    public int totalFloorsClimbed = 0;
    public int bossKilled = 0;
    public int enemyKilled = 0;
    public final int bestWinStreak;
    public int highestScore = 0;

    public ClassStat(List<Run> runs, boolean rotate) {
        this.rotate = rotate;
        if (rotate) {
            bestWinStreak = getHighestRotateWinStreak(runs);
        } else {
            bestWinStreak = getHighestWinStreak(runs);
        }

        for (Run run : runs) {
            playTime += run.runData.playtime;
            if (run.isHeartKill) {
                fastestTime = Math.min(fastestTime, run.runData.playtime);
            }
            if (run.isHeartKill) {
                ++numVictory;
            } else {
                ++numDeath;
            }
            totalFloorsClimbed += run.runData.floor_reached;
            bossKilled += run.bossesKilled;
            enemyKilled += run.enemiesKilled;
            highestScore = Math.max(highestScore, run.runData.score);
        }
    }

    private static int getHighestWinStreak(List<Run> runs) {
        int max = 0;
        int current = 0;
        for (Run run : runs) {
            if (run.isHeartKill) {
                ++current;
            } else {
                max = Math.max(max, current);
                current = 0;
            }
        }
        max = Math.max(max, current);
        return max;
    }


    private static int getHighestRotateWinStreak(List<Run> runs) {
        int max = 0;
        int current = 0;
        AbstractPlayer.PlayerClass currentClass;
        AbstractPlayer.PlayerClass previousClass = null;

        for (Run run : runs) {
            currentClass = run.playerClass;
            if (run.isHeartKill) {
                if (previousClass == null || classFollows(currentClass, previousClass)) {
                    ++current;
                } else {
                    max = Math.max(max, current);
                    current = 1;
                }
            } else {
                max = Math.max(max, current);
                current = 0;
            }
            previousClass = currentClass;
        }

        max = Math.max(max, current);
        return max;
    }

    private static boolean classFollows(AbstractPlayer.PlayerClass currentClass,
                                        AbstractPlayer.PlayerClass previousClass) {
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
