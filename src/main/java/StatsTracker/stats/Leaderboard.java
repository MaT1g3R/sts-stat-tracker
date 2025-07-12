package StatsTracker.stats;

import com.megacrit.cardcrawl.characters.AbstractPlayer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Leaderboard {
    public static final String STREAK_KIND = "streak";
    public static final String SPEEDRUN_KIND = "speedrun";
    public static final String WINRATE_KIND = "winrate-monthly";

    public final List<WinStreak> streaks = new ArrayList<>();
    public final Map<AbstractPlayer.PlayerClass, Speedrun> speedrun = new HashMap<>();
    public final Map<AbstractPlayer.PlayerClass, Winrate> winrate = new HashMap<>();

    public static class Entry {
        public final double score;
        public final AbstractPlayer.PlayerClass playerClass;
        public final String kind;
        public final long timestamp;

        public Entry(double score, AbstractPlayer.PlayerClass playerClass, String kind, long timestamp) {
            this.kind = kind;
            this.score = score;
            this.playerClass = playerClass;
            this.timestamp = timestamp;
        }
    }

    public static class Speedrun {
        public final int time;
        public final long timestamp;
        public final AbstractPlayer.PlayerClass playerClass;

        public Speedrun(int time, long timestamp, AbstractPlayer.PlayerClass playerClass) {
            this.time = time;
            this.timestamp = timestamp;
            this.playerClass = playerClass;
        }

        public Entry toEntry() {
            return new Entry(time, playerClass, SPEEDRUN_KIND, timestamp);
        }
    }

    public static class Winrate {
        public final Rate<String> winRate;
        public long timestamp;
        public final AbstractPlayer.PlayerClass playerClass;

        public Winrate(long timestamp, AbstractPlayer.PlayerClass playerClass) {
            this.winRate = new Rate<>("winrate");
            this.timestamp = timestamp;
            this.playerClass = playerClass;
        }

        public Entry toEntry() {
            return new Entry(winRate.percent(), playerClass, WINRATE_KIND, timestamp);
        }
    }

    public Leaderboard(List<Run> runs) {
        for (AbstractPlayer.PlayerClass playerClass : AbstractPlayer.PlayerClass.values()) {
            WinStreak streak = WinStreak.getWinStreak(runs, playerClass);
            if (streak != null) {
                streaks.add(streak);
            }
        }

        WinStreak streak = WinStreak.getWinStreak(runs, null);
        if (streak != null) {
            streaks.add(streak);
        }

        LocalDate today = LocalDate.from(Instant.now().atZone(ZoneId.of("UTC")));

        for (Run run : runs) {
            AbstractPlayer.PlayerClass playerClass = run.playerClass;

            if (run.isHeartKill) {
                speedrun.putIfAbsent(playerClass, new Speedrun(run.playtime, run.timestamp, playerClass));
                speedrun.computeIfPresent(playerClass, (k, v) -> {
                    if (run.playtime < v.time) {
                        return new Speedrun(run.playtime, run.timestamp, playerClass);
                    }
                    return v;
                });
            }

            LocalDate runDate = LocalDate.from(Instant.ofEpochSecond(run.timestamp).atZone(ZoneId.of("UTC")));

            if (today.getYear() == runDate.getYear() && today.getMonthValue() == runDate.getMonthValue()) {
                // winrate for all characters combined
                winrate.putIfAbsent(null, new Winrate(run.timestamp, null));
                Winrate winrateAll = winrate.get(null);

                // winrate for individual characters
                winrate.putIfAbsent(playerClass, new Winrate(run.timestamp, playerClass));
                Winrate w = winrate.get(playerClass);
                w.timestamp = run.timestamp;
                if (run.isHeartKill) {
                    w.winRate.win++;
                    winrateAll.winRate.win++;
                } else {
                    w.winRate.loss++;
                    winrateAll.winRate.loss++;
                }
            }
        }
    }

    public List<Entry> getEntries() {
        List<Entry> entries = new ArrayList<>();
        streaks.forEach(s -> entries.add(s.toEntry()));
        speedrun.forEach((k, v) -> entries.add(v.toEntry()));
        winrate.forEach((k, v) -> {
            if (v.winRate.getSampleSize() >= 10) {
                entries.add(v.toEntry());
            }
        });
        return entries;
    }
}
