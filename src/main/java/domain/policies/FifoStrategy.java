package main.java.domain.policies;

import main.java.domain.inventory.Batch;
import main.java.domain.inventory.StockLocation;
import main.java.domain.repository.InventoryRepository;
import main.java.domain.shared.Code;

import java.sql.Connection;
import java.util.List;

public final class FifoStrategy extends AbstractBatchStrategy {
    public FifoStrategy(InventoryRepository inventory) { super(inventory); }

    @Override
    protected List<Batch> candidates(Connection con, Code productCode, StockLocation location) {
        // We already fetch in (expiry NULL last, expiry asc, received_at asc) order.
        // FIFO means we ignore expiry prioritization: treat null expiry as “last” which is fine here.
        return inventory.findDeductionCandidates(con, productCode, location);
    }
}