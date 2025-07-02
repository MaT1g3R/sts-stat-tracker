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

    private final Config config;
    private final HTTPClient httpClient;

    public StatsTracker() {
        try {
            config = new Config();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        httpClient = new HTTPClient(config.getEndpoint(), config.userID, config.token);
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new StatsTracker();
    }

    @Override
    public void receivePostInitialize() {
        nobbers = ImageMaster.loadImage("StatsTracker/img/nob-1.png");
        runHistoryManager = new RunHistoryManager();

        ModPanel settingsPanel = new ModPanel();
        ModLabeledButton syncButton = new ModLabeledButton("Sync all runs", 400f, 600f, settingsPanel, (btn -> {
        }));
        ModLabeledToggleButton
                shareButton =
                new ModLabeledToggleButton("Auto share runs",
                        400f,
                        700f,
                        Color.WHITE,
                        FontHelper.buttonLabelFont,
                        false,
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

        BaseMod.registerModBadge(ImageMaster.loadImage("StatsTracker/img/nob-32.png"),
                "Stats Tracker",
                "vmService",
                "track and share your run statistics",
                settingsPanel);
    }
}
