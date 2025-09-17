package main.java.application.services;

import main.java.domain.repository.ShortageEventRepository;
import main.java.infrastructure.concurrency.Tx;

import java.util.List;

/** Service layer for persisting shortage events. */
public final class ShortageEventService {
    private final Tx tx;
    private final ShortageEventRepository repo;

    public ShortageEventService(Tx tx, ShortageEventRepository repo) {
        this.tx = tx; this.repo = repo;
    }

    public void record(String message) {
        tx.inTx(con -> { repo.save(con, message); return null; });
    }

    public List<String> list() {
        return tx.inTx(repo::list);
    }

    public void clear() {
        tx.inTx(con -> { repo.clear(con); return null; });
    }
}

