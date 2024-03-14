package StatsTracker.patches;

import StatsTracker.Utils;
import StatsTracker.stats.*;
import StatsTracker.ui.CharStatRenderer;
import StatsTracker.ui.MoreStatsScreen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.stats.AchievementGrid;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;

import java.util.Arrays;
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

    public static String colorForClass(String s) {
        String color = getCharColor();
        return Arrays.stream(s.split(" ")).map(w -> color + w).reduce((s1, s2) -> s1 + " " + s2).orElse("");
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

    private static float getScrollY(StatsScreen s) {
        return Utils.getField(s, StatsScreen.class, "scrollY");
    }

    private static void renderCharacterStats(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        renderHeader(sb, NAMES[0], screenX, renderY);
        new CharStatRenderer(moreStatsScreen.getClassStat()).render(sb, screenX, renderY);
        renderY -= 400.0F * Settings.scale;

        for (int i = 0; i < 4; i++) {
            String name = NAMES[i + 2];
            renderHeader(sb, name, screenX, renderY);
            new CharStatRenderer(moreStatsScreen.classStats[i]).render(sb, screenX, renderY);
            renderY -= 400.0F * Settings.scale;
        }
    }

    private static void render2Columns(SpriteBatch sb, float screenX, float renderY, String s1, String s2) {
        FontHelper.renderSmartText(sb,
                FontHelper.panelNameFont,
                s1,
                screenX + 75.0F * Settings.scale,
                renderY + 766.0F * Settings.yScale,
                9999.0F,
                38.0F * Settings.scale,
                Settings.CREAM_COLOR);

        FontHelper.renderSmartText(sb,
                FontHelper.panelNameFont,
                s2,
                screenX + 675.0F * Settings.scale,
                renderY + 766.0F * Settings.yScale,
                9999.0F,
                38.0F * Settings.scale,
                Settings.CREAM_COLOR);
    }

    private static void renderCardPickRate(StatsScreen s, SpriteBatch sb, boolean act1, float screenX) {
        float renderY = getScrollY(s);
        String name = act1 ? "Card pick rate act 1" : "Card pick rate after act 1";
        name += " (top 200)";
        renderHeader(sb, colorForClass(name), screenX, renderY);

        StringBuilder builder = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();

        ClassStat cs = moreStatsScreen.getClassStat();
        List<Rate<Card>> picks = act1 ? cs.cardPicksAct1 : cs.cardPicksAfterAct1;

        int len = Math.min(picks.size(), 200);
        for (int i = 0; i < len; i++) {
            Rate<Card> c = picks.get(i);
            if (i <= len / 2) {
                builder.append(c.what.toString())
                        .append(" #y")
                        .append(c.percent())
                        .append("% ")
                        .append(c.winLoss())
                        .append(" NL ");
            } else {
                builder2.append(c.what.toString())
                        .append(" #y")
                        .append(c.percent())
                        .append("% ")
                        .append(c.winLoss())
                        .append(" NL ");
            }
        }
        render2Columns(sb, screenX, renderY, builder.toString(), builder2.toString());
    }


    private static void renderCardWinRate(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        String name = "Card win rate (top 200)";
        renderHeader(sb, colorForClass(name), screenX, renderY);

        StringBuilder builder = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();

        ClassStat cs = moreStatsScreen.getClassStat();
        int len = Math.min(cs.cardWinRate.size(), 200);
        for (int i = 0; i < len; i++) {
            Rate<Card> c = cs.cardWinRate.get(i);
            if (i <= len / 2) {
                builder.append(c.what.toString())
                        .append(" #y")
                        .append(c.percent())
                        .append("% ")
                        .append(c.winLoss())
                        .append(" NL ");
            } else {
                builder2.append(c.what.toString())
                        .append(" #y")
                        .append(c.percent())
                        .append("% ")
                        .append(c.winLoss())
                        .append(" NL ");
            }
        }
        render2Columns(sb, screenX, renderY, builder.toString(), builder2.toString());
    }

    private static void renderNeowBonus(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        renderHeader(sb, colorForClass("Neow win rate"), screenX, renderY);

        StringBuilder builder = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();

        ClassStat cs = moreStatsScreen.getClassStat();
        for (Rate<Neow> n : cs.neowWinRate) {
            builder.append(n.what.toString())
                    .append(" #y")
                    .append(n.percent())
                    .append("% ")
                    .append(n.winLoss())
                    .append(" NL ");
        }
        for (Rate<Neow> n : cs.neowPickRate) {
            builder2.append(n.what.toString())
                    .append(" #y")
                    .append(n.percent())
                    .append("% ")
                    .append(n.winLoss())
                    .append(" NL ");
        }

        FontHelper.renderSmartText(sb,
                FontHelper.panelNameFont,
                builder.toString(),
                screenX + 75.0F * Settings.scale,
                renderY + 766.0F * Settings.yScale,
                9999.0F,
                38.0F * Settings.scale,
                Settings.CREAM_COLOR);

        renderY -= 44.0F * Settings.scale * cs.neowWinRate.size();
        renderY -= 100 * Settings.scale;
        renderHeader(sb, colorForClass("Neow pick rate"), screenX, renderY);
        FontHelper.renderSmartText(sb,
                FontHelper.panelNameFont,
                builder2.toString(),
                screenX + 75.0F * Settings.scale,
                renderY + 766.0F * Settings.yScale,
                9999.0F,
                38.0F * Settings.scale,
                Settings.CREAM_COLOR);
    }

    private static void renderBossRelicPickRate(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        ClassStat cs = moreStatsScreen.getClassStat();
        renderHeader(sb, colorForClass("Boss relic pick rate (act 1)"), screenX, renderY);

        StringBuilder act1 = new StringBuilder();
        StringBuilder act12 = new StringBuilder();

        StringBuilder act2 = new StringBuilder();
        StringBuilder act22 = new StringBuilder();

        for (int i = 0; i < cs.bossRelicPickRateAct1.size(); i++) {
            Rate<BossRelic> r = cs.bossRelicPickRateAct1.get(i);
            if (i <= cs.bossRelicPickRateAct1.size() / 2) {
                act1.append(r.what.toString())
                        .append(" #y")
                        .append(r.percent())
                        .append("% ")
                        .append(r.winLoss())
                        .append(" NL ");
            } else {
                act12.append(r.what.toString())
                        .append(" #y")
                        .append(r.percent())
                        .append("% ")
                        .append(r.winLoss())
                        .append(" NL ");
            }
        }
        for (int i = 0; i < cs.bossRelicPickRateAct2.size(); i++) {
            Rate<BossRelic> r = cs.bossRelicPickRateAct2.get(i);
            if (i <= cs.bossRelicPickRateAct2.size() / 2) {
                act2.append(r.what.toString())
                        .append(" #y")
                        .append(r.percent())
                        .append("% ")
                        .append(r.winLoss())
                        .append(" NL ");
            } else {
                act22.append(r.what.toString())
                        .append(" #y")
                        .append(r.percent())
                        .append("% ")
                        .append(r.winLoss())
                        .append(" NL ");
            }
        }

        render2Columns(sb, screenX, renderY, act1.toString(), act12.toString());

        renderY -= 22.0F * Settings.scale * cs.bossRelicPickRateAct1.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Boss relic pick rate (act 2)"), screenX, renderY);
        render2Columns(sb, screenX, renderY, act2.toString(), act22.toString());
    }

    private static void renderBossRelicWinRate(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        ClassStat cs = moreStatsScreen.getClassStat();
        renderHeader(sb, colorForClass("Boss relic win rate (act 1)"), screenX, renderY);

        StringBuilder act1 = new StringBuilder();
        StringBuilder act12 = new StringBuilder();

        StringBuilder act2 = new StringBuilder();
        StringBuilder act22 = new StringBuilder();

        StringBuilder swap = new StringBuilder();
        StringBuilder swap2 = new StringBuilder();

        for (int i = 0; i < cs.bossRelicWinRateAct1.size(); i++) {
            Rate<BossRelic> r = cs.bossRelicWinRateAct1.get(i);
            if (i <= cs.bossRelicWinRateAct1.size() / 2) {
                act1.append(r.what.toString())
                        .append(" #y")
                        .append(r.percent())
                        .append("% ")
                        .append(r.winLoss())
                        .append(" NL ");
            } else {
                act12.append(r.what.toString())
                        .append(" #y")
                        .append(r.percent())
                        .append("% ")
                        .append(r.winLoss())
                        .append(" NL ");
            }
        }
        for (int i = 0; i < cs.bossRelicWinRateAct2.size(); i++) {
            Rate<BossRelic> r = cs.bossRelicWinRateAct2.get(i);
            if (i <= cs.bossRelicWinRateAct2.size() / 2) {
                act2.append(r.what.toString())
                        .append(" #y")
                        .append(r.percent())
                        .append("% ")
                        .append(r.winLoss())
                        .append(" NL ");
            } else {
                act22.append(r.what.toString())
                        .append(" #y")
                        .append(r.percent())
                        .append("% ")
                        .append(r.winLoss())
                        .append(" NL ");
            }
        }
        for (int i = 0; i < cs.bossRelicWinRateSwap.size(); i++) {
            Rate<BossRelic> r = cs.bossRelicWinRateSwap.get(i);
            if (i <= cs.bossRelicWinRateSwap.size() / 2) {
                swap.append(r.what.toString())
                        .append(" #y")
                        .append(r.percent())
                        .append("% ")
                        .append(r.winLoss())
                        .append(" NL ");
            } else {
                swap2.append(r.what.toString())
                        .append(" #y")
                        .append(r.percent())
                        .append("% ")
                        .append(r.winLoss())
                        .append(" NL ");
            }
        }

        render2Columns(sb, screenX, renderY, act1.toString(), act12.toString());

        renderY -= 22.0F * Settings.scale * cs.bossRelicPickRateAct1.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Boss relic win rate (act 2)"), screenX, renderY);
        render2Columns(sb, screenX, renderY, act2.toString(), act22.toString());

        renderY -= 22.0F * Settings.scale * cs.bossRelicPickRateAct2.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Boss swap win rate"), screenX, renderY);
        render2Columns(sb, screenX, renderY, swap.toString(), swap2.toString());
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
            case "Card win rate":
                renderCardWinRate(s, sb, screenX);
                break;
            case "Neow bonus":
                renderNeowBonus(s, sb, screenX);
                break;
            case "Boss relic pick rate":
                renderBossRelicPickRate(s, sb, screenX);
                break;
            case "Boss relic win rate":
                renderBossRelicWinRate(s, sb, screenX);
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
