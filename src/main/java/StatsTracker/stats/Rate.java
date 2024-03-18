package StatsTracker.stats;

import StatsTracker.Utils;

public class Rate<T> implements Comparable<Rate<T>>, SampleSize {
    public final T what;
    public int win = 0;
    public int loss = 0;

    public Rate(T what) {
        this.what = what;
    }

    public int total() {
        return win + loss;
    }

    public double percent() {
        return Utils.calculatePercent(win, total());
    }

    public String winLoss() {
        return "(" + win + "/" + total() + ")";
    }

    @Override
    public int compareTo(Rate rate) {
        if (percent() == rate.percent()) {
            return what.toString().compareTo(rate.what.toString());
        }
        return percent() < rate.percent() ? 1 : -1;
    }

    @Override
    public int getSampleSize() {
        return total();
    }
}
