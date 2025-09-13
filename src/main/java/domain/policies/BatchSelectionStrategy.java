package main.java.domain.policies;

import main.java.domain.inventory.StockLocation;
import main.java.domain.shared.Code;

import java.sql.Connection;

public interface BatchSelectionStrategy {
    /** Deducts quantity for product at the given location using the strategy’s order. **/
    void deduct(Connection con, Code productCode, int quantity, StockLocation location);
}