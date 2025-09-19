package main.java.cli.manager.product;

import main.java.application.usecase.ProductManagementUseCase;
import main.java.application.usecase.CategoryManagementUseCase;
import main.java.domain.product.Category;
import main.java.domain.shared.Currency;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public final class ProductManagementCLI {
    private final ProductManagementUseCase productUseCase;
    private final CategoryManagementUseCase categoryUseCase;
    private final Scanner scanner;

    public ProductManagementCLI(ProductManagementUseCase productUseCase, CategoryManagementUseCase categoryUseCase) {
        this.productUseCase = productUseCase;
        this.categoryUseCase = categoryUseCase;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            displayMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addProductWithCategory();
                case "2" -> addProductManualCode();
                case "3" -> updateProduct();
                case "4" -> viewProduct();
                case "5" -> viewAllProducts();
                case "6" -> deleteProduct();
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
        System.out.println("1. Add New Product (Select Category)");
        System.out.println("2. Add New Product (Manual Code)");
        System.out.println("3. Update Product");
        System.out.println("4. View Product Details");
        System.out.println("5. View All Products");
        System.out.println("6. Delete Product");
        System.out.println("0. Back to Manager Menu");
        System.out.println("=".repeat(50));
        System.out.print("Choose an option: ");
    }

    private void addProductWithCategory() {
        System.out.println("\nADD NEW PRODUCT (SELECT CATEGORY)");
        System.out.println("-".repeat(40));

        try {
            // Show available categories
            List<Category> categories = categoryUseCase.getAllActiveCategories();
            if (categories.isEmpty()) {
                System.out.println("No active categories available. Please create categories first.");
                pressEnterToContinue();
                return;
            }

            System.out.println("Available categories:");
            for (int i = 0; i < categories.size(); i++) {
                Category cat = categories.get(i);
                System.out.printf("%d. %s - %s (Next code: %s)%n",
                    i + 1, cat.code(), cat.name(), cat.generateNextProductCode());
            }

            System.out.print("Select category number: ");
            int choice = Integer.parseInt(scanner.nextLine().trim());

            if (choice < 1 || choice > categories.size()) {
                System.out.println("Invalid selection.");
                pressEnterToContinue();
                return;
            }

            Category selectedCategory = categories.get(choice - 1);
            System.out.printf("Selected category: %s - %s%n", selectedCategory.code(), selectedCategory.name());
            System.out.printf("Product code will be: %s%n", selectedCategory.generateNextProductCode());

            String name = getValidProductName("Enter product name: ");
            BigDecimal price = getValidPrice("Enter product price: ");

            var request = new ProductManagementUseCase.CreateProductWithCategoryRequest(
                selectedCategory.code(), name, price);
            var result = productUseCase.createProductWithCategory(request);

            System.out.printf("Product created successfully with code: %s%n", result.generatedCode());

        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void addProductManualCode() {
        System.out.println("\nADD NEW PRODUCT (MANUAL CODE)");
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
                        BigDecimal newPrice = getValidPrice("Enter new product price (" + Currency.SYMBOL + "): ");
                        updateProductField(code, product.name(), newPrice);
                        System.out.println("Product price updated successfully!");
                        return;
                    }
                    case "3" -> {
                        String newName = getValidProductName("Enter new product name: ");
                        BigDecimal newPrice = getValidPrice("Enter new product price (" + Currency.SYMBOL + "): ");
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
                System.out.printf("%-15s %-30s %10s%n", "CODE", "NAME", "PRICE (" + Currency.SYMBOL + ")");
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
        System.out.printf("Price: %s%-25.2f %n", Currency.SYMBOL + " ", product.price());
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
