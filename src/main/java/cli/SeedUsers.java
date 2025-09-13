package main.java.cli;

import main.java.domain.repository.UserRepository;
import main.java.domain.user.Role;
import main.java.domain.user.User;
import main.java.infrastructure.security.PasswordEncoder;

/**
 * Utility class to ensure demo user accounts exist in the system.
 * Creates default cashier and manager accounts for testing.
 */
public final class SeedUsers {

    public static void ensure(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        // Create demo cashier account
        if (userRepository.findByUsername("cashier").isEmpty()) {
            var cashierPassword = passwordEncoder.hash("cashier123");
            var cashier = new User(0L, "cashier", cashierPassword, "cashier@syos.com", Role.CASHIER);
            userRepository.upsert(cashier);
            System.out.println("Created demo cashier account (username: cashier, password: cashier123)");
        }

        // Create demo manager account
        if (userRepository.findByUsername("manager").isEmpty()) {
            var managerPassword = passwordEncoder.hash("manager123");
            var manager = new User(0L, "manager", managerPassword, "manager@syos.com", Role.MANAGER);
            userRepository.upsert(manager);
            System.out.println("Created demo manager account (username: manager, password: manager123)");
        }

        // Create demo user account
        if (userRepository.findByUsername("user").isEmpty()) {
            var userPassword = passwordEncoder.hash("user123");
            var user = new User(0L, "user", userPassword, "user@syos.com", Role.USER);
            userRepository.upsert(user);
            System.out.println("Created demo user account (username: user, password: user123)");
        }
    }
}
