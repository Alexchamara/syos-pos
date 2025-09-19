package main.java.infrastructure.persistence;

import main.java.domain.product.Product;
import main.java.domain.repository.ProductRepository;
import main.java.domain.shared.Code;
import main.java.domain.shared.Money;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcProductRepository implements ProductRepository {
    private final DataSource ds;
    public JdbcProductRepository(DataSource ds){ this.ds = ds; }

    @Override public void upsert(Product p) {
        String sql = """
      INSERT INTO product(code,name,price_cents,category_code) VALUES(?,?,?,?)
      ON DUPLICATE KEY UPDATE name=VALUES(name), price_cents=VALUES(price_cents), category_code=VALUES(category_code)
    """;
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, p.code().value());
            ps.setString(2, p.name());
            ps.setLong(3, p.price().amount().movePointRight(2).longValueExact());
            ps.setString(4, p.categoryCode());
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public Optional<Product> findByCode(Code code) {
        String sql = "SELECT code,name,price_cents,category_code FROM product WHERE code=?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, code.value());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                var price = Money.of(rs.getLong("price_cents"));
                var categoryCode = rs.getString("category_code");
                return Optional.of(new Product(new Code(rs.getString("code")), rs.getString("name"), price, categoryCode));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public List<Product> findAll() {
        String sql = "SELECT code,name,price_cents,category_code FROM product ORDER BY code";
        List<Product> products = new ArrayList<>();
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var price = Money.of(rs.getLong("price_cents"));
                    var categoryCode = rs.getString("category_code");
                    products.add(new Product(new Code(rs.getString("code")), rs.getString("name"), price, categoryCode));
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        return products;
    }

    @Override public boolean deleteByCode(Code code) {
        String sql = "DELETE FROM product WHERE code=?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, code.value());
            return ps.executeUpdate() > 0;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}