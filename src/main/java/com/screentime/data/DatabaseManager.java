package com.screentime.data;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private final Path dbFilePath;
    private final String dbUrl;
    public DatabaseManager(Path path) {
        this.dbFilePath = path;
        this.dbUrl = "jdbc:sqlite:" + path.toAbsolutePath().normalize();
    }
    public Connection getConnection() throws SQLException { return DriverManager.getConnection(dbUrl); }
}
