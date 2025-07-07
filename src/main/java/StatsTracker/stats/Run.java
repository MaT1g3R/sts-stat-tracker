package StatsTracker.stats;

import StatsTracker.StatsTracker;
import StatsTracker.YearMonth;
import com.badlogic.gdx.files.FileHandle;
import com.google.gson.JsonSyntaxException;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.screens.stats.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static basemod.BaseMod.gson;

public class Run implements Comparable<Run> {
    private transient final RunData runData;

    public int portalFloor = -1;

    public final AbstractPlayer.PlayerClass playerClass;

    public final boolean isHeartKill;
    public final boolean isA20;
    public final boolean abandoned;
    public final long timestamp;
    public final String killedBy;
    public final int floorsReached;
    public final int playtime;
    public final int score;
    public final int gold;
    public final int maxHP;
    public final int potionsCreated;
    public final int lessonsLearned;
    public final int maxDagger;
    public final int maxAlgo;
    public final int maxSearingBlow;

    public final int enemiesKilled;
    public final int bossesKilled;
    public final Neow neowPicked;
    public final List<Neow> neowSkipped = new ArrayList<>();
    public final List<String> relicsPurchased;

    public final List<CardChoice> cardChoices = new ArrayList<>();
    public final List<Card> masterDeck;
    public final BossRelic bossSwapRelic;
    public final List<BossRelicChoiceStats> bossRelicChoiceStats;
    public final List<EncounterStats> encounterStats = new ArrayList<>();
    public final List<String> relics;
    public final List<EventStats> eventStats;
    public final List<String> itemsPurged;

    private Run(RunData runData, AbstractPlayer.PlayerClass playerClass) {
        this.runData = runData;
        this.playerClass = playerClass;
        this.isHeartKill = isHeartKill();
        this.isA20 = runData.is_ascension_mode && runData.ascension_level == 20;
        this.timestamp = Long.parseLong(runData.timestamp);

        boolean abandoned = false;
        if (runData.killed_by == null || runData.killed_by.isEmpty()) {
            if (!runData.victory) {
                abandoned = true;
            }
        }
        this.abandoned = abandoned;

        masterDeck = runData.master_deck.stream().map(Card::fromString).collect(Collectors.toList());
        killedBy = runData.killed_by;
        floorsReached = runData.floor_reached;
        relics = runData.relics;
        playtime = runData.playtime;
        eventStats = runData.event_choices;
        score = runData.score;
        itemsPurged = runData.items_purged;

        Pair<Integer, Integer> enemiesKilled = enemiesKilled();
        this.enemiesKilled = enemiesKilled.getLeft();
        this.bossesKilled = enemiesKilled.getRight();

        for (EventStats s : runData.event_choices) {
            if (s.event_name.equals("SecretPortal") && s.player_choice.equals("Took Portal")) {
                portalFloor = s.floor;
            }

            if (s.event_name.equals("N'loth") && s.relics_lost != null) {
                s.relics_lost.stream().filter(Objects::nonNull).filter(x -> !x.isEmpty()).forEach(relics::add);
            }
        }

        relicsPurchased = runData.items_purchased.stream().filter(relics::contains).collect(Collectors.toList());

        int singingBowlFloor = -1;
        for (ObtainStats r : runData.relics_obtained) {
            int floor = r.floor;
            String key = r.key;
            if (key.equals("Singing Bowl")) {
                singingBowlFloor = floor;
            }
        }

        if (runData.neow_bonus != null && !runData.neow_bonus.isEmpty()) {
            this.neowPicked = new Neow(runData.neow_bonus, runData.neow_cost);
        } else {
            this.neowPicked = null;
        }
        for (int i = 0; i < runData.neow_bonuses_skipped_log.size(); i++) {
            this.neowSkipped.add(new Neow(runData.neow_bonuses_skipped_log.get(i),
                    runData.neow_costs_skipped_log.get(i)));
        }

        bossSwapRelic = this.bossSwapRelic();
        bossRelicChoiceStats = runData.boss_relics;

        Map<Integer, Integer> potByFloor = new HashMap<>();
        for (int i = 0; i < runData.potion_use_per_floor.size(); ++i) {
            int floor = i + 1;
            int potionsUsed = Math.min(runData.potion_use_per_floor.get(i).size(), 9);
            potByFloor.put(floor, potionsUsed);
        }

        for (BattleStats bs : runData.damage_taken) {
            int potsUsed = potByFloor.getOrDefault(bs.floor, -1);
            EncounterStats e = new EncounterStats(bs.floor, bs.enemies, bs.damage, bs.turns, potsUsed);
            encounterStats.add(e);
        }
        gold = parseGold();
        maxHP = getMaxHP();
        potionsCreated = getPotionsCreated();
        lessonsLearned = getLessonsLearned();
        maxSearingBlow = parseSearingBlow();

        AtomicInteger maxDagger = new AtomicInteger();
        AtomicInteger maxAlgo = new AtomicInteger();
        runData.improvable_cards.forEach((n, cs) -> {
            String name = n.toLowerCase();
            if (name.contains("ritualdagger")) {
                cs.forEach(c -> {
                    maxDagger.set(Math.max(maxDagger.get(), c));
                });
            } else if (name.contains("genetic algorithm")) {
                cs.forEach(c -> {
                    maxAlgo.set(Math.max(maxAlgo.get(), c));
                });
            }
        });
        this.maxDagger = maxDagger.get();
        this.maxAlgo = maxAlgo.get();

        for (CardChoiceStats c : runData.card_choices) {
            Card picked = Card.fromStringIgnoreUpgrades(c.picked);
            List<Card>
                    notPicked =
                    c.not_picked.stream().map(Card::fromStringIgnoreUpgrades).collect(Collectors.toList());
            if (!picked.name.equals("SKIP") && !picked.name.equals("Singing Bowl")) {
                if (-1 < singingBowlFloor && singingBowlFloor <= c.floor) {
                    notPicked.add(Card.SingingBowl());
                } else {
                    notPicked.add(Card.SKIP());
                }
            }
            cardChoices.add(new CardChoice(picked, notPicked, c.floor));
        }
    }

