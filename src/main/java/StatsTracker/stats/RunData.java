package StatsTracker.stats;


import com.megacrit.cardcrawl.screens.stats.*;

import java.util.*;

public class RunData {
    public String character_chosen;
    public String loadout;
    public String build_version;
    public String seed_played;
    public boolean chose_seed;
    public String timestamp;
    public String local_time;
    public boolean victory;
    public boolean is_daily;
    public boolean is_trial;
    public boolean is_endless;
    public boolean is_ascension_mode;
    public boolean is_special_run;
    public Long special_seed;
    public boolean isUploaded;
    public int score;
    public int ascension_level;
    public int floor_reached;
    public int gold;
    public int playtime;
    public int purchased_purges;
    public String killed_by;
    public String neow_bonus;
    public String neow_cost;
    public int rested;
    public int rituals;
    public int upgraded;
    public int meditates;
    public List<String> master_deck;
    public List<String> relics = new ArrayList<>();
    public int circlet_count;
    public List<String> path_taken;
    public List<String> path_per_floor;
    public List<Integer> current_hp_per_floor;
    public List<Integer> max_hp_per_floor;
    public List<String> items_purchased = new ArrayList<>();
    public List<Integer> item_purchase_floors;
    public List<String> items_purged;
    public List<Integer> items_purged_floors;
    public List<Integer> gold_per_floor;
    public List<String> daily_mods;
    public List<BattleStats> damage_taken = new ArrayList<>();
    public List<EventStats> event_choices = new ArrayList<>();
    public List<CardChoiceStats> card_choices;
    public List<ObtainStats> relics_obtained = new ArrayList<>();
    public List<ObtainStats> potions_obtained;
    public List<BossRelicChoiceStats> boss_relics;
    public List<CampfireChoice> campfire_choices;

    // Run history+ stats
    public List<String> score_breakdown = new ArrayList<>();
    public List<String> neow_bonuses_skipped_log = new ArrayList<>();
    public List<String> neow_costs_skipped_log = new ArrayList<>();
    public List<List<String>> potion_use_per_floor = new ArrayList<>();
    public static Comparator<RunData> orderByTimestampDesc = (o1, o2) -> o2.timestamp.compareTo(o1.timestamp);
    public Map<String, List<Integer>> improvable_cards = new HashMap<>();
    public List<List<String>> potions_obtained_alchemize = new ArrayList<>();
    public List<List<String>> lesson_learned_per_floor = new ArrayList<>();

    public RunData() {
    }
}
