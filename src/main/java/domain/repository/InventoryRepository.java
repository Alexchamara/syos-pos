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
}