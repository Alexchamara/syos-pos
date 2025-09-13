package main.java.application.services;

import main.java.infrastructure.concurrency.Tx;

import java.sql.ResultSet;

public final class BillNumberService {
    private final Tx tx;

    public BillNumberService(Tx tx) { this.tx = tx; }

    /** Returns next serial like C-000001 for the given scope (COUNTER -> 'C'). */
    public String next(String scope) {
        return tx.inTx(con -> {
            try (var ps = con.prepareStatement(
                    "SELECT next_val FROM bill_number WHERE scope=? FOR UPDATE")) {
                ps.setString(1, scope);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalStateException("Unknown scope: " + scope);
                    long current = rs.getLong(1);
                    long next = current + 1;

                    try (var up = con.prepareStatement(
                            "UPDATE bill_number SET next_val=? WHERE scope=?")) {
                        up.setLong(1, next);
                        up.setString(2, scope);
                        up.executeUpdate();
                    }

                    String prefix = scope.isBlank() ? "X" : scope.substring(0,1).toUpperCase();
                    return String.format("%s-%06d", prefix, current);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}