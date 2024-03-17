package StatsTracker.stats;

import java.util.ArrayList;
import java.util.List;

public class Mean implements Comparable<Mean> {
    public String what;
    private final List<Double> values = new ArrayList<>();

    private double cachedMean = 0;
    private int cachedCount = 0;

    public Mean(String what) {
        this.what = what;
    }

    public void add(double value) {
        values.add(value);
    }

    public void add(int value) {
        values.add((double) value);
    }

    public double mean() {
        int size = values.size();
        if (size == cachedCount) {
            return cachedMean;
        }
        cachedCount = size;
        cachedMean = values.stream().reduce(Double::sum).map(x -> x / size).orElse(0D);
        return cachedMean;
    }

    @Override
    public int compareTo(Mean mean) {
        double diff = mean.mean() - this.mean();
        if (diff == 0) {
            return what.compareTo(mean.what);
        }
        return diff > 0 ? 1 : -1;
    }
}
