package StatsTracker.stats;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.screens.stats.CardChoiceStats;

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

    public final List<Rate<Card>> cardPicksAct1;
    public final List<Rate<Card>> cardPicksAfterAct1;
    public final List<Rate<Card>> cardWinRate;


    public ClassStat(List<Run> runs, boolean rotate) {
        this.rotate = rotate;
        if (rotate) {
            bestWinStreak = getHighestRotateWinStreak(runs);
        } else {
            bestWinStreak = getHighestWinStreak(runs);
        }

        Map<Card, Rate<Card>> cardPicksAct1 = new java.util.HashMap<>();
        Map<Card, Rate<Card>> cardPicksAfterAct1 = new java.util.HashMap<>();
        Map<Card, Rate<Card>> cardWinRate = new java.util.HashMap<>();

        for (Run run : runs) {
            for (CardChoiceStats c : run.runData.card_choices) {
                Map<Card, Rate<Card>> map;
                if (c.floor >= 17) {
                    map = cardPicksAfterAct1;
                } else {
                    map = cardPicksAct1;
                }

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

            run.runData.master_deck.stream().map(Card::fromStringIgnoreUpgrades).distinct().forEach(card -> {
                cardWinRate.putIfAbsent(card, new Rate<>(card));
                if (run.isHeartKill) {
                    cardWinRate.get(card).win++;
                } else {
                    cardWinRate.get(card).loss++;
                }
            });

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

        this.cardPicksAct1 = cardPicksAct1.values().stream().sorted().collect(Collectors.toList());
        this.cardPicksAfterAct1 = cardPicksAfterAct1.values().stream().sorted().collect(Collectors.toList());
        this.cardWinRate = cardWinRate.values().stream().sorted().collect(Collectors.toList());
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
