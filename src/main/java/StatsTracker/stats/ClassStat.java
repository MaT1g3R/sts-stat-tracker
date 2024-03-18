package StatsTracker.stats;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.screens.stats.BattleStats;
import com.megacrit.cardcrawl.screens.stats.BossRelicChoiceStats;
import com.megacrit.cardcrawl.screens.stats.CardChoiceStats;
import com.megacrit.cardcrawl.screens.stats.EventStats;

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
    public final int bestWinStreak;
    public int highestScore = 0;

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

    private static class StatCollector {
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
        Map<Integer, Map<String, Mean>>
                encounterHPLossMap =
                new HashMap<Integer, Map<String, Mean>>() {{
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

        private void cardPickStats(Run run) {
            for (CardChoiceStats c : run.runData.card_choices) {
                Map<Card, Rate<Card>> map = c.floor >= 17 ? cardPicksAfterAct1 : cardPicksAct1;
                Card picked = Card.fromStringIgnoreUpgrades(c.picked);
                map.putIfAbsent(picked, new Rate<>(picked));
                map.get(picked).win++;
                List<Card>
                        notPicked =
                        c.not_picked.stream().map(Card::fromStringIgnoreUpgrades).collect(Collectors.toList());

                if (!picked.name.equals("SKIP") && !picked.name.equals("Singing Bowl")) {
                    if (-1 < run.singingBowlFloor && run.singingBowlFloor <= c.floor) {
                        notPicked.add(Card.SingingBowl());
                    } else {
                        notPicked.add(Card.SKIP());
                    }
                }

                notPicked.forEach(c1 -> {
                    map.putIfAbsent(c1, new Rate<>(c1));
                    map.get(c1).loss++;
                });
            }
        }

        private void deckStats(Run run) {
            run.runData.master_deck.stream().map(Card::fromStringIgnoreUpgrades).distinct().forEach(card -> {
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
            String nlothsGift = "Nloth's Gift";
            String nloth = "N'loth";

            if (run.runData.neow_bonus != null && run.runData.neow_bonus.equals("BOSS_RELIC")) {
                String s = "";
                if (!run.runData.relics.isEmpty()) {
                    s = run.runData.relics.get(0);
                }
                if (s.equals(nlothsGift)) {
                    for (EventStats e : run.runData.event_choices) {
                        if (e.event_name.equals(nloth)) {
                            s = e.relics_lost.stream().findFirst().orElse("");
                        }
                    }
                }
                if (!s.isEmpty()) {
                    BossRelic b = new BossRelic(s, 0);
                    bossRelicWinRateSwap.putIfAbsent(b, new Rate<>(b));
                    if (run.isHeartKill) {
                        bossRelicWinRateSwap.get(b).win++;
                    } else {
                        bossRelicWinRateSwap.get(b).loss++;
                    }
                }
            }

            for (int i = 0; i < run.runData.boss_relics.size(); ++i) {
                BossRelicChoiceStats bs = run.runData.boss_relics.get(i);
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
            Map<Integer, Integer> potByFloor = new HashMap<>();
            for (int i = 0; i < run.runData.potion_use_per_floor.size(); ++i) {
                int floor = i + 1;
                int potionsUsed = Math.min(run.runData.potion_use_per_floor.get(i).size(), 9);
                potByFloor.put(floor, potionsUsed);
            }

            for (BattleStats bs : run.runData.damage_taken) {
                if (bs.enemies.equals("Gremlin Nob")) {
                    nob.win++;
                }
                int act = run.getAct(bs.floor);

                if (!run.runData.potion_use_per_floor.isEmpty()) {
                    int potsUsed = potByFloor.getOrDefault(bs.floor, -1);
                    if (potsUsed >= 0) {
                        encounterPotionUseMap.get(act).putIfAbsent(bs.enemies, new Mean(bs.enemies));
                        encounterPotionUseMap.get(act).get(bs.enemies).add(potsUsed);
                    }
                }

                Map<String, Rate<String>> ded = encounterDeadRate.get(act);
                ded.putIfAbsent(bs.enemies, new Rate<>(bs.enemies));
                ded.get(bs.enemies).loss++;

                Map<String, Mean> m = encounterHPLossMap.get(act);
                m.putIfAbsent(bs.enemies, new Mean(bs.enemies));
                double damage = bs.damage;
                if (damage >= 99999) {
                    damage -= 99999;
                }
                m.get(bs.enemies).add(damage);
            }

            if (run.runData.killed_by != null && !run.runData.killed_by.isEmpty()) {
                String killedBy = run.runData.killed_by;
                if (killedBy.equals("Gremlin Nob")) {
                    nob.win--;
                    nob.loss++;
                }
                int act = run.getAct(run.runData.floor_reached);
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

            int floor = run.runData.floor_reached;
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
            run.runData.relics.forEach(r -> {
                relicWinRate.putIfAbsent(r, new Rate<>(r));
                Rate<String> rate = relicWinRate.get(r);
                if (run.isHeartKill) {
                    rate.win++;
                } else {
                    rate.loss++;
                }
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
        }
    }

    public ClassStat(List<Run> runs, boolean rotate) {
        this.rotate = rotate;
        if (rotate) {
            bestWinStreak = getHighestRotateWinStreak(runs);
        } else {
            bestWinStreak = getHighestWinStreak(runs);
        }

        StatCollector collector = new StatCollector();

        for (Run run : runs) {
            collector.collect(run);

            playTime += run.runData.playtime;
            if (run.isHeartKill) {
                fastestTime = Math.min(fastestTime, run.runData.playtime);
            }
            if (run.isHeartKill) {
                ++numVictory;
            } else {
                ++numDeath;
            }
            totalFloorsClimbed += run.runData.floor_reached;
            bossKilled += run.bossesKilled;
            enemyKilled += run.enemiesKilled;
            highestScore = Math.max(highestScore, run.runData.score);
        }

        collector.finalise(this);
    }

    private static int getHighestWinStreak(List<Run> runs) {
        int max = 0;
        int current = 0;
        for (Run run : runs) {
            if (run.isHeartKill) {
                ++current;
            } else {
                max = Math.max(max, current);
                current = 0;
            }
        }
        max = Math.max(max, current);
        return max;
    }


    private static int getHighestRotateWinStreak(List<Run> runs) {
        int max = 0;
        int current = 0;
        AbstractPlayer.PlayerClass currentClass;
        AbstractPlayer.PlayerClass previousClass = null;

        for (Run run : runs) {
            currentClass = run.playerClass;
            if (run.isHeartKill) {
                if (previousClass == null || classFollows(currentClass, previousClass)) {
                    ++current;
                } else {
                    max = Math.max(max, current);
                    current = 1;
                }
            } else {
                max = Math.max(max, current);
                current = 0;
            }
            previousClass = currentClass;
        }

        max = Math.max(max, current);
        return max;
    }

    private static boolean classFollows(AbstractPlayer.PlayerClass currentClass,
                                        AbstractPlayer.PlayerClass previousClass) {
        switch (currentClass) {
            case IRONCLAD:
                return previousClass == AbstractPlayer.PlayerClass.WATCHER;
            case THE_SILENT:
                return previousClass == AbstractPlayer.PlayerClass.IRONCLAD;
            case DEFECT:
                return previousClass == AbstractPlayer.PlayerClass.THE_SILENT;
            case WATCHER:
                return previousClass == AbstractPlayer.PlayerClass.DEFECT;
            default:
                return false;
        }
    }
}
