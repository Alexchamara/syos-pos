package main.java.config;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Database configuration class that handles connections to the MySQL database
 * using direct JDBC connections and simple classpath-based SQL migrations.
 */
public class Db implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(Db.class.getName());

    // Default to quiet logging (show warnings/errors only)
    static {
        LOGGER.setLevel(Level.WARNING);
    }

    // Properties file name
    private static final String PROPERTIES_FILE = "application.properties";

    // Classpath locations inside src/main/resources
    private static final String[] MIGRATION_SCRIPTS = {
            "main/resources/db/migration/V1__create_users_table.sql",
            "main/resources/db/migration/V2__init_product_and_batch.sql",
            "main/resources/db/migration/V3__billing_tables.sql",
            "main/resources/db/migration/V4__seed_household_products.sql",
            "main/resources/db/migration/V5__bill_number_sequence.sql"
    };

    private final Properties properties;
    private final String jdbcUrl;
    private final String username;
    private final String password;

    private Connection currentConnection; // optional, if you later add pooling

    /**
     * Creates a new database configuration instance.
     * Loads properties and initializes the database.
     */
    public Db() {
        this.properties = loadProperties();
        this.jdbcUrl = properties.getProperty("db.url");
        this.username = properties.getProperty("db.user");
        this.password = properties.getProperty("db.pass");

        loadMySQLDriver();
        testConnection();
        initializeDatabase();
    }

    /** Load MySQL JDBC driver */
    private void loadMySQLDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            LOGGER.fine("MySQL JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC driver not found. Ensure mysql-connector-j is on the classpath.", e);
            throw new RuntimeException("MySQL JDBC driver not found", e);
        }
    }

    /** Test database connection */
    private void testConnection() {
        try (Connection ignored = createConnection()) {
            LOGGER.fine("Database connection test successful");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to database. Check MySQL server and credentials.", e);
            throw new RuntimeException("Database connection failed", e);
        }
    }

    /**
     * Creates a new database connection (auto-commit true).
     */
    private Connection createConnection() throws SQLException {
        Connection c = DriverManager.getConnection(jdbcUrl, username, password);
        c.setAutoCommit(true);
        return c;
    }

    /**
     * Gets a database connection (new each call for simplicity).
     */
    public Connection getConnection() throws SQLException {
        return createConnection();
    }

    /** Expose a simple DataSource for compatibility with other code. */
    public DataSource getDataSource() {
        return new SimpleDataSource();
    }

    /** Simple javax.sql.DataSource implementation backed by DriverManager. */
    public class SimpleDataSource implements DataSource {
        @Override public Connection getConnection() throws SQLException { return Db.this.getConnection(); }
        @Override public Connection getConnection(String u, String p) throws SQLException {
            Connection c = DriverManager.getConnection(jdbcUrl, u, p);
            c.setAutoCommit(true);
            return c;
        }
        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) { /* no-op */ }
        @Override public void setLoginTimeout(int seconds) { /* no-op */ }
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("getParentLogger not supported");
        }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isAssignableFrom(getClass())) return iface.cast(this);
            throw new SQLException("Cannot unwrap to " + iface.getName());
        }
        @Override public boolean isWrapperFor(Class<?> iface) { return iface.isAssignableFrom(getClass()); }
    }

    /**
     * Loads database configuration from application.properties on the classpath.
     * Falls back to defaults if not found.
     */
    private Properties loadProperties() {
        Properties props = new Properties();

        // Try without leading slash
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES_FILE);
        if (in == null) {
            // Try with leading slash
            in = Db.class.getResourceAsStream("/" + PROPERTIES_FILE);
        }

        if (in != null) {
            try (InputStream is = in) {
                props.load(is);
                LOGGER.fine("Database properties loaded from classpath: " + PROPERTIES_FILE);
                return props;
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed reading database properties from classpath", e);
            }
        } else {
            LOGGER.fine("application.properties not found on classpath; using defaults");
        }

        return getDefaultProperties();
    }

    /**
     * Default local development properties (adjust as needed).
     */
    private Properties getDefaultProperties() {
        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:mysql://localhost:3306/SYOS?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        props.setProperty("db.user", "Alex");
        props.setProperty("db.pass", "Alex@20020930"); // empty by default

        LOGGER.fine("Using default database properties");
        return props;
    }

    /** Initialize DB: create database if needed, then run migrations. */
    private void initializeDatabase() {
        LOGGER.fine("Starting database initialization");
        createDatabaseIfNotExists();
        executeInitScripts();
    }

    /** Create database if it doesn't exist (based on db name in JDBC URL). */
    private void createDatabaseIfNotExists() {
        String dbName = extractDatabaseName(jdbcUrl);
        if (dbName == null) {
            LOGGER.warning("Could not extract database name from URL: " + jdbcUrl);
            return;
        }

        // server URL without schema
        int lastSlash = jdbcUrl.lastIndexOf('/');
        if (lastSlash < 0) {
            LOGGER.warning("Unexpected JDBC URL format: " + jdbcUrl);
            return;
        }
        String serverUrl = jdbcUrl.substring(0, lastSlash) + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        try (Connection c = DriverManager.getConnection(serverUrl, username, password);
             Statement st = c.createStatement()) {
            st.execute("CREATE DATABASE IF NOT EXISTS `" + dbName + "`");
            LOGGER.fine("Database '" + dbName + "' created or already exists");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not create database '" + dbName + "'", e);
        }
    }

    /** Extract the database name from a JDBC URL. */
    private String extractDatabaseName(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0) return null;
        int qm = url.indexOf('?', lastSlash);
        return (qm == -1) ? url.substring(lastSlash + 1) : url.substring(lastSlash + 1, qm);
    }

    /** Execute SQL migration scripts from classpath in order. */
    private void executeInitScripts() {
        for (String scriptPath : MIGRATION_SCRIPTS) {
            String sql = readResourceFile(scriptPath);
            if (sql == null || sql.trim().isEmpty()) {
                LOGGER.warning("Could not find or empty migration script: " + scriptPath);
                continue;
            }

            try (Connection connection = getConnection();
                 Statement statement = connection.createStatement()) {

                // naive split by ';' – adequate for simple migrations without custom DELIMITER
                String[] statements = sql.split(";\\s*");
                for (String stmt : statements) {
                    String s = stmt.trim();
                    if (s.isEmpty()) continue;
                    try {
                        statement.execute(s);
                        LOGGER.fine("Executed SQL from " + scriptPath + ": " +
                                s.substring(0, Math.min(s.length(), 80)) + "...");
                    } catch (SQLException e) {
                        LOGGER.log(Level.FINE, "Error executing SQL statement from " + scriptPath + ": " + e.getMessage());
                    }
                }

                LOGGER.fine("Database initialization completed for " + scriptPath);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize database with script: " + scriptPath, e);
            }
        }
    }

    /** Read a resource file from classpath into a String. */
    private String readResourceFile(String resourcePath) {
        // Accept both "db/migration/..." and "/db/migration/..."
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;

        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(normalized);
        if (in == null) {
            in = Db.class.getResourceAsStream("/" + normalized);
        }
        if (in == null) {
            LOGGER.fine("Could not find resource: " + resourcePath);
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read resource file: " + resourcePath, e);
            return null;
        }
    }

    /** Close any open connection on shutdown (if one was kept). */
    @Override
    public void close() {
        if (currentConnection != null) {
            try {
                if (!currentConnection.isClosed()) {
                    currentConnection.close();
                    LOGGER.fine("Database connection closed");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }
}