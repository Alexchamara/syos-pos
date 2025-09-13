package main.java.infrastructure.persistence;

import main.java.domain.inventory.Batch;
import main.java.domain.inventory.StockLocation;
import main.java.domain.repository.InventoryRepository;
import main.java.domain.shared.Code;
import main.java.domain.shared.Quantity;

import java.sql.Connection;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class JdbcInventoryRepository implements InventoryRepository {

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
}