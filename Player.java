import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private final String name;
    private final boolean human;
    private int money;
    private final List<Integer> hand = new ArrayList<>();

    public Player(String name, boolean human, int startingMoney) {
        this.name = name;
        this.human = human;
        this.money = startingMoney;
    }

    public String getName() {
        return name;
    }

    public boolean isHuman() {
        return human;
    }

    public int getMoney() {
        return money;
    }

    public List<Integer> getHand() {
        return Collections.unmodifiableList(hand);
    }

    public void addMoney(int amount) {
        money += amount;
    }

    public void spend(int amount) {
        money -= amount;
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

    public int highestCard() {
        return hand.stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public int lowestCard() {
        return hand.stream().mapToInt(Integer::intValue).min().orElse(0);
    }
}
