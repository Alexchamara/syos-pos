package main.java.cli.manager;

import main.java.application.services.AvailabilityService;
import main.java.application.usecase.TransferStockUseCase;
import main.java.application.usecase.QuoteUseCase;
import main.java.domain.inventory.StockLocation;
import main.java.domain.inventory.Batch;
import main.java.domain.repository.InventoryRepository;
import main.java.domain.shared.Code;
import main.java.infrastructure.concurrency.Tx;

import java.util.Scanner;
import java.util.List;
import java.util.Map;

public final class TransferFromMainCLI {
    private final TransferStockUseCase usecase;
    private final AvailabilityService availabilityService;
    private final QuoteUseCase quoteUseCase;
    private final InventoryRepository inventoryRepository;
    private final Tx tx;

    public TransferFromMainCLI(TransferStockUseCase usecase, AvailabilityService availabilityService, QuoteUseCase quoteUseCase, InventoryRepository inventoryRepository, Tx tx) {
        this.usecase = usecase;
        this.availabilityService = availabilityService;
//        this.availabilityService = availabilityService;
        this.quoteUseCase = quoteUseCase;
        this.inventoryRepository = inventoryRepository;
        this.tx = tx;
    }

    public void run() {
        var sc = new Scanner(System.in);
        System.out.println("\n=== Transfer Stock Between Locations ===");

        while (true) {
            try {
                // Step 1: Get and validate product code
                String productCode = getValidProductCode(sc);
                if (productCode == null) return;

                // Step 2: Show current stock levels across all locations
                showStockLevels(productCode);

                // Step 3: Get source location
                StockLocation fromLocation = getValidSourceLocation(sc, productCode);
                if (fromLocation == null) continue;

                // Step 4: Get destination location
                StockLocation toLocation = getValidDestinationLocation(sc, fromLocation);
                if (toLocation == null) continue;

                // Step 5: Show batch details if requested
                if (askToShowBatchDetails(sc, productCode, fromLocation)) {
                    showBatchDetails(productCode, fromLocation);
                }

                // Step 6: Get and validate quantity
                int quantity = getValidQuantity(sc, productCode, fromLocation);
                if (quantity <= 0) continue; // User chose to go back

                // Step 7: Confirm and execute transfer
                if (confirmTransfer(sc, productCode, fromLocation, toLocation, quantity)) {
                    executeTransfer(productCode, fromLocation, toLocation, quantity);
                    System.out.println("✓ Transfer completed successfully!");

                    // Show updated stock levels
                    System.out.println("\n=== Updated Stock Levels ===");
                    showStockLevels(productCode);
                } else {
                    System.out.println("Transfer cancelled.");
                }

                // Ask if user wants to perform another transfer
                System.out.print("\nPerform another transfer? [y/N]: ");
                if (!"y".equalsIgnoreCase(sc.next().trim())) {
                    break;
                }

            } catch (Exception e) {
                System.out.println("✗ Error: " + e.getMessage());
                System.out.println("Please try again.");
            }
        }

        System.out.println("Transfer operations completed.");
    }

    private String getValidProductCode(Scanner sc) {
        while (true) {
            System.out.print("Product code (or 'exit' to quit): ");
            String code = sc.next().trim();

            if ("exit".equalsIgnoreCase(code)) {
                return null;
            }

            if (code.isEmpty()) {
                System.out.println("✗ Product code cannot be empty. Please try again.");
                continue;
            }

            // Validate product exists
            if (!quoteUseCase.productExists(code)) {
                System.out.println("✗ Product code '" + code + "' does not exist. Please try again.");
                continue;
            }

            return code;
        }
    }

    private void showStockLevels(String productCode) {
        System.out.println("\n=== Current Stock Levels for " + productCode + " ===");
        Map<StockLocation, Integer> availability = availabilityService.getAvailabilityAcrossAllLocations(productCode);

        for (StockLocation location : StockLocation.values()) {
            int qty = availability.getOrDefault(location, 0);
            System.out.println(location.name() + ": " + qty + " units");
        }
        System.out.println();
    }

