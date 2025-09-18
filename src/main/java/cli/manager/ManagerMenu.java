package main.java.cli.manager;

//import com.synex.syos.application.reports.DailySalesReport;

import main.java.application.services.ShortageEventService;
import main.java.application.reports.ReorderReport;
import javax.sql.DataSource;
import java.util.Scanner;

public final class ManagerMenu {
    private final DataSource ds;
    private final Runnable checkout; // optional allow manager to checkout too
    private final ShortageEventService shortageEvents;

    public ManagerMenu(DataSource ds, Runnable checkout, ShortageEventService shortageEvents) {
        this.ds = ds; this.checkout = checkout; this.shortageEvents = shortageEvents;
    }

    public void run() {
        var sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n[MANAGER] 1) Daily Sales  2) Checkout  3) Low-Stock Records  4) Shortage Alerts  0) Logout");
            switch (sc.nextLine().trim()) {
//                case "1" -> new DailySalesReport(ds).run();
                case "2" -> checkout.run();
                case "3" -> new ReorderReport(ds, 50).run();
                case "4" -> showShortages(sc);
                case "0" -> { return; }
                default -> System.out.println("?");
            }
        }
    }

    private void showShortages(Scanner sc) {
        var events = shortageEvents.list();
        if (events.isEmpty()) {
            System.out.println("No shortage notifications.");
            return;
        }
        System.out.println("-- Shortage Notifications --");
        for (var e : events) System.out.println(e);
        System.out.print("Clear all? [y/N]: ");
        if ("y".equalsIgnoreCase(sc.nextLine().trim())) {
            shortageEvents.clear();
            System.out.println("Cleared.");
        }
    }
}