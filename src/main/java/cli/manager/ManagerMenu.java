package main.java.cli.manager;

//import com.synex.syos.application.reports.DailySalesReport;

import main.java.application.services.ShortageEventService; // changed
import javax.sql.DataSource;
import java.util.Scanner;

public final class ManagerMenu {
    private final DataSource ds;
    private final Runnable checkout; // optional allow manager to checkout too
    private final ShortageEventService shortageEvents; // new

    public ManagerMenu(DataSource ds, Runnable checkout, ShortageEventService shortageEvents) { // updated
        this.ds = ds; this.checkout = checkout; this.shortageEvents = shortageEvents;
    }

    public void run() {
        var sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n[MANAGER] 1) Daily Sales  2) Checkout  3) Shortage Alerts  0) Logout");
            switch (sc.nextLine().trim()) {
//                case "1" -> new DailySalesReport(ds).run();
                case "2" -> checkout.run();
                case "3" -> showShortages(sc);
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