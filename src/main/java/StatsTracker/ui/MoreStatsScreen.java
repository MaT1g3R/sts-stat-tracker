package StatsTracker.ui;

import StatsTracker.StatsTracker;
import StatsTracker.YearMonth;
import StatsTracker.stats.ClassStat;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.options.DropdownMenuListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MoreStatsScreen implements DropdownMenuListener {
    public DropdownMenu startDateDropdown;
    public DropdownMenu endDateDropDown;
    public DropdownMenu classDropdown;
    public DropdownMenu statTypeDropdown;
    public DropdownMenu abandonDropdown;
    public DropdownMenu sampleSizeDropdown;
    public ArrayList<YearMonth> startDates;
    public ArrayList<YearMonth> endDates;
    public ClassStat[] classStats;
    public final String[] CLASSES = new String[]{"All", "Ironclad", "Silent", "Defect", "Watcher"};
    public final String[]
            STAT_TYPES =
            new String[]{"Overall",
                    "Card pick rate act 1",
                    "Card pick rate after act 1",
                    "Card win rate",
                    "Neow bonus",
                    "Boss relic pick rate",
                    "Boss relic win rate",
                    "Encounter average HP loss",
                    "Encounter mortality rate",
                    "Encounter average potions used",
                    "Relic win rate",
                    "Relic win rate when purchased",
            };

    public String currentClass() {
        return CLASSES[classDropdown.getSelectedIndex()];
    }

    public ClassStat getClassStat() {
        return classStats[classStats.length - 1];
    }

    @Override
    public void changedSelectionTo(DropdownMenu dropdownMenu, int i, String s) {
        int startDateIndex = startDateDropdown.getSelectedIndex();
        int endDateIndex = endDateDropDown.getSelectedIndex();

        setClassStats(startDates.get(startDateIndex),
                endDates.get(endDateIndex),
                currentClass(),
                abandonDropdown.getSelectedIndex() == 0);
    }

    public void update() {
        startDateDropdown.update();
        endDateDropDown.update();
        classDropdown.update();
        statTypeDropdown.update();
        abandonDropdown.update();
        sampleSizeDropdown.update();
    }

    public void refreshData() {
        StatsTracker.runHistoryManager.refreshData();
        startDates = StatsTracker.runHistoryManager.getDates().collect(Collectors.toCollection(ArrayList::new));
        if (startDates.isEmpty()) {
            LocalDate today = LocalDate.now();
            startDates.add(new YearMonth(today.getYear(), today.getMonthValue()));
        }
        endDates = new ArrayList<>(startDates);

        endDates.add(0, startDates.get(startDates.size() - 1));

        startDateDropdown =
                new DropdownMenu(this,
                        startDates.stream().map(x -> x.toString()).collect(Collectors.toCollection(ArrayList::new)),
                        FontHelper.tipBodyFont,
                        Settings.LIGHT_YELLOW_COLOR);
        endDateDropDown =
                new DropdownMenu(this,
                        endDates.stream().map(x -> x.toString()).collect(Collectors.toCollection(ArrayList::new)),
                        FontHelper.tipBodyFont,
                        Settings.LIGHT_YELLOW_COLOR);

        classDropdown = new DropdownMenu(this, CLASSES, FontHelper.tipBodyFont, Settings.LIGHT_YELLOW_COLOR);

        statTypeDropdown = new DropdownMenu(null, STAT_TYPES, FontHelper.tipBodyFont, Settings.LIGHT_YELLOW_COLOR);

        abandonDropdown =
                new DropdownMenu(this, new String[]{"Yes", "No"}, FontHelper.tipBodyFont, Settings.LIGHT_YELLOW_COLOR);

        sampleSizeDropdown =
                new DropdownMenu(null,
                        new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"},
                        FontHelper.tipBodyFont,
                        Settings.LIGHT_YELLOW_COLOR);

        setClassStats(startDates.get(startDateDropdown.getSelectedIndex()),
                endDates.get(endDateDropDown.getSelectedIndex()),
                CLASSES[classDropdown.getSelectedIndex()], abandonDropdown.getSelectedIndex() == 0);
    }

    public boolean isDropDownOpen() {
        return startDateDropdown.isOpen || endDateDropDown.isOpen || classDropdown.isOpen;
    }

    private static ClassStat[] getClassStats(YearMonth startDate,
                                             YearMonth endDate,
                                             String className,
                                             boolean includeAbandons) {
        ClassStat
                ironcladCS =
                new ClassStat(StatsTracker.runHistoryManager.getIroncladRuns(startDate, endDate, includeAbandons),
                        false);
        ClassStat
                silentCS =
                new ClassStat(StatsTracker.runHistoryManager.getSilentRuns(startDate, endDate, includeAbandons), false);
        ClassStat
                defectCS =
                new ClassStat(StatsTracker.runHistoryManager.getDefectRuns(startDate, endDate, includeAbandons), false);
        ClassStat
                watcherCS =
                new ClassStat(StatsTracker.runHistoryManager.getWatcherRuns(startDate, endDate, includeAbandons),
                        false);

        ClassStat allCS;
        switch (className) {
            case "Ironclad":
                allCS = ironcladCS;
                break;
            case "Silent":
                allCS = silentCS;
                break;
            case "Defect":
                allCS = defectCS;
                break;
            case "Watcher":
                allCS = watcherCS;
                break;
            default:
                allCS =
                        new ClassStat(StatsTracker.runHistoryManager.getAllRuns(startDate, endDate, includeAbandons),
                                true);
        }
        return new ClassStat[]{ironcladCS, silentCS, defectCS, watcherCS, allCS};
    }

    private void setClassStats(YearMonth startDate, YearMonth endDate, String className, boolean includeAbandons) {
        classStats = getClassStats(startDate, endDate, className, includeAbandons);
    }
}
