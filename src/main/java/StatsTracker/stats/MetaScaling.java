package StatsTracker.stats;

import StatsTracker.StatsTracker;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;

import java.util.List;

public class MetaScaling {
    public int maxRelic = 0;
    public int maxGold = 0;
    public int maxRemoves = 0;
    public int maxDagger = 0;
    public int maxHP = 0;
    public int maxSearingBlow = 0;
    public int maxAlch = 0;
    public int maxAlgo = 0;
    public int maxLesson = 0;
    private String info = "";
    private String info2 = "";

    public MetaScaling() {
    }

    public void addRun(RunData run) {
        this.maxRelic = Math.max(this.maxRelic, run.relics.size());
        this.maxGold = Math.max(this.maxGold, parseGold(run));
        this.maxRemoves = Math.max(this.maxRemoves, run.items_purged.size());

        if (!run.max_hp_per_floor.isEmpty()) {
            this.maxHP = Math.max(this.maxHP, run.max_hp_per_floor.get(run.max_hp_per_floor.size() - 1));
        }
        parseSearingBlow(run);

        int potionsCreated = 0;
        for (List<String> l : run.potions_obtained_alchemize) {
            potionsCreated += l.size();
        }
        this.maxAlch = Math.max(this.maxAlch, potionsCreated);

        int lessonLearned = 0;
        for (List<String> l : run.lesson_learned_per_floor) {
            lessonLearned += l.size();
        }
        this.maxLesson = Math.max(this.maxLesson, lessonLearned);

        run.improvable_cards.forEach((n, cs) -> {
            String name = n.toLowerCase();
            if (name.contains("ritualdagger")) {
                cs.forEach(c -> {
                    this.maxDagger = Math.max(this.maxDagger, c);
                });
            } else if (name.contains("genetic algorithm")) {
                cs.forEach(c -> {
                    this.maxAlgo = Math.max(this.maxAlgo, c);
                });
            }
        });
    }

    private int parseGold(RunData run) {
        for (String score : run.score_breakdown) {
            if (score.contains("Money Money")) {
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

    private void parseSearingBlow(RunData run) {
        for (String card : run.master_deck) {
            // Extract Searing Blow upgrade count from the master deck
            // Example: Searing Blow+8
            if (card.contains("Searing Blow")) {
                try {
                    this.maxSearingBlow = Math.max(this.maxSearingBlow, Integer.parseInt(card.split("\\+")[1]));
                } catch (Exception e) {
                    StatsTracker.logger.warn("Failed to parse Searing Blow upgrade count from master deck: " + card);
                }
            }
        }
    }

    public void render(SpriteBatch sb, float screenX, float renderY) {
        this.info = "Max Relics: #y" + this.maxRelic + " NL ";
        this.info += "Max Gold: #y" + this.maxGold + " NL ";
        this.info += "Max Removes: #y" + this.maxRemoves + " NL ";
        this.info += "Max Ritual Dagger: #y" + this.maxDagger + " NL ";
        this.info += "Max HP: #y" + this.maxHP;

        this.info2 = "Max Searing Blow: #y" + this.maxSearingBlow + " NL ";
        this.info2 += "Max Potions Created: #y" + this.maxAlch + " NL ";
        this.info2 += "Max Genetic Algorithm: #y" + this.maxAlgo + " NL ";
        this.info2 += "Max Lessons Learned: #y" + this.maxLesson;

        FontHelper.renderSmartText(sb,
                FontHelper.panelNameFont,
                this.info,
                screenX + 75.0F * Settings.scale,
                renderY + 766.0F * Settings.yScale,
                9999.0F,
                38.0F * Settings.scale,
                Settings.CREAM_COLOR);
        FontHelper.renderSmartText(sb,
                FontHelper.panelNameFont,
                this.info2,
                screenX + 675.0F * Settings.scale,
                renderY + 766.0F * Settings.yScale,
                9999.0F,
                38.0F * Settings.scale,
                Settings.CREAM_COLOR);
    }
}
