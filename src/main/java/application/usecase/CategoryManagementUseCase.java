package main.java.application.usecase;

import main.java.domain.product.Category;
import main.java.domain.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

public final class CategoryManagementUseCase {
    private final CategoryRepository categoryRepository;

    public CategoryManagementUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllActiveCategories() {
        return categoryRepository.findAllActive();
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Optional<Category> findCategory(String code) {
        return categoryRepository.findByCode(code);
    }

    public CreateCategoryResult createCategory(CreateCategoryRequest request) {
        // Check if category already exists
        Optional<Category> existing = categoryRepository.findByCode(request.code);

        Category category = new Category(
            request.code,
            request.name,
            request.description,
            request.prefix,
            1, // Start sequence at 1
            request.displayOrder,
            true
        );

        categoryRepository.save(category);

        return existing.isPresent() ? CreateCategoryResult.UPDATED : CreateCategoryResult.SUCCESS;
    }

    public void updateCategory(UpdateCategoryRequest request) {
        Category existingCategory = categoryRepository.findByCode(request.code)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.code));

        Category updatedCategory = new Category(
            request.code,
            request.name != null ? request.name : existingCategory.name(),
            request.description != null ? request.description : existingCategory.description(),
            existingCategory.prefix(), // Prefix should not change to maintain product code consistency
            existingCategory.nextSequence(),
            request.displayOrder != null ? request.displayOrder : existingCategory.displayOrder(),
            request.active != null ? request.active : existingCategory.active()
        );

        categoryRepository.save(updatedCategory);
    }

    public void deleteCategory(String code) {
        categoryRepository.delete(code);
    }

    public String generateProductCode(String categoryCode) {
        Category category = categoryRepository.findByCode(categoryCode)
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryCode));

        String productCode = category.generateNextProductCode();
        categoryRepository.incrementSequenceAndSave(categoryCode);

        return productCode;
    }

    public record CreateCategoryRequest(
        String code,
        String name,
        String description,
        String prefix,
        int displayOrder
    ) {}

    public record UpdateCategoryRequest(
        String code,
        String name,
        String description,
        Integer displayOrder,
        Boolean active
    ) {}

    public enum CreateCategoryResult {
        SUCCESS, UPDATED
    }
}
