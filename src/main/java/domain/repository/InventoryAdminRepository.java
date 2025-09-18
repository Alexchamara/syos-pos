package main.java.domain.repository;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface InventoryAdminRepository {
    long insertBatch(Connection con, String productCode, String location,
                     LocalDateTime receivedAt, LocalDate expiry, int qty);
}