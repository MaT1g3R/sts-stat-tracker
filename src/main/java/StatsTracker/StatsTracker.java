package StatsTracker;

import basemod.*;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.options.DropdownMenuListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@SpireInitializer
public class StatsTracker implements PostInitializeSubscriber {
    public static final Logger logger = LogManager.getLogger(StatsTracker.class.getName());
    public static Texture nobbers;
    public static RunHistoryManager runHistoryManager;

    public static Config config;
    private RunUploader runUploader;

    public static StatsTracker instance;

    public StatsTracker() {
        try {
            config = new Config();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BaseMod.subscribe(this);
    }

    public static void initialize() {
        instance = new StatsTracker();
    }

    private abstract static class ModDropdownMenu implements IUIElement, DropdownMenuListener {
        protected final DropdownMenu dropdownMenu;
        private final float x;
        private final float y;

        public ModDropdownMenu(float x, float y) {
            this.dropdownMenu = getDropdownMenu();
            this.x = x;
            this.y = y;
        }

        protected abstract DropdownMenu getDropdownMenu();

        @Override
        public void render(SpriteBatch spriteBatch) {
            this.dropdownMenu.render(spriteBatch, x * Settings.scale, y * Settings.scale);
        }

        @Override
        public void update() {
            this.dropdownMenu.update();
        }

        @Override
        public int renderLayer() {
            return 2;
        }

        @Override
        public int updateOrder() {
            return 1;
        }
    }

    private static class LeaderboardProfileDropdown extends ModDropdownMenu {
        public LeaderboardProfileDropdown(float x, float y) {
            super(x, y);
        }

        protected DropdownMenu getDropdownMenu() {
            ArrayList<String> options = new ArrayList<>(Collections.singletonList(Config.NONE_PROFILE));

            Map<String, String> profiles = CardCrawlGame.saveSlotPref.get();
            Arrays.asList("PROFILE_NAME", "1_PROFILE_NAME", "2_PROFILE_NAME").forEach(profileKey -> {
                String profile = profiles.get(profileKey);
                if (profile != null && !profile.isEmpty()) {
                    options.add(profile);
                }
            });
            options.add(Config.ALL_PROFILE);

            String selected = config.getLeaderboardProfile();
            if (selected != null && !selected.isEmpty()) {
                options.remove(selected);
                options.add(0, selected);
            }

            return new DropdownMenu(this, options, FontHelper.tipBodyFont, Settings.LIGHT_YELLOW_COLOR);
        }

        @Override
        public void changedSelectionTo(DropdownMenu dropdownMenu, int i, String s) {
            try {
                config.setLeaderboardProfile(s);
            } catch (IOException e) {
                logger.error(e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void receivePostInitialize() {
        nobbers = ImageMaster.loadImage("StatsTracker/img/nob-1.png");
        runHistoryManager = new RunHistoryManager();
        HTTPClient httpClient = new HTTPClient(config.getEndpoint(), config.userID, config.token);
        runUploader = new RunUploader(httpClient, runHistoryManager);

        ModPanel settingsPanel = new ModPanel();
        float x = 400;
        float y = 750;

        ModLabel leaderboardLabel = new ModLabel("Leaderboard Profile", x + 600, y, settingsPanel, s -> {
        });
        LeaderboardProfileDropdown leaderboardProfileDropdown = new LeaderboardProfileDropdown(x + 600, y - 50);

        ModLabeledToggleButton shareButton = new ModLabeledToggleButton("Auto share runs", x, y, Color.WHITE, FontHelper.buttonLabelFont, config.getAutoSync(), settingsPanel, (lbl -> {
        }), (btn -> {
            try {
                config.setAutoSync(btn.enabled);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
        y -= 100;
        ModLabeledButton syncButton = new ModLabeledButton("Sync all runs", x, y, settingsPanel, (this::uploadAllRuns));

        settingsPanel.addUIElement(leaderboardLabel);
        settingsPanel.addUIElement(leaderboardProfileDropdown);
        settingsPanel.addUIElement(syncButton);
        settingsPanel.addUIElement(shareButton);

        if (config.getDebug()) {
            y -= 100;
            ModLabeledButton debugButton = new ModLabeledButton("Super secret debug button", x, y, settingsPanel, btn -> this.uploadIncrementalRuns(0));
            settingsPanel.addUIElement(debugButton);
        }

        BaseMod.registerModBadge(ImageMaster.loadImage("StatsTracker/img/nob-32.png"), "Stats Tracker", "vmService", "track and share your run statistics", settingsPanel);
    }

    private class UploadTask implements Runnable {
        private final ModLabeledButton btn;

        public UploadTask(ModLabeledButton btn) {
            this.btn = btn;
        }

        @Override
        public void run() {
            runUploader.uploadAllRuns(btn);
        }
    }

    private class IncrementalUploadTask implements Runnable {
        private int delaySeconds = 0;

        public IncrementalUploadTask(int delaySeconds) {
            this.delaySeconds = delaySeconds;
        }

        @Override
        public void run() {
            if (delaySeconds > 0) {
                try {
                    logger.info("sleeping {} seconds", delaySeconds);
                    Thread.sleep(1000L * delaySeconds);
                    logger.info("sleeping done");
                } catch (InterruptedException e) {
                    logger.error(e);
                    throw new RuntimeException(e);
                }
            }
            runUploader.uploadIncrementalRuns();
        }
    }

    private void uploadAllRuns(ModLabeledButton btn) {
        Thread uploadThread = new Thread(new UploadTask(btn));
        uploadThread.start();
    }

    public void uploadIncrementalRuns(int delaySeconds) {
        Thread uploadThread = new Thread(new IncrementalUploadTask(delaySeconds));
        uploadThread.start();
    }
}
