package main.java.domain.product;

import main.java.domain.shared.Code;
import main.java.domain.shared.Money;

public final class Product {
    private final Code code;
    private final String name;
    private final Money price;
    private final String categoryCode;

    public Product(Code code, String name, Money price) {
        this(code, name, price, null);
    }

    public Product(Code code, String name, Money price, String categoryCode) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is null or blank");
        if (price == null) throw new IllegalArgumentException("price is null");
        this.code = code;
        this.name = name.trim();
        this.price = price;
        this.categoryCode = categoryCode;
    }

    public Code code(){ return code; }
    public String name(){ return name; }
    public Money price(){ return price; }
    public String categoryCode(){ return categoryCode; }
}
