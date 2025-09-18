package main.java.infrastructure.events;

import main.java.domain.events.DomainEvent;
import main.java.domain.events.EventPublisher;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class SimpleBus implements EventPublisher {
    private final List<Consumer<DomainEvent>> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<DomainEvent> l) { listeners.add(l); }

    @Override public void publish(DomainEvent e) { listeners.forEach(l -> l.accept(e)); }
}