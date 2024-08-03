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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.megacrit.cardcrawl.screens.stats.StatsScreen.NAMES;
import static com.megacrit.cardcrawl.screens.stats.StatsScreen.renderHeader;

public class StatsScreenPatch {
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

    @SpirePatch(clz = StatsScreen.class, method = "refreshData")
    public static class RefreshDataPatch {
        public static void Replace(StatsScreen __instance) {
            StatsScreen.achievements = new AchievementGrid();
            moreStatsScreen.refreshData();
        }
    }

    @SpirePatch(clz = StatsScreen.class, method = SpirePatch.CONSTRUCTOR)
    public static class ConstructorPatch {
        public static void Postfix(StatsScreen __instance) {
            moreStatsScreen = new MoreStatsScreen();
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

    public static String colorText(String s, String color) {
        return Arrays.stream(s.split(" ")).map(w -> color + w).reduce((s1, s2) -> s1 + " " + s2).orElse("");
    }

    public static String colorForClass(String s) {
        String color = getCharColor();
        return colorText(s, color);
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

    public static <T extends SampleSize> void renderT(SpriteBatch sb,
                                                      float screenX,
                                                      float renderY,
                                                      int maxRows,
                                                      List<T> xs,
                                                      Function<T, String> toString) {
        xs =
                xs.stream()
                        .filter(x -> x.getSampleSize() >= moreStatsScreen.sampleSizeDropdown.getSelectedIndex())
                        .collect(
                                Collectors.toList());
        int len = xs.size();
        if (maxRows > 0) {
            len = Math.min(len, maxRows);
        }
        StringBuilder builder = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();

        for (int i = 0; i < len; i++) {
            T x = xs.get(i);
            if (i <= len / 2) {
                builder.append(toString.apply(x)).append(" NL ");
            } else {
                builder2.append(toString.apply(x)).append(" NL ");
            }
        }
        render2Columns(sb, screenX, renderY, builder.toString(), builder2.toString());
    }

    public static <A> void renderRates(SpriteBatch sb,
                                       float screenX,
                                       float renderY,
                                       int maxRows,
                                       List<Rate<A>> rates) {
        renderT(sb,
                screenX,
                renderY,
                maxRows,
                rates,
                (Rate<A> c) -> c.what.toString() + " #y" + c.percent() + "% " + c.winLoss());
    }

    public static <A> void renderRates1Col(SpriteBatch sb,
                                           float screenX,
                                           float renderY,
                                           int maxRows,
                                           List<Rate<A>> rates) {
        rates =
                rates.stream()
                        .filter(x -> x.getSampleSize() >= moreStatsScreen.sampleSizeDropdown.getSelectedIndex())
                        .collect(
                                Collectors.toList());
        int len = rates.size();
        if (maxRows > 0) {
            len = Math.min(len, maxRows);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i++) {
            Rate<A> c = rates.get(i);
            builder.append(c.what.toString())
                    .append(" #y")
                    .append(c.percent())
                    .append("% ")
                    .append(c.winLoss())
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
    }

    private static void renderCardPickRate(StatsScreen s, SpriteBatch sb, boolean act1, float screenX) {
        float renderY = getScrollY(s);
        String name = act1 ? "Card pick rate act 1" : "Card pick rate after act 1";
        name += " (top 200)";
        renderHeader(sb, colorForClass(name), screenX, renderY);

        ClassStat cs = moreStatsScreen.getClassStat();
        List<Rate<Card>> picks = act1 ? cs.cardPicksAct1 : cs.cardPicksAfterAct1;
        renderRates(sb, screenX, renderY, 200, picks);
    }


    private static void renderCardWinRate(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        String name = "Card win rate (top 200)";
        renderHeader(sb, colorForClass(name), screenX, renderY);

        ClassStat cs = moreStatsScreen.getClassStat();
        renderRates(sb, screenX, renderY, 200, cs.cardWinRate);
    }

    private static void renderNeowBonus(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        renderHeader(sb, colorForClass("Neow win rate"), screenX, renderY);

        ClassStat cs = moreStatsScreen.getClassStat();
        renderRates1Col(sb, screenX, renderY, 0, cs.neowWinRate);

        renderY -= 44.0F * Settings.scale * cs.neowWinRate.size();
        renderY -= 100 * Settings.scale;
        renderHeader(sb, colorForClass("Neow pick rate"), screenX, renderY);

        renderRates1Col(sb, screenX, renderY, 0, cs.neowPickRate);
    }

    private static void renderBossRelicPickRate(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        ClassStat cs = moreStatsScreen.getClassStat();
        renderHeader(sb, colorForClass("Boss relic pick rate (act 1)"), screenX, renderY);

        renderRates(sb, screenX, renderY, 0, cs.bossRelicPickRateAct1);

        renderY -= 22.0F * Settings.scale * cs.bossRelicPickRateAct1.size();
        renderY -= 100 * Settings.scale;
        renderHeader(sb, colorForClass("Boss relic pick rate (act 2)"), screenX, renderY);

        renderRates(sb, screenX, renderY, 0, cs.bossRelicPickRateAct2);
    }

    private static void renderBossRelicWinRate(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        ClassStat cs = moreStatsScreen.getClassStat();

        renderHeader(sb, colorForClass("Boss relic win rate (act 1)"), screenX, renderY);
        renderRates(sb, screenX, renderY, 0, cs.bossRelicWinRateAct1);

        renderY -= 22.0F * Settings.scale * cs.bossRelicPickRateAct1.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Boss relic win rate (act 2)"), screenX, renderY);
        renderRates(sb, screenX, renderY, 0, cs.bossRelicWinRateAct2);

        renderY -= 22.0F * Settings.scale * cs.bossRelicPickRateAct2.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Boss swap win rate"), screenX, renderY);
        renderRates(sb, screenX, renderY, 0, cs.bossRelicWinRateSwap);
    }

    private static void renderEncounterHPLoss(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        ClassStat cs = moreStatsScreen.getClassStat();
        List<Mean> act1 = cs.averageDamageTaken.getOrDefault(1, new ArrayList<>());
        List<Mean> act2 = cs.averageDamageTaken.getOrDefault(2, new ArrayList<>());
        List<Mean> act3 = cs.averageDamageTaken.getOrDefault(3, new ArrayList<>());
        List<Mean> act4 = cs.averageDamageTaken.getOrDefault(4, new ArrayList<>());

        renderHeader(sb, colorForClass("Average encounter HP loss (act 1)"), screenX, renderY);
        renderT(sb, screenX, renderY, 0, act1, x -> x.what + " #y" + Utils.round(x.mean(), 3));
        renderY -= 22.0F * Settings.scale * act1.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Average encounter HP loss (act 2)"), screenX, renderY);
        renderT(sb, screenX, renderY, 0, act2, x -> x.what + " #y" + Utils.round(x.mean(), 3));
        renderY -= 22.0F * Settings.scale * act2.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Average encounter HP loss (act 3)"), screenX, renderY);
        renderT(sb, screenX, renderY, 0, act3, x -> x.what + " #y" + Utils.round(x.mean(), 3));
        renderY -= 22.0F * Settings.scale * act3.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Average encounter HP loss (act 4)"), screenX, renderY);
        renderT(sb, screenX, renderY, 0, act4, x -> x.what + " #y" + Utils.round(x.mean(), 3));
    }

    private static void renderEncounterMortalityRate(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        ClassStat cs = moreStatsScreen.getClassStat();
        List<Rate<String>> act1 = cs.encounterDeadRate.get(1);
        List<Rate<String>> act2 = cs.encounterDeadRate.get(2);
        List<Rate<String>> act3 = cs.encounterDeadRate.get(3);
        List<Rate<String>> act4 = cs.encounterDeadRate.get(4);

        renderHeader(sb, colorForClass("Encounter mortality rate (act 1)"), screenX, renderY);
        renderRates(sb, screenX, renderY, 0, act1);
        renderY -= 22.0F * Settings.scale * act1.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Encounter mortality rate (act 2)"), screenX, renderY);
        renderRates(sb, screenX, renderY, 0, act2);
        renderY -= 22.0F * Settings.scale * act2.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Encounter mortality rate (act 3)"), screenX, renderY);
        renderRates(sb, screenX, renderY, 0, act3);
        renderY -= 22.0F * Settings.scale * act3.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Encounter mortality rate (act 4)"), screenX, renderY);
        renderRates(sb, screenX, renderY, 0, act4);
    }

    private static void renderEncounterPotionsUsed(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        ClassStat cs = moreStatsScreen.getClassStat();
        List<Mean> act1 = cs.averagePotionUse.getOrDefault(1, new ArrayList<>());
        List<Mean> act2 = cs.averagePotionUse.getOrDefault(2, new ArrayList<>());
        List<Mean> act3 = cs.averagePotionUse.getOrDefault(3, new ArrayList<>());
        List<Mean> act4 = cs.averagePotionUse.getOrDefault(4, new ArrayList<>());

        renderHeader(sb, colorForClass("Average encounter potions used (act 1)"), screenX, renderY);
        renderT(sb, screenX, renderY, 0, act1, x -> x.what + " #y" + Utils.round(x.mean(), 3));
        renderY -= 22.0F * Settings.scale * act1.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Average encounter potions used (act 2)"), screenX, renderY);
        renderT(sb, screenX, renderY, 0, act2, x -> x.what + " #y" + Utils.round(x.mean(), 3));
        renderY -= 22.0F * Settings.scale * act2.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Average encounter potions used (act 3)"), screenX, renderY);
        renderT(sb, screenX, renderY, 0, act3, x -> x.what + " #y" + Utils.round(x.mean(), 3));
        renderY -= 22.0F * Settings.scale * act3.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Average encounter potions used (act 4)"), screenX, renderY);
        renderT(sb, screenX, renderY, 0, act4, x -> x.what + " #y" + Utils.round(x.mean(), 3));
    }

    private static void renderRelicWinRate(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        String name = "Relic win rate";
        renderHeader(sb, colorForClass(name), screenX, renderY);

        ClassStat cs = moreStatsScreen.getClassStat();
        renderRates(sb, screenX, renderY, 0, cs.relicWinRate);
    }

    private static void renderRelicWinRateWhenPurchased(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        String name = "Relic win rate when purchased";
        renderHeader(sb, colorForClass(name), screenX, renderY);

        ClassStat cs = moreStatsScreen.getClassStat();
        renderRates(sb, screenX, renderY, 0, cs.relicPurchasedWinRate);
    }

    private static void renderEventWinRate(StatsScreen s, SpriteBatch sb, float screenX) {
        float renderY = getScrollY(s);
        ClassStat cs = moreStatsScreen.getClassStat();
        List<Rate<String>> act1 = cs.eventWinRateAct1;
        List<Rate<String>> act2 = cs.eventWinRateAct2;
        List<Rate<String>> act3 = cs.eventWinRateAct3;

        renderHeader(sb, colorForClass("Event win rate (act 1)"), screenX, renderY);
        renderRates(sb, screenX, renderY, 0, act1);
        renderY -= 22.0F * Settings.scale * act1.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Event win rate (act 2)"), screenX, renderY);
        renderRates(sb, screenX, renderY, 0, act2);
        renderY -= 22.0F * Settings.scale * act2.size();
        renderY -= 100 * Settings.scale;

        renderHeader(sb, colorForClass("Event win rate (act 3)"), screenX, renderY);
        renderRates(sb, screenX, renderY, 0, act3);
    }

    public static void renderStatScreen(StatsScreen s, SpriteBatch sb) {
        float screenX = Utils.getField(s, StatsScreen.class, "screenX");
        float scrollY = Utils.getField(s, StatsScreen.class, "scrollY");
        boolean renderSampleSize = true;

        switch (moreStatsScreen.STAT_TYPES[moreStatsScreen.statTypeDropdown.getSelectedIndex()]) {
            case "Overall":
                renderSampleSize = false;
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
            case "Encounter average HP loss":
                renderEncounterHPLoss(s, sb, screenX);
                break;
            case "Encounter mortality rate":
                renderEncounterMortalityRate(s, sb, screenX);
                break;
            case "Encounter average potions used":
                renderEncounterPotionsUsed(s, sb, screenX);
                break;
            case "Relic win rate":
                renderRelicWinRate(s, sb, screenX);
                break;
            case "Relic win rate when purchased":
                renderRelicWinRateWhenPurchased(s, sb, screenX);
                break;
            case "Event win rate per act":
                renderEventWinRate(s, sb, screenX);
                break;
        }

        float labelY = scrollY + screenPosY(950);
        float dropdownY = labelY - screenPosY(40);

        float xDiff = 170;
        float startDateX = screenX + screenPosX(-270);
        float endDateX = startDateX + screenPosX(xDiff);
        float characterX = endDateX + screenPosX(xDiff);
        float statTypeX = characterX + screenPosX(xDiff);
        float includeAbandonsX = statTypeX + screenPosX(375);
        float sampleSizeX = includeAbandonsX + screenPosX(xDiff * 2);

        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, "Start date", startDateX, labelY, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, "End date", endDateX, labelY, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, "Character", characterX, labelY, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, "Stat type", statTypeX, labelY, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb,
                FontHelper.tipBodyFont,
                "Include Abandons?",
                includeAbandonsX,
                labelY,
                Settings.CREAM_COLOR);
        if (renderSampleSize) {
            FontHelper.renderSmartText(sb,
                    FontHelper.tipBodyFont,
                    "Min sample size",
                    sampleSizeX,
                    labelY,
                    Settings.CREAM_COLOR);
        }

        moreStatsScreen.startDateDropdown.render(sb, startDateX, dropdownY);
        moreStatsScreen.endDateDropDown.render(sb, endDateX, dropdownY);
        moreStatsScreen.classDropdown.render(sb, characterX, dropdownY);
        moreStatsScreen.statTypeDropdown.render(sb, statTypeX, dropdownY);
        moreStatsScreen.abandonDropdown.render(sb, includeAbandonsX, dropdownY);
        if (renderSampleSize) {
            moreStatsScreen.sampleSizeDropdown.render(sb, sampleSizeX, dropdownY);
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
}
