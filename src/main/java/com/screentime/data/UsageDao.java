package com.screentime.data;
import java.sql.*;

public class UsageDao {
    private final DatabaseManager databaseManager;
    public UsageDao() { this(DatabaseManager.getInstance()); }
    public UsageDao(DatabaseManager dm) { this.databaseManager = dm; }

    public synchronized void recordSession(com.screentime.core.TrackingSession session) {
        if (session == null || session.getDurationSeconds() <= 0) return;
        if (!session.getStartTime().toLocalDate().equals(session.getEndTime().toLocalDate())) {
            for (com.screentime.core.TrackingSession split : session.splitAtMidnight()) {
                recordSession(split);
            }
            return;
        }
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

    public long getTodayUsageSeconds() { return getUsageForDate(java.time.LocalDate.now()); }
    public long getUsageForDate(java.time.LocalDate date) {
        String sql = "SELECT total_active_seconds FROM daily_usage WHERE date = ?";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return rs.getLong("total_active_seconds"); }
        } catch (SQLException ignored) {}
        return 0L;
    }

    public DailyUsageSummary getDailySummary(java.time.LocalDate date) {
        String sql = "SELECT total_active_seconds, total_idle_seconds FROM daily_usage WHERE date = ?";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return new DailyUsageSummary(date, rs.getLong("total_active_seconds"), rs.getLong("total_idle_seconds"));
            }
        } catch (SQLException ignored) {}
        return new DailyUsageSummary(date, 0L, 0L);
    }

    public java.util.List<AppUsage> getTopAppsToday(int limit) { return getTopAppsForDate(java.time.LocalDate.now(), limit); }
    public java.util.List<AppUsage> getTopAppsForDate(java.time.LocalDate date, int limit) {
        String sql = "SELECT app_name, seconds_used FROM app_usage WHERE date = ? ORDER BY seconds_used DESC LIMIT ?";
        java.util.List<AppUsage> list = new java.util.ArrayList<>();
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, date.toString());
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(new AppUsage(date, rs.getString("app_name"), rs.getLong("seconds_used")));
            }
        } catch (SQLException ignored) {}
        return list;
    }

    public java.util.List<AppUsage> getAppUsageForDate(java.time.LocalDate date) { return getTopAppsForDate(date, 1000); }

    public java.util.List<DailyUsageSummary> getUsageForDateRange(java.time.LocalDate start, java.time.LocalDate end) {
        String sql = "SELECT date, total_active_seconds, total_idle_seconds FROM daily_usage WHERE date >= ? AND date <= ? ORDER BY date ASC";
        java.util.List<DailyUsageSummary> results = new java.util.ArrayList<>();
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new DailyUsageSummary(java.time.LocalDate.parse(rs.getString("date")), rs.getLong("total_active_seconds"), rs.getLong("total_idle_seconds")));
                }
            }
        } catch (SQLException ignored) {}
        return results;
    }
}