package main.java.application.usecase;

import main.java.domain.inventory.Batch;
import main.java.domain.inventory.StockLocation;
import main.java.domain.repository.InventoryRepository;
import main.java.domain.repository.ProductRepository;
import main.java.domain.shared.Code;
import main.java.infrastructure.concurrency.Tx;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class BatchManagementUseCase {

    private final Tx tx;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public BatchManagementUseCase(DataSource dataSource, InventoryRepository inventoryRepository, ProductRepository productRepository) {
        this.tx = new Tx(dataSource);
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    // ProductManagementUseCase pattern
    public static final class CreateBatchRequest {
        private final String productCode;
        private final StockLocation location;
        private final LocalDate expiry;
        private final int quantity;

        public CreateBatchRequest(String productCode, StockLocation location, LocalDate expiry, int quantity) {
            this.productCode = productCode;
            this.location = location;
            this.expiry = expiry;
            this.quantity = quantity;
        }

        public String productCode() { return productCode; }
        public StockLocation location() { return location; }
        public LocalDate expiry() { return expiry; }
        public int quantity() { return quantity; }
    }

    public static final class UpdateBatchRequest {
        private final long batchId;
        private final LocalDate expiry;
        private final int quantity;

        public UpdateBatchRequest(long batchId, LocalDate expiry, int quantity) {
            this.batchId = batchId;
            this.expiry = expiry;
            this.quantity = quantity;
        }

        public long batchId() { return batchId; }
        public LocalDate expiry() { return expiry; }
        public int quantity() { return quantity; }
    }

    // ProductManagementUseCase pattern
    public static final class BatchInfo {
        private final long id;
        private final String productCode;
        private final StockLocation location;
        private final LocalDateTime receivedAt;
        private final LocalDate expiry;
        private final int quantity;

        public BatchInfo(Batch batch) {
            this.id = batch.id();
            this.productCode = batch.productCode().value();
            this.location = batch.location();
            this.receivedAt = batch.receivedAt();
            this.expiry = batch.expiry();
            this.quantity = batch.quantity().value();
        }

        public long id() { return id; }
        public String productCode() { return productCode; }
        public StockLocation location() { return location; }
        public LocalDateTime receivedAt() { return receivedAt; }
        public LocalDate expiry() { return expiry; }
        public int quantity() { return quantity; }
    }

    // enums
    public enum CreateResult {
        SUCCESS,
        PRODUCT_NOT_EXISTS,
        INVALID_INPUT
    }

    public enum UpdateResult {
        SUCCESS,
        NOT_FOUND,
        INVALID_INPUT
    }

    public enum DeleteResult {
        SUCCESS,
        NOT_FOUND,
        INVALID_INPUT
    }

    /**
     * Create a new batch
     */
    public CreateResult createBatch(CreateBatchRequest request) {
        try {
            // Basic validation
            validateCreateRequest(request);

            return tx.inTx(con -> {
                // Validate product exists
                var product = productRepository.findByCode(new Code(request.productCode()));
                if (product.isEmpty()) {
                    return CreateResult.PRODUCT_NOT_EXISTS;
                }

                inventoryRepository.createBatch(
                    con,
                    new Code(request.productCode()),
                    request.location(),
                    LocalDateTime.now(),
                    request.expiry(),
                    request.quantity()
                );

                return CreateResult.SUCCESS;
            });
        } catch (IllegalArgumentException e) {
            return CreateResult.INVALID_INPUT;
        }
    }

    /**
     * Update an existing batch
     */
    public UpdateResult updateBatch(UpdateBatchRequest request) {
        try {
            // Basic validation
            if (request.batchId() <= 0) {
                throw new IllegalArgumentException("Batch ID must be positive");
            }
            if (request.quantity() < 0) {
                throw new IllegalArgumentException("Quantity cannot be negative");
            }
            if (request.expiry() != null && request.expiry().isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("Expiry date cannot be in the past");
            }

            return tx.inTx(con -> {
                // Check if batch exists
                if (!inventoryRepository.batchExists(con, request.batchId())) {
                    return UpdateResult.NOT_FOUND;
                }

                inventoryRepository.updateBatch(con, request.batchId(), request.expiry(), request.quantity());
                return UpdateResult.SUCCESS;
            });
        } catch (IllegalArgumentException e) {
            return UpdateResult.INVALID_INPUT;
        }
    }

    /**
     * Delete a batch
     */
    public DeleteResult deleteBatch(long batchId) {
        try {
            if (batchId <= 0) {
                throw new IllegalArgumentException("Batch ID must be positive");
            }

            return tx.inTx(con -> {
                if (!inventoryRepository.batchExists(con, batchId)) {
                    return DeleteResult.NOT_FOUND;
                }

                inventoryRepository.deleteBatch(con, batchId);
                return DeleteResult.SUCCESS;
            });
        } catch (IllegalArgumentException e) {
            return DeleteResult.INVALID_INPUT;
        }
    }

    /**
     * Find batch by ID
     */
    public Optional<BatchInfo> findBatch(long batchId) {
        if (batchId <= 0) {
            return Optional.empty();
        }

        try {
            return tx.inTx(con ->
                inventoryRepository.findBatchById(con, batchId)
                    .map(BatchInfo::new)
            );
        } catch (Exception e) {
            System.err.println("Error finding batch: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * List all batches
     */
    public List<BatchInfo> listAllBatches() {
        try {
            return tx.inTx(con ->
                inventoryRepository.findAllBatches(con)
                    .stream()
                    .map(BatchInfo::new)
                    .toList()
            );
        } catch (Exception e) {
            System.err.println("Error listing all batches: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * List batches by product code
     */
    public List<BatchInfo> listBatchesByProduct(String productCode) {
        try {
            if (productCode == null || productCode.isBlank()) {
                return List.of();
            }

            return tx.inTx(con ->
                inventoryRepository.findBatchesByProduct(con, new Code(productCode.trim().toUpperCase()))
                    .stream()
                    .map(BatchInfo::new)
                    .toList()
            );
        } catch (Exception e) {
            System.err.println("Error listing batches by product '" + productCode + "': " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * List batches by location
     */
    public List<BatchInfo> listBatchesByLocation(StockLocation location) {
        try {
            if (location == null) {
                return List.of();
            }

            return tx.inTx(con ->
                inventoryRepository.findBatchesByLocation(con, location)
                    .stream()
                    .map(BatchInfo::new)
                    .toList()
            );
        } catch (Exception e) {
            System.err.println("Error listing batches by location '" + location + "': " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Check if product exists for validation
     */
    public boolean productExists(String productCode) {
        try {
            if (productCode == null || productCode.isBlank()) {
                return false;
            }
            return productRepository.findByCode(new Code(productCode.trim().toUpperCase())).isPresent();
        } catch (Exception e) {
            System.err.println("Error checking product existence: " + e.getMessage());
            return false;
        }
    }

    // Private validation method
    private void validateCreateRequest(CreateBatchRequest request) {
        if (request.productCode() == null || request.productCode().isBlank()) {
            throw new IllegalArgumentException("Product code cannot be null or blank");
        }
        if (request.location() == null) {
            throw new IllegalArgumentException("Stock location cannot be null");
        }
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (request.expiry() != null && request.expiry().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiry date cannot be in the past");
        }
    }
}
