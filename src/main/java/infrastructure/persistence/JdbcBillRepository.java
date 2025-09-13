package main.java.infrastructure.persistence;

import main.java.domain.billing.Bill;
import main.java.domain.billing.BillLine;
import main.java.domain.repository.BillRepository;

import java.sql.Connection;
import java.sql.Statement;

public final class JdbcBillRepository implements BillRepository {

    @Override
    public long save(Connection con, Bill bill) {
        try {
            var sql = """
        INSERT INTO bill(serial, date_time, subtotal_cents, discount_cents, total_cents, cash_cents, change_cents)
        VALUES(?,?,?,?,?,?,?)
      """;
            try (var ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, bill.serial());
                ps.setTimestamp(2, java.sql.Timestamp.valueOf(bill.dateTime()));
                ps.setLong(3, bill.subtotal().amount().movePointRight(2).longValueExact());
                ps.setLong(4, bill.discount().amount().movePointRight(2).longValueExact());
                ps.setLong(5, bill.total().amount().movePointRight(2).longValueExact());
                ps.setLong(6, bill.cash().amount().movePointRight(2).longValueExact());
                ps.setLong(7, bill.change().amount().movePointRight(2).longValueExact());
                ps.executeUpdate();
                try (var keys = ps.getGeneratedKeys()) {
                    keys.next();
                    long id = keys.getLong(1);
                    insertLines(con, id, bill);
                    return id;
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void insertLines(Connection con, long billId, Bill bill) throws Exception {
        var sql = """
      INSERT INTO bill_line(bill_id, product_code, name, qty, unit_price_cents, line_total_cents)
      VALUES(?,?,?,?,?,?)
    """;
        try (var ps = con.prepareStatement(sql)) {
            for (BillLine l : bill.lines()) {
                ps.setLong(1, billId);
                ps.setString(2, l.productCode().value());
                ps.setString(3, l.name());
                ps.setInt(4, l.qty().value());
                ps.setLong(5, l.unitPrice().amount().movePointRight(2).longValueExact());
                ps.setLong(6, l.lineTotal().amount().movePointRight(2).longValueExact());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}