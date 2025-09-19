package main.java.cli.cashier;

import javax.sql.DataSource;
import java.util.Scanner;

public final class CashierMenu {
    private final Runnable checkout;
    private final DataSource ds;

    public CashierMenu(Runnable checkout, DataSource ds) {
        this.checkout = checkout; this.ds = ds;
    }

    public void run() {
        var sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n[CASHIER] 1) Checkout  0) Logout");
            switch (sc.nextLine().trim()) {
                case "1" -> checkout.run();
                case "0" -> { return; }
                default -> System.out.println("?");
            }
        }
    }
}