    private static List<String> bannedCards = Arrays.asList("pride", "omega");
    private static List<String> bannedRelics = Arrays.asList("bargainbundle", "pet ghost", "replayfunnel");
    private static List<String>
            bannedNeows =
            Arrays.asList("CHOOSE_OTHER_CHAR_RANDOM_RARE_CARD",
                    "SWAP_MEMBERSHIP_COURIER",
                    "CHOOSE_OTHER_CHAR_RANDOM_UNCOMMON_CARD",
                    "CHOOSE_OTHER_CHAR_RANDOM_COMMON_CARD",
                    "GAIN_POTION_SLOT",
                    "GAIN_TWO_POTION_SLOTS",
                    "GAIN_TWO_RANDOM_COMMON_RELICS",
                    "GAIN_UNCOMMON_RELIC"
            );
    private static List<String> bannedBossRelics = Arrays.asList(
            "Honey Jar",
            "Snecko Heart",
            "Unceasing Top",
            "Anchor",
            "Bag of Preparation",
            "Abe's Treasure",
            "TungstenRod",
            "ChewingGum",
            "Letter Opener"
    );


    public boolean valid() {
        boolean
                valid =
                !runData.chose_seed && !runData.is_special_run && !runData.is_daily && !runData.is_endless && isA20;
        if (!valid) {
            return false;
        }

        for (Card c : this.masterDeck) {
            if (c.name.contains(":")) {
                return false;
            }
            if (bannedCards.contains(c.name.toLowerCase())) {
                return false;
            }
        }

        for (String r : this.relics) {
            if (r.contains(":")) {
                return false;
            }
            if (bannedRelics.contains(r.toLowerCase())) {
                return false;
            }
        }

        for (BossRelicChoiceStats b : bossRelicChoiceStats) {
            if (b == null) {
                continue;
            }
            if (b.picked != null && b.picked.contains(":")) {
                return false;
            }
            if (b.picked != null && bannedBossRelics.contains(b.picked)) {
                return false;
            }
            for (String s : b.not_picked) {
                if (s != null && s.contains(":")) {
                    return false;
                }
                if (s != null && bannedBossRelics.contains(s)) {
                    return false;
                }
            }
        }

        if (bossSwapRelic != null && bannedBossRelics.contains(bossSwapRelic.name)) {
            return false;
        }

        if (neowPicked == null) {
            return false;
        }

        if (bannedNeows.contains(neowPicked.bonus)) {
            return false;
        }

        for (Neow n : neowSkipped) {
            if (bannedNeows.contains(n.bonus)) {
                return false;
            }
        }

        return true;
    }

    private int parseGold() {
        for (String score : runData.score_breakdown) {
            if (score.contains("Money Money") || score.contains("I Like Gold")) {
                // Extract gold amount from the score breakdown line
                // Example: Money Money (1596)
                try {
                    return Integer.parseInt(score.split("\\(")[1].split("\\)")[0]);
                } catch (Exception e) {
                    StatsTracker.logger.warn("Failed to parse gold amount from score breakdown: " + score);
                    return 0;
                }
            }
        }
        return 0;
    }

    private int getMaxHP() {
        if (runData.max_hp_per_floor.isEmpty()) {
            return 0;
        }
        return runData.max_hp_per_floor.get(runData.max_hp_per_floor.size() - 1);
    }

