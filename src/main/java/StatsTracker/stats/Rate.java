package StatsTracker.stats;

import StatsTracker.Utils;

public class Rate<T> implements Comparable<Rate<T>> {
    public final T what;
    public int win = 0;
    public int loss = 0;

    public Rate(T what) {
        this.what = what;
    }

    public int total() {
        return win + loss;
    }

    public double pickRatePercent() {
        return Utils.calculatePercent(win, total());
    }

    public String winLoss() {
        return "(" + win + "/" + total() + ")";
    }

    @Override
    public int compareTo(Rate rate) {
        if (pickRatePercent() == rate.pickRatePercent()) {
            return what.toString().compareTo(rate.what.toString());
        }
        return pickRatePercent() < rate.pickRatePercent() ? 1 : -1;
    }
}
