package main.java.infrastructure.persistence;

import main.java.domain.product.Product;
import main.java.domain.repository.ProductRepository;
import main.java.domain.shared.Code;
import main.java.domain.shared.Money;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.Optional;

public final class JdbcProductRepository implements ProductRepository {
    private final DataSource ds;
    public JdbcProductRepository(DataSource ds){ this.ds = ds; }

    @Override public void upsert(Product p) {
        String sql = """
      INSERT INTO product(code,name,price_cents) VALUES(?,?,?)
      ON DUPLICATE KEY UPDATE name=VALUES(name), price_cents=VALUES(price_cents)
    """;
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, p.code().value());
            ps.setString(2, p.name());
            ps.setLong(3, p.price().amount().movePointRight(2).longValueExact());
            ps.executeUpdate();
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override public Optional<Product> findByCode(Code code) {
        String sql = "SELECT code,name,price_cents FROM product WHERE code=?";
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setString(1, code.value());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                var price = Money.of(rs.getLong("price_cents"));
                return Optional.of(new Product(new Code(rs.getString("code")), rs.getString("name"), price));
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}