    private int getPotionsCreated() {
        int p = 0;
        for (List<String> l : runData.potions_obtained_alchemize) {
            p += l.size();
        }
        return p;
    }

    private int getLessonsLearned() {
        int c = 0;
        for (List<String> l : runData.lesson_learned_per_floor) {
            c += l.size();
        }
        return c;
    }

    private int parseSearingBlow() {
        int m = 0;
        for (Card card : masterDeck) {
            if (card.name.equals("Searing Blow")) {
                try {
                    m = Math.max(m, card.upgrades);
                } catch (Exception e) {
                    StatsTracker.logger.warn("Failed to parse Searing Blow upgrade count from master deck: " + card);
                }
            }
        }
        return m;
    }

    private boolean isHeartKill() {
        if (!runData.victory) {
            return false;
        }
        for (int i = runData.damage_taken.size() - 1; i >= 0; --i) {
            BattleStats bs = runData.damage_taken.get(i);
            if (bs.enemies != null && bs.enemies.equals("The Heart")) {
                return true;
            }
        }
        return false;
    }


    private Pair<Integer, Integer> enemiesKilled() {
        int enemiesKilled = 0;
        int bossesKilled = 0;

        Pattern enemies = Pattern.compile("Enemies Slain \\((\\d+)\\):.*");
        Pattern bosses = Pattern.compile("Bosses Slain \\((\\d+)\\):.*");

        for (String s : runData.score_breakdown) {
            Matcher e = enemies.matcher(s);
            Matcher b = bosses.matcher(s);
            if (e.matches()) {
                enemiesKilled = Integer.parseInt(e.group(1));
            } else if (b.matches()) {
                bossesKilled = Integer.parseInt(b.group(1));
            }
        }

        return Pair.of(enemiesKilled, bossesKilled);
    }

    private BossRelic bossSwapRelic() {
        String nlothsGift = "Nloth's Gift";
        String nloth = "N'loth";
        String s = "";

        if (neowPicked != null && Objects.equals(neowPicked.bonus, "BOSS_RELIC")) {
            if (!runData.relics.isEmpty()) {
                s = runData.relics.get(0);
            }
            if (s.equals(nlothsGift)) {
                for (EventStats e : runData.event_choices) {
                    if (e.event_name.equals(nloth)) {
                        s = e.relics_lost.stream().findFirst().orElse("");
                    }
                }
            }
        }

        if (!s.isEmpty()) {
            return new BossRelic(s, 0);
        }
        return null;
    }

    public String toJSON() {
        return gson.toJson(this);
    }

    public static Optional<Run> fromFile(FileHandle file) {
        RunData data;
        AbstractPlayer.PlayerClass playerClass;

        try {
            data = gson.fromJson(file.readString(), RunData.class);
        } catch (JsonSyntaxException e) {
            StatsTracker.logger.info("Failed to load RunData from JSON file: " + file.path());
            return Optional.empty();
        }

        try {
            playerClass = AbstractPlayer.PlayerClass.valueOf(data.character_chosen);
        } catch (NullPointerException | IllegalArgumentException e) {
            StatsTracker.logger.info("Run file " +
                    file.path() +
                    " does not use a real character: " +
                    data.character_chosen);
            return Optional.empty();
        }

        if (data.timestamp == null) {
            data.timestamp = file.nameWithoutExtension();
            String exampleDaysSinceUnixStr = "17586";
            boolean assumeDaysSinceUnix = data.timestamp.length() == exampleDaysSinceUnixStr.length();
            if (assumeDaysSinceUnix) {
                try {
                    long secondsInDay = 86400L;
                    long days = Long.parseLong(data.timestamp);
                    data.timestamp = Long.toString(days * secondsInDay);
                } catch (NumberFormatException e) {
                    StatsTracker.logger.info("Run file " +
                            file.path() +
                            " name is could not be parsed into a Timestamp.");
                    return Optional.empty();
                }
            }
        }

        return Optional.of(new Run(data, playerClass));
    }

    public boolean isInRange(YearMonth startDate, YearMonth endDate) {
        YearMonth date = YearMonth.fromDate(new Date(Long.parseLong(runData.timestamp) * 1000));
        return date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0;
    }

    public int getAct(int floor) {
        if (floor <= 17) {
            return 1;
        }
        if (floor <= 34) {
            return 2;
        }

        if (portalFloor > 0) {
            if (floor <= portalFloor + 3) {
                return 3;
            } else {
                return 4;
            }
        }

        if (floor <= 52) {
            return 3;
        }
        return 4;
    }

    @Override
    public int compareTo(Run run) {
        return RunData.orderByTimestampDesc.compare(run.runData, this.runData);
    }
}
