package com.screentime.data;

public class UsageDao {
    private final DatabaseManager databaseManager;
    public UsageDao() { this(DatabaseManager.getInstance()); }
    public UsageDao(DatabaseManager dm) { this.databaseManager = dm; }
}