    private StockLocation getValidSourceLocation(Scanner sc, String productCode) {
        while (true) {
            System.out.print("Source location [MAIN_STORE/SHELF/WEB] (or 'back' to change product): ");
            String input = sc.next().trim().toUpperCase();

            if ("BACK".equals(input)) {
                return null;
            }

            StockLocation location;
            try {
                location = StockLocation.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("✗ Invalid location. Valid options: MAIN_STORE, SHELF, WEB");
                continue;
            }

            // Check if source location has any stock
            int available = availabilityService.available(productCode, location);
            if (available <= 0) {
                System.out.println("✗ No stock available at " + location.name() + " (" + available + " units)");
                continue;
            }

            return location;
        }
    }

    private StockLocation getValidDestinationLocation(Scanner sc, StockLocation fromLocation) {
        while (true) {
            System.out.print("Destination location [MAIN_STORE/SHELF/WEB] (or 'back' to change source): ");
            String input = sc.next().trim().toUpperCase();

            if ("BACK".equals(input)) {
                return null;
            }

            StockLocation location;
            try {
                location = StockLocation.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("✗ Invalid location. Valid options: MAIN_STORE, SHELF, WEB");
                continue;
            }

            if (location == fromLocation) {
                System.out.println("✗ Destination cannot be the same as source location");
                continue;
            }

            return location;
        }
    }

    private boolean askToShowBatchDetails(Scanner sc, String productCode, StockLocation location) {
        System.out.print("View batch details for " + productCode + " at " + location.name() + "? [y/N]: ");
        return "y".equalsIgnoreCase(sc.next().trim());
    }

    private void showBatchDetails(String productCode, StockLocation location) {
        System.out.println("\n=== Batch Details for " + productCode + " at " + location.name() + " ===");

        List<Batch> batches = tx.inTx(con ->
            inventoryRepository.findDeductionCandidates(con, new Code(productCode), location)
        );

        if (batches.isEmpty()) {
            System.out.println("No batches found for this product at this location.");
        } else {
            System.out.printf("%-10s %-20s %-12s %-10s%n", "Batch ID", "Received At", "Expiry", "Quantity");
            System.out.println("--------------------------------------------------------");

            for (Batch batch : batches) {
                System.out.printf("%-10d %-20s %-12s %-10d%n",
                    batch.id(),
                    batch.receivedAt().toString().substring(0, 16), // Show date and time without seconds
                    batch.expiry() != null ? batch.expiry().toString() : "No expiry",
                    batch.quantity().value()
                );
            }
        }
        System.out.println();
    }

    private int getValidQuantity(Scanner sc, String productCode, StockLocation fromLocation) {
        int availableQty = availabilityService.available(productCode, fromLocation);

        while (true) {
            System.out.print("Quantity to transfer (max " + availableQty + ") (or 0 to go back): ");

            try {
                int quantity = sc.nextInt();

                if (quantity == 0) {
                    return 0; // Go back
                }

                if (quantity < 0) {
                    System.out.println("✗ Quantity must be positive");
                    continue;
                }

                if (quantity > availableQty) {
                    System.out.println("✗ Insufficient stock. Available: " + availableQty + ", Requested: " + quantity);
                    continue;
                }

                return quantity;

            } catch (Exception e) {
                System.out.println("✗ Invalid number. Please enter a valid quantity.");
                sc.nextLine(); // Clear invalid input
            }
        }
    }

    private boolean confirmTransfer(Scanner sc, String productCode, StockLocation from, StockLocation to, int quantity) {
        System.out.println("\n=== Transfer Confirmation ===");
        System.out.println("Product: " + productCode);
        System.out.println("From: " + from.name());
        System.out.println("To: " + to.name());
        System.out.println("Quantity: " + quantity);
        System.out.print("Confirm transfer? [y/N]: ");

        return "y".equalsIgnoreCase(sc.next().trim());
    }

    private void executeTransfer(String productCode, StockLocation from, StockLocation to, int quantity) throws Exception {
        try {
            usecase.transfer(productCode, from, to, quantity);
        } catch (Exception e) {
            throw new Exception("Transfer failed: " + e.getMessage(), e);
        }
    }
}