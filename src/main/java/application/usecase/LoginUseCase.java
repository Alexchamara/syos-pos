package main.java.application.usecase;

import main.java.domain.repository.UserRepository;
import main.java.domain.user.Role;
import main.java.infrastructure.security.PasswordEncoder;

public final class LoginUseCase {
    public record Session(String username, Role role) {}
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public LoginUseCase(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    public Session login(String username, String password) {
        var user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!encoder.matches(password, user.passwordHash()))
            throw new IllegalArgumentException("Invalid credentials");

        return new Session(user.username(), user.role());
    }
}