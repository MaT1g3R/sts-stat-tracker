package StatsTracker;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.core.CardCrawlGame;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class RunHistoryManager {
    public List<Run> allRuns = new ArrayList<>();
    public List<Run> ironcladRuns = new ArrayList<>();
    public List<Run> silentRuns = new ArrayList<>();
    public List<Run> defectRuns = new ArrayList<>();
    public List<Run> watcherRuns = new ArrayList<>();


    private Stream<Run> loadData() {
        return Arrays.stream(Gdx.files.local("runs" + File.separator).list())
                .filter(s -> {
                    if (CardCrawlGame.saveSlot == 0) {
                        return !s.name().contains("0_") && !s.name().contains("1_") && !s.name().contains("2_");
                    } else {
                        return s.name().contains(CardCrawlGame.saveSlot + "_");
                    }
                })
                .flatMap(s -> Arrays.stream(s.list()).map(Run::fromFile))
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .filter(d -> !d.runData.chose_seed)
                .filter(d -> d.isA20)
                .sorted();
    }


    public void refreshData() {
        this.allRuns.clear();
        this.ironcladRuns.clear();
        this.silentRuns.clear();
        this.defectRuns.clear();
        this.watcherRuns.clear();

        loadData().forEach((run) -> {
            switch (run.playerClass) {
                case IRONCLAD:
                    this.allRuns.add(run);
                    this.ironcladRuns.add(run);
                    break;
                case THE_SILENT:
                    this.allRuns.add(run);
                    this.silentRuns.add(run);
                    break;
                case DEFECT:
                    this.allRuns.add(run);
                    this.defectRuns.add(run);
                    break;
                case WATCHER:
                    this.allRuns.add(run);
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
