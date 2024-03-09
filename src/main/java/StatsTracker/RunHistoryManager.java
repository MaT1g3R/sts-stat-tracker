package StatsTracker;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonSyntaxException;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.screens.stats.BattleStats;
import com.megacrit.cardcrawl.screens.stats.RunData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static basemod.BaseMod.gson;

public class RunHistoryManager {
    public List<RunData> allRuns = new ArrayList<>();
    public List<RunData> ironcladRuns = new ArrayList<>();
    public List<RunData> silentRuns = new ArrayList<>();
    public List<RunData> defectRuns = new ArrayList<>();
    public List<RunData> watcherRuns = new ArrayList<>();

    public static boolean IsHeartKill(RunData run) {
        if (!run.victory) {
            return false;
        }
        for (int i = run.damage_taken.size() - 1; i >= 0; --i) {
            BattleStats bs = run.damage_taken.get(i);
            if (bs.enemies != null && bs.enemies.equals("The Heart")) {
                return true;
            }
        }
        return false;
    }

    private Stream<RunData> loadData() {
        FileHandle[] subFolders = Gdx.files.local("runs" + File.separator).list();

        ArrayList<RunData> tmpRuns = new ArrayList<>();

        FileHandle[] var2 = subFolders;
        int var3 = subFolders.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            FileHandle subFolder = var2[var4];
            switch (CardCrawlGame.saveSlot) {
                case 0:
                    if (subFolder.name().contains("0_") ||
                            subFolder.name().contains("1_") ||
                            subFolder.name().contains("2_")) {
                        continue;
                    }
                    break;
                default:
                    if (!subFolder.name().contains(CardCrawlGame.saveSlot + "_")) {
                        continue;
                    }
            }

            FileHandle[] var6 = subFolder.list();
            int var7 = var6.length;

            for (int var8 = 0; var8 < var7; ++var8) {
                FileHandle file = var6[var8];

                try {
                    RunData data = gson.fromJson(file.readString(), RunData.class);
                    if (data != null && data.timestamp == null) {
                        data.timestamp = file.nameWithoutExtension();
                        String exampleDaysSinceUnixStr = "17586";
                        boolean assumeDaysSinceUnix = data.timestamp.length() == exampleDaysSinceUnixStr.length();
                        if (assumeDaysSinceUnix) {
                            try {
                                long secondsInDay = 86400L;
                                long days = Long.parseLong(data.timestamp);
                                data.timestamp = Long.toString(days * secondsInDay);
                            } catch (NumberFormatException var18) {
                                StatsTracker.logger.info("Run file " +
                                        file.path() +
                                        " name is could not be parsed into a Timestamp.");
                                data = null;
                            }
                        }
                    }

                    if (data != null) {
                        try {
                            AbstractPlayer.PlayerClass.valueOf(data.character_chosen);
                            if (!data.is_ascension_mode || data.ascension_level != 20) {
                                continue;
                            }
                            tmpRuns.add(data);
                        } catch (NullPointerException | IllegalArgumentException var17) {
                            StatsTracker.logger.info("Run file " +
                                    file.path() +
                                    " does not use a real character: " +
                                    data.character_chosen);
                        }
                    }
                } catch (JsonSyntaxException e) {
                    StatsTracker.logger.info("Failed to load RunData from JSON file: " + file.path());
                }
            }
        }

        return tmpRuns.stream().sorted((o1, o2) -> RunData.orderByTimestampDesc.compare(o1, o2));
    }

    public void refreshData() {
        this.allRuns.clear();
        this.ironcladRuns.clear();
        this.silentRuns.clear();
        this.defectRuns.clear();
        this.watcherRuns.clear();

        loadData().forEach((run) -> {
            AbstractPlayer.PlayerClass
                    playerClass =
                    AbstractPlayer.PlayerClass.valueOf(run.character_chosen);
            this.allRuns.add(run);
            switch (playerClass) {
                case IRONCLAD:
                    this.ironcladRuns.add(run);
                    break;
                case THE_SILENT:
                    this.silentRuns.add(run);
                    break;
                case DEFECT:
                    this.defectRuns.add(run);
                    break;
                case WATCHER:
                    this.watcherRuns.add(run);
                    break;
            }
        });

        StatsTracker.logger.info("Loaded " + this.allRuns.size() + " runs.");
        StatsTracker.logger.info("Loaded " + this.ironcladRuns.size() + " ironclad runs.");
        StatsTracker.logger.info("Loaded " + this.silentRuns.size() + " silent runs.");
        StatsTracker.logger.info("Loaded " + this.defectRuns.size() + " defect runs.");
        StatsTracker.logger.info("Loaded " + this.watcherRuns.size() + " watcher runs.");
    }
}
