package StatsTracker;

import basemod.BaseMod;
import basemod.ModLabeledButton;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@SpireInitializer
public class StatsTracker implements PostInitializeSubscriber {
    public static final Logger logger = LogManager.getLogger(StatsTracker.class.getName());
    public static Texture nobbers;
    public static RunHistoryManager runHistoryManager;

    public final Config config;
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

    @Override
    public void receivePostInitialize() {
        nobbers = ImageMaster.loadImage("StatsTracker/img/nob-1.png");
        runHistoryManager = new RunHistoryManager();
        HTTPClient httpClient = new HTTPClient(config.getEndpoint(), config.userID, config.token);
        runUploader = new RunUploader(httpClient, runHistoryManager);

        ModPanel settingsPanel = new ModPanel();
        ModLabeledButton
                syncButton =
                new ModLabeledButton("Sync all runs", 400f, 600f, settingsPanel, (this::uploadAllRuns));
        ModLabeledToggleButton
                shareButton =
                new ModLabeledToggleButton("Auto share runs",
                        400f,
                        700f,
                        Color.WHITE,
                        FontHelper.buttonLabelFont,
                        config.getAutoSync(),
                        settingsPanel,
                        (lbl -> {
                        }),
                        (btn -> {
                            try {
                                config.setAutoSync(btn.enabled);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));


        settingsPanel.addUIElement(syncButton);
        settingsPanel.addUIElement(shareButton);


        ModLabeledButton
                debugButton =
                new ModLabeledButton("Super secret debug button",
                        400,
                        500,
                        settingsPanel,
                        btn -> this.uploadIncrementalRuns(0));
        settingsPanel.addUIElement(debugButton);


        BaseMod.registerModBadge(ImageMaster.loadImage("StatsTracker/img/nob-32.png"),
                "Stats Tracker",
                "vmService",
                "track and share your run statistics",
                settingsPanel);
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
