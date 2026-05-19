import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private final String name;
    private final boolean human;
    private int biddingCash;
    private int saleEarnings;
    private final List<Integer> hand = new ArrayList<>();

    public Player(String name, boolean human, int startingMoney) {
        this.name = name;
        this.human = human;
        this.biddingCash = startingMoney;
        this.saleEarnings = 0;
    }

    public String getName() {
        return name;
    }

    public boolean isHuman() {
        return human;
    }

    public int getBiddingCash() {
        return biddingCash;
    }

    public int getSaleEarnings() {
        return saleEarnings;
    }

    public int getFinalTotal() {
        return biddingCash + saleEarnings;
    }

    public List<Integer> getHand() {
        return Collections.unmodifiableList(hand);
    }

    public void spendFromBidding(int amount) {
        biddingCash -= amount;
    }

    public void addSaleEarnings(int amount) {
        saleEarnings += amount;
    }

    public void addCard(int card) {
        hand.add(card);
    }

    public boolean removeCard(int card) {
        return hand.remove(Integer.valueOf(card));
    }

    public boolean hasCard(int card) {
        return hand.contains(card);
    }

    public int handSize() {
        return hand.size();
    }
}
