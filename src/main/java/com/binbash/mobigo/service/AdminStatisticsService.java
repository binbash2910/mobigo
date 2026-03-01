package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.User;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import com.binbash.mobigo.domain.enumeration.RideStatusEnum;
import com.binbash.mobigo.repository.BookingRepository;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.repository.RideRepository;
import com.binbash.mobigo.repository.UserRepository;
import com.binbash.mobigo.service.dto.AdminStatisticsDTO;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for computing admin-level platform statistics.
 */
@Service
@Transactional(readOnly = true)
public class AdminStatisticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminStatisticsService.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRENCH);

    private final UserRepository userRepository;
    private final PeopleRepository peopleRepository;
    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;

    public AdminStatisticsService(
        UserRepository userRepository,
        PeopleRepository peopleRepository,
        RideRepository rideRepository,
        BookingRepository bookingRepository
    ) {
        this.userRepository = userRepository;
        this.peopleRepository = peopleRepository;
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Compute global admin statistics for the platform dashboard.
     */
    public AdminStatisticsDTO getAdminStatistics() {
        LOG.debug("Computing admin statistics dashboard");

        AdminStatisticsDTO dto = new AdminStatisticsDTO();

        // Totals
        dto.setTotalUsers(userRepository.count());
        List<People> allPeople = peopleRepository.findAll();
        dto.setTotalPeople(allPeople.size());

        List<Ride> allRides = rideRepository.findAll();
        dto.setTotalRides(allRides.size());

        List<Booking> allBookings = bookingRepository.findAll();
        dto.setTotalBookings(allBookings.size());

        // CNI verification stats
        dto.setPendingVerifications(allPeople.stream().filter(p -> p.getCniStatut() == null || "PENDING".equals(p.getCniStatut())).count());
        dto.setRejectedVerifications(allPeople.stream().filter(p -> "REJECTED".equals(p.getCniStatut())).count());
        dto.setVerifiedUsers(allPeople.stream().filter(p -> "VERIFIED".equals(p.getCniStatut())).count());

        // Rides by status
        Map<String, Long> ridesByStatus = allRides
            .stream()
            .collect(Collectors.groupingBy(r -> r.getStatut().name(), Collectors.counting()));
        for (RideStatusEnum status : RideStatusEnum.values()) {
            ridesByStatus.putIfAbsent(status.name(), 0L);
        }
        dto.setRidesByStatus(ridesByStatus);

        // Bookings by status
        Map<String, Long> bookingsByStatus = allBookings
            .stream()
            .collect(Collectors.groupingBy(b -> b.getStatut().name(), Collectors.counting()));
        for (BookingStatusEnum status : BookingStatusEnum.values()) {
            bookingsByStatus.putIfAbsent(status.name(), 0L);
        }
        dto.setBookingsByStatus(bookingsByStatus);

        // Monthly activity (last 12 months)
        dto.setMonthlyActivity(buildMonthlyActivity(allRides, allBookings));

        // Recent activity (last 10 actions)
        dto.setRecentActivity(buildRecentActivity(allRides, allPeople));

        return dto;
    }

    private List<AdminStatisticsDTO.MonthlyStats> buildMonthlyActivity(List<Ride> rides, List<Booking> bookings) {
        YearMonth now = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            months.add(now.minusMonths(i));
        }

        // Rides by month (using createdDate from AbstractAuditingEntity)
        Map<YearMonth, Long> ridesByMonth = rides
            .stream()
            .filter(r -> r.getCreatedDate() != null)
            .collect(Collectors.groupingBy(r -> YearMonth.from(r.getCreatedDate().atZone(ZoneId.systemDefault())), Collectors.counting()));

        // Bookings by month
        Map<YearMonth, Long> bookingsByMonth = bookings
            .stream()
            .filter(b -> b.getDateReservation() != null)
            .collect(Collectors.groupingBy(b -> YearMonth.from(b.getDateReservation()), Collectors.counting()));

        List<AdminStatisticsDTO.MonthlyStats> result = new ArrayList<>();
        for (YearMonth ym : months) {
            AdminStatisticsDTO.MonthlyStats stats = new AdminStatisticsDTO.MonthlyStats();
            stats.setYear(ym.getYear());
            stats.setMonth(ym.getMonthValue());
            stats.setLabel(ym.atDay(1).format(MONTH_FORMATTER));
            stats.setRidesCreated(ridesByMonth.getOrDefault(ym, 0L));
            stats.setBookingsMade(bookingsByMonth.getOrDefault(ym, 0L));
            // newUsers: count users created in this month (we don't have user list here, so 0 for now)
            stats.setNewUsers(0);
            result.add(stats);
        }
        return result;
    }

    private List<AdminStatisticsDTO.RecentActivity> buildRecentActivity(List<Ride> rides, List<People> people) {
        List<AdminStatisticsDTO.RecentActivity> activities = new ArrayList<>();

        // Recent rides created
        rides
            .stream()
            .filter(r -> r.getCreatedDate() != null)
            .sorted(Comparator.comparing(Ride::getCreatedDate).reversed())
            .limit(5)
            .forEach(r -> {
                AdminStatisticsDTO.RecentActivity a = new AdminStatisticsDTO.RecentActivity();
                a.setType("RIDE_CREATED");
                a.setDescription(r.getVilleDepart() + " → " + r.getVilleArrivee());
                a.setTimestamp(r.getCreatedDate().toString());
                activities.add(a);
            });

        // Recent CNI verifications
        people
            .stream()
            .filter(p -> "VERIFIED".equals(p.getCniStatut()) && p.getCniVerifieAt() != null)
            .sorted(Comparator.comparing(People::getCniVerifieAt).reversed())
            .limit(5)
            .forEach(p -> {
                AdminStatisticsDTO.RecentActivity a = new AdminStatisticsDTO.RecentActivity();
                a.setType("CNI_VERIFIED");
                String name = (p.getPrenom() != null ? p.getPrenom() : "") + (p.getNom() != null ? " " + p.getNom().charAt(0) + "." : "");
                a.setDescription(name.trim());
                a.setTimestamp(p.getCniVerifieAt().toString());
                activities.add(a);
            });

        // Sort all by timestamp descending, take 10
        activities.sort((a, b) -> {
            Instant ia = Instant.parse(a.getTimestamp());
            Instant ib = Instant.parse(b.getTimestamp());
            return ib.compareTo(ia);
        });

        return activities.stream().limit(10).collect(Collectors.toList());
    }
}
