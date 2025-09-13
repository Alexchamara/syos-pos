package main.java.cli.demo;

import main.java.application.usecase.CheckoutCashUseCase;
import main.java.domain.inventory.StockLocation;
import main.java.domain.pricing.NoDiscount;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates concurrent access to the checkout system.
 * Shows how multiple cashiers can process transactions simultaneously.
 */
public final class ConcurrencyDemo {

    public static void run(CheckoutCashUseCase checkoutUseCase) {
        System.out.println("\n=== CONCURRENCY DEMO ===");
        System.out.println("Simulating multiple concurrent checkouts...");

        ExecutorService executor = Executors.newFixedThreadPool(3);

        // Simulate 3 concurrent transactions
        for (int i = 1; i <= 3; i++) {
            final int cashierId = i;
            executor.submit(() -> {
                try {
                    // Create a sample cart for each cashier
                    var cart = List.of(
                        new CheckoutCashUseCase.Item("CLN003", 2),
                        new CheckoutCashUseCase.Item("CLN004", 1)
                    );

                    long cashCents = 5000; // $50.00
                    var location = StockLocation.SHELF;
                    var discountPolicy = new NoDiscount(); // No discount for demo
                    String scope = "DEMO"; // Demo scope

                    System.out.println("[Cashier " + cashierId + "] Starting checkout...");

                    var bill = checkoutUseCase.handle(cart, cashCents, location, discountPolicy, scope);

                    System.out.println("[Cashier " + cashierId + "] Completed checkout - Bill: " +
                        bill.serial() + ", Total: " + bill.total().cents() + "¢");

                } catch (Exception e) {
                    System.out.println("[Cashier " + cashierId + "] Failed: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                System.out.println("Demo timed out");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.out.println("Demo interrupted");
            executor.shutdownNow();
        }

        System.out.println("=== CONCURRENCY DEMO COMPLETE ===\n");
    }
}
