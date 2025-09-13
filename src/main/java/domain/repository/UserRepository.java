package main.java.domain.repository;

import main.java.domain.user.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    void upsert(User user);
}