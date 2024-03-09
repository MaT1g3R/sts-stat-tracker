package StatsTracker;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SpireInitializer
public class StatsTracker implements PostInitializeSubscriber {
    public static final Logger logger = LogManager.getLogger(StatsTracker.class.getName());

    public static RunHistoryManager runHistoryManager = new RunHistoryManager();

    public StatsTracker() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
    }

    @Override
    public void receivePostInitialize() {
    }
}
