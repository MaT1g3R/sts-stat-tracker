package StatsTracker.stats;

import java.util.HashMap;
import java.util.Map;

public class Card implements Comparable<Card> {

    public final String name;
    public final int upgrades;

    public Card(String name, int upgrades) {
        this.name = name;
        this.upgrades = upgrades;
    }

    public static Card SKIP() {
        return new Card("SKIP", 0);
    }

    public static Card SingingBowl() {
        return new Card("Singing Bowl", 0);
    }


    public static Card fromString(String s) {
        String[] parts = s.split("\\+");
        String name = parts[0];
        int upgrades = 0;
        if (parts.length > 1) {
            upgrades = Integer.parseInt(parts[1]);
        } else if (name.contains("+")) {
            upgrades = 1;
        }
        return new Card(name, upgrades);
    }

    public static Card fromStringIgnoreUpgrades(String s) {
        return fromString(s.split("\\+")[0]);
    }

    @Override
    public int compareTo(Card card) {
        if (this.name.equals(card.name)) {
            return this.upgrades - card.upgrades;
        }
        return this.name.compareTo(card.name);
    }

    @Override
    public String toString() {
        String localizedName = StatsTracker.Utils.getLocalizedName(this.name);
        switch (this.upgrades) {
            case 0:
                return localizedName;
            case 1:
                return localizedName + "+";
            default:
                return localizedName + "+" + this.upgrades;
        }
    }

    @Override
    public int hashCode() {
        return 31 * this.upgrades + this.name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return this.upgrades == card.upgrades && this.name.equals(card.name);
    }
}
