package StatsTracker.stats;

public class BossRelic implements Comparable<BossRelic> {

    public final String name;
    public final int act;

    public BossRelic(String name, int act) {
        this.name = name;
        this.act = act;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return act * 31 + name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BossRelic that = (BossRelic) o;
        return act == that.act && name.equals(that.name);
    }

    @Override
    public int compareTo(BossRelic bossRelic) {
        if (this.name.equals(bossRelic.name)) {
            return this.act - bossRelic.act;
        }
        return this.name.compareTo(bossRelic.name);
    }
}
