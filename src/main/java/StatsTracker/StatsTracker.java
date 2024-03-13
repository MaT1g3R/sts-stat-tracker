package StatsTracker;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SpireInitializer
public class StatsTracker implements PostInitializeSubscriber {
    public static final Logger logger = LogManager.getLogger(StatsTracker.class.getName());
    public static Texture nobbers;
    public static RunHistoryManager runHistoryManager;

    public StatsTracker() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new StatsTracker();
    }

    @Override
    public void receivePostInitialize() {
        nobbers = ImageMaster.loadImage("StatsTracker/img/nob-1.png");
        runHistoryManager = new RunHistoryManager();
    }
}
