package com.binbash.mobigo.service.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for the user statistics dashboard.
 */
public class StatisticsDTO {

    private DriverStats driverStats;
    private PassengerStats passengerStats;
    private List<MonthlyActivity> monthlyActivity;
    private List<TopRoute> topRoutes;
    private GlobalSummary globalSummary;

    // ── Inner classes ───────────────────────────────────────────────────

    public static class DriverStats {

        private long totalTrips;
        private Map<String, Long> tripsByStatus;
        private double totalRevenue;
        private long totalPassengers;
        private double averageRating;
        private long totalRatings;
        private double completionRate;

        public long getTotalTrips() {
            return totalTrips;
        }

        public void setTotalTrips(long totalTrips) {
            this.totalTrips = totalTrips;
        }

        public Map<String, Long> getTripsByStatus() {
            return tripsByStatus;
        }

        public void setTripsByStatus(Map<String, Long> tripsByStatus) {
            this.tripsByStatus = tripsByStatus;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(double totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public long getTotalPassengers() {
            return totalPassengers;
        }

        public void setTotalPassengers(long totalPassengers) {
            this.totalPassengers = totalPassengers;
        }

        public double getAverageRating() {
            return averageRating;
        }

        public void setAverageRating(double averageRating) {
            this.averageRating = averageRating;
        }

        public long getTotalRatings() {
            return totalRatings;
        }

        public void setTotalRatings(long totalRatings) {
            this.totalRatings = totalRatings;
        }

        public double getCompletionRate() {
            return completionRate;
        }

        public void setCompletionRate(double completionRate) {
            this.completionRate = completionRate;
        }
    }

    public static class PassengerStats {

        private long totalBookings;
        private Map<String, Long> bookingsByStatus;
        private double totalSpent;
        private long tripsCompleted;
        private double averageRatingGiven;
        private long ratingsGiven;

        public long getTotalBookings() {
            return totalBookings;
        }

        public void setTotalBookings(long totalBookings) {
            this.totalBookings = totalBookings;
        }

        public Map<String, Long> getBookingsByStatus() {
            return bookingsByStatus;
        }

        public void setBookingsByStatus(Map<String, Long> bookingsByStatus) {
            this.bookingsByStatus = bookingsByStatus;
        }

        public double getTotalSpent() {
            return totalSpent;
        }

        public void setTotalSpent(double totalSpent) {
            this.totalSpent = totalSpent;
        }

        public long getTripsCompleted() {
            return tripsCompleted;
        }

        public void setTripsCompleted(long tripsCompleted) {
            this.tripsCompleted = tripsCompleted;
        }

        public double getAverageRatingGiven() {
            return averageRatingGiven;
        }

        public void setAverageRatingGiven(double averageRatingGiven) {
            this.averageRatingGiven = averageRatingGiven;
        }

        public long getRatingsGiven() {
            return ratingsGiven;
        }

        public void setRatingsGiven(long ratingsGiven) {
            this.ratingsGiven = ratingsGiven;
        }
    }

    public static class MonthlyActivity {

        private int year;
        private int month;
        private String label;
        private long ridesCreated;
        private long bookingsMade;
        private double revenueEarned;
        private double amountSpent;
        private long passengersTransported;

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

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
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

        public double getRevenueEarned() {
            return revenueEarned;
        }

        public void setRevenueEarned(double revenueEarned) {
            this.revenueEarned = revenueEarned;
        }

        public double getAmountSpent() {
            return amountSpent;
        }

        public void setAmountSpent(double amountSpent) {
            this.amountSpent = amountSpent;
        }

        public long getPassengersTransported() {
            return passengersTransported;
        }

        public void setPassengersTransported(long passengersTransported) {
            this.passengersTransported = passengersTransported;
        }
    }

    public static class TopRoute {

        private String villeDepart;
        private String villeArrivee;
        private long count;
        private double totalRevenue;

        public String getVilleDepart() {
            return villeDepart;
        }

        public void setVilleDepart(String villeDepart) {
            this.villeDepart = villeDepart;
        }

        public String getVilleArrivee() {
            return villeArrivee;
        }

        public void setVilleArrivee(String villeArrivee) {
            this.villeArrivee = villeArrivee;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(double totalRevenue) {
            this.totalRevenue = totalRevenue;
        }
    }

    public static class GlobalSummary {

        private double totalEarnings;
        private double totalSpendings;
        private double netBalance;
        private long totalTripsAsDriver;
        private long totalTripsAsPassenger;
        private String memberSince;

        public double getTotalEarnings() {
            return totalEarnings;
        }

        public void setTotalEarnings(double totalEarnings) {
            this.totalEarnings = totalEarnings;
        }

        public double getTotalSpendings() {
            return totalSpendings;
        }

        public void setTotalSpendings(double totalSpendings) {
            this.totalSpendings = totalSpendings;
        }

        public double getNetBalance() {
            return netBalance;
        }

        public void setNetBalance(double netBalance) {
            this.netBalance = netBalance;
        }

        public long getTotalTripsAsDriver() {
            return totalTripsAsDriver;
        }

        public void setTotalTripsAsDriver(long totalTripsAsDriver) {
            this.totalTripsAsDriver = totalTripsAsDriver;
        }

        public long getTotalTripsAsPassenger() {
            return totalTripsAsPassenger;
        }

        public void setTotalTripsAsPassenger(long totalTripsAsPassenger) {
            this.totalTripsAsPassenger = totalTripsAsPassenger;
        }

        public String getMemberSince() {
            return memberSince;
        }

        public void setMemberSince(String memberSince) {
            this.memberSince = memberSince;
        }
    }

    // ── Root getters/setters ────────────────────────────────────────────

    public DriverStats getDriverStats() {
        return driverStats;
    }

    public void setDriverStats(DriverStats driverStats) {
        this.driverStats = driverStats;
    }

    public PassengerStats getPassengerStats() {
        return passengerStats;
    }

    public void setPassengerStats(PassengerStats passengerStats) {
        this.passengerStats = passengerStats;
    }

    public List<MonthlyActivity> getMonthlyActivity() {
        return monthlyActivity;
    }

    public void setMonthlyActivity(List<MonthlyActivity> monthlyActivity) {
        this.monthlyActivity = monthlyActivity;
    }

    public List<TopRoute> getTopRoutes() {
        return topRoutes;
    }

    public void setTopRoutes(List<TopRoute> topRoutes) {
        this.topRoutes = topRoutes;
    }

    public GlobalSummary getGlobalSummary() {
        return globalSummary;
    }

    public void setGlobalSummary(GlobalSummary globalSummary) {
        this.globalSummary = globalSummary;
    }
}
