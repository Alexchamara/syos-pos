package main.java.domain.repository;

import main.java.domain.inventory.Batch;
import main.java.domain.inventory.StockLocation;
import main.java.domain.shared.Code;

import java.sql.Connection;
import java.util.List;

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
}