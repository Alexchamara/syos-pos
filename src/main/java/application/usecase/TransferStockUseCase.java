package main.java.application.usecase;

import main.java.domain.inventory.StockLocation;
import main.java.domain.policies.BatchSelectionStrategy;
import main.java.domain.repository.InventoryAdminRepository;
import main.java.domain.repository.InventoryRepository;
import main.java.domain.shared.Code;
import main.java.infrastructure.concurrency.Tx;

import java.time.LocalDateTime;

public final class TransferStockUseCase {
    private final Tx tx;
    private final InventoryRepository inv;
    private final InventoryAdminRepository admin;
    private final BatchSelectionStrategy pickStrategy; // FEFO or FIFO

    public TransferStockUseCase(Tx tx,
                                InventoryRepository inv,
                                InventoryAdminRepository admin,
                                BatchSelectionStrategy pickStrategy) {
        this.tx = tx; this.inv = inv; this.admin = admin; this.pickStrategy = pickStrategy;
    }

    /**
     * Transfers quantity from source to destination using the same picking strategy as sales.
     */
    public void transfer(String productCode, StockLocation from, StockLocation to, int qty) {
        // Input validation
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Product code cannot be empty");
        }

        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got: " + qty);
        }

        if (from == null) {
            throw new IllegalArgumentException("Source location cannot be null");
        }

        if (to == null) {
            throw new IllegalArgumentException("Destination location cannot be null");
        }

        if (from == to) {
            throw new IllegalArgumentException("Source and destination locations cannot be the same");
        }

        tx.inTx(con -> {
            // Check if there's sufficient stock at source location
            int availableStock = inv.totalAvailable(con, productCode, from.name());
            if (availableStock < qty) {
                throw new RuntimeException(String.format(
                    "Insufficient stock at %s. Available: %d, Requested: %d",
                    from.name(), availableStock, qty));
            }

            try {
                // 1) Deduct from source location using FEFO/FIFO
                pickStrategy.deduct(con, new Code(productCode), qty, from);

                // 2) Insert a single batch at destination
                admin.insertBatch(con, productCode, to.name(), LocalDateTime.now(), null, qty);

                // 3) Log the movement for audit trail
                logInventoryMovement(con, productCode, from, to, qty);

            } catch (Exception e) {
                throw new RuntimeException("Transfer operation failed: " + e.getMessage(), e);
            }

            return null;
        });
    }

    /**
     * Logs inventory movement for audit purposes.
     */
    private void logInventoryMovement(java.sql.Connection con, String productCode,
                                    StockLocation from, StockLocation to, int qty) {
        try (var ps = con.prepareStatement(
                "INSERT INTO inventory_movement(product_code, from_location, to_location, quantity, note, movement_time) VALUES(?,?,?,?,?,NOW())")) {
            ps.setString(1, productCode);
            ps.setString(2, from.name());
            ps.setString(3, to.name());
            ps.setInt(4, qty);
            ps.setString(5, "manual_transfer");
            ps.executeUpdate();
        } catch (Exception e) {
            // Log the error but don't fail the transfer
            System.err.println("Warning: Failed to log inventory movement: " + e.getMessage());
        }
    }
}