package StatsTracker.stats;

import StatsTracker.Utils;

public class PickSkip<T> implements Comparable<PickSkip<T>> {
    public final T what;
    public int picks = 0;
    public int skips = 0;

    public PickSkip(T what) {
        this.what = what;
    }

    public double pickRatePercent() {
        return Utils.calculatePercent(picks, picks + skips);
    }

    @Override
    public int compareTo(PickSkip pickSkip) {
        if (pickRatePercent() == pickSkip.pickRatePercent()) {
            return what.toString().compareTo(pickSkip.what.toString());
        }
        return pickRatePercent() < pickSkip.pickRatePercent() ? 1 : -1;
    }
}
