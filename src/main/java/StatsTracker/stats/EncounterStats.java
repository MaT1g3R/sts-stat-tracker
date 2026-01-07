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
        this.enemies = enemies;
        this.damage = damage;
        this.turns = turns;
        this.potionsUsed = potionsUsed;
    }
}
