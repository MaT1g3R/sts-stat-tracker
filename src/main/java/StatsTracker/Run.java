package StatsTracker;

import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonSyntaxException;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.screens.stats.BattleStats;
import com.megacrit.cardcrawl.screens.stats.RunData;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static basemod.BaseMod.gson;

public class Run implements Comparable<Run> {
    public final RunData runData;
    public final AbstractPlayer.PlayerClass playerClass;

    public final boolean isHeartKill;
    public final boolean isA20;

    public final List<String> scoreBreakdown;
    public final int enemiesKilled;
    public final int bossesKilled;

    private Run(RunData runData, AbstractPlayer.PlayerClass playerClass) {
        this.runData = runData;
        this.playerClass = playerClass;
        this.isHeartKill = isHeartKill();
        this.isA20 = runData.is_ascension_mode && runData.ascension_level == 20;
        this.scoreBreakdown = getScoreBreakdown(runData);

        Pair<Integer, Integer> enemiesKilled = enemiesKilled();
        this.enemiesKilled = enemiesKilled.getLeft();
        this.bossesKilled = enemiesKilled.getRight();
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

    private static List<String> getScoreBreakdown(RunData runData) {
        Field scoreBreakdownField;
        try {
            scoreBreakdownField = runData.getClass().getField("score_breakdown");
        } catch (NoSuchFieldException e) {
            return new ArrayList<>();
        }
        try {
            List<String> f = (List<String>) scoreBreakdownField.get(runData);
            if (f == null) {
                return new ArrayList<>();
            }
            return f;
        } catch (IllegalAccessException e) {
            return new ArrayList<>();
        }
    }

    private Pair<Integer, Integer> enemiesKilled() {
        int enemiesKilled = 0;
        int bossesKilled = 0;

        Pattern enemies = Pattern.compile("Enemies Slain \\((\\d+)\\):.*");
        Pattern bosses = Pattern.compile("Bosses Slain \\((\\d+)\\):.*");

        for (String s : scoreBreakdown) {
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

    @Override
    public int compareTo(Run run) {
        return RunData.orderByTimestampDesc.compare(run.runData, this.runData);
    }
}
