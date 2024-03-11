package StatsTracker.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.screens.stats.AchievementGrid;

public class AchievementGridPatch {
    @SpirePatch(clz = AchievementGrid.class, method = "update")
    public static class UpdatePatch {
        public static void Replace(AchievementGrid __instance) {
        }
    }
}
