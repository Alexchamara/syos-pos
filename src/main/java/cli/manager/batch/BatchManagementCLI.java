package main.java.cli.manager.batch;

import main.java.application.usecase.BatchManagementUseCase;
import main.java.domain.inventory.StockLocation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class BatchManagementCLI {

    private final BatchManagementUseCase batchUseCase;
    private final Scanner scanner;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public BatchManagementCLI(BatchManagementUseCase batchUseCase) {
        this.batchUseCase = batchUseCase;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            displayMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addBatch();
                case "2" -> updateBatch();
                case "3" -> viewBatch();
                case "4" -> viewAllBatches();
                case "5" -> viewBatchesByProduct();
                case "6" -> viewBatchesByLocation();
                case "7" -> deleteBatch();
                case "0" -> { return; }
                default -> {
                    System.out.println("Invalid option. Please try again.");
                    pressEnterToContinue();
                }
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("BATCH MANAGEMENT");
        System.out.println("=".repeat(60));
        System.out.println("1. Add New Batch");
        System.out.println("2. Update Batch");
        System.out.println("3. View Batch Details");
        System.out.println("4. View All Batches");
        System.out.println("5. View Batches by Product");
        System.out.println("6. View Batches by Location");
        System.out.println("7. Delete Batch");
        System.out.println("0. Back to Manager Menu");
        System.out.println("=".repeat(60));
        System.out.print("Choose an option: ");
    }

    private void addBatch() {
        System.out.println("\nADD NEW BATCH");
        System.out.println("-".repeat(40));

        try {
            String productCode = getValidProductCode("Enter product code: ");

            // Validate product exists
            if (!batchUseCase.productExists(productCode)) {
                System.out.println("Error: Product with code '" + productCode + "' does not exist.");
                System.out.println("Please create the product first before adding batches.");
                pressEnterToContinue();
                return;
            }

            StockLocation location = getValidLocation("Enter stock location (MAIN_STORE/SHELF/WEB): ");
            LocalDate expiry = getValidExpiryDate("Enter expiry date (yyyy-MM-dd) or press Enter for no expiry: ");
            int quantity = getValidQuantity("Enter quantity: ");

            var request = new BatchManagementUseCase.CreateBatchRequest(productCode, location, expiry, quantity);
            var result = batchUseCase.createBatch(request);

            switch (result) {
                case SUCCESS -> System.out.println("Batch created successfully!");
                case PRODUCT_NOT_EXISTS -> System.out.println("Error: Product does not exist.");
                case INVALID_INPUT -> System.out.println("Error: Invalid input provided.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void updateBatch() {
        System.out.println("\nUPDATE BATCH");
        System.out.println("-".repeat(40));

        try {
            long batchId = getValidBatchId("Enter batch ID to update: ");

            var existingBatch = batchUseCase.findBatch(batchId);
            if (existingBatch.isEmpty()) {
                System.out.println("Batch not found with ID: " + batchId);
                pressEnterToContinue();
                return;
            }

            var batch = existingBatch.get();
            System.out.println("Current batch details:");
            displayBatchInfo(batch);
            System.out.println();

            // Show field selection menu
            while (true) {
                displayUpdateFieldMenu();
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> {
                        LocalDate newExpiry = getValidExpiryDate("Enter new expiry date (yyyy-MM-dd) or press Enter for no expiry: ");
                        updateBatchField(batchId, newExpiry, batch.quantity());
                        System.out.println("Batch expiry updated successfully!");
                        return;
                    }
                    case "2" -> {
                        int newQuantity = getValidQuantity("Enter new quantity: ");
                        updateBatchField(batchId, batch.expiry(), newQuantity);
                        System.out.println("Batch quantity updated successfully!");
                        return;
                    }
                    case "3" -> {
                        LocalDate newExpiry = getValidExpiryDate("Enter new expiry date (yyyy-MM-dd) or press Enter for no expiry: ");
                        int newQuantity = getValidQuantity("Enter new quantity: ");
                        updateBatchField(batchId, newExpiry, newQuantity);
                        System.out.println("Batch updated successfully!");
                        return;
                    }
                    case "0" -> {
                        System.out.println("Update cancelled.");
                        return;
                    }
                    default -> System.out.println("Invalid option. Please try again.");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void displayUpdateFieldMenu() {
        System.out.println("Which field would you like to update?");
        System.out.println("-".repeat(40));
        System.out.println("1. Update Expiry Date Only");
        System.out.println("2. Update Quantity Only");
        System.out.println("3. Update Both Expiry and Quantity");
        System.out.println("0. Cancel Update");
        System.out.println("-".repeat(40));
        System.out.print("Choose an option: ");
    }

    private void updateBatchField(long batchId, LocalDate expiry, int quantity) {
        var request = new BatchManagementUseCase.UpdateBatchRequest(batchId, expiry, quantity);
        var result = batchUseCase.updateBatch(request);

        if (result != BatchManagementUseCase.UpdateResult.SUCCESS) {
            System.out.println("Failed to update batch: " + result);
        }
    }

    private void viewBatch() {
        System.out.println("\nVIEW BATCH DETAILS");
        System.out.println("-".repeat(40));

        try {
            long batchId = getValidBatchId("Enter batch ID: ");

            var batch = batchUseCase.findBatch(batchId);
            if (batch.isEmpty()) {
                System.out.println("Batch not found with ID: " + batchId);
            } else {
                System.out.println();
                displayBatchInfo(batch.get());
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void viewAllBatches() {
        System.out.println("\nALL BATCHES");
        System.out.println("-".repeat(100));

        try {
            System.out.println("Loading all batches...");
            var batches = batchUseCase.listAllBatches();

            if (batches.isEmpty()) {
                System.out.println("No batches found in the system.");
                System.out.println("This could mean:");
                System.out.println("- The database is empty (no batches have been added yet)");
                System.out.println("- There might be a database connection issue");
                System.out.println("- Try adding a new batch first using option 1");
            } else {
                System.out.printf("%-8s %-15s %-12s %-20s %-12s %-10s%n",
                    "ID", "PRODUCT", "LOCATION", "RECEIVED", "EXPIRY", "QUANTITY");
                System.out.println("-".repeat(100));

                for (var batch : batches) {
                    System.out.printf("%-8d %-15s %-12s %-20s %-12s %-10d%n",
                        batch.id(),
                        truncate(batch.productCode(), 15),
                        batch.location(),
                        batch.receivedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        batch.expiry() != null ? batch.expiry().format(DATE_FORMATTER) : "No expiry",
                        batch.quantity());
                }

                System.out.println("-".repeat(100));
                System.out.println("Total batches: " + batches.size());
            }

        } catch (Exception e) {
            System.out.println("✗ An error occurred while retrieving batches.");
            System.out.println("Error details: " + e.getMessage());
            System.out.println("This might indicate a database connection issue or corrupted data.");
        }

        pressEnterToContinue();
    }

    private void viewBatchesByProduct() {
        System.out.println("\nVIEW BATCHES BY PRODUCT");
        System.out.println("-".repeat(40));

        try {
            String productCode = getValidProductCode("Enter product code: ");

            if (!batchUseCase.productExists(productCode)) {
                System.out.println("✗ Product with code '" + productCode + "' does not exist.");
                pressEnterToContinue();
                return;
            }

            System.out.println("Searching for batches...");
            var batches = batchUseCase.listBatchesByProduct(productCode);

            if (batches.isEmpty()) {
                System.out.println("No batches found for product: " + productCode);
                System.out.println("This could mean:");
                System.out.println("- No batches have been added for this product yet");
                System.out.println("- All batches for this product have been consumed");
            } else {
                System.out.println("\nBatches for product: " + productCode);
                System.out.println("-".repeat(90));
                System.out.printf("%-8s %-12s %-20s %-12s %-10s%n",
                    "ID", "LOCATION", "RECEIVED", "EXPIRY", "QUANTITY");
                System.out.println("-".repeat(90));

                int totalQuantity = 0;
                for (var batch : batches) {
                    System.out.printf("%-8d %-12s %-20s %-12s %-10d%n",
                        batch.id(),
                        batch.location(),
                        batch.receivedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        batch.expiry() != null ? batch.expiry().format(DATE_FORMATTER) : "No expiry",
                        batch.quantity());
                    totalQuantity += batch.quantity();
                }

                System.out.println("-".repeat(90));
                System.out.println("Total batches: " + batches.size() + " | Total quantity: " + totalQuantity);
            }

        } catch (Exception e) {
            System.out.println("An error occurred while retrieving batches.");
            System.out.println("Error details: " + e.getMessage());
            System.out.println("This might indicate a database connection issue or corrupted data.");
        }

        pressEnterToContinue();
    }

    private void viewBatchesByLocation() {
        System.out.println("\nVIEW BATCHES BY LOCATION");
        System.out.println("-".repeat(40));

        try {
            StockLocation location = getValidLocation("Enter location (MAIN_STORE/SHELF/WEB): ");

            System.out.println("Searching for batches at " + location + "...");
            var batches = batchUseCase.listBatchesByLocation(location);

            if (batches.isEmpty()) {
                System.out.println("No batches found at location: " + location);
                System.out.println("This could mean:");
                System.out.println("- No batches have been added to this location yet");
                System.out.println("- All batches at this location have been consumed");
                System.out.println("- Try adding a new batch first using option 1");
            } else {
                System.out.println("\nBatches at location: " + location);
                System.out.println("-".repeat(85));
                System.out.printf("%-8s %-15s %-20s %-12s %-10s%n",
                    "ID", "PRODUCT", "RECEIVED", "EXPIRY", "QUANTITY");
                System.out.println("-".repeat(85));

                int totalQuantity = 0;
                for (var batch : batches) {
                    System.out.printf("%-8d %-15s %-20s %-12s %-10d%n",
                        batch.id(),
                        truncate(batch.productCode(), 15),
                        batch.receivedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                        batch.expiry() != null ? batch.expiry().format(DATE_FORMATTER) : "No expiry",
                        batch.quantity());
                    totalQuantity += batch.quantity();
                }

                System.out.println("-".repeat(85));
                System.out.println("Total batches: " + batches.size() + " | Total quantity: " + totalQuantity);
            }

        } catch (Exception e) {
            System.out.println("An error occurred while retrieving batches.");
            System.out.println("Error details: " + e.getMessage());
            System.out.println("This might indicate a database connection issue or corrupted data.");
        }

        pressEnterToContinue();
    }

    private void deleteBatch() {
        System.out.println("\nDELETE BATCH");
        System.out.println("-".repeat(40));

        try {
            long batchId = getValidBatchId("Enter batch ID to delete: ");

            var batch = batchUseCase.findBatch(batchId);
            if (batch.isEmpty()) {
                System.out.println("Batch not found with ID: " + batchId);
                pressEnterToContinue();
                return;
            }

            System.out.println("Batch to delete:");
            displayBatchInfo(batch.get());

            if (confirmAction("Are you sure you want to delete this batch? (y/N): ")) {
                var result = batchUseCase.deleteBatch(batchId);

                switch (result) {
                    case SUCCESS -> System.out.println("Batch deleted successfully!");
                    case NOT_FOUND -> System.out.println("Batch not found.");
                    case INVALID_INPUT -> System.out.println("Invalid batch ID.");
                }
            } else {
                System.out.println("Delete operation cancelled.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    // Input validation methods with loops for invalid input
    private String getValidProductCode(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (!input.isBlank()) {
                return input.toUpperCase();
            }

            System.out.println("Product code cannot be empty. Please try again.");
        }
    }

    private StockLocation getValidLocation(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toUpperCase();

            try {
                return StockLocation.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid location. Valid options: MAIN_STORE, SHELF, WEB");
                System.out.println("Please try again.");
            }
        }
    }

    private LocalDate getValidExpiryDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.isBlank()) {
                return null; // No expiry date
            }

            try {
                LocalDate date = LocalDate.parse(input, DATE_FORMATTER);
                if (date.isBefore(LocalDate.now())) {
                    System.out.println("Expiry date cannot be in the past. Please try again.");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use yyyy-MM-dd (e.g., 2025-12-31)");
                System.out.println("Please try again.");
            }
        }
    }

    private int getValidQuantity(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                int quantity = Integer.parseInt(input);
                if (quantity >= 0) {
                    return quantity;
                }
                System.out.println("Quantity cannot be negative. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid quantity format. Please enter a valid number.");
                System.out.println("Please try again.");
            }
        }
    }

    private long getValidBatchId(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                long id = Long.parseLong(input);
                if (id > 0) {
                    return id;
                }
                System.out.println("Batch ID must be positive. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid batch ID format. Please enter a valid number.");
                System.out.println("Please try again.");
            }
        }
    }

    private boolean confirmAction(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        return "y".equalsIgnoreCase(input) || "yes".equalsIgnoreCase(input);
    }

    private void displayBatchInfo(BatchManagementUseCase.BatchInfo batch) {
        System.out.println("─".repeat(50));
        System.out.println("BATCH DETAILS" + " ".repeat(37));
        System.out.println("─".repeat(50));
        System.out.printf("ID           : %-33d %n", batch.id());
        System.out.printf("Product Code : %-33s %n", batch.productCode());
        System.out.printf("Location     : %-33s %n", batch.location());
        System.out.printf("Received     : %-33s %n",
            batch.receivedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.printf("Expiry       : %-33s %n",
            batch.expiry() != null ? batch.expiry().format(DATE_FORMATTER) : "No expiry");
        System.out.printf("Quantity     : %-33d %n", batch.quantity());
        System.out.println("─".repeat(50));
    }

    private void pressEnterToContinue() {
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
    }
}
