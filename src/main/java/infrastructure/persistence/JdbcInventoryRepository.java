package main.java.infrastructure.persistence;

import main.java.domain.inventory.Batch;
import main.java.domain.inventory.StockLocation;
import main.java.domain.repository.InventoryRepository;
import main.java.domain.shared.Code;
import main.java.domain.shared.Quantity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class JdbcInventoryRepository implements InventoryRepository {

    private final DataSource dataSource;

    public JdbcInventoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Batch> findDeductionCandidates(Connection con, Code product, StockLocation loc) {
        String sql = """
      SELECT id, product_code, location, received_at, expiry, quantity
      FROM batch
      WHERE product_code=? AND location=? AND quantity>0
      ORDER BY (CASE WHEN expiry IS NULL THEN 1 ELSE 0 END), expiry ASC, received_at ASC
    """;
        try (var ps = con.prepareStatement(sql)) {
            ps.setString(1, product.value());
            ps.setString(2, loc.name());
            try (var rs = ps.executeQuery()) {
                List<Batch> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Batch(
                            rs.getLong("id"),
                            new Code(rs.getString("product_code")),
                            StockLocation.valueOf(rs.getString("location")),
                            rs.getTimestamp("received_at").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                            rs.getDate("expiry") == null ? null : rs.getDate("expiry").toLocalDate(),
                            new Quantity(rs.getInt("quantity"))
                    ));
                }
                return out;
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public void deductFromBatch(Connection con, long batchId, int take) {
        String sql = "UPDATE batch SET quantity = quantity - ?, version = version + 1 WHERE id=? AND quantity >= ?";
        try (var ps = con.prepareStatement(sql)) {
            ps.setInt(1, take);
            ps.setLong(2, batchId);
            ps.setInt(3, take);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new IllegalStateException("Concurrent update or insufficient qty for batch " + batchId);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public int totalAvailable(Connection con, String productCode, String location) {
        String sql = "SELECT COALESCE(SUM(quantity),0) AS q " +
                "FROM batch WHERE product_code=? AND location=?";
        try (var ps = con.prepareStatement(sql)) {
            ps.setString(1, productCode);
            ps.setString(2, location);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("q");
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public void transferStock(Connection con, String productCode, StockLocation fromLocation, StockLocation toLocation, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be positive");
        }

        // Get batches from source location using FEFO/FIFO strategy
        List<Batch> candidates = findDeductionCandidates(con, new Code(productCode), fromLocation);

        int remaining = quantity;
        for (Batch batch : candidates) {
            if (remaining <= 0) break;

            int available = batch.quantity().value();
            int toTransfer = Math.min(remaining, available);

            // Deduct from source batch
            deductFromBatch(con, batch.id(), toTransfer);

            // Create new batch in destination location with current timestamp to avoid unique constraint issues
            String insertSql = """
                INSERT INTO batch (product_code, location, received_at, expiry, quantity, version)
                VALUES (?, ?, ?, ?, ?, 0)
                """;

            try (var ps = con.prepareStatement(insertSql)) {
                ps.setString(1, productCode);
                ps.setString(2, toLocation.name());
                ps.setTimestamp(3, java.sql.Timestamp.valueOf(LocalDateTime.now())); // Use current time for transfer
                if (batch.expiry() != null) {
                    ps.setDate(4, java.sql.Date.valueOf(batch.expiry()));
                } else {
                    ps.setDate(4, null);
                }
                ps.setInt(5, toTransfer);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create destination batch", e);
            }

            remaining -= toTransfer;
        }

        if (remaining > 0) {
            throw new IllegalStateException("Insufficient stock in " + fromLocation +
                " to transfer " + quantity + " of " + productCode + ". Missing: " + remaining);
        }
    }
}