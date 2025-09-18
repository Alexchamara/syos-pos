package main.java.application.services;

import java.math.BigDecimal;

/**
 * Validation service following Single Responsibility Principle
 * Provides centralized validation logic for product-related operations
 */
public final class ProductValidationService {

    public static void validateProductCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Product code cannot be null or blank");
        }

        String trimmedCode = code.trim();
        if (trimmedCode.length() > 20) {
            throw new IllegalArgumentException("Product code cannot exceed 20 characters");
        }

        if (!trimmedCode.matches("^[A-Za-z0-9_-]+$")) {
            throw new IllegalArgumentException("Product code can only contain letters, numbers, underscores, and hyphens");
        }
    }

    public static void validateProductName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }

        String trimmedName = name.trim();
        if (trimmedName.length() > 100) {
            throw new IllegalArgumentException("Product name cannot exceed 100 characters");
        }
    }

    public static void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Product price cannot be null");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be positive");
        }

        if (price.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new IllegalArgumentException("Product price cannot exceed $999,999.99");
        }

        // Check for reasonable decimal places (max 2)
        if (price.scale() > 2) {
            throw new IllegalArgumentException("Product price cannot have more than 2 decimal places");
        }
    }
}
