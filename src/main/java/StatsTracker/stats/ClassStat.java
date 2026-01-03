package StatsTracker.stats;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.screens.stats.BossRelicChoiceStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassStat {
    public final boolean rotate;
    public long playTime = 0;
    public long fastestTime = Integer.MAX_VALUE;
    public int numVictory = 0;
    public int numDeath = 0;
    public int totalFloorsClimbed = 0;
    public int bossKilled = 0;
    public int enemyKilled = 0;
    public int bestWinStreak = 0;
    public int highestScore = 0;
    public Mean averageWinningTime;
    public Mean averageDeckSize;
    public int minDeckSize = 0;
    public int maxDeckSize = 0;
    public List<Rate<Card>> cardPicksAct1;
    public List<Rate<Card>> cardPicksAfterAct1;
    public List<Rate<Card>> cardWinRate;
    public List<Rate<Neow>> neowWinRate;
    public List<Rate<Neow>> neowPickRate;
    public List<Rate<BossRelic>> bossRelicPickRateAct1;
    public List<Rate<BossRelic>> bossRelicPickRateAct2;
    public List<Rate<BossRelic>> bossRelicWinRateAct1;
    public List<Rate<BossRelic>> bossRelicWinRateAct2;
    public List<Rate<BossRelic>> bossRelicWinRateSwap;
    public Map<Integer, List<Mean>> averageDamageTaken = new HashMap<>();
    public Map<Integer, List<Rate<String>>> encounterDeadRate = new HashMap<>();
    public Rate<String> nob = new Rate<>("nob survival rate");
    public Map<Integer, List<Mean>> averagePotionUse = new HashMap<>();
    public Map<Integer, Rate<Integer>> survivalRatePerAct = new HashMap<>();
    public List<Rate<String>> relicWinRate;
    public List<Rate<String>> relicPurchasedWinRate;

    public List<Rate<String>> eventWinRateAct1;
    public List<Rate<String>> eventWinRateAct2;
    public List<Rate<String>> eventWinRateAct3;
    public MetaScaling metaScaling = new MetaScaling();

    private static class StatCollector {
        Mean averageWinningTime = new Mean("Average Time (wins)");
        Mean averageDeckSize = new Mean("Average Deck Size");
        Map<Card, Rate<Card>> cardPicksAct1 = new HashMap<>();
        Map<Card, Rate<Card>> cardPicksAfterAct1 = new HashMap<>();
        Map<Card, Rate<Card>> cardWinRate = new HashMap<>();
        Map<Neow, Rate<Neow>> neowWinRate = new HashMap<>();
        Map<Neow, Rate<Neow>> neowPickRate = new HashMap<>();
        Map<BossRelic, Rate<BossRelic>> bossRelicPickRateAct1 = new HashMap<>();
        Map<BossRelic, Rate<BossRelic>> bossRelicPickRateAct2 = new HashMap<>();
        Map<BossRelic, Rate<BossRelic>> bossRelicWinRateAct1 = new HashMap<>();
        Map<BossRelic, Rate<BossRelic>> bossRelicWinRateAct2 = new HashMap<>();
        Map<BossRelic, Rate<BossRelic>> bossRelicWinRateSwap = new HashMap<>();
        Map<Integer, Map<String, Mean>> encounterHPLossMap = new HashMap<Integer, Map<String, Mean>>() {{
            put(1, new HashMap<>());
            put(2, new HashMap<>());
            put(3, new HashMap<>());
            put(4, new HashMap<>());
        }};
        Map<Integer, List<Mean>> avgEncounterDamage = new HashMap<>();

        Map<Integer, Map<String, Rate<String>>> encounterDeadRate = new HashMap<Integer, Map<String, Rate<String>>>() {{
            put(1, new HashMap<>());
            put(2, new HashMap<>());
            put(3, new HashMap<>());
            put(4, new HashMap<>());
        }};

        Map<Integer, Map<String, Mean>> encounterPotionUseMap = new HashMap<Integer, Map<String, Mean>>() {{
            put(1, new HashMap<>());
            put(2, new HashMap<>());
            put(3, new HashMap<>());
            put(4, new HashMap<>());
        }};

        Map<Integer, List<Mean>> averagePotionUse = new HashMap<>();
        Rate<String> nob = new Rate<>("nob survival rate");
        Map<Integer, Rate<Integer>> survivalRatePerAct = new HashMap<Integer, Rate<Integer>>() {{
            put(1, new Rate<>(1));
            put(2, new Rate<>(2));
            put(3, new Rate<>(3));
            put(4, new Rate<>(4));
        }};

        Map<String, Rate<String>> relicPurchasedWinRate = new HashMap<>();
        Map<String, Rate<String>> relicWinRate = new HashMap<>();
        Map<Integer, Map<String, Rate<String>>> eventWinRate = new HashMap<Integer, Map<String, Rate<String>>>() {{
            put(1, new HashMap<>());
            put(2, new HashMap<>());
            put(3, new HashMap<>());
        }};
        private void deckSizeStats(Run run) {
            averageDeckSize.add(run.masterDeck.size());
        }

        private void cardPickStats(Run run) {
            for (CardChoice c : run.cardChoices) {
                Map<Card, Rate<Card>> map = c.floor >= 17 ? cardPicksAfterAct1 : cardPicksAct1;
                map.putIfAbsent(c.picked, new Rate<>(c.picked));
                map.get(c.picked).win++;
                c.skipped.forEach(c1 -> {
                    map.putIfAbsent(c1, new Rate<>(c1));
                    map.get(c1).loss++;
                });
            }
        }

        private void deckStats(Run run) {
            run.masterDeck.stream()
                    .map(c -> Card.fromStringIgnoreUpgrades(c.name))
                    .distinct()
                    .forEach(card -> {
                        cardWinRate.putIfAbsent(card, new Rate<>(card));
                        if (run.isHeartKill) {
                            cardWinRate.get(card).win++;
                        } else {
                            cardWinRate.get(card).loss++;
                        }
                    });
        }

        private void neowStats(Run run) {
            if (run.neowPicked != null) {
                neowWinRate.putIfAbsent(run.neowPicked, new Rate<>(run.neowPicked));
                if (run.isHeartKill) {
                    neowWinRate.get(run.neowPicked).win++;
                } else {
                    neowWinRate.get(run.neowPicked).loss++;
                }
            }

            if (run.neowPicked != null && !run.neowSkipped.isEmpty()) {
                neowPickRate.putIfAbsent(run.neowPicked, new Rate<>(run.neowPicked));
                neowPickRate.get(run.neowPicked).win++;
                run.neowSkipped.forEach(neow -> {
                    neowPickRate.putIfAbsent(neow, new Rate<>(neow));
                    neowPickRate.get(neow).loss++;
                });
            }
        }

        private void bossRelicStats(Run run) {
            if (run.bossSwapRelic != null) {
                bossRelicWinRateSwap.putIfAbsent(run.bossSwapRelic, new Rate<>(run.bossSwapRelic));
                if (run.isHeartKill) {
                    bossRelicWinRateSwap.get(run.bossSwapRelic).win++;
                } else {
                    bossRelicWinRateSwap.get(run.bossSwapRelic).loss++;
                }
            }

            for (int i = 0; i < run.bossRelicChoiceStats.size(); ++i) {
                BossRelicChoiceStats bs = run.bossRelicChoiceStats.get(i);
                int act = i == 0 ? 1 : 2;
                List<String> skipped = new ArrayList<>(bs.not_picked);
                Map<BossRelic, Rate<BossRelic>> pickRateMap = act == 1 ? bossRelicPickRateAct1 : bossRelicPickRateAct2;
                Map<BossRelic, Rate<BossRelic>> winRateMap = act == 1 ? bossRelicWinRateAct1 : bossRelicWinRateAct2;

                String picked = bs.picked;
                if (picked == null || picked.isEmpty() || picked.equals("SKIP")) {
                    picked = "SKIP";
                } else {
                    skipped.add("SKIP");
                }

                BossRelic p = new BossRelic(picked, act);
                pickRateMap.putIfAbsent(p, new Rate<>(p));
                pickRateMap.get(p).win++;

                winRateMap.putIfAbsent(p, new Rate<>(p));
                if (run.isHeartKill) {
                    winRateMap.get(p).win++;
                } else {
                    winRateMap.get(p).loss++;
                }

                for (String s : skipped) {
                    BossRelic p1 = new BossRelic(s, act);
                    pickRateMap.putIfAbsent(p1, new Rate<>(p1));
                    pickRateMap.get(p1).loss++;
                }
            }
        }


        private void encounterStats(Run run) {
            for (EncounterStats es : run.encounterStats) {
                if (es.enemies.equals("Gremlin Nob")) {
                    nob.win++;
                }
                int act = run.getAct(es.floor);
                int potsUsed = es.potionsUsed;
                if (potsUsed >= 0) {
                    encounterPotionUseMap.get(act).putIfAbsent(es.enemies, new Mean(es.enemies));
                    encounterPotionUseMap.get(act).get(es.enemies).add(potsUsed);
                }

                Map<String, Rate<String>> ded = encounterDeadRate.get(act);
                ded.putIfAbsent(es.enemies, new Rate<>(es.enemies));
                ded.get(es.enemies).loss++;

                Map<String, Mean> m = encounterHPLossMap.get(act);
                m.putIfAbsent(es.enemies, new Mean(es.enemies));
                double damage = es.damage;
                if (damage >= 99999) {
                    damage -= 99999;
                }
                m.get(es.enemies).add(damage);
            }

            if (run.killedBy != null && !run.killedBy.isEmpty()) {
                String killedBy = run.killedBy;
                if (killedBy.equals("Gremlin Nob")) {
                    nob.win--;
                    nob.loss++;
                }
                int act = run.getAct(run.floorsReached);
                Rate<String> k = encounterDeadRate.getOrDefault(act, new HashMap<>()).get(killedBy);
                if (k != null) {
                    k.win++;
                    k.loss--;
                }
            }
        }

        private void finaliseEncounterStats() {
            encounterHPLossMap.forEach((act, xs) -> {
                avgEncounterDamage.put(act, sortedMapValues(xs));
            });

            encounterPotionUseMap.forEach((act, xs) -> {
                averagePotionUse.put(act, sortedMapValues(xs));
            });
        }

        private void survivalRatePerAct(Run run) {
            Rate<Integer> act1 = survivalRatePerAct.get(1);
            Rate<Integer> act2 = survivalRatePerAct.get(2);
            Rate<Integer> act3 = survivalRatePerAct.get(3);
            Rate<Integer> act4 = survivalRatePerAct.get(4);

            if (run.isHeartKill) {
                act1.win++;
                act2.win++;
                act3.win++;
                act4.win++;
                return;
            }

            int floor = run.floorsReached;
            int act = run.getAct(floor);

            switch (act) {
                case 1:
                    act1.loss++;
                    break;
                case 2:
                    act1.win++;
                    act2.loss++;
                    break;
                case 3:
                    act1.win++;
                    act2.win++;
                    act3.loss++;
                    break;
                case 4:
                    act1.win++;
                    act2.win++;
                    act3.win++;
                    act4.loss++;
                    break;
            }
        }

        private void relicStats(Run run) {
            run.relicsPurchased.forEach(r -> {
                relicPurchasedWinRate.putIfAbsent(r, new Rate<>(r));
                Rate<String> rate = relicPurchasedWinRate.get(r);
                if (run.isHeartKill) {
                    rate.win++;
                } else {
                    rate.loss++;
                }
            });
            run.relics.forEach(r -> {
                relicWinRate.putIfAbsent(r, new Rate<>(r));
                Rate<String> rate = relicWinRate.get(r);
                if (run.isHeartKill) {
                    rate.win++;
                } else {
                    rate.loss++;
                }
            });
        }

        private void runTimeStats(Run run) {
            if (run.isHeartKill) {
                averageWinningTime.add(run.playtime);
            }
        }

        private void eventStats(Run run) {
            run.eventStats.forEach(e -> {
                int act = run.getAct(e.floor);
                String event = e.event_name;
                Map<String, Rate<String>> m = eventWinRate.get(act);
                if (m == null) {
                    return;
                }
                m.putIfAbsent(event, new Rate<>(event));
                if (run.isHeartKill) {
                    m.get(event).win++;
                } else {
                    m.get(event).loss++;
                }
            deckSizeStats(run);
            });
        }

        void collect(Run run) {
            cardPickStats(run);
            deckStats(run);
            neowStats(run);
            bossRelicStats(run);
            encounterStats(run);
            survivalRatePerAct(run);
            relicStats(run);
            runTimeStats(run);
            eventStats(run);
        }

        private <A> List<A> sortedMapValues(Map<?, A> map) {
            return map.values().stream().sorted().collect(Collectors.toList());
        }

        void finalise(ClassStat cs) {
            cs.cardPicksAct1 = sortedMapValues(cardPicksAct1);
            cs.cardPicksAfterAct1 = sortedMapValues(cardPicksAfterAct1);
            cs.cardWinRate = sortedMapValues(cardWinRate);
            cs.neowPickRate = sortedMapValues(neowPickRate);
            cs.neowWinRate = sortedMapValues(neowWinRate);

            cs.bossRelicPickRateAct1 = sortedMapValues(bossRelicPickRateAct1);
            cs.bossRelicPickRateAct2 = sortedMapValues(bossRelicPickRateAct2);

            cs.bossRelicWinRateAct1 = sortedMapValues(bossRelicWinRateAct1);
            cs.bossRelicWinRateAct2 = sortedMapValues(bossRelicWinRateAct2);
            cs.bossRelicWinRateSwap = sortedMapValues(bossRelicWinRateSwap);

            finaliseEncounterStats();
            cs.averageDamageTaken = avgEncounterDamage;
            cs.averagePotionUse = averagePotionUse;

            cs.nob = nob;
            encounterDeadRate.forEach((k, v) -> {
                cs.encounterDeadRate.put(k, sortedMapValues(v));
            });

            cs.survivalRatePerAct = survivalRatePerAct;
            cs.relicWinRate = sortedMapValues(relicWinRate);
            cs.relicPurchasedWinRate = sortedMapValues(relicPurchasedWinRate);

            cs.averageDeckSize = averageDeckSize;
            cs.averageWinningTime = averageWinningTime;

            cs.eventWinRateAct1 = sortedMapValues(eventWinRate.get(1));
            cs.eventWinRateAct2 = sortedMapValues(eventWinRate.get(2));
            cs.eventWinRateAct3 = sortedMapValues(eventWinRate.get(3));
        }
    }

    public ClassStat(List<Run> runs, boolean rotate) {
        this.rotate = rotate;
        if (rotate) {
            WinStreak streak = WinStreak.getWinStreak(runs, null);
            if (streak != null) {
                bestWinStreak = streak.streak;
            }
        } else if (!runs.isEmpty()) {
            AbstractPlayer.PlayerClass playerClass = runs.get(0).playerClass;
            WinStreak streak = WinStreak.getWinStreak(runs, playerClass);
            if (streak != null) {
                bestWinStreak = streak.streak;
            }
        } else {
            bestWinStreak = 0;
        }
if (!runs.isEmpty()) {
            minDeckSize = Integer.MAX_VALUE;
        }

        
        StatCollector collector = new StatCollector();

        for (Run run : runs) {
            collector.collect(run);
            this.metaScaling.addRun(run);

            playTime += run.playtime;
            if (run.isHeartKill) {
                fastestTime = Math.min(fastestTime, run.playtime);
            }
            if (run.isHeartKill) {
                ++numVictory;
            } else {
                ++numDeath;
            }
            int deckSize = run.masterDeck.size();
            minDeckSize = Math.min(minDeckSize, deckSize);
            maxDeckSize = Math.max(maxDeckSize, deckSize);
            totalFloorsClimbed += run.floorsReached;
            bossKilled += run.bossesKilled;
            enemyKilled += run.enemiesKilled;
            highestScore = Math.max(highestScore, run.score);
        }

        collector.finalise(this);
    }
}
