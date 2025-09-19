package main.java.cli.manager.category;

import main.java.application.usecase.CategoryManagementUseCase;
import main.java.domain.product.Category;

import java.util.List;
import java.util.Scanner;

public final class CategoryManagementCLI {
    private final CategoryManagementUseCase categoryUseCase;
    private final Scanner scanner;

    public CategoryManagementCLI(CategoryManagementUseCase categoryUseCase) {
        this.categoryUseCase = categoryUseCase;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            displayMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addCategory();
                case "2" -> updateCategory();
                case "3" -> viewCategory();
                case "4" -> viewAllCategories();
                case "5" -> deleteCategory();
                case "6" -> generateProductCode();
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
        System.out.println("CATEGORY MANAGEMENT");
        System.out.println("=".repeat(50));
        System.out.println("1. Add New Category");
        System.out.println("2. Update Category");
        System.out.println("3. View Category Details");
        System.out.println("4. View All Categories");
        System.out.println("5. Delete Category");
        System.out.println("6. Generate Product Code");
        System.out.println("0. Back to Manager Menu");
        System.out.println("=".repeat(50));
        System.out.print("Choose an option: ");
    }

    private void addCategory() {
        System.out.println("\nADD NEW CATEGORY");
        System.out.println("-".repeat(30));

        try {
            String code = getValidCategoryCode("Enter category code (e.g., ELECTRONICS): ");
            String name = getValidInput("Enter category name: ");
            String description = getOptionalInput("Enter category description (optional): ");
            String prefix = getValidPrefix("Enter product code prefix (e.g., ELC): ");
            int displayOrder = getValidDisplayOrder("Enter display order (number): ");

            var request = new CategoryManagementUseCase.CreateCategoryRequest(
                code, name, description, prefix, displayOrder);
            var result = categoryUseCase.createCategory(request);

            switch (result) {
                case SUCCESS -> System.out.println("Category created successfully!");
                case UPDATED -> System.out.println("Category updated successfully!");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void updateCategory() {
        System.out.println("\nUPDATE CATEGORY");
        System.out.println("-".repeat(30));

        try {
            String code = getValidCategoryCode("Enter category code to update: ");

            var existingCategory = categoryUseCase.findCategory(code);
            if (existingCategory.isEmpty()) {
                System.out.println("Category not found with code: " + code);
                pressEnterToContinue();
                return;
            }

            var category = existingCategory.get();
            System.out.println("Current category details:");
            displayCategoryInfo(category);
            System.out.println();

            while (true) {
                displayUpdateFieldMenu();
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> {
                        String newName = getValidInput("Enter new category name: ");
                        var request = new CategoryManagementUseCase.UpdateCategoryRequest(
                            code, newName, null, null, null);
                        categoryUseCase.updateCategory(request);
                        System.out.println("Category name updated successfully!");
                    }
                    case "2" -> {
                        String newDescription = getOptionalInput("Enter new description: ");
                        var request = new CategoryManagementUseCase.UpdateCategoryRequest(
                            code, null, newDescription, null, null);
                        categoryUseCase.updateCategory(request);
                        System.out.println("Category description updated successfully!");
                    }
                    case "3" -> {
                        int newDisplayOrder = getValidDisplayOrder("Enter new display order: ");
                        var request = new CategoryManagementUseCase.UpdateCategoryRequest(
                            code, null, null, newDisplayOrder, null);
                        categoryUseCase.updateCategory(request);
                        System.out.println("Display order updated successfully!");
                    }
                    case "4" -> {
                        boolean active = getValidBoolean("Is category active? (true/false): ");
                        var request = new CategoryManagementUseCase.UpdateCategoryRequest(
                            code, null, null, null, active);
                        categoryUseCase.updateCategory(request);
                        System.out.println("Category status updated successfully!");
                    }
                    case "0" -> { return; }
                    default -> System.out.println("Invalid option. Please try again.");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void viewCategory() {
        System.out.println("\nVIEW CATEGORY DETAILS");
        System.out.println("-".repeat(30));

        try {
            String code = getValidCategoryCode("Enter category code: ");
            var category = categoryUseCase.findCategory(code);

            if (category.isEmpty()) {
                System.out.println("Category not found with code: " + code);
            } else {
                displayCategoryInfo(category.get());
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void viewAllCategories() {
        System.out.println("\nALL CATEGORIES");
        System.out.println("-".repeat(50));

        try {
            List<Category> categories = categoryUseCase.getAllCategories();

            if (categories.isEmpty()) {
                System.out.println("No categories found.");
            } else {
                System.out.printf("%-15s %-25s %-10s %-10s %-8s%n",
                    "CODE", "NAME", "PREFIX", "ORDER", "ACTIVE");
                System.out.println("-".repeat(70));

                for (Category category : categories) {
                    System.out.printf("%-15s %-25s %-10s %-10d %-8s%n",
                        category.code(),
                        category.name(),
                        category.prefix(),
                        category.displayOrder(),
                        category.active() ? "Yes" : "No");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void deleteCategory() {
        System.out.println("\nDELETE CATEGORY");
        System.out.println("-".repeat(30));

        try {
            String code = getValidCategoryCode("Enter category code to delete: ");

            var category = categoryUseCase.findCategory(code);
            if (category.isEmpty()) {
                System.out.println("Category not found with code: " + code);
                pressEnterToContinue();
                return;
            }

            System.out.println("Category to delete:");
            displayCategoryInfo(category.get());

            System.out.print("Are you sure you want to delete this category? (yes/no): ");
            String confirmation = scanner.nextLine().trim().toLowerCase();

            if ("yes".equals(confirmation) || "y".equals(confirmation)) {
                categoryUseCase.deleteCategory(code);
                System.out.println("Category deleted successfully!");
            } else {
                System.out.println("Delete operation cancelled.");
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void generateProductCode() {
        System.out.println("\nGENERATE PRODUCT CODE");
        System.out.println("-".repeat(30));

        try {
            // Show available categories
            List<Category> categories = categoryUseCase.getAllActiveCategories();
            if (categories.isEmpty()) {
                System.out.println("No active categories available.");
                pressEnterToContinue();
                return;
            }

            System.out.println("Available categories:");
            for (int i = 0; i < categories.size(); i++) {
                Category cat = categories.get(i);
                System.out.printf("%d. %s - %s (Next: %s)%n",
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
            String productCode = categoryUseCase.generateProductCode(selectedCategory.code());

            System.out.printf("Generated product code: %s%n", productCode);
            System.out.printf("Category: %s - %s%n", selectedCategory.code(), selectedCategory.name());

        } catch (NumberFormatException e) {
            System.out.println("Invalid number format.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        pressEnterToContinue();
    }

    private void displayUpdateFieldMenu() {
        System.out.println("\nSelect field to update:");
        System.out.println("1. Name");
        System.out.println("2. Description");
        System.out.println("3. Display Order");
        System.out.println("4. Active Status");
        System.out.println("0. Back");
        System.out.print("Choose option: ");
    }

    private void displayCategoryInfo(Category category) {
        System.out.println("Code: " + category.code());
        System.out.println("Name: " + category.name());
        System.out.println("Description: " + (category.description() != null ? category.description() : "N/A"));
        System.out.println("Prefix: " + category.prefix());
        System.out.println("Next Product Code: " + category.generateNextProductCode());
        System.out.println("Display Order: " + category.displayOrder());
        System.out.println("Active: " + (category.active() ? "Yes" : "No"));
    }

    private String getValidCategoryCode(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toUpperCase();
            if (!input.isBlank() && input.matches("^[A-Z_]+$")) {
                return input;
            }
            System.out.println("Invalid category code. Use only uppercase letters and underscores.");
        }
    }

    private String getValidInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isBlank()) {
                return input;
            }
            System.out.println("Input cannot be empty.");
        }
    }

    private String getOptionalInput(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        return input.isBlank() ? null : input;
    }

    private String getValidPrefix(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toUpperCase();
            if (!input.isBlank() && input.matches("^[A-Z]{2,5}$")) {
                return input;
            }
            System.out.println("Invalid prefix. Use 2-5 uppercase letters only.");
        }
    }

    private int getValidDisplayOrder(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format.");
            }
        }
    }

    private boolean getValidBoolean(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            if ("true".equals(input) || "yes".equals(input) || "y".equals(input)) {
                return true;
            } else if ("false".equals(input) || "no".equals(input) || "n".equals(input)) {
                return false;
            }
            System.out.println("Please enter true/false or yes/no.");
        }
    }

    private void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}
