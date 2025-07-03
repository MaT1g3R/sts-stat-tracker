package StatsTracker;


import StatsTracker.stats.Run;
import basemod.ModLabeledButton;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static basemod.BaseMod.gson;

public class RunUploader {
    public static final String gameVersion = "sts1";
    public static final int schemaVersion = 1;

    public static final Logger logger = LogManager.getLogger(RunUploader.class.getName());
    private final HTTPClient httpClient;
    private final RunHistoryManager runHistoryManager;

    public RunUploader(HTTPClient httpClient, RunHistoryManager runHistoryManager) {
        this.httpClient = httpClient;
        this.runHistoryManager = runHistoryManager;
    }

    static class UploadAllRunsRequest {
        public List<Run> runs;
        public String profile;
        public String gameVersion;
        public int schemaVersion;

        UploadAllRunsRequest(List<Run> runs, String profile, String gameVersion, int schemaVersion) {
            this.runs = runs;
            this.profile = profile;
            this.gameVersion = gameVersion;
            this.schemaVersion = schemaVersion;
        }
    }


    public void uploadAllRuns(ModLabeledButton btn) {
        btn.label = "Gathering runs...";
        runHistoryManager.refreshData();
        List<YearMonth> dates = runHistoryManager.getDates().collect(Collectors.toList());
        YearMonth firstRun = dates.get(0);
        YearMonth lastRun = dates.get(dates.size() - 1);
        List<Run> runs = runHistoryManager.getAllRuns(firstRun, lastRun, true);
        String name = CardCrawlGame.playerName;

        try {
            btn.label = "Uploading...";
            httpClient.post("/api/v1/upload-all",
                    gson.toJson(new UploadAllRunsRequest(runs, name, gameVersion, schemaVersion)));
            btn.label = "Upload successful";
        } catch (IOException e) {
            btn.label = "Upload failed";
            logger.error(e);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.error("Sleep interrupted", e);
        }
        btn.label = "Sync all runs";
    }
}
