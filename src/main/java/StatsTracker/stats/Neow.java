package StatsTracker.stats;

import java.util.HashMap;

public class Neow implements Comparable<Neow> {
    public final String bonus;
    public final String cost;

    private static final HashMap<String, String> bonusStrings = new HashMap<String, String>() {{
        put("THREE_CARDS", "Choose one of three cards to obtain.");
        put("RANDOM_COLORLESS", "Choose an uncommon Colorless card to obtain.");
        put("RANDOM_COMMON_RELIC", "Obtain a random common relic.");
        put("REMOVE_CARD", "Remove a card.");
        put("TRANSFORM_CARD", "Transform a card.");
        put("UPGRADE_CARD", "Upgrade a card.");
        put("THREE_ENEMY_KILL", "Enemies in the first three combats have 1 HP.");
        put("THREE_SMALL_POTIONS", "Gain 3 random potions.");
        put("TEN_PERCENT_HP_BONUS", "Gain 10% Max HP.");
        put("ONE_RANDOM_RARE_CARD", "Gain a random Rare card.");
        put("HUNDRED_GOLD", "Gain 100 gold.");
        put("TWO_FIFTY_GOLD", "Gain 250 gold.");
        put("TWENTY_PERCENT_HP_BONUS", "Gain 20% Max HP.");
        put("RANDOM_COLORLESS_2", "Choose a Rare colorless card to obtain.");
        put("THREE_RARE_CARDS", "Choose a Rare card to obtain.");
        put("REMOVE_TWO", "Remove two cards.");
        put("TRANSFORM_TWO_CARDS", "Transform two cards.");
        put("ONE_RARE_RELIC", "Obtain a random Rare relic.");
        put("BOSS_RELIC", "Lose your starter relic. Obtain a random Boss relic.");
    }};

    private static final HashMap<String, String> costStrings = new HashMap<String, String>() {{
        put("CURSE", "Gain a curse.");
        put("NO_GOLD", "Lose all gold.");
        put("TEN_PERCENT_HP_LOSS", "Lose 10% Max HP.");
        put("PERCENT_DAMAGE", "Take damage.");
        put("TWO_STARTER_CARDS", "Add two starter cards.");
        put("ONE_STARTER_CARD", "Add a starter card.");
    }};


    public Neow(String bonus, String cost) {
        this.bonus = bonus;
        this.cost = cost;
    }

    @Override
    public String toString() {
        if (cost == null || cost.equals("NONE")) {
            return bonusStrings.getOrDefault(bonus, bonus);
        }
        String downside = costStrings.getOrDefault(cost, cost);
        return bonusStrings.getOrDefault(bonus, bonus) + " " + downside;
    }

    @Override
    public int compareTo(Neow neow) {
        return this.toString().compareTo(neow.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Neow neow = (Neow) o;
        return this.bonus.equals(neow.bonus) && this.cost.equals(neow.cost);
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
