package main.java.application.services;


import main.java.application.usecase.CheckoutCashUseCase;
import main.java.domain.inventory.StockLocation;
import main.java.domain.repository.InventoryRepository;
import main.java.infrastructure.concurrency.Tx;

import java.util.*;

public final class AvailabilityService {
    private final Tx tx;
    private final InventoryRepository inv;

    public AvailabilityService(Tx tx, InventoryRepository inv) {
        this.tx = tx; this.inv = inv;
    }

    /** Available quantity for a single product at a location. */
    public int available(String productCode, StockLocation loc) {
        return tx.inTx(con -> inv.totalAvailable(con, productCode, loc.name()));
    }

    /** Transfer stock between any two locations. */
    public void transferStock(String productCode, StockLocation fromLocation, StockLocation toLocation, int quantity) {
        tx.inTx(con -> {
            inv.transferStock(con, productCode, fromLocation, toLocation, quantity);
            return null;
        });
    }

    /** Get availability across all stock locations for a product. */
    public Map<StockLocation, Integer> getAvailabilityAcrossAllLocations(String productCode) {
        return tx.inTx(con -> {
            Map<StockLocation, Integer> availability = new HashMap<>();
            for (StockLocation location : StockLocation.values()) {
                int qty = inv.totalAvailable(con, productCode, location.name());
                availability.put(location, qty);
            }
            return availability;
        });
    }
}
