package main.java.domain.shared;

import java.math.BigDecimal;
import java.util.Objects;

public final class Money {
    private final BigDecimal amount;

    public static Money of(long cents) { return new Money(BigDecimal.valueOf(cents, 2)); }
    public static Money of(BigDecimal amount) { return new Money(amount); }

    private Money(BigDecimal amount) {
        this.amount = amount.stripTrailingZeros();
    }

    public BigDecimal amount() { return amount; }
    public long cents() { return amount.multiply(BigDecimal.valueOf(100)).longValue(); }
    public Money plus(Money other) { return new Money(this.amount.add(other.amount)); }
    public Money minus(Money other) { return new Money(this.amount.subtract(other.amount)); }
    public Money times(int n) { return new Money(this.amount.multiply(BigDecimal.valueOf(n))); }

    @Override public boolean equals(Object o) {
        return (o instanceof Money m) && amount.compareTo(m.amount)==0;
    }
    @Override public int hashCode() { return Objects.hash(amount); }
    @Override public String toString() { return amount.toPlainString(); }

}
