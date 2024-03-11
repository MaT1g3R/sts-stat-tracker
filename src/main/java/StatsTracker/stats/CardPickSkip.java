package StatsTracker.stats;

import StatsTracker.Utils;

public class CardPickSkip implements Comparable<CardPickSkip> {
    public final String name;
    public int picks = 0;
    public int skips = 0;

    public CardPickSkip(String name) {
        this.name = name;
    }

    public double pickRatePercent() {
        return Utils.calculatePercent(picks, picks + skips);
    }

    @Override
    public int compareTo(CardPickSkip cardPickSkip) {
        if (pickRatePercent() == cardPickSkip.pickRatePercent()) {
            return name.compareTo(cardPickSkip.name);
        }
        return pickRatePercent() < cardPickSkip.pickRatePercent() ? 1 : -1;
    }
}
