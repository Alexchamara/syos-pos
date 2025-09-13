package main.java.application.services;


import main.java.application.usecase.CheckoutCashUseCase;
import main.java.domain.inventory.StockLocation;
import main.java.domain.repository.InventoryRepository;
import main.java.infrastructure.concurrency.Tx;

import java.util.*;

public final class AvailabilityService {
    public record Shortage(String code, int required, int available) {
        public int missing() { return Math.max(0, required - available); }
    }
    public record CheckResult(boolean allAvailable, List<Shortage> shortages) {}

    private final Tx tx;
    private final InventoryRepository inv;

    public AvailabilityService(Tx tx, InventoryRepository inv) {
        this.tx = tx; this.inv = inv;
    }

    /** Check if the whole cart is available at a given location (SHELF/WEB). */
    public CheckResult checkCart(List<CheckoutCashUseCase.Item> cart, StockLocation loc) {
        Map<String,Integer> need = new HashMap<>();
        for (var it : cart) need.merge(it.code(), it.qty(), Integer::sum);

        List<Shortage> result = tx.inTx(con -> {
            List<Shortage> out = new ArrayList<>();
            for (var e : need.entrySet()) {
                String code = e.getKey();
                int required = e.getValue();
                int available = inv.totalAvailable(con, code, loc.name());
                if (available < required) out.add(new Shortage(code, required, available));
            }
            return out;
        });

        return new CheckResult(result.isEmpty(), result);
    }

    /** Available quantity for a single product at a location. */
    public int available(String productCode, StockLocation loc) {
        return tx.inTx(con -> inv.totalAvailable(con, productCode, loc.name()));
    }
}
