package StatsTracker.patches;

import StatsTracker.StatsTracker;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.metrics.Metrics;

public class MetricsPatch {
    @SpirePatch2(clz = Metrics.class, method = "gatherAllDataAndSave")
    public static class Patch {
        @SpirePostfixPatch
        public static void Postfix(Metrics __instance) {
            StatsTracker.logger.info("running gatherAllDataAndSave patch");
            StatsTracker instance = StatsTracker.instance;
            if (instance == null) {
                StatsTracker.logger.error("StatsTracker instance is null");
                return;
            }
            if (StatsTracker.config == null) {
                StatsTracker.logger.error("StatsTracker config is null");
                return;
            }

            instance.uploadIncrementalRuns(5);
        }
    }
}
