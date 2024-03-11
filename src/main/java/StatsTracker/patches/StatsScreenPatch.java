package StatsTracker.patches;

import StatsTracker.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static com.megacrit.cardcrawl.screens.stats.StatsScreen.NAMES;
import static com.megacrit.cardcrawl.screens.stats.StatsScreen.renderHeader;

public class StatsScreenPatch {
    public static DropdownMenu startDateDropdown;
    public static DropdownMenu endDateDropDown;
    public static ArrayList<YearMonth> startDates;
    public static ArrayList<YearMonth> endDates;
    public static int startDateIndex = 0;
    public static int endDateIndex = 0;

    public static ClassStat[] classStats;

//    @SpirePatch(clz = StatsScreen.class, method = SpirePatch.CONSTRUCTOR)
//    public static class ConstructorPatch {
//        public static void Postfix(StatsScreen __instance) {
//            startDateDropdown =
//                    new DropdownMenu(null, new String[]{"1", "2"}, FontHelper.tipBodyFont, Settings.LIGHT_YELLOW_COLOR);
//            endDateDropDown =
//                    new DropdownMenu(null, new String[]{"3", "4"}, FontHelper.tipBodyFont, Settings.LIGHT_YELLOW_COLOR);
//        }
//    }

    @SpirePatch(clz = StatsScreen.class, method = "updateScrolling")
    public static class UpdateScrollingPatch {
        public static SpireReturn<Void> Prefix(StatsScreen __instance) {
            if (startDateDropdown.isOpen || endDateDropDown.isOpen) {
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(clz = StatsScreen.class, method = "update")
    public static class UpdatePatch {
        public static void Postfix(StatsScreen __instance) {
            startDateDropdown.update();
            endDateDropDown.update();
            int tmpStartDateIndex = startDateDropdown.getSelectedIndex();
            int tmpEndDateIndex = endDateDropDown.getSelectedIndex();
            if (tmpStartDateIndex != startDateIndex || tmpEndDateIndex != endDateIndex) {
                startDateIndex = tmpStartDateIndex;
                endDateIndex = tmpEndDateIndex;
                setClassStats();
            }
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
        public static void Postfix(StatsScreen __instance) {
            StatsTracker.runHistoryManager.refreshData();
            startDates = StatsTracker.runHistoryManager.getDates().collect(Collectors.toCollection(ArrayList::new));
            endDates = new ArrayList<>(startDates);
            endDates.add(0, startDates.get(startDates.size() - 1));

            startDateDropdown =
                    new DropdownMenu(null,
                            startDates.stream().map(x -> x.toString()).collect(Collectors.toCollection(ArrayList::new)),
                            FontHelper.tipBodyFont,
                            Settings.LIGHT_YELLOW_COLOR);
            endDateDropDown =
                    new DropdownMenu(null,
                            endDates.stream().map(x -> x.toString()).collect(Collectors.toCollection(ArrayList::new)),
                            FontHelper.tipBodyFont,
                            Settings.LIGHT_YELLOW_COLOR);
            setClassStats();
        }
    }

    private static float screenPosX(float val) {
        return val * Settings.xScale;
    }

    private static float screenPosY(float val) {
        return val * Settings.yScale;
    }

    public static void renderStatScreen(StatsScreen s, SpriteBatch sb) {
        float renderY = Utils.getField(s, StatsScreen.class, "scrollY");
        float screenX = Utils.getField(s, StatsScreen.class, "screenX");
        float scrollY = Utils.getField(s, StatsScreen.class, "scrollY");

        float labelY = scrollY + screenPosY(950);
        float dropdownY = labelY - screenPosY(40);

        float startDateX = screenX + screenPosX(-270);
        float endDateX = screenX + screenPosX(-100);

        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, "Start date", startDateX, labelY, Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, "End date", endDateX, labelY, Settings.CREAM_COLOR);

        endDateDropDown.render(sb, endDateX, dropdownY);
        StatsScreenPatch.startDateDropdown.render(sb, startDateX, dropdownY);

        renderHeader(sb, NAMES[0], screenX, renderY);
        new CharStatRenderer(classStats[classStats.length - 1]).render(sb, screenX, renderY);
        renderY -= 400.0F * Settings.scale;
        renderHeader(sb, NAMES[1], screenX, renderY);
        StatsScreen.achievements.render(sb, renderY);
        renderY -= 2200.0F * Settings.scale;

        for (int i = 0; i < 4; i++) {
            String name = NAMES[i + 2];
            renderHeader(sb, name, screenX, renderY);
            new CharStatRenderer(classStats[i]).render(sb, screenX, renderY);
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
        YearMonth startDate = startDates.get(startDateIndex);
        YearMonth endDate = endDates.get(endDateIndex);
        ClassStat ironcladCS = new ClassStat(StatsTracker.runHistoryManager.getIroncladRuns(startDate, endDate), false);
        ClassStat silentCS = new ClassStat(StatsTracker.runHistoryManager.getSilentRuns(startDate, endDate), false);
        ClassStat defectCS = new ClassStat(StatsTracker.runHistoryManager.getDefectRuns(startDate, endDate), false);
        ClassStat watcherCS = new ClassStat(StatsTracker.runHistoryManager.getWatcherRuns(startDate, endDate), false);
        ClassStat rotatingCS = new ClassStat(StatsTracker.runHistoryManager.getAllRuns(startDate, endDate), true);
        return new ClassStat[]{ironcladCS, silentCS, defectCS, watcherCS, rotatingCS};
    }

    private static void setClassStats() {
        classStats = getClassStats();
    }
}
