package main.java.infrastructure.persistence;

import main.java.domain.repository.InventoryAdminRepository;

import java.sql.*;

public final class JdbcInventoryAdminRepository implements InventoryAdminRepository {
    @Override
    public long insertBatch(Connection con, String code, String loc,
                            java.time.LocalDateTime receivedAt, java.time.LocalDate expiry, int qty) {
        String sql = "INSERT INTO batch(product_code,location,received_at,expiry,quantity) VALUES(?,?,?,?,?)";
        try (var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code.toUpperCase());
            ps.setString(2, loc);
            ps.setTimestamp(3, Timestamp.valueOf(receivedAt));
            if (expiry == null) ps.setNull(4, Types.DATE); else ps.setDate(4, Date.valueOf(expiry));
            ps.setInt(5, qty);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()){ keys.next(); return keys.getLong(1); }
        } catch (Exception e){ throw new RuntimeException(e); }
    }
}