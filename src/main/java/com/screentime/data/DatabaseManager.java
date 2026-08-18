package com.screentime.data;

import com.screentime.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

/**
 * Manages SQLite database connection setup, automated migrations, and corruption recovery.
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static volatile DatabaseManager instance;

    private final String dbUrl;
    private final Path dbFilePath;

    public DatabaseManager() {
        this(ConfigManager.getInstance().getDatabasePath());
    }

    public DatabaseManager(Path databasePath) {
        this.dbFilePath = databasePath;
        this.dbUrl = "jdbc:sqlite:" + databasePath.toAbsolutePath().normalize();
        initializeSchemaWithRecovery();
    }

    public DatabaseManager(String customJdbcUrl) {
        this.dbUrl = customJdbcUrl;
        this.dbFilePath = null;
        initializeSchemaWithRecovery();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * Obtains a new database connection.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initializeSchemaWithRecovery() {
        try {
            initializeSchema();
        } catch (Throwable t) {
            logger.error("Database schema initialization encountered an error: {}", t.getMessage(), t);
            if (dbFilePath != null && Files.exists(dbFilePath)) {
                logger.warn("Attempting database self-healing recovery for corrupted file: {}", dbFilePath);
                recoverCorruptedDatabase();
            }
        }
    }

    private synchronized void recoverCorruptedDatabase() {
        try {
            Path backupPath = dbFilePath.resolveSibling("screentime.db.corrupted." + Instant.now().toEpochMilli());
            Files.move(dbFilePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            logger.warn("Moved corrupted database to backup: {}. Recreating fresh schema.", backupPath);
            initializeSchema();
            logger.info("Successfully recovered from database corruption with fresh schema.");
        } catch (Throwable t) {
            logger.error("Failed to recover corrupted database automatically: {}", t.getMessage(), t);
        }
    }

    /**
     * Initializes SQLite schema tables and runs migrations if needed.
     */
    public void initializeSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Migration check: if old Phase 0 daily_usage table exists with date_text column
            boolean needsMigration = false;
            try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(daily_usage)")) {
                boolean hasDateText = false;
                boolean hasDate = false;
                while (rs.next()) {
                    String colName = rs.getString("name");
                    if ("date_text".equalsIgnoreCase(colName)) hasDateText = true;
                    if ("date".equalsIgnoreCase(colName)) hasDate = true;
                }
                if (hasDateText && !hasDate) {
                    needsMigration = true;
                }
            } catch (SQLException ignored) {
                // Table might not exist yet
            }

            if (needsMigration) {
                logger.info("Migrating legacy Phase 0 SQLite tables to Phase 2 schema...");
                stmt.execute("DROP TABLE IF EXISTS daily_usage");
                stmt.execute("DROP TABLE IF EXISTS activities");
            }

            String createDailyUsageTable = """
                CREATE TABLE IF NOT EXISTS daily_usage (
                    date TEXT PRIMARY KEY,
                    total_active_seconds INTEGER NOT NULL DEFAULT 0,
                    total_idle_seconds INTEGER NOT NULL DEFAULT 0
                );
            """;

            String createAppUsageTable = """
                CREATE TABLE IF NOT EXISTS app_usage (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    app_name TEXT NOT NULL,
                    seconds_used INTEGER NOT NULL DEFAULT 0,
                    UNIQUE(date, app_name)
                );
            """;

            String createSessionsTable = """
                CREATE TABLE IF NOT EXISTS sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    app_name TEXT NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    duration_seconds INTEGER NOT NULL
                );
            """;

            String createSettingsTable = """
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );
            """;

            String createLimitExtensionsTable = """
                CREATE TABLE IF NOT EXISTS limit_extensions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    requested_minutes INTEGER NOT NULL,
                    requested_at TEXT NOT NULL,
                    reason TEXT
                );
            """;

            String createIndices = """
                CREATE INDEX IF NOT EXISTS idx_sessions_date ON sessions(date);
                CREATE INDEX IF NOT EXISTS idx_app_usage_date ON app_usage(date);
                CREATE INDEX IF NOT EXISTS idx_limit_extensions_date ON limit_extensions(date);
            """;

            stmt.execute(createDailyUsageTable);
            stmt.execute(createAppUsageTable);
            stmt.execute(createSessionsTable);
            stmt.execute(createSettingsTable);
            stmt.execute(createLimitExtensionsTable);
            stmt.execute(createIndices);
            logger.info("Database schema initialized successfully at {}", dbUrl);
        } catch (SQLException e) {
            logger.error("Failed to initialize SQLite schema: {}", e.getMessage(), e);
            throw new RuntimeException("Database initialization failed: " + e.getMessage(), e);
        }
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public Path getDbFilePath() {
        return dbFilePath;
    }
}
