package main.java.domain.shared;

public final class Quantity {
    private final int value;

    public Quantity(int value) {
        if (value < 0) throw  new IllegalArgumentException("Quantity value cannot be negative");
        this.value = value;
    }

    public int value() {  return value; }
    public  Quantity minus(int x) {
        if (value - x < 0) throw new IllegalArgumentException("Insufficient quantity");
        return new Quantity(value - x);
    }

    public Quantity plus(int x) { return new Quantity(value + x); }

    @Override public String toString(){ return Integer.toString(value); }
}
