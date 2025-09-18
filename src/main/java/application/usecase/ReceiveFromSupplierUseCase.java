package main.java.application.usecase;

import main.java.domain.inventory.StockLocation;
import main.java.domain.repository.InventoryAdminRepository;
import main.java.infrastructure.concurrency.Tx;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ReceiveFromSupplierUseCase {
    private final Tx tx;
    private final InventoryAdminRepository repo;

    public ReceiveFromSupplierUseCase(Tx tx, InventoryAdminRepository repo) {
        this.tx = tx; this.repo = repo;
    }

    /** Receive a fresh batch into MAIN store. */
    public long receive(String productCode, int qty, LocalDate expiry) {
        if (qty <= 0) throw new IllegalArgumentException("qty must be > 0");
        return tx.inTx(con ->
                repo.insertBatch(con, productCode, StockLocation.MAIN_STORE.name(),
                        LocalDateTime.now(), expiry, qty));
    }
}