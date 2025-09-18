package main.java.domain.events;

import main.java.domain.shared.Code;

public record LowStockEvent(Code productCode, int remaining) implements DomainEvent {}