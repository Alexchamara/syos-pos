package main.java.infrastructure.persistence;

import main.java.domain.repository.UserRepository;
import main.java.domain.user.Role;
import main.java.domain.user.User;

import javax.sql.DataSource;
import java.util.Optional;

public final class JdbcUserRepository implements UserRepository {
    private final DataSource ds;
    public JdbcUserRepository(DataSource ds){ this.ds = ds; }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, email, role FROM users WHERE username=?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("email"),
                        Role.valueOf(rs.getString("role"))
                ));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    public void upsert(User u) {
        String sql = """
        INSERT INTO users(username,password_hash,email,role) VALUES(?,?,?,?)
        ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash), email=VALUES(email), role=VALUES(role)
    """;
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, u.username());
            ps.setString(2, u.passwordHash());
            ps.setString(3, u.email());
            ps.setString(4, u.role().name());
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}