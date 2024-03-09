package StatsTracker.patches;

import StatsTracker.CharStatRenderer;
import StatsTracker.ClassStat;
import StatsTracker.StatsTracker;
import StatsTracker.Utils;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;

import static com.megacrit.cardcrawl.screens.stats.StatsScreen.NAMES;
import static com.megacrit.cardcrawl.screens.stats.StatsScreen.renderHeader;

public class StatsScreenPatch {
    @SpirePatch(clz = StatsScreen.class, method = "render")
    public static class RenderPatch {
        public static void Replace(StatsScreen __instance, SpriteBatch sb) {
            renderStatScreen(__instance, sb);
            __instance.button.render(sb);
            ScrollBar scrollBar = Utils.getField(__instance, StatsScreen.class, "scrollBar");
            scrollBar.render(sb);
        }
    }

    @SpirePatch(clz = StatsScreen.class, method = "refreshData")
    public static class RefreshDataPatch {
        public static void Postfix(StatsScreen __instance) {
            StatsTracker.runHistoryManager.refreshData();
        }
    }

    public static void renderStatScreen(StatsScreen s, SpriteBatch sb) {
        float renderY = Utils.getField(s, StatsScreen.class, "scrollY");
        float screenX = Utils.getField(s, StatsScreen.class, "screenX");
        float scrollY = Utils.getField(s, StatsScreen.class, "scrollY");

        renderHeader(sb, NAMES[0], screenX, renderY);
        StatsScreen.all.render(sb, screenX, renderY);
        renderY -= 400.0F * Settings.scale;
        renderHeader(sb, NAMES[1], screenX, renderY);
        StatsScreen.achievements.render(sb, renderY);
        renderY -= 2200.0F * Settings.scale;

        ClassStat[] allClassStats = getClassStats();

        for (int i = 0; i < 4; i++) {
            String name = NAMES[i + 2];
            renderHeader(sb, name, screenX, renderY);
            new CharStatRenderer(allClassStats[i]).render(sb, screenX, renderY);
            renderY -= 400.0F * Settings.scale;
        }

        if (Settings.isControllerMode) {
            s.allCharsHb.move(300.0F * Settings.scale, scrollY + 600.0F * Settings.scale);
            s.ironcladHb.move(300.0F * Settings.scale, scrollY - 1600.0F * Settings.scale);
            if (s.silentHb != null) {
                s.silentHb.move(300.0F * Settings.scale, scrollY - 2000.0F * Settings.scale);
            }

            if (s.defectHb != null) {
                s.defectHb.move(300.0F * Settings.scale, scrollY - 2400.0F * Settings.scale);
            }

            if (s.watcherHb != null) {
                s.watcherHb.move(300.0F * Settings.scale, scrollY - 2800.0F * Settings.scale);
            }
        }
    }

    private static ClassStat[] getClassStats() {
        ClassStat ironcladCS = new ClassStat(StatsTracker.runHistoryManager.ironcladRuns);
        ClassStat silentCS = new ClassStat(StatsTracker.runHistoryManager.silentRuns);
        ClassStat defectCS = new ClassStat(StatsTracker.runHistoryManager.defectRuns);
        ClassStat watcherCS = new ClassStat(StatsTracker.runHistoryManager.watcherRuns);
        ClassStat rotatingCS = new ClassStat(StatsTracker.runHistoryManager.allRuns);

        return new ClassStat[]{ironcladCS, silentCS, defectCS, watcherCS, rotatingCS};
    }
}
