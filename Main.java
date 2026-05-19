import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            new FourSaleGame(scanner).run();
        } finally {
            scanner.close();
        }
    }
}
