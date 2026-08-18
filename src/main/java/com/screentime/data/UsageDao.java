package com.screentime.data;

import com.screentime.core.TrackingSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Data Access Object for screen time usage persistence, session logging,
 * daily and application aggregations, and date-range metrics.
 */
public class UsageDao {

    private static final Logger logger = LoggerFactory.getLogger(UsageDao.class);
    private final DatabaseManager databaseManager;

    public UsageDao() {
        this(DatabaseManager.getInstance());
    }

    public UsageDao(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager, "DatabaseManager must not be null");
    }

    /**
     * Records a completed TrackingSession.
     * Handles splitting multi-day sessions crossing midnight and atomically updates
     * `sessions`, `app_usage`, and `daily_usage`.
     *
     * @param session The closed TrackingSession.
     */
    public synchronized void recordSession(TrackingSession session) {
        if (session == null || session.getDurationSeconds() <= 0) {
            return;
        }

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime startZdt = session.getStartTime().atZone(zone);
        ZonedDateTime endZdt = session.getEndTime().atZone(zone);

        LocalDate startDate = startZdt.toLocalDate();
        LocalDate endDate = endZdt.toLocalDate();

        if (startDate.equals(endDate)) {
            // Single-day session
            insertSingleSession(startDate, session.getAppName(), session.getStartTime(), session.getEndTime(), session.getDurationSeconds());
        } else {
            // Multi-day session: split across midnight boundaries
            ZonedDateTime currentBoundaryStart = startZdt;
            LocalDate currentDate = startDate;

            while (!currentDate.isAfter(endDate)) {
                ZonedDateTime currentBoundaryEnd;
                if (currentDate.equals(endDate)) {
                    currentBoundaryEnd = endZdt;
                } else {
                    currentBoundaryEnd = currentDate.plusDays(1).atStartOfDay(zone);
                }

                long duration = Duration.between(currentBoundaryStart, currentBoundaryEnd).getSeconds();
                if (duration > 0) {
                    insertSingleSession(currentDate, session.getAppName(), currentBoundaryStart.toInstant(), currentBoundaryEnd.toInstant(), duration);
                }

                currentDate = currentDate.plusDays(1);
                currentBoundaryStart = currentDate.atStartOfDay(zone);
            }
        }
    }

    private void insertSingleSession(LocalDate date, String appName, Instant start, Instant end, long durationSeconds) {
        String dateStr = date.toString();

        String insertSessionSql = """
            INSERT INTO sessions (date, app_name, start_time, end_time, duration_seconds)
            VALUES (?, ?, ?, ?, ?)
        """;

        String upsertAppUsageSql = """
            INSERT INTO app_usage (date, app_name, seconds_used)
            VALUES (?, ?, ?)
            ON CONFLICT(date, app_name) DO UPDATE SET
                seconds_used = seconds_used + excluded.seconds_used
        """;

        String upsertDailyUsageSql = """
            INSERT INTO daily_usage (date, total_active_seconds, total_idle_seconds)
            VALUES (?, ?, 0)
            ON CONFLICT(date) DO UPDATE SET
                total_active_seconds = total_active_seconds + excluded.total_active_seconds
        """;

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert session record
                try (PreparedStatement psSession = conn.prepareStatement(insertSessionSql)) {
                    psSession.setString(1, dateStr);
                    psSession.setString(2, appName);
                    psSession.setString(3, start.toString());
                    psSession.setString(4, end.toString());
                    psSession.setLong(5, durationSeconds);
                    psSession.executeUpdate();
                }

                // 2. Upsert app usage
                try (PreparedStatement psApp = conn.prepareStatement(upsertAppUsageSql)) {
                    psApp.setString(1, dateStr);
                    psApp.setString(2, appName);
                    psApp.setLong(3, durationSeconds);
                    psApp.executeUpdate();
                }

                // 3. Upsert daily usage
                try (PreparedStatement psDaily = conn.prepareStatement(upsertDailyUsageSql)) {
                    psDaily.setString(1, dateStr);
                    psDaily.setLong(2, durationSeconds);
                    psDaily.executeUpdate();
                }

                conn.commit();
                logger.debug("Persisted session for app '{}' on {}: {}s", appName, dateStr, durationSeconds);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Failed to record session for app '{}' on {}: {}", appName, dateStr, e.getMessage(), e);
        }
    }

    /**
     * Gets the total active screen time in seconds for today.
     */
    public int getTodayUsageSeconds() {
        return (int) getActiveSecondsForDate(LocalDate.now());
    }

    /**
     * Gets the total active seconds recorded for a given date.
     */
    public long getActiveSecondsForDate(LocalDate date) {
        String sql = "SELECT total_active_seconds FROM daily_usage WHERE date = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("total_active_seconds");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to query active seconds for date {}: {}", date, e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Retrieves aggregated metrics and per-app breakdown for a specific date.
     */
    public DailyUsageSummary getUsageForDate(LocalDate date) {
        long activeSeconds = 0;
        long idleSeconds = 0;

        String dailySql = "SELECT total_active_seconds, total_idle_seconds FROM daily_usage WHERE date = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(dailySql)) {
            ps.setString(1, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    activeSeconds = rs.getLong("total_active_seconds");
                    idleSeconds = rs.getLong("total_idle_seconds");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch daily summary for date {}: {}", date, e.getMessage(), e);
        }

        List<AppUsage> apps = getTopAppsForDate(date, Integer.MAX_VALUE);
        return new DailyUsageSummary(date, activeSeconds, idleSeconds, apps);
    }

    /**
     * Retrieves a list of DailyUsageSummary objects for a range of dates [start, end] inclusive.
     */
    public List<DailyUsageSummary> getUsageForDateRange(LocalDate start, LocalDate end) {
        List<DailyUsageSummary> results = new ArrayList<>();
        if (start == null || end == null || start.isAfter(end)) {
            return results;
        }

        LocalDate cur = start;
        while (!cur.isAfter(end)) {
            results.add(getUsageForDate(cur));
            cur = cur.plusDays(1);
        }
        return results;
    }

    /**
     * Returns top applications by usage for a given date sorted descending by seconds_used.
     */
    public List<AppUsage> getTopAppsForDate(LocalDate date, int limit) {
        List<AppUsage> list = new ArrayList<>();
        String sql = """
            SELECT id, date, app_name, seconds_used
            FROM app_usage
            WHERE date = ?
            ORDER BY seconds_used DESC
            LIMIT ?
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setInt(2, Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AppUsage(
                            rs.getLong("id"),
                            LocalDate.parse(rs.getString("date")),
                            rs.getString("app_name"),
                            rs.getLong("seconds_used")
                    ));
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch top apps for date {}: {}", date, e.getMessage(), e);
        }
        return list;
    }

    /**
     * Saves a periodic snapshot of today's running total to disk.
     * Uses MAX so that a snapshot never decreases already persisted totals.
     */
    public synchronized void savePeriodicDailyActiveSnapshot(LocalDate date, long totalActiveSeconds) {
        String sql = """
            INSERT INTO daily_usage (date, total_active_seconds, total_idle_seconds)
            VALUES (?, ?, 0)
            ON CONFLICT(date) DO UPDATE SET
                total_active_seconds = MAX(total_active_seconds, excluded.total_active_seconds)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setLong(2, Math.max(0, totalActiveSeconds));
            ps.executeUpdate();
            logger.debug("Saved periodic daily active snapshot for {}: {}s", date, totalActiveSeconds);
        } catch (SQLException e) {
            logger.error("Failed to save periodic snapshot for {}: {}", date, e.getMessage(), e);
        }
    }

    /**
     * Increments daily idle seconds.
     */
    public synchronized void addIdleSeconds(LocalDate date, long idleSeconds) {
        if (idleSeconds <= 0) return;
        String sql = """
            INSERT INTO daily_usage (date, total_active_seconds, total_idle_seconds)
            VALUES (?, 0, ?)
            ON CONFLICT(date) DO UPDATE SET
                total_idle_seconds = total_idle_seconds + excluded.total_idle_seconds
        """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setLong(2, idleSeconds);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to add idle seconds for {}: {}", date, e.getMessage(), e);
        }
    }

    // --- Limit Extensions & Settings ---

    public void recordLimitExtension(LimitExtensionRecord record) {
        String sql = """
            INSERT INTO limit_extensions (date, requested_minutes, requested_at, reason)
            VALUES (?, ?, ?, ?)
        """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getDate().toString());
            ps.setInt(2, record.getRequestedMinutes());
            ps.setString(3, record.getRequestedAt().toString());
            ps.setString(4, record.getReason());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to record limit extension: {}", e.getMessage(), e);
        }
    }

    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get setting '{}': {}", key, e.getMessage(), e);
        }
        return defaultValue;
    }

    public void setSetting(String key, String value) {
        String sql = """
            INSERT INTO settings (key, value)
            VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
        """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to set setting '{}': {}", key, e.getMessage(), e);
        }
    }
}
