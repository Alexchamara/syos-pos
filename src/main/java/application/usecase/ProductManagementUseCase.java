package main.java.application.usecase;

import main.java.domain.product.Product;
import main.java.domain.repository.ProductRepository;
import main.java.domain.shared.Code;
import main.java.domain.shared.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public final class ProductManagementUseCase {
    private final ProductRepository productRepository;
    private final CategoryManagementUseCase categoryUseCase;

    public ProductManagementUseCase(ProductRepository productRepository, CategoryManagementUseCase categoryUseCase) {
        this.productRepository = productRepository;
        this.categoryUseCase = categoryUseCase;
    }

    public static final class CreateProductRequest {
        private final String code;
        private final String name;
        private final BigDecimal price;

        public CreateProductRequest(String code, String name, BigDecimal price) {
            this.code = code;
            this.name = name;
            this.price = price;
        }

        public String code() { return code; }
        public String name() { return name; }
        public BigDecimal price() { return price; }
    }

    public static final class CreateProductWithCategoryRequest {
        private final String categoryCode;
        private final String name;
        private final BigDecimal price;

        public CreateProductWithCategoryRequest(String categoryCode, String name, BigDecimal price) {
            this.categoryCode = categoryCode;
            this.name = name;
            this.price = price;
        }

        public String categoryCode() { return categoryCode; }
        public String name() { return name; }
        public BigDecimal price() { return price; }
    }

    public static final class ProductInfo {
        private final String code;
        private final String name;
        private final BigDecimal price;

        public ProductInfo(Product product) {
            this.code = product.code().value();
            this.name = product.name();
            this.price = product.price().amount();
        }

        public String code() { return code; }
        public String name() { return name; }
        public BigDecimal price() { return price; }
    }

    public enum CreateResult {
        SUCCESS,
        UPDATED
    }

    public enum DeleteResult {
        SUCCESS,
        NOT_FOUND
    }

    public CreateResult createProduct(CreateProductRequest request) {
        validateCreateRequest(request);

        Code code = new Code(request.code());
        Optional<Product> existingProduct = productRepository.findByCode(code);

        Product product = new Product(
            code,
            request.name(),
            Money.of(request.price())
        );

        productRepository.upsert(product);

        return existingProduct.isPresent() ? CreateResult.UPDATED : CreateResult.SUCCESS;
    }

    public CreateProductWithCategoryResult createProductWithCategory(CreateProductWithCategoryRequest request) {
        validateCreateWithCategoryRequest(request);

        // Generate product code automatically based on category
        String generatedCode = categoryUseCase.generateProductCode(request.categoryCode());

        Code code = new Code(generatedCode);
        Product product = new Product(
            code,
            request.name(),
            Money.of(request.price()),
            request.categoryCode()  // Pass the category code to the Product constructor
        );

        productRepository.upsert(product);

        return new CreateProductWithCategoryResult(CreateResult.SUCCESS, generatedCode);
    }

    public Optional<ProductInfo> findProduct(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Product code cannot be null or blank");
        }

        return productRepository.findByCode(new Code(code))
            .map(ProductInfo::new);
    }

    public List<ProductInfo> listAllProducts() {
        return productRepository.findAll()
            .stream()
            .map(ProductInfo::new)
            .toList();
    }

    public DeleteResult deleteProduct(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Product code cannot be null or blank");
        }

        boolean deleted = productRepository.deleteByCode(new Code(code));
        return deleted ? DeleteResult.SUCCESS : DeleteResult.NOT_FOUND;
    }

    private void validateCreateRequest(CreateProductRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new IllegalArgumentException("Product code cannot be null or blank");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be positive");
        }
    }

    private void validateCreateWithCategoryRequest(CreateProductWithCategoryRequest request) {
        if (request.categoryCode() == null || request.categoryCode().isBlank()) {
            throw new IllegalArgumentException("Category code cannot be null or blank");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Product name cannot be null or blank");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Product price must be positive");
        }
    }

    public static final class CreateProductWithCategoryResult {
        private final CreateResult result;
        private final String generatedCode;

        public CreateProductWithCategoryResult(CreateResult result, String generatedCode) {
            this.result = result;
            this.generatedCode = generatedCode;
        }

        public CreateResult result() { return result; }
        public String generatedCode() { return generatedCode; }
    }
}
