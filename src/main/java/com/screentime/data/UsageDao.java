package com.screentime.data;
import java.sql.*;

public class UsageDao {
    private final DatabaseManager databaseManager;
    public UsageDao() { this(DatabaseManager.getInstance()); }
    public UsageDao(DatabaseManager dm) { this.databaseManager = dm; }

    public synchronized void recordSession(com.screentime.core.TrackingSession session) {
        if (session == null || session.getDurationSeconds() <= 0) return;
        try (Connection conn = databaseManager.getConnection()) {
            String sql = "INSERT INTO sessions(date, app_name, start_time, end_time, duration_seconds) VALUES(?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, session.getDate().toString());
                pstmt.setString(2, session.getAppName());
                pstmt.setString(3, session.getStartTime().toString());
                pstmt.setString(4, session.getEndTime().toString());
                pstmt.setLong(5, session.getDurationSeconds());
                pstmt.executeUpdate();
            }
            String upsertDailySql = "INSERT INTO daily_usage (date, total_active_seconds, total_idle_seconds) VALUES (?, ?, ?) ON CONFLICT(date) DO UPDATE SET total_active_seconds = total_active_seconds + excluded.total_active_seconds, total_idle_seconds = total_idle_seconds + excluded.total_idle_seconds;";
            try (PreparedStatement pDaily = conn.prepareStatement(upsertDailySql)) {
                pDaily.setString(1, session.getDate().toString());
                pDaily.setLong(2, session.isIdle() ? 0 : session.getDurationSeconds());
                pDaily.setLong(3, session.isIdle() ? session.getDurationSeconds() : 0);
                pDaily.executeUpdate();
            }
            if (!session.isIdle()) {
                String upsertAppSql = "INSERT INTO app_usage (date, app_name, seconds_used) VALUES (?, ?, ?) ON CONFLICT(date, app_name) DO UPDATE SET seconds_used = seconds_used + excluded.seconds_used;";
                try (PreparedStatement pApp = conn.prepareStatement(upsertAppSql)) {
                    pApp.setString(1, session.getDate().toString());
                    pApp.setString(2, session.getAppName());
                    pApp.setLong(3, session.getDurationSeconds());
                    pApp.executeUpdate();
                }
            }
        } catch (SQLException ignored) {}
    }
}
