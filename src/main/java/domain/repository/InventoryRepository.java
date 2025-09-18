package main.java.domain.repository;

import main.java.domain.inventory.Batch;
import main.java.domain.inventory.StockLocation;
import main.java.domain.shared.Code;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    // FEFO/FIFO candidates by product+location ordered by (expiry asc nulls last, receivedAt asc)
    List<Batch> findDeductionCandidates(Connection con, Code product, StockLocation loc);
    void deductFromBatch(Connection con, long batchId, int take);

    // Sum of quantities for product at location (e.g., SHELF / WEB)
    int totalAvailable(Connection con, String productCode, String location);

    // Transfer stock from one location to another
    void transferStock(Connection con, String productCode, StockLocation fromLocation, StockLocation toLocation, int quantity);

    // Convenience: true if available >= required.
    default boolean hasAtLeast(Connection con, String productCode, String location, int required) {
        return totalAvailable(con, productCode, location) >= required;
    }

    // Reminder for low quantity
    int remainingQuantity(java.sql.Connection con, String productCode, String location);

    // Batch CRUD operations
    List<Batch> findAllBatches(Connection con);
    List<Batch> findBatchesByProduct(Connection con, Code productCode);
    List<Batch> findBatchesByLocation(Connection con, StockLocation location);
    Optional<Batch> findBatchById(Connection con, long batchId);

    long createBatch(Connection con, Code productCode, StockLocation location,
                     LocalDateTime receivedAt, LocalDate expiry, int quantity);

    void updateBatch(Connection con, long batchId, LocalDate expiry, int quantity);

    void deleteBatch(Connection con, long batchId);

    boolean batchExists(Connection con, long batchId);
}