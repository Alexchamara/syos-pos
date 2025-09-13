package main.java.domain.shared;

import java.util.Objects;

public final class Code {
    private final String value;

    public Code(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Product Code cannot be null or blank");
        this.value = value.trim();
    }

    public String value() { return value; }

    @Override public boolean equals(Object o){ return (o instanceof Code c) && value.equalsIgnoreCase(c.value); }
    @Override public int hashCode(){ return Objects.hash(value.toLowerCase()); }
    @Override public String toString(){ return value; }
}
