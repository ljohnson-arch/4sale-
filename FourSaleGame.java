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
        System.out.println("Unspent bidding cash counts toward your final score after selling.");
        System.out.println();
    }

    private void setupPlayers() {
        System.out.println("This game has " + GameConfig.NUM_PLAYERS
                + " players. Choose how many are human and how many are computer.");
        int humanCount;
        int computerCount;
        while (true) {
            humanCount = promptPlayerCount("human", GameConfig.NUM_PLAYERS);
            computerCount = promptPlayerCount("computer", GameConfig.NUM_PLAYERS);
            if (humanCount + computerCount == GameConfig.NUM_PLAYERS) {
                break;
            }
            System.out.println("Human and computer players must add up to "
                    + GameConfig.NUM_PLAYERS + ". Try again.");
            System.out.println();
        }
        System.out.println("Starting game with " + humanCount + " human and " + computerCount
                + " computer player(s).");
        System.out.println();

        String[] computerNames = {"Alex", "Blake", "Casey", "Dana"};
        for (int i = 0; i < humanCount; i++) {
            String name = humanCount == 1 ? "You" : "Player " + (i + 1);
            players.add(new Player(name, true, GameConfig.STARTING_MONEY));
        }
        for (int i = 0; i < computerCount; i++) {
            players.add(new Player(computerNames[i], false, GameConfig.STARTING_MONEY));
        }
    }

    private int promptPlayerCount(String role, int max) {
        while (true) {
            System.out.print("How many " + role + " players? (0-" + max + "): ");
            String line = scanner.nextLine().trim();
            try {
                int count = Integer.parseInt(line);
                if (count < 0 || count > max) {
                    System.out.println("Enter a number from 0 to " + max + ".");
                    continue;
                }
                return count;
            } catch (NumberFormatException e) {
                System.out.println("Enter a whole number.");
            }
        }
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
        System.out.println("Each raise must be at least the current high bid plus that step.");
        System.out.println("Players take turns until everyone has passed.");
        System.out.println("Your final bid (or $0) decides which card you get.");
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
        int[] finalBids = new int[GameConfig.NUM_PLAYERS];
        boolean[] passed = new boolean[GameConfig.NUM_PLAYERS];
        int step = 0;
        int lastBid = 0;
        boolean auctionOpen = false;
        int passedCount = 0;
        int current = startingBidder;
        int highBidderIndex = -1;

        System.out.println(players.get(startingBidder).getName() + " bids first.");

        while (passedCount < GameConfig.NUM_PLAYERS) {
            if (passed[current]) {
                current = (current + 1) % GameConfig.NUM_PLAYERS;
                continue;
            }

            if (auctionOpen && highBidderIndex >= 0 && allOthersPassed(passed, highBidderIndex)) {
                Player winner = players.get(highBidderIndex);
                System.out.println(winner.getName() + " wins the auction at $"
                        + formatMoney(finalBids[highBidderIndex]) + "!");
                break;
            }

            Player player = players.get(current);
            boolean opening = !auctionOpen;
            int bid = takeTurnBid(player, lot, roundIndex, opening, lastBid, step, finalBids[current]);

            if (bid > 0) {
                finalBids[current] = bid;
                if (!auctionOpen) {
                    step = bid;
                    auctionOpen = true;
                    System.out.println("Bid step: $" + formatMoney(step) + " per raise.");
                }
                lastBid = bid;
                highBidderIndex = current;
            } else {
                passed[current] = true;
                passedCount++;
                if (finalBids[current] > 0) {
                    System.out.println(player.getName() + " passes (stays at $"
                            + formatMoney(finalBids[current]) + ").");
                } else {
                    System.out.println(player.getName() + " passes.");
                }

                if (auctionOpen && highBidderIndex >= 0 && allOthersPassed(passed, highBidderIndex)) {
                    Player winner = players.get(highBidderIndex);
                    System.out.println(winner.getName() + " wins the auction at $"
                            + formatMoney(finalBids[highBidderIndex]) + "!");
                    break;
                }
            }

            current = (current + 1) % GameConfig.NUM_PLAYERS;
        }

        if (!auctionOpen) {
            System.out.println("Everyone passed — cards assigned for free by rank.");
        }
        return finalBids;
    }

    private boolean allOthersPassed(boolean[] passed, int highBidderIndex) {
        for (int i = 0; i < passed.length; i++) {
            if (i != highBidderIndex && !passed[i]) {
                return false;
            }
        }
        return true;
    }

    private int takeTurnBid(Player player, List<Integer> lot, int roundIndex, boolean opening,
            int lastBid, int step, int playerCurrentBid) {
        int minRequired = minimumRequiredBid(opening, lastBid, step);
        if (player.getBiddingCash() < minRequired) {
            System.out.println(player.getName() + " cannot afford the minimum bid of $"
                    + formatMoney(minRequired) + " and must pass.");
            return 0;
        }
        if (player.isHuman()) {
            return promptHumanBid(player, lot, opening, lastBid, step, playerCurrentBid, minRequired);
        }
        if (opening) {
            int thousands = ai.chooseOpeningBidThousands(player, lot, roundIndex);
            int bid = thousands * GameConfig.BID_UNIT;
            if (bid > 0) {
                System.out.println(player.getName() + " opens at $" + formatMoney(bid) + ".");
            }
            return bid;
        }
        int minBid = lastBid + step;
        int thousands = ai.chooseBidThousands(player, lot, minBid, step, roundIndex,
                playerCurrentBid);
        int bid = thousands * GameConfig.BID_UNIT;
        if (bid > 0) {
            System.out.println(player.getName() + " bids $" + formatMoney(bid) + ".");
        }
        return bid;
    }

    private int minimumRequiredBid(boolean opening, int lastBid, int step) {
        if (opening) {
            return GameConfig.MIN_OPENING_BID_THOUSANDS * GameConfig.BID_UNIT;
        }
        return lastBid + step;
    }

    private int promptHumanBid(Player player, List<Integer> lot, boolean opening,
            int lastBid, int step, int yourCurrentBid, int minRequired) {
        while (true) {
            System.out.println("Your bidding cash: $" + formatMoney(player.getBiddingCash()));
            if (yourCurrentBid > 0) {
                System.out.println("Your bid so far this auction: $" + formatMoney(yourCurrentBid));
            }
            if (opening) {
                System.out.print("Open the auction for lot " + formatCards(lot)
                        + " — thousands (min " + (minRequired / GameConfig.BID_UNIT)
                        + " = $" + formatMoney(minRequired) + "), or 0 to pass: ");
            } else {
                System.out.println("High bid: $" + formatMoney(lastBid)
                        + " | step: $" + formatMoney(step)
                        + " | minimum raise: $" + formatMoney(minRequired));
                System.out.print("Bid thousands (min " + (minRequired / GameConfig.BID_UNIT)
                        + "), or 0 to pass and leave the auction: ");
            }

            String line = scanner.nextLine().trim().replace(",", "").replace("$", "");
            if (line.equalsIgnoreCase("pass") || line.equals("0")) {
                System.out.println("You pass.");
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
                if (bid > player.getBiddingCash()) {
                    System.out.println("You only have $" + formatMoney(player.getBiddingCash())
                            + " — you must pass.");
                    return 0;
                }

                if (opening) {
                    if (bid > 0) {
                        return bid;
                    }
                    continue;
                }

                if (bid < minRequired) {
                    System.out.println("Bid must be at least $" + formatMoney(minRequired) + ".");
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
                player.spendFromBidding(price);
            }
            player.addCard(card);
            if (price > 0) {
                System.out.println("  " + player.getName() + " pays $" + formatMoney(price)
                        + " and receives card " + card + ".");
            } else {
                System.out.println("  " + player.getName() + " receives card " + card
                        + " (did not bid).");
            }
        }
    }

    private void runSellingPhase() {
        System.out.println("--- ROUND 2: SELLING ---");
        System.out.println("Four prices appear (whole $1,000 amounts). Offer one card each.");
        System.out.println("Highest card earns the highest price, and so on.");
        System.out.println("Sale money is tracked separately; leftover bidding cash is added at the end.");
        System.out.println();

        for (int round = 0; round < GameConfig.ROUNDS; round++) {
            List<Integer> prices = new ArrayList<>();
            for (int i = 0; i < GameConfig.CARDS_PER_ROUND; i++) {
                int thousands = random.nextInt(GameConfig.MAX_SELL_PRICE_THOUSANDS + 1);
                prices.add(thousands * GameConfig.BID_UNIT);
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
            player.addSaleEarnings(payout);
            System.out.println("  " + player.getName() + " sells card " + card
                    + " for $" + formatMoney(payout) + ".");
        }
    }

    private void announceWinner() {
        System.out.println("=== FINAL SCORES ===");
        List<Player> ranking = new ArrayList<>(players);
        ranking.sort(Comparator.comparingInt(Player::getFinalTotal).reversed());

        for (int i = 0; i < ranking.size(); i++) {
            Player player = ranking.get(i);
            System.out.println((i + 1) + ". " + player.getName() + ": $"
                    + formatMoney(player.getFinalTotal())
                    + " ($" + formatMoney(player.getBiddingCash()) + " leftover + $"
                    + formatMoney(player.getSaleEarnings()) + " from sales)");
        }

        Player winner = ranking.get(0);
        System.out.println();
        if (winner.isHuman()) {
            System.out.println(winner.getName() + " wins!");
        } else {
            System.out.println(winner.getName() + " wins. Better luck next time.");
        }
    }

    private void pauseBetweenPhases() {
        System.out.println("Bidding complete. Leftover cash is kept for your final score.");
        for (Player player : players) {
            System.out.println("  " + player.getName() + ": $"
                    + formatMoney(player.getBiddingCash()) + " remaining");
        }
        System.out.println("Press Enter to start selling...");
        scanner.nextLine();
        System.out.println();
    }

    private void printPlayerStatus() {
        System.out.println("Standings:");
        for (Player player : players) {
            String hand = player.handSize() == 0 ? "none"
                    : formatCards(new ArrayList<>(player.getHand()));
            String moneyLine = player.getSaleEarnings() > 0
                    ? "$" + formatMoney(player.getBiddingCash()) + " bidding + $"
                            + formatMoney(player.getSaleEarnings()) + " sales"
                    : "$" + formatMoney(player.getBiddingCash()) + " cash";
            System.out.println("  " + player.getName() + " — " + moneyLine + " | cards: " + hand);
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
