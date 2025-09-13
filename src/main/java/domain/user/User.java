package main.java.domain.user;

public final class User {
    private final long id;
    private final String username;
    private final String passwordHash;
    private final String email;
    private final Role role;

    public User(long id, String username, String passwordHash, String email, Role role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.role = role;
    }
    public long id(){ return id; }
    public String username(){ return username; }
    public String passwordHash(){ return passwordHash; }
    public String email(){ return email; }
    public Role role(){ return role; }
}