package main.java.infrastructure.persistence;

import main.java.domain.repository.ShortageEventRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public final class JdbcShortageEventRepository implements ShortageEventRepository {
    private final DataSource ds;
    public JdbcShortageEventRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public void save(Connection con, String message) {
        String sql = "INSERT INTO notify_shortage(message) VALUES (?)";
        try (var ps = con.prepareStatement(sql)) {
            ps.setString(1, message);
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public List<String> list(Connection con) {
        String sql = "SELECT created_at, message FROM notify_shortage ORDER BY created_at DESC";
        try (var ps = con.prepareStatement(sql)) {
            try (var rs = ps.executeQuery()) {
                List<String> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(rs.getTimestamp("created_at") + " | " + rs.getString("message"));
                }
                return out;
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public void clear(Connection con) {
        try (var ps = con.prepareStatement("DELETE FROM notify_shortage")) {
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
