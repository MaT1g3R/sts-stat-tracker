package StatsTracker;

import StatsTracker.stats.Run;
import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.core.CardCrawlGame;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RunHistoryManager {
    private final List<Run> allRuns = new ArrayList<>();
    private final List<Run> ironcladRuns = new ArrayList<>();
    private final List<Run> silentRuns = new ArrayList<>();
    private final List<Run> defectRuns = new ArrayList<>();
    private final List<Run> watcherRuns = new ArrayList<>();

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
                .filter(Run::valid)
                .sorted();
    }

    public Stream<YearMonth> getDates() {
        return allRuns.stream()
                .map(run -> run.timestamp)
                .map(ts -> new Date(ts * 1000))
                .map(YearMonth::fromDate)
                .distinct()
                .sorted();
    }

    private static List<Run> getRunsInRange(List<Run> runs, YearMonth start, YearMonth end, boolean includeAbandons) {
        return runs.stream().filter(run -> run.isInRange(start, end)).filter(run -> {
            if (includeAbandons) {
                return true;
            }
            return !run.abandoned;
        }).collect(Collectors.toList());
    }

    public List<Run> getAllRuns(YearMonth start, YearMonth end, boolean includeAbandons) {
        return getRunsInRange(allRuns, start, end, includeAbandons);
    }

    public List<Run> getIroncladRuns(YearMonth start, YearMonth end, boolean includeAbandons) {
        return getRunsInRange(ironcladRuns, start, end, includeAbandons);
    }

    public List<Run> getSilentRuns(YearMonth start, YearMonth end, boolean includeAbandons) {
        return getRunsInRange(silentRuns, start, end, includeAbandons);
    }

    public List<Run> getDefectRuns(YearMonth start, YearMonth end, boolean includeAbandons) {
        return getRunsInRange(defectRuns, start, end, includeAbandons);
    }

    public List<Run> getWatcherRuns(YearMonth start, YearMonth end, boolean includeAbandons) {
        return getRunsInRange(watcherRuns, start, end, includeAbandons);
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
