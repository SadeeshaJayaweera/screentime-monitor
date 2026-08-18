package com.screentime.data;
import java.nio.file.Path;
import java.sql.*;

public class DatabaseManager {
    private final Path dbFilePath;
    private final String dbUrl;
    public DatabaseManager(Path path) {
        this.dbFilePath = path;
        this.dbUrl = "jdbc:sqlite:" + path.toAbsolutePath().normalize();
        initializeSchema();
    }
    public Connection getConnection() throws SQLException { return DriverManager.getConnection(dbUrl); }
    public void initializeSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS daily_usage (date TEXT PRIMARY KEY, total_active_seconds INTEGER NOT NULL DEFAULT 0, total_idle_seconds INTEGER NOT NULL DEFAULT 0);");
        } catch (SQLException ignored) {}
    }
}
