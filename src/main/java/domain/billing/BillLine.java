package main.java.domain.billing;

import main.java.domain.shared.Code;
import main.java.domain.shared.Money;
import main.java.domain.shared.Quantity;

public final class BillLine {
    private final Code productCode;
    private final String name;
    private final Quantity qty;
    private final Money unitPrice;

    public BillLine(Code productCode, String name, Quantity qty, Money unitPrice) {
        this.productCode = productCode; this.name = name; this.qty = qty; this.unitPrice = unitPrice;
    }
    public Code productCode(){ return productCode; }
    public String name(){ return name; }
    public Quantity qty(){ return qty; }
    public Money unitPrice(){ return unitPrice; }
    public Money lineTotal(){ return unitPrice.times(qty.value()); }
}