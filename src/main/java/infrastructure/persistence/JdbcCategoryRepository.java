package main.java.infrastructure.persistence;

import main.java.domain.product.Category;
import main.java.domain.repository.CategoryRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcCategoryRepository implements CategoryRepository {
    private final DataSource dataSource;

    public JdbcCategoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Category> findAllActive() {
        String sql = "SELECT code, name, description, prefix, next_sequence, display_order, active " +
                    "FROM category WHERE active = TRUE ORDER BY display_order ASC";

        return executeQuery(sql);
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT code, name, description, prefix, next_sequence, display_order, active " +
                    "FROM category ORDER BY display_order ASC";

        return executeQuery(sql);
    }

    @Override
    public Optional<Category> findByCode(String code) {
        String sql = "SELECT code, name, description, prefix, next_sequence, display_order, active " +
                    "FROM category WHERE code = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapToCategory(rs));
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find category by code: " + code, e);
        }
    }

    @Override
    public void save(Category category) {
        String sql = "INSERT INTO category (code, name, description, prefix, next_sequence, display_order, active) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "name = VALUES(name), description = VALUES(description), " +
                    "prefix = VALUES(prefix), next_sequence = VALUES(next_sequence), " +
                    "display_order = VALUES(display_order), active = VALUES(active)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category.code());
            stmt.setString(2, category.name());
            stmt.setString(3, category.description());
            stmt.setString(4, category.prefix());
            stmt.setInt(5, category.nextSequence());
            stmt.setInt(6, category.displayOrder());
            stmt.setBoolean(7, category.active());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save category: " + category.code(), e);
        }
    }

    @Override
    public void delete(String code) {
        String sql = "UPDATE category SET active = FALSE WHERE code = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            int updated = stmt.executeUpdate();

            if (updated == 0) {
                throw new RuntimeException("Category not found: " + code);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete category: " + code, e);
        }
    }

    @Override
    public Category incrementSequenceAndSave(String categoryCode) {
        String sql = "UPDATE category SET next_sequence = next_sequence + 1 WHERE code = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoryCode);
            int updated = stmt.executeUpdate();

            if (updated == 0) {
                throw new RuntimeException("Category not found: " + categoryCode);
            }

            return findByCode(categoryCode)
                .orElseThrow(() -> new RuntimeException("Category not found after update: " + categoryCode));

        } catch (SQLException e) {
            throw new RuntimeException("Failed to increment sequence for category: " + categoryCode, e);
        }
    }

    private List<Category> executeQuery(String sql) {
        List<Category> categories = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categories.add(mapToCategory(rs));
            }

            return categories;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute category query", e);
        }
    }

    private Category mapToCategory(ResultSet rs) throws SQLException {
        return new Category(
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("prefix"),
            rs.getInt("next_sequence"),
            rs.getInt("display_order"),
            rs.getBoolean("active")
        );
    }
}
