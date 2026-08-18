package com.screentime.data;
import java.nio.file.Path;

public class DatabaseManager {
    private final Path dbFilePath;
    public DatabaseManager(Path path) { this.dbFilePath = path; }
}
