package main.java.application.services;

import main.java.domain.inventory.StockLocation;
import main.java.domain.repository.InventoryRepository;
import main.java.infrastructure.concurrency.Tx;

/**
 * Service responsible for main store inventory operations.
 */
public final class MainStoreService {

    private final Tx tx;
    private final InventoryRepository inventoryRepository;

    public MainStoreService(Tx tx, InventoryRepository inventoryRepository) {
        this.tx = tx;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Get available quantity in main store for a specific product.
     */
    public int getAvailableQuantity(String productCode) {
        return tx.inTx(con ->
            inventoryRepository.totalAvailable(con, productCode, StockLocation.MAIN_STORE.name())
        );
    }
}
