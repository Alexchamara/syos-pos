package main.java.cli.signin;

import main.java.application.usecase.LoginUseCase;
import main.java.domain.user.Role;

import java.util.Scanner;

public final class LoginScreen {
    private final LoginUseCase login;

    public LoginScreen(LoginUseCase login){ this.login = login; }

    public LoginUseCase.Session prompt() {
        var sc = new Scanner(System.in);
        System.out.println("=== Login ===");
        System.out.print("Username: ");
        String u = sc.nextLine().trim();
        System.out.print("Password: ");
        String p = sc.nextLine().trim();

        try {
            var session = login.login(u, p);
            System.out.println("Welcome, " + session.username() + " (" + session.role() + ")");
            return session;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static void route(LoginUseCase.Session session, Runnable cashierMenu, Runnable managerMenu) {
        if (session == null) return;
        if (session.role() == Role.MANAGER) managerMenu.run(); else cashierMenu.run();
    }
}