package StatsTracker.stats;

import java.util.HashMap;
import java.util.Map;

public final class EncounterStats {
    public final int floor;
    public final String enemies;
    public final int damage;
    public final int turns;
    public final int potionsUsed;

    public EncounterStats(int floor, String enemies, int damage, int turns, int potionsUsed) {
        this.floor = floor;
        this.enemies = getNameReplacementMap().getOrDefault(enemies, enemies);
        this.damage = damage;
        this.turns = turns;
        this.potionsUsed = potionsUsed;
    }

    public static Map<String, String> getNameReplacementMap() {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("BookOfStabbing", "Book of Stabbing");
        replacements.put("HealerTank", "Centurion and Healer");
        replacements.put("3_Byrds", "3 Byrds");
        replacements.put("GiantHead", "Giant Head");
        replacements.put("Darkling Encounter", "3 Darklings");
        replacements.put("Gremlin Leader Combat", "Gremlin Leader");
        replacements.put("SlimeBoss", "Slime Boss");
        replacements.put("Double Orb Walker", "2 Orb Walkers");
        replacements.put("JawWorm", "Jaw Worm");
        replacements.put("Shelled Parasite", "Shell Parasite");
        replacements.put("GremlinNob", "Gremlin Nob");
        replacements.put("Sentries", "3 Sentries");

        return replacements;
    }
}
