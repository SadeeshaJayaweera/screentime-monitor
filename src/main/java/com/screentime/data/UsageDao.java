package com.screentime.data;
import java.sql.*;

public class UsageDao {
    private final DatabaseManager databaseManager;
    public UsageDao() { this(DatabaseManager.getInstance()); }
    public UsageDao(DatabaseManager dm) { this.databaseManager = dm; }

    public synchronized void recordSession(com.screentime.core.TrackingSession session) {
        if (session == null || session.getDurationSeconds() <= 0) return;
        String sql = "INSERT INTO sessions(date, app_name, start_time, end_time, duration_seconds) VALUES(?, ?, ?, ?, ?)";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, session.getDate().toString());
            pstmt.setString(2, session.getAppName());
            pstmt.setString(3, session.getStartTime().toString());
            pstmt.setString(4, session.getEndTime().toString());
            pstmt.setLong(5, session.getDurationSeconds());
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }
}
