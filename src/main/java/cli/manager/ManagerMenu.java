package main.java.cli.manager;

//import com.synex.syos.application.reports.DailySalesReport;

import javax.sql.DataSource;
import java.util.Scanner;

public final class ManagerMenu {
    private final DataSource ds;
    private final Runnable checkout; // optional allow manager to checkout too

    public ManagerMenu(DataSource ds, Runnable checkout) { this.ds = ds; this.checkout = checkout; }

    public void run() {
        var sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n[MANAGER] 1) Daily Sales  2) Checkout  0) Logout");
            switch (sc.nextLine().trim()) {
//                case "1" -> new DailySalesReport(ds).run();
                case "2" -> checkout.run();
                case "0" -> { return; }
                default -> System.out.println("?");
            }
        }
    }
}