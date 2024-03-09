package StatsTracker;

import com.megacrit.cardcrawl.screens.stats.RunData;

import java.util.List;

public class ClassStat {
    public long playTime = 0;
    public long fastestTime = Integer.MAX_VALUE;
    public int numVictory = 0;
    public int numDeath = 0;
    public int totalFloorsClimbed = 0;
    public int bossKilled = 0;
    public int enemyKilled = 0;
    public int bestWinStreak = 0;
    public int highestScore = 0;

    public ClassStat(List<RunData> runs) {
        bestWinStreak = getHighestWinStreak(runs);

        for (RunData rd : runs) {
            boolean isHeartKill = RunHistoryManager.IsHeartKill(rd);
            playTime += rd.playtime;
            if (isHeartKill) {
                fastestTime = Math.min(fastestTime, rd.playtime);
            }
            if (isHeartKill) {
                ++numVictory;
            } else {
                ++numDeath;
            }
            totalFloorsClimbed += rd.floor_reached;
            highestScore = Math.max(highestScore, rd.score);
        }
    }

    private static int getHighestWinStreak(List<RunData> runs) {
        int max = 0;
        int current = 0;
        for (RunData rd : runs) {
            boolean isHeartKill = RunHistoryManager.IsHeartKill(rd);
            if (isHeartKill) {
                ++current;
            } else {
                max = Math.max(max, current);
                current = 0;
            }
        }
        max = Math.max(max, current);
        return max;
    }
}
