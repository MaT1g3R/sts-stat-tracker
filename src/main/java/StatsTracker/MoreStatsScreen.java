package StatsTracker;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.options.DropdownMenu;
import com.megacrit.cardcrawl.screens.options.DropdownMenuListener;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class MoreStatsScreen implements DropdownMenuListener {
    public DropdownMenu startDateDropdown;
    public DropdownMenu endDateDropDown;
    public DropdownMenu classDropdown;
    public ArrayList<YearMonth> startDates;
    public ArrayList<YearMonth> endDates;
    public ClassStat[] classStats;
    public AbstractPlayer.PlayerClass playerClass;

    @Override
    public void changedSelectionTo(DropdownMenu dropdownMenu, int i, String s) {
        int startDateIndex = startDateDropdown.getSelectedIndex();
        int endDateIndex = endDateDropDown.getSelectedIndex();

        setClassStats(startDates.get(startDateIndex), endDates.get(endDateIndex));
    }

    public void update() {
        startDateDropdown.update();
        endDateDropDown.update();
        classDropdown.update();
    }

    public void refreshData() {
        StatsTracker.runHistoryManager.refreshData();
        startDates = StatsTracker.runHistoryManager.getDates().collect(Collectors.toCollection(ArrayList::new));
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

        classDropdown = new DropdownMenu(this, new String[]{""}, FontHelper.tipBodyFont, Settings.LIGHT_YELLOW_COLOR);

        setClassStats(startDates.get(startDateDropdown.getSelectedIndex()),
                endDates.get(endDateDropDown.getSelectedIndex()));
    }

    public boolean isDropDownOpen() {
        return startDateDropdown.isOpen || endDateDropDown.isOpen || classDropdown.isOpen;
    }

    private static ClassStat[] getClassStats(YearMonth startDate, YearMonth endDate) {
        ClassStat ironcladCS = new ClassStat(StatsTracker.runHistoryManager.getIroncladRuns(startDate, endDate), false);
        ClassStat silentCS = new ClassStat(StatsTracker.runHistoryManager.getSilentRuns(startDate, endDate), false);
        ClassStat defectCS = new ClassStat(StatsTracker.runHistoryManager.getDefectRuns(startDate, endDate), false);
        ClassStat watcherCS = new ClassStat(StatsTracker.runHistoryManager.getWatcherRuns(startDate, endDate), false);
        ClassStat rotatingCS = new ClassStat(StatsTracker.runHistoryManager.getAllRuns(startDate, endDate), true);
        return new ClassStat[]{ironcladCS, silentCS, defectCS, watcherCS, rotatingCS};
    }

    private void setClassStats(YearMonth startDate, YearMonth endDate) {
        classStats = getClassStats(startDate, endDate);
    }
}
