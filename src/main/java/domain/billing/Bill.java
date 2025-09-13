package main.java.domain.billing;

import main.java.domain.shared.Money;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Bill {
    private final long id;              // DB PK (0 for new)
    private final String serial;        // generated (e.g., YYYYMMDD-####)
    private final LocalDateTime dateTime;
    private final List<BillLine> lines;
    private final Money subtotal;
    private final Money discount;
    private final Money total;
    private final Money cash;
    private final Money change;

    private Bill(long id, String serial, LocalDateTime dateTime,
                 List<BillLine> lines, Money subtotal, Money discount, Money total,
                 Money cash, Money change) {
        this.id = id; this.serial = serial; this.dateTime = dateTime;
        this.lines = Collections.unmodifiableList(lines);
        this.subtotal = subtotal; this.discount = discount; this.total = total;
        this.cash = cash; this.change = change;
    }

    public long id(){ return id; }
    public String serial(){ return serial; }
    public LocalDateTime dateTime(){ return dateTime; }
    public List<BillLine> lines(){ return lines; }
    public Money subtotal(){ return subtotal; }
    public Money discount(){ return discount; }
    public Money total(){ return total; }
    public Money cash(){ return cash; }
    public Money change(){ return change; }

    public static final class Builder {
        private long id;
        private String serial;
        private LocalDateTime dateTime = LocalDateTime.now();
        private final List<BillLine> lines = new ArrayList<>();
        private Money discount = Money.of(0);
        private Money cash = Money.of(0);

        public Builder serial(String s){ this.serial = s; return this; }
        public Builder addLine(BillLine l){ this.lines.add(l); return this; }
        public Builder discount(Money d){ this.discount = d; return this; }
        public Builder cash(Money c){ this.cash = c; return this; }

        public Bill build() {
            var subtotal = lines.stream().map(BillLine::lineTotal).reduce(Money.of(0), Money::plus);
            var total = subtotal.minus(discount);
            if (total.amount().signum() < 0) throw new IllegalArgumentException("Discount > subtotal");
            if (cash.amount().compareTo(total.amount()) < 0) throw new IllegalArgumentException("Cash < total");
            var change = cash.minus(total);
            if (serial == null || serial.isBlank()) serial = "TMP-" + System.nanoTime();
            return new Bill(id, serial, dateTime, new ArrayList<>(lines), subtotal, discount, total, cash, change);
        }
    }
}