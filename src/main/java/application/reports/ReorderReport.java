package main.java.application.reports;

import javax.sql.DataSource;
import java.sql.ResultSet;

public final class ReorderReport {
    private final DataSource ds;
    private final int threshold; // e.g., 50

    public ReorderReport(DataSource ds, int threshold) { this.ds = ds; this.threshold = threshold; }

    public void run() {
        String sql = """
      SELECT product_code, COALESCE(SUM(quantity),0) AS qty
      FROM batch
      WHERE UPPER(location) = 'SHELF'
      GROUP BY product_code
      HAVING qty < ?
      ORDER BY qty ASC
    """;
        try (var con = ds.getConnection(); var ps = con.prepareStatement(sql)) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n-- Low Stock Reorder List (SHELF < " + threshold + ") --");
                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-12s qty=%d%n", rs.getString("product_code"), rs.getInt("qty"));
                }
                if (!any) System.out.println("All good. No items below threshold.");
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}