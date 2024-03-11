package StatsTracker;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class YearMonth implements Comparable<YearMonth> {
    public final int year;
    public final int month;

    public YearMonth(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public static YearMonth fromDate(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return new YearMonth(localDate.getYear(), localDate.getMonthValue());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YearMonth that = (YearMonth) o;
        return year == that.year && month == that.month;
    }

    @Override
    public int hashCode() {
        return 31 * year + month;
    }

    public String toString() {
        String monthString = month < 10 ? "0" + month : "" + month;
        return year + "-" + monthString;
    }

    @Override
    public int compareTo(YearMonth yearMonth) {
        if (this.year == yearMonth.year) {
            return this.month - yearMonth.month;
        }
        return this.year - yearMonth.year;
    }
}
