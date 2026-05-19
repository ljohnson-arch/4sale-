public final class GameConfig {
    public static final int NUM_PLAYERS = 4;
    public static final int NUM_CARDS = 20;
    public static final int CARDS_PER_ROUND = 4;
    public static final int STARTING_MONEY = 18_000;
    public static final int BID_UNIT = 1_000;
    public static final int MIN_OPENING_BID_THOUSANDS = 1;
    public static final int MAX_SELL_PRICE = 10_000;
    public static final int ROUNDS = NUM_CARDS / CARDS_PER_ROUND;

    private GameConfig() {
    }
}
