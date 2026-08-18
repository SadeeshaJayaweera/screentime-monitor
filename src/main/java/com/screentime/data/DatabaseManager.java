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
            stmt.execute("CREATE TABLE IF NOT EXISTS app_usage (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, app_name TEXT NOT NULL, seconds_used INTEGER NOT NULL DEFAULT 0, UNIQUE(date, app_name));");
            stmt.execute("CREATE TABLE IF NOT EXISTS sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, app_name TEXT NOT NULL, start_time TEXT NOT NULL, end_time TEXT NOT NULL, duration_seconds INTEGER NOT NULL);");
            stmt.execute("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT NOT NULL);");
        } catch (SQLException ignored) {}
    }
}
