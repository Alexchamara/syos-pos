package main.java.cli.manager.product;

import main.java.application.usecase.ProductManagementUseCase;

import java.math.BigDecimal;
import java.util.Scanner;

public final class ProductManagementCLI {
    private final ProductManagementUseCase productUseCase;
    private final Scanner scanner;

    public ProductManagementCLI(ProductManagementUseCase productUseCase) {
        this.productUseCase = productUseCase;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            displayMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addProduct();
                case "2" -> updateProduct();
                case "3" -> viewProduct();
                case "4" -> viewAllProducts();
                case "5" -> deleteProduct();
                case "0" -> { return; }
                default -> {
                    System.out.println("Invalid option. Please try again.");
                    pressEnterToContinue();
                }
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("PRODUCT MANAGEMENT");
        System.out.println("=".repeat(50));
        System.out.println("1. Add New Product");
        System.out.println("2. Update Product");
        System.out.println("3. View Product Details");
        System.out.println("4. View All Products");
        System.out.println("5. Delete Product");
        System.out.println("0. Back to Manager Menu");
        System.out.println("=".repeat(50));
        System.out.print("Choose an option: ");
    }

    private void addProduct() {
        System.out.println("\nADD NEW PRODUCT");
        System.out.println("-".repeat(30));

        try {
            String code = getValidProductCode("Enter product code: ");
            String name = getValidProductName("Enter product name: ");
            BigDecimal price = getValidPrice("Enter product price: ");

            var request = new ProductManagementUseCase.CreateProductRequest(code, name, price);
            var result = productUseCase.createProduct(request);

            switch (result) {
                case SUCCESS -> System.out.println("Product created successfully!");
                case UPDATED -> System.out.println("Product updated successfully!");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void updateProduct() {
        System.out.println("\nUPDATE PRODUCT");
        System.out.println("-".repeat(30));

        try {
            String code = getValidProductCode("Enter product code to update: ");

            var existingProduct = productUseCase.findProduct(code);
            if (existingProduct.isEmpty()) {
                System.out.println("Product not found with code: " + code);
                pressEnterToContinue();
                return;
            }

            var product = existingProduct.get();
            System.out.println("Current product details:");
            displayProductInfo(product);
            System.out.println();

            // Show field selection menu
            while (true) {
                displayUpdateFieldMenu();
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> {
                        String newName = getValidProductName("Enter new product name: ");
                        updateProductField(code, newName, product.price());
                        System.out.println("Product name updated successfully!");
                        return;
                    }
                    case "2" -> {
                        BigDecimal newPrice = getValidPrice("Enter new product price ($): ");
                        updateProductField(code, product.name(), newPrice);
                        System.out.println("Product price updated successfully!");
                        return;
                    }
                    case "3" -> {
                        String newName = getValidProductName("Enter new product name: ");
                        BigDecimal newPrice = getValidPrice("Enter new product price ($): ");
                        updateProductField(code, newName, newPrice);
                        System.out.println("Product updated successfully!");
                        return;
                    }
                    case "0" -> {
                        System.out.println("Update cancelled.");
                        return;
                    }
                    default -> {
                        System.out.println("Invalid option. Please try again.");
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void displayUpdateFieldMenu() {
        System.out.println("Which field would you like to update?");
        System.out.println("-".repeat(35));
        System.out.println("1. Update Name Only");
        System.out.println("2. Update Price Only");
        System.out.println("3. Update Both Name and Price");
        System.out.println("0. Cancel Update");
        System.out.println("-".repeat(35));
        System.out.print("Choose an option: ");
    }

    private void updateProductField(String code, String name, BigDecimal price) {
        var request = new ProductManagementUseCase.CreateProductRequest(code, name, price);
        productUseCase.createProduct(request);
    }

    private void viewProduct() {
        System.out.println("\nVIEW PRODUCT DETAILS");
        System.out.println("-".repeat(30));

        try {
            String code = getValidProductCode("Enter product code: ");

            var product = productUseCase.findProduct(code);
            if (product.isEmpty()) {
                System.out.println("Product not found with code: " + code);
            } else {
                System.out.println();
                displayProductInfo(product.get());
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void viewAllProducts() {
        System.out.println("\nALL PRODUCTS");
        System.out.println("-".repeat(60));

        try {
            var products = productUseCase.listAllProducts();

            if (products.isEmpty()) {
                System.out.println("No products found in the system.");
            } else {
                System.out.printf("%-15s %-30s %10s%n", "CODE", "NAME", "PRICE ($)");
                System.out.println("-".repeat(60));

                for (var product : products) {
                    System.out.printf("%-15s %-30s %10.2f%n",
                        product.code(),
                        truncate(product.name(), 30),
                        product.price());
                }

                System.out.println("-".repeat(60));
                System.out.println("Total products: " + products.size());
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void deleteProduct() {
        System.out.println("\nDELETE PRODUCT");
        System.out.println("-".repeat(30));

        try {
            String code = getValidProductCode("Enter product code to delete: ");

            var product = productUseCase.findProduct(code);
            if (product.isEmpty()) {
                System.out.println("Product not found with code: " + code);
                pressEnterToContinue();
                return;
            }

            System.out.println("Product to delete:");
            displayProductInfo(product.get());

            if (confirmAction("Are you sure you want to delete this product? (y/N): ")) {
                var result = productUseCase.deleteProduct(code);

                switch (result) {
                    case SUCCESS -> System.out.println("Product deleted successfully!");
                    case NOT_FOUND -> System.out.println("Product not found.");
                }
            } else {
                System.out.println("Delete operation cancelled.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

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

    private String getValidProductName(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (!input.isBlank()) {
                return input;
            }

            // For updates, allow empty to keep existing name
            if (prompt.contains("[") && prompt.contains("]")) {
                return "";
            }

            System.out.println("Product name cannot be empty. Please try again.");
        }
    }

    private BigDecimal getValidPrice(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                BigDecimal price = new BigDecimal(input);
                if (price.compareTo(BigDecimal.ZERO) > 0) {
                    return price;
                }
                System.out.println("Price must be positive. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid price format. Please enter a valid number.");
            }
        }
    }

    private BigDecimal getValidPriceWithDefault(String prompt, BigDecimal defaultValue) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.isBlank()) {
                return defaultValue;
            }

            try {
                BigDecimal price = new BigDecimal(input);
                if (price.compareTo(BigDecimal.ZERO) > 0) {
                    return price;
                }
                System.out.println("Price must be positive. Please try again.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid price format. Please enter a valid number.");
            }
        }
    }

    private boolean confirmAction(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes");
    }

    private void displayProductInfo(ProductManagementUseCase.ProductInfo product) {
        System.out.println("─".repeat(40));
        System.out.println("Product Details");
        System.out.println("─".repeat(40));
        System.out.printf("Code:  %-30s %n", product.code());
        System.out.printf("Name:  %-30s%n", truncate(product.name(), 30));
        System.out.printf("Price: $%-28.2f %n", product.price());
        System.out.println("─".repeat(40));
    }

    private void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
