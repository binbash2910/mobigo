package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.Rating;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.User;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import com.binbash.mobigo.domain.enumeration.RideStatusEnum;
import com.binbash.mobigo.repository.BookingRepository;
import com.binbash.mobigo.repository.RatingRepository;
import com.binbash.mobigo.repository.RideRepository;
import com.binbash.mobigo.repository.UserRepository;
import com.binbash.mobigo.service.dto.StatisticsDTO;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for computing user statistics dashboard data.
 */
@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticsService.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);
    private static final Set<BookingStatusEnum> ACTIVE_BOOKING_STATUSES = Set.of(BookingStatusEnum.CONFIRME, BookingStatusEnum.EFFECTUE);

    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;

    public StatisticsService(
        RideRepository rideRepository,
        BookingRepository bookingRepository,
        RatingRepository ratingRepository,
        UserRepository userRepository
    ) {
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.ratingRepository = ratingRepository;
        this.userRepository = userRepository;
    }

    /**
     * Compute full statistics for the given user.
     */
    public StatisticsDTO getDashboard(String login) {
        LOG.debug("Computing statistics dashboard for user '{}'", login);

        // Fetch all relevant data
        List<Ride> myRides = rideRepository.findByCreatedBy(login);
        List<Booking> driverBookings = bookingRepository.findByTrajetCreatedBy(login);
        List<Booking> passengerBookings = bookingRepository.findByPassagerUserLogin(login);
        List<Rating> ratingsReceived = ratingRepository.findByConducteurUserLogin(login);
        List<Rating> ratingsGiven = ratingRepository.findByPassagerUserLogin(login);

        StatisticsDTO dto = new StatisticsDTO();
        dto.setDriverStats(buildDriverStats(myRides, driverBookings, ratingsReceived));
        dto.setPassengerStats(buildPassengerStats(passengerBookings, ratingsGiven));
        dto.setMonthlyActivity(buildMonthlyActivity(myRides, driverBookings, passengerBookings));
        dto.setTopRoutes(buildTopRoutes(myRides, passengerBookings));
        dto.setGlobalSummary(buildGlobalSummary(dto.getDriverStats(), dto.getPassengerStats(), login));

        return dto;
    }

    // ── Driver Stats ────────────────────────────────────────────────────

    private StatisticsDTO.DriverStats buildDriverStats(List<Ride> rides, List<Booking> driverBookings, List<Rating> ratingsReceived) {
        StatisticsDTO.DriverStats stats = new StatisticsDTO.DriverStats();

        stats.setTotalTrips(rides.size());

        // Trips by status
        Map<String, Long> tripsByStatus = rides.stream().collect(Collectors.groupingBy(r -> r.getStatut().name(), Collectors.counting()));
        // Ensure all statuses are present
        for (RideStatusEnum status : RideStatusEnum.values()) {
            tripsByStatus.putIfAbsent(status.name(), 0L);
        }
        stats.setTripsByStatus(tripsByStatus);

        // Revenue from active bookings on my rides (montantTotal minus commission = driver's share)
        double totalRevenue = driverBookings
            .stream()
            .filter(b -> ACTIVE_BOOKING_STATUSES.contains(b.getStatut()))
            .mapToDouble(b -> {
                double montant = b.getMontantTotal() != null ? b.getMontantTotal() : 0;
                double commission = b.getCommission() != null ? b.getCommission() : 0;
                return montant - commission;
            })
            .sum();
        stats.setTotalRevenue(totalRevenue);

        // Total commission deducted by platform
        double totalCommission = driverBookings
            .stream()
            .filter(b -> ACTIVE_BOOKING_STATUSES.contains(b.getStatut()))
            .mapToDouble(b -> b.getCommission() != null ? b.getCommission() : 0)
            .sum();
        stats.setTotalCommission(totalCommission);

        // Total passengers transported
        long totalPassengers = driverBookings
            .stream()
            .filter(b -> ACTIVE_BOOKING_STATUSES.contains(b.getStatut()))
            .mapToLong(b -> b.getNbPlacesReservees() != null ? b.getNbPlacesReservees() : 0)
            .sum();
        stats.setTotalPassengers(totalPassengers);

        // Average rating
        stats.setTotalRatings(ratingsReceived.size());
        if (!ratingsReceived.isEmpty()) {
            double avgRating = ratingsReceived.stream().mapToDouble(r -> r.getNote() != null ? r.getNote() : 0).average().orElse(0);
            stats.setAverageRating(Math.round(avgRating * 10.0) / 10.0);
        }

        // Completion rate
        long completedTrips = rides.stream().filter(r -> r.getStatut() == RideStatusEnum.EFFECTUE).count();
        long finishedTrips = rides
            .stream()
            .filter(r -> r.getStatut() == RideStatusEnum.EFFECTUE || r.getStatut() == RideStatusEnum.ANNULE)
            .count();
        stats.setCompletionRate(finishedTrips > 0 ? Math.round(((double) completedTrips / finishedTrips) * 100.0) / 1.0 : 0);

        return stats;
    }

    // ── Passenger Stats ─────────────────────────────────────────────────

    private StatisticsDTO.PassengerStats buildPassengerStats(List<Booking> passengerBookings, List<Rating> ratingsGiven) {
        StatisticsDTO.PassengerStats stats = new StatisticsDTO.PassengerStats();

        stats.setTotalBookings(passengerBookings.size());

        // Bookings by status
        Map<String, Long> bookingsByStatus = passengerBookings
            .stream()
            .collect(Collectors.groupingBy(b -> b.getStatut().name(), Collectors.counting()));
        for (BookingStatusEnum status : BookingStatusEnum.values()) {
            bookingsByStatus.putIfAbsent(status.name(), 0L);
        }
        stats.setBookingsByStatus(bookingsByStatus);

        // Total spent
        double totalSpent = passengerBookings
            .stream()
            .filter(b -> ACTIVE_BOOKING_STATUSES.contains(b.getStatut()))
            .mapToDouble(b -> b.getMontantTotal() != null ? b.getMontantTotal() : 0)
            .sum();
        stats.setTotalSpent(totalSpent);

        // Trips completed as passenger
        long tripsCompleted = passengerBookings
            .stream()
            .filter(b -> b.getStatut() == BookingStatusEnum.EFFECTUE || b.getStatut() == BookingStatusEnum.CONFIRME)
            .count();
        stats.setTripsCompleted(tripsCompleted);

        // Average rating given
        stats.setRatingsGiven(ratingsGiven.size());
        if (!ratingsGiven.isEmpty()) {
            double avgGiven = ratingsGiven.stream().mapToDouble(r -> r.getNote() != null ? r.getNote() : 0).average().orElse(0);
            stats.setAverageRatingGiven(Math.round(avgGiven * 10.0) / 10.0);
        }

        return stats;
    }

    // ── Monthly Activity ────────────────────────────────────────────────

    private List<StatisticsDTO.MonthlyActivity> buildMonthlyActivity(
        List<Ride> rides,
        List<Booking> driverBookings,
        List<Booking> passengerBookings
    ) {
        // Build last 12 months
        YearMonth now = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            months.add(now.minusMonths(i));
        }

        // Index rides by month
        Map<YearMonth, Long> ridesByMonth = rides
            .stream()
            .filter(r -> r.getDateDepart() != null)
            .collect(Collectors.groupingBy(r -> YearMonth.from(r.getDateDepart()), Collectors.counting()));

        // Index driver bookings revenue by month (montantTotal - commission = driver's share)
        Map<YearMonth, Double> revenueByMonth = driverBookings
            .stream()
            .filter(b -> ACTIVE_BOOKING_STATUSES.contains(b.getStatut()) && b.getTrajet() != null && b.getTrajet().getDateDepart() != null)
            .collect(
                Collectors.groupingBy(
                    b -> YearMonth.from(b.getTrajet().getDateDepart()),
                    Collectors.summingDouble(b -> {
                        double montant = b.getMontantTotal() != null ? b.getMontantTotal() : 0;
                        double commission = b.getCommission() != null ? b.getCommission() : 0;
                        return montant - commission;
                    })
                )
            );

        // Index driver bookings passengers by month
        Map<YearMonth, Long> passengersByMonth = driverBookings
            .stream()
            .filter(b -> ACTIVE_BOOKING_STATUSES.contains(b.getStatut()) && b.getTrajet() != null && b.getTrajet().getDateDepart() != null)
            .collect(
                Collectors.groupingBy(
                    b -> YearMonth.from(b.getTrajet().getDateDepart()),
                    Collectors.summingLong(b -> b.getNbPlacesReservees() != null ? b.getNbPlacesReservees() : 0)
                )
            );

        // Index passenger bookings by month
        Map<YearMonth, Long> bookingsByMonth = passengerBookings
            .stream()
            .filter(b -> b.getDateReservation() != null)
            .collect(Collectors.groupingBy(b -> YearMonth.from(b.getDateReservation()), Collectors.counting()));

        // Index passenger spending by month
        Map<YearMonth, Double> spendingByMonth = passengerBookings
            .stream()
            .filter(b -> ACTIVE_BOOKING_STATUSES.contains(b.getStatut()) && b.getDateReservation() != null)
            .collect(
                Collectors.groupingBy(
                    b -> YearMonth.from(b.getDateReservation()),
                    Collectors.summingDouble(b -> b.getMontantTotal() != null ? b.getMontantTotal() : 0)
                )
            );

        List<StatisticsDTO.MonthlyActivity> result = new ArrayList<>();
        for (YearMonth ym : months) {
            StatisticsDTO.MonthlyActivity activity = new StatisticsDTO.MonthlyActivity();
            activity.setYear(ym.getYear());
            activity.setMonth(ym.getMonthValue());
            activity.setLabel(ym.atDay(1).format(MONTH_FORMATTER));
            activity.setRidesCreated(ridesByMonth.getOrDefault(ym, 0L));
            activity.setBookingsMade(bookingsByMonth.getOrDefault(ym, 0L));
            activity.setRevenueEarned(revenueByMonth.getOrDefault(ym, 0.0));
            activity.setAmountSpent(spendingByMonth.getOrDefault(ym, 0.0));
            activity.setPassengersTransported(passengersByMonth.getOrDefault(ym, 0L));
            result.add(activity);
        }
        return result;
    }

    // ── Top Routes ──────────────────────────────────────────────────────

    private List<StatisticsDTO.TopRoute> buildTopRoutes(List<Ride> rides, List<Booking> passengerBookings) {
        Map<String, long[]> routeMap = new LinkedHashMap<>();

        // Count driver rides per route
        for (Ride r : rides) {
            String key = r.getVilleDepart() + " → " + r.getVilleArrivee();
            routeMap.computeIfAbsent(key, k -> new long[] { 0 }).clone();
            long[] counts = routeMap.computeIfAbsent(key, k -> new long[] { 0 });
            counts[0]++;
        }

        // Count passenger bookings per route
        for (Booking b : passengerBookings) {
            if (b.getTrajet() != null) {
                Ride r = b.getTrajet();
                String key = r.getVilleDepart() + " → " + r.getVilleArrivee();
                long[] counts = routeMap.computeIfAbsent(key, k -> new long[] { 0 });
                counts[0]++;
            }
        }

        return routeMap
            .entrySet()
            .stream()
            .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
            .limit(5)
            .map(entry -> {
                String[] parts = entry.getKey().split(" → ");
                StatisticsDTO.TopRoute route = new StatisticsDTO.TopRoute();
                route.setVilleDepart(parts[0]);
                route.setVilleArrivee(parts.length > 1 ? parts[1] : "");
                route.setCount(entry.getValue()[0]);
                return route;
            })
            .collect(Collectors.toList());
    }

    // ── Global Summary ──────────────────────────────────────────────────

    private StatisticsDTO.GlobalSummary buildGlobalSummary(
        StatisticsDTO.DriverStats driverStats,
        StatisticsDTO.PassengerStats passengerStats,
        String login
    ) {
        StatisticsDTO.GlobalSummary summary = new StatisticsDTO.GlobalSummary();
        summary.setTotalEarnings(driverStats.getTotalRevenue());
        summary.setTotalSpendings(passengerStats.getTotalSpent());
        summary.setTotalCommission(driverStats.getTotalCommission());
        summary.setNetBalance(driverStats.getTotalRevenue() - passengerStats.getTotalSpent());
        summary.setTotalTripsAsDriver(driverStats.getTotalTrips());
        summary.setTotalTripsAsPassenger(passengerStats.getTripsCompleted());

        // Member since
        userRepository
            .findOneByLogin(login)
            .ifPresent(user -> {
                if (user.getCreatedDate() != null) {
                    LocalDate memberDate = user.getCreatedDate().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    summary.setMemberSince(memberDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            });

        return summary;
    }
}
