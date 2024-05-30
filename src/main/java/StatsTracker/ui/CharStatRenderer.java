package StatsTracker.ui;

import StatsTracker.StatsTracker;
import StatsTracker.Utils;
import StatsTracker.stats.ClassStat;
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
        if (cs.averageWinningTime.mean() != 0D) {
            this.info +=
                    cs.averageWinningTime.what +
                            ": " +
                            CharStat.formatHMSM((long) cs.averageWinningTime.mean()) +
                            " NL ";
        }

        this.info = this.info + CharStat.TEXT[23] + cs.highestScore + " NL ";
        if (cs.bestWinStreak > 0) {
            if (cs.rotate) {
                this.info = this.info + "Best Win Streak (rotate): #y" + cs.bestWinStreak + " NL ";

            } else {
                this.info = this.info + CharStat.TEXT[22] + cs.bestWinStreak + " NL ";
            }
        }

        this.info =
                this.info +
                        "Win Rate: #y" +
                        Utils.calculatePercent(cs.numVictory, cs.numDeath + cs.numVictory) +
                        "%" +
                        " NL ";
        this.info +=
                "Survival rate per act: NL #y" +
                        cs.survivalRatePerAct.get(1).percent() +
                        "% #y" +
                        cs.survivalRatePerAct.get(2).percent() +
                        "% #y" +
                        cs.survivalRatePerAct.get(3).percent() +
                        "% #y" +
                        cs.survivalRatePerAct.get(4).percent() + "% NL ";

        this.info2 = CharStat.TEXT[17] + cs.numVictory + " NL ";
        this.info2 = this.info2 + CharStat.TEXT[18] + cs.numDeath + " NL ";
        this.info2 = this.info2 + CharStat.TEXT[19] + cs.totalFloorsClimbed + " NL ";
        this.info2 = this.info2 + CharStat.TEXT[20] + cs.bossKilled + " NL ";
        this.info2 = this.info2 + CharStat.TEXT[21] + cs.enemyKilled + " NL ";
        this.info2 += "         survival rate: #y" + cs.nob.percent() + "% #y" + cs.nob.winLoss() + " NL ";
    }

    public void render(SpriteBatch sb, float screenX, float renderY) {
        sb.draw(StatsTracker.nobbers,
                screenX + 675.0F * Settings.scale,
                renderY + 531.0F * Settings.yScale,
                StatsTracker.nobbers.getWidth() * 0.5F * Settings.scale,
                StatsTracker.nobbers.getHeight() * 0.5F * Settings.scale
        );
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
