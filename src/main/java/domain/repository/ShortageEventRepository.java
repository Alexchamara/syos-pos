package main.java.domain.repository;

import java.sql.Connection;
import java.util.List;

public interface ShortageEventRepository {
    void save(Connection con, String message);
    List<String> list(Connection con);
    void clear(Connection con);
}

