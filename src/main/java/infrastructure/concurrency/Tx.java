package main.java.infrastructure.concurrency;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;

public final class Tx {
    private final DataSource ds;
    public Tx(DataSource ds) { this.ds = ds; }

    public <T> T inTx(Function<Connection, T> work) {
        try (var con = ds.getConnection()) {
            boolean old = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                T result = work.apply(con);
                con.commit();
                return result;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            } catch (Exception e) {
                con.rollback();
                throw new RuntimeException(e);
            } finally {
                con.setAutoCommit(old);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}