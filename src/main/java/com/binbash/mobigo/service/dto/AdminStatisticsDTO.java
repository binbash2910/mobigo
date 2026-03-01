package com.binbash.mobigo.service.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for the admin statistics dashboard.
 */
public class AdminStatisticsDTO {

    private long totalUsers;
    private long totalPeople;
    private long totalRides;
    private long totalBookings;
    private long pendingVerifications;
    private long rejectedVerifications;
    private long verifiedUsers;
    private Map<String, Long> ridesByStatus;
    private Map<String, Long> bookingsByStatus;
    private List<MonthlyStats> monthlyActivity;
    private List<RecentActivity> recentActivity;

    // ── Inner classes ───────────────────────────────────────────────────

    public static class MonthlyStats {

        private String label;
        private int year;
        private int month;
        private long newUsers;
        private long ridesCreated;
        private long bookingsMade;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public long getNewUsers() {
            return newUsers;
        }

        public void setNewUsers(long newUsers) {
            this.newUsers = newUsers;
        }

        public long getRidesCreated() {
            return ridesCreated;
        }

        public void setRidesCreated(long ridesCreated) {
            this.ridesCreated = ridesCreated;
        }

        public long getBookingsMade() {
            return bookingsMade;
        }

        public void setBookingsMade(long bookingsMade) {
            this.bookingsMade = bookingsMade;
        }
    }

    public static class RecentActivity {

        private String type;
        private String description;
        private String timestamp;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    // ── Root getters/setters ────────────────────────────────────────────

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalPeople() {
        return totalPeople;
    }

    public void setTotalPeople(long totalPeople) {
        this.totalPeople = totalPeople;
    }

    public long getTotalRides() {
        return totalRides;
    }

    public void setTotalRides(long totalRides) {
        this.totalRides = totalRides;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public long getPendingVerifications() {
        return pendingVerifications;
    }

    public void setPendingVerifications(long pendingVerifications) {
        this.pendingVerifications = pendingVerifications;
    }

    public long getRejectedVerifications() {
        return rejectedVerifications;
    }

    public void setRejectedVerifications(long rejectedVerifications) {
        this.rejectedVerifications = rejectedVerifications;
    }

    public long getVerifiedUsers() {
        return verifiedUsers;
    }

    public void setVerifiedUsers(long verifiedUsers) {
        this.verifiedUsers = verifiedUsers;
    }

    public Map<String, Long> getRidesByStatus() {
        return ridesByStatus;
    }

    public void setRidesByStatus(Map<String, Long> ridesByStatus) {
        this.ridesByStatus = ridesByStatus;
    }

    public Map<String, Long> getBookingsByStatus() {
        return bookingsByStatus;
    }

    public void setBookingsByStatus(Map<String, Long> bookingsByStatus) {
        this.bookingsByStatus = bookingsByStatus;
    }

    public List<MonthlyStats> getMonthlyActivity() {
        return monthlyActivity;
    }

    public void setMonthlyActivity(List<MonthlyStats> monthlyActivity) {
        this.monthlyActivity = monthlyActivity;
    }

    public List<RecentActivity> getRecentActivity() {
        return recentActivity;
    }

    public void setRecentActivity(List<RecentActivity> recentActivity) {
        this.recentActivity = recentActivity;
    }
}
