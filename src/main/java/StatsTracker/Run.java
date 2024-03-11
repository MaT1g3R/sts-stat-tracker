package StatsTracker;

import StatsTracker.stats.RunData;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonSyntaxException;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.screens.stats.BattleStats;
import com.megacrit.cardcrawl.screens.stats.ObtainStats;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static basemod.BaseMod.gson;

public class Run implements Comparable<Run> {
    public final RunData runData;
    public final AbstractPlayer.PlayerClass playerClass;

    public final boolean isHeartKill;
    public final boolean isA20;

    public final int enemiesKilled;
    public final int bossesKilled;
    public int singingBowlFloor = -1;

    private Run(RunData runData, AbstractPlayer.PlayerClass playerClass) {
        this.runData = runData;
        this.playerClass = playerClass;
        this.isHeartKill = isHeartKill();
        this.isA20 = runData.is_ascension_mode && runData.ascension_level == 20;

        Pair<Integer, Integer> enemiesKilled = enemiesKilled();
        this.enemiesKilled = enemiesKilled.getLeft();
        this.bossesKilled = enemiesKilled.getRight();

        for (ObtainStats r : runData.relics_obtained) {
            int floor = r.floor;
            String key = r.key;
            if (key.equals("Singing Bowl")) {
                this.singingBowlFloor = floor;
            }
        }
    }

    private boolean isHeartKill() {
        if (!runData.victory) {
            return false;
        }
        for (int i = runData.damage_taken.size() - 1; i >= 0; --i) {
            BattleStats bs = runData.damage_taken.get(i);
            if (bs.enemies != null && bs.enemies.equals("The Heart")) {
                return true;
            }
        }
        return false;
    }


    private Pair<Integer, Integer> enemiesKilled() {
        int enemiesKilled = 0;
        int bossesKilled = 0;

        Pattern enemies = Pattern.compile("Enemies Slain \\((\\d+)\\):.*");
        Pattern bosses = Pattern.compile("Bosses Slain \\((\\d+)\\):.*");

        for (String s : runData.score_breakdown) {
            Matcher e = enemies.matcher(s);
            Matcher b = bosses.matcher(s);
            if (e.matches()) {
                enemiesKilled = Integer.parseInt(e.group(1));
            } else if (b.matches()) {
                bossesKilled = Integer.parseInt(b.group(1));
            }
        }

        return Pair.of(enemiesKilled, bossesKilled);
    }


    public static Optional<Run> fromFile(FileHandle file) {
        RunData data;
        AbstractPlayer.PlayerClass playerClass;

        try {
            data = gson.fromJson(file.readString(), RunData.class);
        } catch (JsonSyntaxException e) {
            StatsTracker.logger.info("Failed to load RunData from JSON file: " + file.path());
            return Optional.empty();
        }

        try {
            playerClass = AbstractPlayer.PlayerClass.valueOf(data.character_chosen);
        } catch (NullPointerException | IllegalArgumentException e) {
            StatsTracker.logger.info("Run file " +
                    file.path() +
                    " does not use a real character: " +
                    data.character_chosen);
            return Optional.empty();
        }

        if (data.timestamp == null) {
            data.timestamp = file.nameWithoutExtension();
            String exampleDaysSinceUnixStr = "17586";
            boolean assumeDaysSinceUnix = data.timestamp.length() == exampleDaysSinceUnixStr.length();
            if (assumeDaysSinceUnix) {
                try {
                    long secondsInDay = 86400L;
                    long days = Long.parseLong(data.timestamp);
                    data.timestamp = Long.toString(days * secondsInDay);
                } catch (NumberFormatException e) {
                    StatsTracker.logger.info("Run file " +
                            file.path() +
                            " name is could not be parsed into a Timestamp.");
                    return Optional.empty();
                }
            }
        }

        return Optional.of(new Run(data, playerClass));
    }

    public boolean isInRange(YearMonth startDate, YearMonth endDate) {
        YearMonth date = YearMonth.fromDate(new Date(Long.parseLong(runData.timestamp) * 1000));
        return date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0;
    }

    @Override
    public int compareTo(Run run) {
        return RunData.orderByTimestampDesc.compare(run.runData, this.runData);
    }
}
