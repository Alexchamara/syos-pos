package main.java.domain.product;

public final class Category {
    private final String code;
    private final String name;
    private final String description;
    private final String prefix;
    private final int nextSequence;
    private final int displayOrder;
    private final boolean active;

    public Category(String code, String name, String description, String prefix,
                   int nextSequence, int displayOrder, boolean active) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("code is null or blank");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name is null or blank");
        if (prefix == null || prefix.isBlank()) throw new IllegalArgumentException("prefix is null or blank");

        this.code = code.trim();
        this.name = name.trim();
        this.description = description != null ? description.trim() : null;
        this.prefix = prefix.trim().toUpperCase();
        this.nextSequence = nextSequence;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public String code() { return code; }
    public String name() { return name; }
    public String description() { return description; }
    public String prefix() { return prefix; }
    public int nextSequence() { return nextSequence; }
    public int displayOrder() { return displayOrder; }
    public boolean active() { return active; }

    public String generateNextProductCode() {
        return String.format("%s%03d", prefix, nextSequence);
    }

    public Category withIncrementedSequence() {
        return new Category(code, name, description, prefix, nextSequence + 1, displayOrder, active);
    }

    @Override
    public String toString() {
        return String.format("%s - %s", code, name);
    }
}
