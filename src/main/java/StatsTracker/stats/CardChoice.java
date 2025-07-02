package StatsTracker.stats;

import java.util.List;

public class CardChoice {
    public Card picked;
    public List<Card> skipped;
    public int floor;

    public CardChoice(Card picked, List<Card> skipped, int floor) {
        this.picked = picked;
        this.skipped = skipped;
        this.floor = floor;
    }
}
