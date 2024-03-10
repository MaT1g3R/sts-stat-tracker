package StatsTracker;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.stats.CharStat;

public class CharStatRenderer {
    private String info;
    private String info2;


    public CharStatRenderer(ClassStat cs) {
        this.info = CharStat.TEXT[0] + CharStat.formatHMSM(cs.playTime) + " NL ";
        if (cs.fastestTime != 0L) {
            this.info = this.info + CharStat.TEXT[13] + CharStat.formatHMSM(cs.fastestTime) + " NL ";
        }

        this.info = this.info + CharStat.TEXT[23] + cs.highestScore + " NL ";
        if (cs.bestWinStreak > 0) {
            this.info = this.info + CharStat.TEXT[22] + cs.bestWinStreak + " NL ";
        }

        this.info2 = CharStat.TEXT[17] + cs.numVictory + " NL ";
        this.info2 = this.info2 + CharStat.TEXT[18] + cs.numDeath + " NL ";
        this.info2 = this.info2 + CharStat.TEXT[19] + cs.totalFloorsClimbed + " NL ";
        this.info2 = this.info2 + CharStat.TEXT[20] + cs.bossKilled + " NL ";
        this.info2 = this.info2 + CharStat.TEXT[21] + cs.enemyKilled + " NL ";
    }

    public void render(SpriteBatch sb, float screenX, float renderY) {
        FontHelper.renderSmartText(sb,
                FontHelper.panelNameFont,
                this.info,
                screenX + 75.0F * Settings.scale,
                renderY + 766.0F * Settings.yScale,
                9999.0F,
                38.0F * Settings.scale,
                Settings.CREAM_COLOR);
        if (this.info2 != null) {
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
}
