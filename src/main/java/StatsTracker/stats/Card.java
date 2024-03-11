package StatsTracker.stats;

import java.util.HashMap;
import java.util.Map;

public class Card implements Comparable<Card> {
    private static final Map<String, String> nameReplacementMap = getNameReplacementMap();

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
        String name = nameReplacementMap.getOrDefault(parts[0], parts[0]);
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

    private static Map<String, String> getNameReplacementMap() {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("Lockon", "Bullseye");
        replacements.put("Steam", "Steam Barrier");
        replacements.put("Steam Power", "Overclock");
        replacements.put("Redo", "Recursion");
        replacements.put("Undo", "Equilibrium");
        replacements.put("Gash", "Claw");
        replacements.put("Venomology", "Alchemize");
        replacements.put("Night Terror", "Nightmare");
        replacements.put("NightTerror", "Nightmare");
        replacements.put("Crippling Poison", "Crippling Cloud");
        replacements.put("CripplingPoison", "Crippling Cloud");
        replacements.put("Underhanded Strike", "Sneaky Strike");
        replacements.put("UnderhandedStrike", "Sneaky Strike");
        replacements.put("Clear The Mind", "Tranquility");
        replacements.put("ClearTheMind", "Tranquility");
        replacements.put("Wireheading", "Foresight");
        replacements.put("Vengeance", "Simmering Fury");
        replacements.put("Adaptation", "Rushdown");
        replacements.put("Path to Victory", "Pressure Points");
        replacements.put("PathToVictory", "Pressure Points");
        replacements.put("Ghostly", "Apparition");
        return replacements;
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
        switch (this.upgrades) {
            case 0:
                return this.name;
            case 1:
                return this.name + "+";
            default:
                return this.name + "+" + this.upgrades;
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
