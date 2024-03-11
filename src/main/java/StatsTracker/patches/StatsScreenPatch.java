package StatsTracker.patches;

import StatsTracker.CharStatRenderer;
import StatsTracker.MoreStatsScreen;
import StatsTracker.Utils;
import StatsTracker.stats.Card;
import StatsTracker.stats.ClassStat;
import StatsTracker.stats.PickSkip;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.stats.AchievementGrid;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;

import java.util.List;

import static com.megacrit.cardcrawl.screens.stats.StatsScreen.NAMES;
import static com.megacrit.cardcrawl.screens.stats.StatsScreen.renderHeader;

public class StatsScreenPatch {
    public static MoreStatsScreen moreStatsScreen;

    private static float screenPosX(float val) {
        return val * Settings.xScale;
    }

    private static float screenPosY(float val) {
        return val * Settings.yScale;
    }

    public static String getCharColor() {
        switch (moreStatsScreen.currentClass()) {
            case "Ironclad":
                return "#r";
            case "Silent":
                return "#g";
            case "Defect":
                return "#b";
            case "Watcher":
                return "#p";
            default:
                return "#y";
        }
    }

    @SpirePatch(clz = StatsScreen.class, method = SpirePatch.CONSTRUCTOR)
    public static class ConstructorPatch {
        public static void Postfix(StatsScreen __instance) {
            moreStatsScreen = new MoreStatsScreen();
        }
    }

    @SpirePatch(clz = StatsScreen.class, method = "updateScrolling")
    public static class UpdateScrollingPatch {
        public static SpireReturn<Void> Prefix(StatsScreen __instance) {
            if (moreStatsScreen.isDropDownOpen()) {
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = StatsScreen.class, method = "update")
    public static class UpdatePatch {
        public static void Postfix(StatsScreen __instance) {
            moreStatsScreen.update();
        }
    }

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
        public static void Replace(StatsScreen __instance) {
            StatsScreen.achievements = new AchievementGrid();
            moreStatsScreen.refreshData();
        }
    }

    private static void renderCharacterStats(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = Utils.getField(s, StatsScreen.class, "scrollY");
        renderHeader(sb, NAMES[0], screenX, renderY);
        new CharStatRenderer(moreStatsScreen.classStats[moreStatsScreen.classStats.length - 1]).render(sb,
                screenX,
                renderY);
        renderY -= 400.0F * Settings.scale;

        for (int i = 0; i < 4; i++) {
            String name = NAMES[i + 2];
            renderHeader(sb, name, screenX, renderY);
            new CharStatRenderer(moreStatsScreen.classStats[i]).render(sb, screenX, renderY);
            renderY -= 400.0F * Settings.scale;
        }
    }

    private static void renderCardPickRate(StatsScreen s, SpriteBatch sb, boolean act1, float screenX) {
        float renderY = Utils.getField(s, StatsScreen.class, "scrollY");

        String name = act1 ? "Card pick rate act 1" : "Card pick rate after act 1";
        name += " (top 200)";
        String color = getCharColor();
        StringBuilder nameBuilder = new StringBuilder();
        for (String part : name.split(" ")) {
            nameBuilder.append(color).append(part).append(" ");
        }

        renderHeader(sb, nameBuilder.toString(), screenX, renderY);

        StringBuilder builder = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();

        ClassStat cs = moreStatsScreen.getClassStat();
        List<PickSkip<Card>> picks = act1 ? cs.cardPicksAct1 : cs.cardPicksAfterAct1;

        int len = Math.min(picks.size(), 200);
        for (int i = 0; i < len; i++) {
            PickSkip<Card> c = picks.get(i);
            if (i <= len / 2) {
                builder.append(c.what.toString()).append(" ").append(c.pickRatePercent()).append("% NL ");
            } else {
                builder2.append(c.what.toString()).append(" ").append(c.pickRatePercent()).append("% NL ");
            }
        }

        FontHelper.renderSmartText(sb,
                FontHelper.panelNameFont,
                builder.toString(),
                screenX + 75.0F * Settings.scale,
                renderY + 766.0F * Settings.yScale,
                9999.0F,
                38.0F * Settings.scale,
                Settings.CREAM_COLOR);

        FontHelper.renderSmartText(sb,
                FontHelper.panelNameFont,
                builder2.toString(),
                screenX + 675.0F * Settings.scale,
                renderY + 766.0F * Settings.yScale,
                9999.0F,
                38.0F * Settings.scale,
                Settings.CREAM_COLOR);
    }

    public static void renderStatScreen(StatsScreen s, SpriteBatch sb) {
        float screenX = Utils.getField(s, StatsScreen.class, "screenX");
        float scrollY = Utils.getField(s, StatsScreen.class, "scrollY");

        switch (moreStatsScreen.STAT_TYPES[moreStatsScreen.statTypeDropdown.getSelectedIndex()]) {
            case "Overall":
                renderCharacterStats(s, sb, screenX);
                break;
            case "Card pick rate act 1":
                renderCardPickRate(s, sb, true, screenX);
                break;
            case "Card pick rate after act 1":
                renderCardPickRate(s, sb, false, screenX);
                break;
        }

        float labelY = scrollY + screenPosY(950);
        float dropdownY = labelY - screenPosY(40);

        float xDiff = 170;
        float startDateX = screenX + screenPosX(-270);
        float endDateX = startDateX + screenPosX(xDiff);
        float characterX = endDateX + screenPosX(xDiff);
        float statTypeX = characterX + screenPosX(xDiff);

        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, "Start date", startDateX, labelY, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, "End date", endDateX, labelY, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, "Character", characterX, labelY, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, "Stat type", statTypeX, labelY, Settings.CREAM_COLOR);

        moreStatsScreen.startDateDropdown.render(sb, startDateX, dropdownY);
        moreStatsScreen.endDateDropDown.render(sb, endDateX, dropdownY);
        moreStatsScreen.classDropdown.render(sb, characterX, dropdownY);
        moreStatsScreen.statTypeDropdown.render(sb, statTypeX, dropdownY);

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
}
