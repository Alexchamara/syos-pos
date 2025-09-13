package main.java.domain.policies;

import main.java.domain.inventory.Batch;
import main.java.domain.inventory.StockLocation;
import main.java.domain.repository.InventoryRepository;
import main.java.domain.shared.Code;

import java.sql.Connection;
import java.util.List;

public final class FefoStrategy extends AbstractBatchStrategy {
    public FefoStrategy(InventoryRepository inventory) { super(inventory); }

    @Override
    protected List<Batch> candidates(Connection con, Code productCode, StockLocation location) {
        // Same ordered query already prioritizes earlier expiry first, then oldest received.
        return inventory.findDeductionCandidates(con, productCode, location);
    }
}