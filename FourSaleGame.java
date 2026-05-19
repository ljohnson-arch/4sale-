import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class FourSaleGame {
    private final Scanner scanner;
    private final Random random;
    private final ComputerAI ai;
    private final List<Player> players = new ArrayList<>();

    public FourSaleGame(Scanner scanner) {
        this.scanner = scanner;
        this.random = new Random();
        this.ai = new ComputerAI(random);
    }

    public void run() {
        printWelcome();
        setupPlayers();
        List<Integer> deck = buildShuffledDeck();

        runBiddingPhase(deck);
        pauseBetweenPhases();
        runSellingPhase();
        announceWinner();
    }

    private void printWelcome() {
        System.out.println();
        System.out.println("=== 4SALE ===");
        System.out.println("Buy low in Round 1, sell high in Round 2.");
        System.out.println("Cards are ranked 1 (lowest) through 20 (highest).");
        System.out.println("Each player starts with $" + formatMoney(GameConfig.STARTING_MONEY) + ".");
        System.out.println();
    }

    private void setupPlayers() {
        players.add(new Player("You", true, GameConfig.STARTING_MONEY));
        players.add(new Player("Alex", false, GameConfig.STARTING_MONEY));
        players.add(new Player("Blake", false, GameConfig.STARTING_MONEY));
        players.add(new Player("Casey", false, GameConfig.STARTING_MONEY));
    }

    private List<Integer> buildShuffledDeck() {
        List<Integer> deck = new ArrayList<>();
        for (int i = 1; i <= GameConfig.NUM_CARDS; i++) {
            deck.add(i);
        }
        Collections.shuffle(deck, random);
        return deck;
    }

    private void runBiddingPhase(List<Integer> deck) {
        System.out.println("--- ROUND 1: BIDDING ---");
        System.out.println("Four cards are auctioned at a time.");
        System.out.println("Bids are in $1,000 steps. The opening bid sets the step.");
        System.out.println("Each later bid must be at least the previous bid plus that step.");
        System.out.println("Highest bid wins the highest card, and so on.");
        System.out.println();

        for (int round = 0; round < GameConfig.ROUNDS; round++) {
            List<Integer> lot = new ArrayList<>();
            for (int i = 0; i < GameConfig.CARDS_PER_ROUND; i++) {
                lot.add(deck.remove(0));
            }
            lot.sort(Collections.reverseOrder());

            System.out.println("Auction " + (round + 1) + " of " + GameConfig.ROUNDS);
            System.out.println("Cards up for bid: " + formatCards(lot));

            int startingBidder = random.nextInt(GameConfig.NUM_PLAYERS);
            int[] bids = collectBids(lot, round, startingBidder);
            resolveBidding(lot, bids);
            printPlayerStatus();
            System.out.println();
        }
    }

    private int[] collectBids(List<Integer> lot, int roundIndex, int startingBidder) {
        int[] bids = new int[GameConfig.NUM_PLAYERS];
        int step = 0;
        int lastBid = 0;
        boolean stepSet = false;

        for (int offset = 0; offset < GameConfig.NUM_PLAYERS; offset++) {
            int playerIndex = (startingBidder + offset) % GameConfig.NUM_PLAYERS;
            Player player = players.get(playerIndex);
            boolean opening = offset == 0;

            int bid;
            if (player.isHuman()) {
                bid = promptHumanBid(player, lot, opening, lastBid, step);
            } else if (opening) {
                int thousands = ai.chooseOpeningBidThousands(player, lot, roundIndex);
                bid = thousands * GameConfig.BID_UNIT;
                if (bid > 0) {
                    System.out.println(player.getName() + " opens at $" + formatMoney(bid)
                            + " (step $" + formatMoney(bid) + ").");
                } else {
                    System.out.println(player.getName() + " passes.");
                }
            } else {
                int minBid = lastBid + step;
                int thousands = ai.chooseBidThousands(player, lot, minBid, step, roundIndex);
                bid = thousands * GameConfig.BID_UNIT;
                if (bid > 0) {
                    System.out.println(player.getName() + " bids $" + formatMoney(bid) + ".");
                } else {
                    System.out.println(player.getName() + " passes.");
                }
            }

            bids[playerIndex] = bid;

            if (bid > 0) {
                if (!stepSet) {
                    step = bid;
                    stepSet = true;
                }
                lastBid = bid;
            }
        }

        if (stepSet) {
            System.out.println("Bid step this auction: $" + formatMoney(step) + ".");
        }
        return bids;
    }

    private int promptHumanBid(Player player, List<Integer> lot, boolean opening,
            int lastBid, int step) {
        while (true) {
            System.out.println("Your cash: $" + formatMoney(player.getMoney()));
            if (opening) {
                System.out.print("Opening bid for lot " + formatCards(lot)
                        + " — enter thousands (e.g. 2 = $2,000), or 0 to pass: ");
            } else {
                int minBid = lastBid + step;
                System.out.println("Current high bid: $" + formatMoney(lastBid)
                        + ". Step: $" + formatMoney(step) + ".");
                System.out.print("Minimum bid: $" + formatMoney(minBid)
                        + " — enter thousands (e.g. " + (minBid / GameConfig.BID_UNIT)
                        + " = $" + formatMoney(minBid) + "), or 0 to pass: ");
            }

            String line = scanner.nextLine().trim().replace(",", "").replace("$", "");
            if (line.equalsIgnoreCase("pass") || line.equals("0")) {
                if (opening) {
                    return 0;
                }
                int minBid = lastBid + step;
                if (player.getMoney() >= minBid) {
                    System.out.println("You can afford the minimum. Enter "
                            + (minBid / GameConfig.BID_UNIT) + " or higher, or pass with 0.");
                    continue;
                }
                return 0;
            }

            try {
                int thousands = Integer.parseInt(line);
                if (line.length() >= 4 && thousands % GameConfig.BID_UNIT == 0) {
                    thousands = thousands / GameConfig.BID_UNIT;
                }

                if (thousands < 0) {
                    System.out.println("Bid cannot be negative.");
                    continue;
                }
                if (thousands > 0 && thousands < GameConfig.MIN_OPENING_BID_THOUSANDS && opening) {
                    System.out.println("Opening bid must be at least "
                            + GameConfig.MIN_OPENING_BID_THOUSANDS + " thousand.");
                    continue;
                }

                int bid = thousands * GameConfig.BID_UNIT;
                if (bid > player.getMoney()) {
                    System.out.println("You only have $" + formatMoney(player.getMoney()) + ".");
                    continue;
                }

                if (opening) {
                    if (bid > 0) {
                        return bid;
                    }
                    continue;
                }

                int minBid = lastBid + step;
                if (bid < minBid) {
                    System.out.println("Bid must be at least $" + formatMoney(minBid) + ".");
                    continue;
                }
                if (bid == lastBid) {
                    System.out.println("Bid must be higher than $" + formatMoney(lastBid) + ".");
                    continue;
                }
                if (bid % GameConfig.BID_UNIT != 0) {
                    System.out.println("Bids must be in $" + formatMoney(GameConfig.BID_UNIT)
                            + " increments.");
                    continue;
                }
                return bid;
            } catch (NumberFormatException e) {
                System.out.println("Enter a number of thousands (e.g. 2) or 0 to pass.");
            }
        }
    }

    private void resolveBidding(List<Integer> lot, int[] bids) {
        List<Integer> sortedCards = new ArrayList<>(lot);
        sortedCards.sort(Collections.reverseOrder());

        List<BidEntry> ranking = new ArrayList<>();
        for (int i = 0; i < GameConfig.NUM_PLAYERS; i++) {
            ranking.add(new BidEntry(i, bids[i]));
        }
        ranking.sort(Comparator.comparingInt(BidEntry::bid).reversed()
                .thenComparingInt(BidEntry::playerIndex));

        System.out.println();
        System.out.println("Results:");
        for (int i = 0; i < GameConfig.NUM_PLAYERS; i++) {
            BidEntry entry = ranking.get(i);
            Player player = players.get(entry.playerIndex());
            int card = sortedCards.get(i);
            int price = entry.bid();

            if (price > 0) {
                player.spend(price);
            }
            player.addCard(card);
            if (price > 0) {
                System.out.println("  " + player.getName() + " pays $" + formatMoney(price)
                        + " and receives card " + card + ".");
            } else {
                System.out.println("  " + player.getName() + " passes and receives card "
                        + card + ".");
            }
        }
    }

    private void runSellingPhase() {
        System.out.println("--- ROUND 2: SELLING ---");
        System.out.println("Four prices appear. Offer one card each.");
        System.out.println("Highest card earns the highest price, and so on.");
        System.out.println();

        for (int round = 0; round < GameConfig.ROUNDS; round++) {
            List<Integer> prices = new ArrayList<>();
            for (int i = 0; i < GameConfig.CARDS_PER_ROUND; i++) {
                prices.add(random.nextInt(GameConfig.MAX_SELL_PRICE + 1));
            }
            prices.sort(Collections.reverseOrder());

            System.out.println("Sale " + (round + 1) + " of " + GameConfig.ROUNDS);
            System.out.println("Prices offered: " + formatPrices(prices));

            int[] offers = collectOffers(prices);
            resolveSelling(prices, offers);
            printPlayerStatus();
            System.out.println();
        }
    }

    private int[] collectOffers(List<Integer> prices) {
        int[] offers = new int[GameConfig.NUM_PLAYERS];
        System.out.println("Choose one card from your hand to sell.");

        for (Player player : players) {
            if (player.isHuman()) {
                offers[indexOf(player)] = promptHumanOffer(player);
            } else {
                int card = ai.chooseCardToSell(player, prices);
                offers[indexOf(player)] = card;
                System.out.println(player.getName() + " offers card " + card + ".");
            }
        }
        return offers;
    }

    private int promptHumanOffer(Player player) {
        while (true) {
            System.out.println("Your hand: " + formatCards(new ArrayList<>(player.getHand())));
            System.out.print("Card to sell (1-20): ");
            String line = scanner.nextLine().trim();
            try {
                int card = Integer.parseInt(line);
                if (!player.hasCard(card)) {
                    System.out.println("You do not have card " + card + ".");
                    continue;
                }
                return card;
            } catch (NumberFormatException e) {
                System.out.println("Enter a card number.");
            }
        }
    }

    private void resolveSelling(List<Integer> prices, int[] offers) {
        List<Integer> sortedPrices = new ArrayList<>(prices);
        sortedPrices.sort(Collections.reverseOrder());

        List<OfferEntry> ranking = new ArrayList<>();
        for (int i = 0; i < GameConfig.NUM_PLAYERS; i++) {
            ranking.add(new OfferEntry(i, offers[i]));
        }
        ranking.sort(Comparator.comparingInt(OfferEntry::card).reversed()
                .thenComparingInt(OfferEntry::playerIndex));

        System.out.println();
        System.out.println("Results:");
        for (int i = 0; i < GameConfig.NUM_PLAYERS; i++) {
            OfferEntry entry = ranking.get(i);
            Player player = players.get(entry.playerIndex());
            int card = entry.card();
            int payout = sortedPrices.get(i);

            player.removeCard(card);
            player.addMoney(payout);
            System.out.println("  " + player.getName() + " sells card " + card
                    + " for $" + formatMoney(payout) + ".");
        }
    }

    private void announceWinner() {
        System.out.println("=== FINAL SCORES ===");
        List<Player> ranking = new ArrayList<>(players);
        ranking.sort(Comparator.comparingInt(Player::getMoney).reversed());

        for (int i = 0; i < ranking.size(); i++) {
            Player player = ranking.get(i);
            System.out.println((i + 1) + ". " + player.getName() + ": $"
                    + formatMoney(player.getMoney()));
        }

        Player winner = ranking.get(0);
        System.out.println();
        if (winner.isHuman()) {
            System.out.println("You win!");
        } else {
            System.out.println(winner.getName() + " wins. Better luck next time.");
        }
    }

    private void pauseBetweenPhases() {
        System.out.println("Bidding complete. Press Enter to start selling...");
        scanner.nextLine();
        System.out.println();
    }

    private void printPlayerStatus() {
        System.out.println("Standings:");
        for (Player player : players) {
            String hand = player.handSize() == 0 ? "none"
                    : formatCards(new ArrayList<>(player.getHand()));
            System.out.println("  " + player.getName() + " — $"
                    + formatMoney(player.getMoney()) + " | cards: " + hand);
        }
    }

    private int indexOf(Player target) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i) == target) {
                return i;
            }
        }
        throw new IllegalStateException("Player not found");
    }

    private String formatMoney(int amount) {
        return String.format("%,d", amount);
    }

    private String formatCards(List<Integer> cards) {
        List<Integer> sorted = new ArrayList<>(cards);
        Collections.sort(sorted);
        return sorted.toString();
    }

    private String formatPrices(List<Integer> prices) {
        List<Integer> sorted = new ArrayList<>(prices);
        Collections.sort(sorted, Collections.reverseOrder());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("$").append(formatMoney(sorted.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private record BidEntry(int playerIndex, int bid) {
    }

    private record OfferEntry(int playerIndex, int card) {
    }
}
