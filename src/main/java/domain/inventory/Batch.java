package main.java.domain.inventory;

import main.java.domain.shared.Code;
import main.java.domain.shared.Quantity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Batch {
    private final long id;
    private final Code productCode;
    private final StockLocation location;
    private final LocalDateTime receivedAt;
    private final LocalDate expiry;
    private final Quantity quantity;

    public Batch(long id, Code productCode, StockLocation location, LocalDateTime receivedAt, LocalDate expiry, Quantity quantity) {
        this.id = id;
        this.productCode = productCode;
        this.location = location;
        this.receivedAt = receivedAt;
        this.expiry = expiry;
        this.quantity = quantity;
    }

    public long id(){  return id; }
    public Code productCode(){ return productCode; }
    public StockLocation location(){ return location; }
    public LocalDateTime receivedAt(){ return receivedAt; }
    public LocalDate expiry(){ return expiry; }
    public Quantity quantity(){ return quantity; }
}
