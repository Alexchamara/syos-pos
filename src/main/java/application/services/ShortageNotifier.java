package main.java.application.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Simple in-memory notification store for stock shortages that neither SHELF nor WEB can fulfill. */
public final class ShortageNotifier {
    private static final List<String> EVENTS = Collections.synchronizedList(new ArrayList<>());

    private ShortageNotifier() {}

    public static void record(String message) {
        EVENTS.add(Instant.now() + " | " + message);
    }

    public static List<String> list() {
        synchronized (EVENTS) { return new ArrayList<>(EVENTS); }
    }

    public static void clear() { EVENTS.clear(); }
}

