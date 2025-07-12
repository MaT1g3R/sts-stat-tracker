package StatsTracker;


import StatsTracker.stats.Leaderboard;
import StatsTracker.stats.Run;
import basemod.ModLabeledButton;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
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

    public void uploadLeaderboard(List<Run> runs) throws IOException {
        String profile = CardCrawlGame.playerName;
        String selectedProfile = StatsTracker.config.getLeaderboardProfile();

        if (selectedProfile == null || selectedProfile.isEmpty()) {
            logger.info("No leaderboard profile selected");
            return;
        }

        if (selectedProfile.equals(Config.NONE_PROFILE)) {
            logger.info("'NONE' leaderboard profile selected");
            return;
        }

        if (!selectedProfile.equals(profile) && !selectedProfile.equals(Config.ALL_PROFILE)) {
            logger.info("Selected profile: {}, current profile: {}", selectedProfile, profile);
            return;
        }
        logger.info("Uploading leaderboard for {}", profile);
        List<Leaderboard.Entry> entries = new Leaderboard(runs).getEntries();
        httpClient.post("/api/v1/leaderboard", gson.toJson(entries));
    }

    private List<Run> getAllRuns() {
        List<YearMonth> dates = runHistoryManager.getDates().collect(Collectors.toList());
        YearMonth firstRun = dates.get(0);
        YearMonth lastRun = dates.get(dates.size() - 1);
        return runHistoryManager.getAllRuns(firstRun, lastRun, true);
    }

    public void uploadAllRuns(ModLabeledButton btn) {
        btn.label = "Gathering runs...";
        runHistoryManager.refreshData();

        List<Run> runs = getAllRuns();
        String name = CardCrawlGame.playerName;

        try {
            btn.label = "Uploading...";
            httpClient.post("/api/v1/upload-all", gson.toJson(new UploadAllRunsRequest(runs, name, gameVersion, schemaVersion)));
            uploadLeaderboard(runs);
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

    static class IncrementalResponse {
        Long lastRunTime = null;
        List<Long> outdated = new ArrayList<>();
    }

    public void uploadIncrementalRuns() {
        runHistoryManager.refreshData();
        String name = CardCrawlGame.playerName;
        String endpoint = "";
        try {
            endpoint = String.format("/api/v1/increment?profile=%s&game-version=%s&schema-version=%d", URLEncoder.encode(name, "UTF-8"), gameVersion, schemaVersion);
        } catch (UnsupportedEncodingException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
        String js = "";
        try {
            js = httpClient.get(endpoint);
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
        IncrementalResponse resp = gson.fromJson(js, IncrementalResponse.class);
        List<Run> runs = runHistoryManager.getIncrementalRuns(resp.lastRunTime, resp.outdated);
        if (runs.isEmpty()) {
            logger.info("No incremental runs found");
            return;
        }
        logger.info("{} incremental runs found", runs.size());
        try {
            httpClient.post("/api/v1/upload-all", gson.toJson(new UploadAllRunsRequest(runs, name, gameVersion, schemaVersion)));
            uploadLeaderboard(getAllRuns());
        } catch (IOException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
        logger.info("{} incremental runs uploaded", runs.size());
    }
}
