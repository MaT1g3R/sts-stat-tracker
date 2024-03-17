package StatsTracker.stats;

import StatsTracker.StatsTracker;
import StatsTracker.YearMonth;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonSyntaxException;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.screens.stats.BattleStats;
import com.megacrit.cardcrawl.screens.stats.EventStats;
import com.megacrit.cardcrawl.screens.stats.ObtainStats;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static basemod.BaseMod.gson;

public class Run implements Comparable<Run> {
    public final RunData runData;
    public final AbstractPlayer.PlayerClass playerClass;

    public final boolean isHeartKill;
    public final boolean isA20;

    public final int enemiesKilled;
    public final int bossesKilled;
    public int singingBowlFloor = -1;
    private int portalFloor = -1;
    public final Neow neowPicked;
    public final List<Neow> neowSkipped = new ArrayList<>();
    public final List<String> relicsPurchased;

    private Run(RunData runData, AbstractPlayer.PlayerClass playerClass) {
        this.runData = runData;
        this.playerClass = playerClass;
        this.isHeartKill = isHeartKill();
        this.isA20 = runData.is_ascension_mode && runData.ascension_level == 20;

        List<String> relics = new ArrayList<>(runData.relics);

        Pair<Integer, Integer> enemiesKilled = enemiesKilled();
        this.enemiesKilled = enemiesKilled.getLeft();
        this.bossesKilled = enemiesKilled.getRight();

        for (EventStats s : runData.event_choices) {
            if (s.event_name.equals("SecretPortal") && s.player_choice.equals("Took Portal")) {
                portalFloor = s.floor;
            }

            if (s.event_name.equals("N'loth") && s.relics_lost != null) {
                s.relics_lost.stream().filter(Objects::nonNull).filter(x -> !x.isEmpty()).forEach(relics::add);
            }
        }

        relicsPurchased = runData.items_purchased.stream().filter(relics::contains).collect(Collectors.toList());

        for (ObtainStats r : runData.relics_obtained) {
            int floor = r.floor;
            String key = r.key;
            if (key.equals("Singing Bowl")) {
                this.singingBowlFloor = floor;
            }
        }

        if (runData.neow_bonus != null && !runData.neow_bonus.isEmpty()) {
            this.neowPicked = new Neow(runData.neow_bonus, runData.neow_cost);
        } else {
            this.neowPicked = null;
        }
        for (int i = 0; i < runData.neow_bonuses_skipped_log.size(); i++) {
            this.neowSkipped.add(new Neow(runData.neow_bonuses_skipped_log.get(i),
                    runData.neow_costs_skipped_log.get(i)));
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

    public int getAct(int floor) {
        if (floor <= 17) {
            return 1;
        }
        if (floor <= 34) {
            return 2;
        }

        if (portalFloor > 0) {
            if (floor <= portalFloor + 3) {
                return 3;
            } else {
                return 4;
            }
        }

        if (floor <= 52) {
            return 3;
        }
        return 4;
    }

    @Override
    public int compareTo(Run run) {
        return RunData.orderByTimestampDesc.compare(run.runData, this.runData);
    }

}
