import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ComputerAI {
    private final Random random;

    public ComputerAI(Random random) {
        this.random = random;
    }

    public int chooseOpeningBidThousands(Player player, List<Integer> lot, int roundIndex) {
        List<Integer> sortedLot = new ArrayList<>(lot);
        Collections.sort(sortedLot, Collections.reverseOrder());
        int topCard = sortedLot.get(0);

        int maxThousands = player.getMoney() / GameConfig.BID_UNIT;
        if (maxThousands < GameConfig.MIN_OPENING_BID_THOUSANDS) {
            return 0;
        }

        int desired = 1 + topCard / 8 + roundIndex / 2;
        if (random.nextDouble() < 0.4) {
            desired++;
        }
        desired = Math.max(GameConfig.MIN_OPENING_BID_THOUSANDS, desired);
        desired = Math.min(desired, maxThousands);

        int roundsLeft = GameConfig.ROUNDS - roundIndex;
        int budgetCap = Math.max(1, player.getMoney() / (GameConfig.BID_UNIT * Math.max(1, roundsLeft) * 2));
        return Math.min(desired, budgetCap);
    }

    public int chooseBidThousands(Player player, List<Integer> lot, int minBid, int step,
            int roundIndex) {
        int minThousands = minBid / GameConfig.BID_UNIT;
        int maxThousands = player.getMoney() / GameConfig.BID_UNIT;

        if (maxThousands < minThousands) {
            return 0;
        }

        List<Integer> sortedLot = new ArrayList<>(lot);
        Collections.sort(sortedLot, Collections.reverseOrder());
        int topCard = sortedLot.get(0);

        boolean lotIsStrong = topCard >= 14;
        boolean lotIsWeak = topCard <= 8;

        if (lotIsWeak && minThousands >= 6) {
            return 0;
        }

        if (lotIsStrong && player.getMoney() > minBid + step * 2) {
            if (random.nextDouble() < 0.55) {
                int raise = 1 + random.nextInt(2);
                return Math.min(minThousands + raise, maxThousands);
            }
        }

        if (random.nextDouble() < 0.25 && minThousands + 1 <= maxThousands) {
            return minThousands + 1;
        }

        return minThousands;
    }

    public int chooseCardToSell(Player player, List<Integer> prices) {
        List<Integer> sortedPrices = new ArrayList<>(prices);
        Collections.sort(sortedPrices, Collections.reverseOrder());

        int topPrice = sortedPrices.get(0);
        int lowPrice = sortedPrices.get(sortedPrices.size() - 1);
        int priceSpread = topPrice - lowPrice;

        List<Integer> hand = new ArrayList<>(player.getHand());
        Collections.sort(hand);

        if (priceSpread < 1500 || topPrice < 3500) {
            return hand.get(0);
        }
        if (topPrice > 7500) {
            return hand.get(hand.size() - 1);
        }

        int midIndex = hand.size() / 2;
        if (random.nextBoolean() && hand.size() > 1) {
            return hand.get(Math.max(0, midIndex - 1));
        }
        return hand.get(midIndex);
    }
}
