package main.java.domain.policies;

import main.java.domain.inventory.Batch;
import main.java.domain.inventory.StockLocation;
import main.java.domain.repository.InventoryRepository;
import main.java.domain.shared.Code;

import java.sql.Connection;
import java.util.List;

abstract class AbstractBatchStrategy implements BatchSelectionStrategy {
    protected final InventoryRepository inventory;

    protected AbstractBatchStrategy(InventoryRepository inventory) {
        this.inventory = inventory;
    }

    @Override
    public void deduct(Connection con, Code productCode, int qtyNeeded, StockLocation location) {
        int remaining = qtyNeeded;
        for (Batch b : candidates(con, productCode, location)) {
            if (remaining <= 0) break;
            int take = Math.min(remaining, b.quantity().value());
            if (take > 0) {
                inventory.deductFromBatch(con, b.id(), take);
                remaining -= take;
            }
        }
        if (remaining > 0) {
            throw new IllegalStateException("Insufficient stock for " + productCode.value() + " need=" + qtyNeeded);
        }
    }

    protected abstract List<Batch> candidates(Connection con, Code productCode, StockLocation location);
}