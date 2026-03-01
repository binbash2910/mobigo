package com.binbash.mobigo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import com.binbash.mobigo.domain.enumeration.RideStatusEnum;
import com.binbash.mobigo.repository.BookingRepository;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.repository.RideRepository;
import com.binbash.mobigo.repository.UserRepository;
import com.binbash.mobigo.service.dto.AdminStatisticsDTO;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link AdminStatisticsService}.
 */
@ExtendWith(MockitoExtension.class)
class AdminStatisticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PeopleRepository peopleRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private BookingRepository bookingRepository;

    private AdminStatisticsService service;

    @BeforeEach
    void setUp() {
        service = new AdminStatisticsService(userRepository, peopleRepository, rideRepository, bookingRepository);
    }

    @Test
    void shouldReturnTotalCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(peopleRepository.findAll()).thenReturn(createPeopleList());
        when(rideRepository.findAll()).thenReturn(createRideList());
        when(bookingRepository.findAll()).thenReturn(createBookingList());

        AdminStatisticsDTO result = service.getAdminStatistics();

        assertThat(result.getTotalUsers()).isEqualTo(10);
        assertThat(result.getTotalPeople()).isEqualTo(3);
        assertThat(result.getTotalRides()).isEqualTo(3);
        assertThat(result.getTotalBookings()).isEqualTo(2);
    }

    @Test
    void shouldCountCniVerificationStatuses() {
        when(userRepository.count()).thenReturn(5L);
        when(peopleRepository.findAll()).thenReturn(createPeopleList());
        when(rideRepository.findAll()).thenReturn(Collections.emptyList());
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        AdminStatisticsDTO result = service.getAdminStatistics();

        assertThat(result.getVerifiedUsers()).isEqualTo(1);
        assertThat(result.getRejectedVerifications()).isEqualTo(1);
        assertThat(result.getPendingVerifications()).isEqualTo(1);
    }

    @Test
    void shouldGroupRidesByStatus() {
        when(userRepository.count()).thenReturn(1L);
        when(peopleRepository.findAll()).thenReturn(Collections.emptyList());
        when(rideRepository.findAll()).thenReturn(createRideList());
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        AdminStatisticsDTO result = service.getAdminStatistics();

        assertThat(result.getRidesByStatus()).containsEntry("OUVERT", 2L);
        assertThat(result.getRidesByStatus()).containsEntry("EFFECTUE", 1L);
        assertThat(result.getRidesByStatus()).containsEntry("ANNULE", 0L);
        assertThat(result.getRidesByStatus()).containsEntry("COMPLET", 0L);
    }

    @Test
    void shouldGroupBookingsByStatus() {
        when(userRepository.count()).thenReturn(1L);
        when(peopleRepository.findAll()).thenReturn(Collections.emptyList());
        when(rideRepository.findAll()).thenReturn(Collections.emptyList());
        when(bookingRepository.findAll()).thenReturn(createBookingList());

        AdminStatisticsDTO result = service.getAdminStatistics();

        assertThat(result.getBookingsByStatus()).containsEntry("EN_ATTENTE", 1L);
        assertThat(result.getBookingsByStatus()).containsEntry("CONFIRME", 1L);
        assertThat(result.getBookingsByStatus()).containsEntry("ANNULE", 0L);
    }

    @Test
    void shouldReturn12MonthsOfActivity() {
        when(userRepository.count()).thenReturn(1L);
        when(peopleRepository.findAll()).thenReturn(Collections.emptyList());
        when(rideRepository.findAll()).thenReturn(Collections.emptyList());
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        AdminStatisticsDTO result = service.getAdminStatistics();

        assertThat(result.getMonthlyActivity()).hasSize(12);
    }

    @Test
    void shouldReturnRecentActivity() {
        when(userRepository.count()).thenReturn(1L);
        when(peopleRepository.findAll()).thenReturn(createPeopleList());
        when(rideRepository.findAll()).thenReturn(createRideList());
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        AdminStatisticsDTO result = service.getAdminStatistics();

        assertThat(result.getRecentActivity()).isNotNull();
        assertThat(result.getRecentActivity().size()).isLessThanOrEqualTo(10);
    }

    @Test
    void shouldHandleEmptyData() {
        when(userRepository.count()).thenReturn(0L);
        when(peopleRepository.findAll()).thenReturn(Collections.emptyList());
        when(rideRepository.findAll()).thenReturn(Collections.emptyList());
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        AdminStatisticsDTO result = service.getAdminStatistics();

        assertThat(result.getTotalUsers()).isZero();
        assertThat(result.getTotalPeople()).isZero();
        assertThat(result.getTotalRides()).isZero();
        assertThat(result.getTotalBookings()).isZero();
        assertThat(result.getMonthlyActivity()).hasSize(12);
        assertThat(result.getRecentActivity()).isEmpty();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private List<People> createPeopleList() {
        People p1 = new People();
        p1.setCniStatut("VERIFIED");
        p1.setPrenom("Jean");
        p1.setNom("Dupont");
        p1.setCniVerifieAt(Instant.now());

        People p2 = new People();
        p2.setCniStatut("REJECTED");

        People p3 = new People();
        p3.setCniStatut(null); // pending

        return Arrays.asList(p1, p2, p3);
    }

    private List<Ride> createRideList() {
        Ride r1 = new Ride();
        r1.setStatut(RideStatusEnum.OUVERT);
        r1.setVilleDepart("Douala");
        r1.setVilleArrivee("Yaoundé");
        r1.setCreatedDate(Instant.now());

        Ride r2 = new Ride();
        r2.setStatut(RideStatusEnum.OUVERT);
        r2.setVilleDepart("Yaoundé");
        r2.setVilleArrivee("Bafoussam");
        r2.setCreatedDate(Instant.now());

        Ride r3 = new Ride();
        r3.setStatut(RideStatusEnum.EFFECTUE);
        r3.setVilleDepart("Douala");
        r3.setVilleArrivee("Kribi");
        r3.setCreatedDate(Instant.now());

        return Arrays.asList(r1, r2, r3);
    }

    private List<Booking> createBookingList() {
        Booking b1 = new Booking();
        b1.setStatut(BookingStatusEnum.EN_ATTENTE);
        b1.setDateReservation(LocalDate.now());

        Booking b2 = new Booking();
        b2.setStatut(BookingStatusEnum.CONFIRME);
        b2.setDateReservation(LocalDate.now());

        return Arrays.asList(b1, b2);
    }
}
