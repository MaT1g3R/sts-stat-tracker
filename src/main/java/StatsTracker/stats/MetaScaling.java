package StatsTracker.stats;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;

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

    public void addRun(Run run) {
        this.maxRelic = Math.max(this.maxRelic, run.relics.size());
        this.maxGold = Math.max(this.maxGold, run.gold);
        this.maxRemoves = Math.max(this.maxRemoves, run.itemsPurged.size());

        this.maxHP = Math.max(this.maxHP, run.maxHP);
        this.maxSearingBlow = Math.max(this.maxSearingBlow, run.maxSearingBlow);
        this.maxAlch = Math.max(this.maxAlch, run.potionsCreated);

        this.maxLesson = Math.max(this.maxLesson, run.lessonsLearned);
        this.maxDagger = Math.max(this.maxDagger, run.maxDagger);
        this.maxAlgo = Math.max(this.maxAlgo, run.maxAlgo);
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
