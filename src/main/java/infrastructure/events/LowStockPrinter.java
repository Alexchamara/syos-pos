package main.java.infrastructure.events;

import main.java.domain.events.DomainEvent;
import main.java.domain.events.LowStockEvent;

import java.util.function.Consumer;

public final class LowStockPrinter implements Consumer<DomainEvent> {
    @Override public void accept(DomainEvent e) {
        if (e instanceof LowStockEvent ev) {
            System.out.println("LOW STOCK: " + ev.productCode().value() + " = " + ev.remaining());
        }
    }
}