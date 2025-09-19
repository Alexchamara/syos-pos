package main.java.cli.manager;

import main.java.application.services.ShortageEventService;
import main.java.application.reports.ReorderReport;
import main.java.cli.manager.product.ProductManagementCLI;
import main.java.cli.manager.batch.BatchManagementCLI;
import main.java.cli.manager.category.CategoryManagementCLI;
import javax.sql.DataSource;
import java.util.Scanner;

public final class ManagerMenu {
    private final DataSource ds;
    private final Runnable checkout;
    private final ShortageEventService shortageEvents;
    private final Runnable receiveToMain;
    private final Runnable transferFromMain;
    private final ProductManagementCLI productManagement;
    private final BatchManagementCLI batchManagement;
    private final CategoryManagementCLI categoryManagement;

    public ManagerMenu(DataSource ds, Runnable checkout, ShortageEventService shortageEvents,
                      Runnable receiveToMain, Runnable transferFromMain,
                      ProductManagementCLI productManagement, BatchManagementCLI batchManagement,
                      CategoryManagementCLI categoryManagement) {
        this.ds = ds;
        this.checkout = checkout;
        this.shortageEvents = shortageEvents;
        this.receiveToMain = receiveToMain;
        this.transferFromMain = transferFromMain;
        this.productManagement = productManagement;
        this.batchManagement = batchManagement;
        this.categoryManagement = categoryManagement;
    }

    public void run() {
        var sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n[MANAGER] 1) Daily Sales  2) Checkout  3) Reorder <50  4) New Batch to MAIN  5) Transfer Batch MAIN->SHELF/WEB  6) Product Management  7) Batch Management  8) Category Management  0) Logout");
            switch (sc.nextLine().trim()) {
//                case "1" -> new DailySalesReport(ds).run();
                case "2" -> checkout.run();
                case "3" -> new ReorderReport(ds, 50).run();
//                case "4" -> showShortages(sc);
                case "5" -> transferFromMain.run();
                case "6" -> productManagement.run();
                case "7" -> batchManagement.run();
                case "8" -> categoryManagement.run();
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
