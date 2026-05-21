package com.binbash.mobigo.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.binbash.mobigo.IntegrationTest;
import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.LedgerAccount;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.Vehicle;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import com.binbash.mobigo.domain.enumeration.LedgerAccountType;
import com.binbash.mobigo.domain.enumeration.RideStatusEnum;
import com.binbash.mobigo.repository.BookingRepository;
import com.binbash.mobigo.repository.LedgerAccountRepository;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.repository.RideRepository;
import com.binbash.mobigo.repository.VehicleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for the wallet hold/confirm/void flow exercised end-to-end
 * against the embedded database.
 */
@IntegrationTest
@Transactional
class WalletSettlementFlowIT {

    @Autowired
    private WalletService walletService;

    @Autowired
    private LedgerAccountRepository ledgerAccountRepository;

    @Autowired
    private PeopleRepository peopleRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private BookingRepository bookingRepository;

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private People buildPerson(String nom, String telephone, String cni) {
        People p = new People();
        p.setNom(nom);
        p.setTelephone(telephone);
        p.setCni(cni);
        p.setActif("Y");
        p.setDateNaissance(LocalDate.of(1990, 1, 1));
        return p;
    }

    private Vehicle buildVehicle(People owner) {
        Vehicle v = new Vehicle();
        v.setMarque("Toyota");
        v.setModele("Corolla");
        v.setAnnee("2020");
        v.setCarteGrise("CG-001");
        v.setImmatriculation("LT-123-AB");
        v.setNbPlaces(4);
        v.setCouleur("Blanc");
        v.setActif("Y");
        v.setProprietaire(owner);
        return v;
    }

    private Ride buildRide(Vehicle vehicle) {
        Ride r = new Ride();
        r.setVilleDepart("Yaoundé");
        r.setVilleArrivee("Douala");
        r.setDateDepart(LocalDate.now().plusDays(1));
        r.setDateArrivee(LocalDate.now().plusDays(1));
        r.setHeureDepart("08");
        r.setHeureArrivee("12");
        r.setMinuteDepart("00");
        r.setMinuteArrivee("00");
        r.setPrixParPlace(5000f);
        r.setNbrePlaceDisponible(3);
        r.setStatut(RideStatusEnum.OUVERT);
        r.setVehicule(vehicle);
        return r;
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    @Test
    void holdAndConfirmBookingSettlement() {
        // 1. Persist driver & passenger
        People driver = peopleRepository.saveAndFlush(buildPerson("Dupont", "+237699000001", "CNI-DRV-01"));
        People passenger = peopleRepository.saveAndFlush(buildPerson("Martin", "+237699000002", "CNI-PAS-01"));

        // 2. Persist vehicle and ride
        Vehicle vehicle = vehicleRepository.saveAndFlush(buildVehicle(driver));
        Ride ride = rideRepository.saveAndFlush(buildRide(vehicle));

        // 3. Seed passenger wallet with 20 000
        LedgerAccount passAccount = walletService.getOrCreateAccount(LedgerAccountType.PASSENGER, passenger.getId());
        passAccount.setBalance(new BigDecimal("20000"));
        ledgerAccountRepository.saveAndFlush(passAccount);

        // 4. Create booking: montantTotal=6000, commission=1000
        Booking booking = new Booking();
        booking.setNbPlacesReservees(1L);
        booking.setMontantTotal(6000f);
        booking.setCommission(1000f);
        booking.setDateReservation(LocalDate.now());
        booking.setStatut(BookingStatusEnum.EN_ATTENTE);
        booking.setPassager(passenger);
        booking.setTrajet(ride);
        booking = bookingRepository.saveAndFlush(booking);

        // 5. Hold for booking: reserves 6000 as DRAFT debit on passenger account
        walletService.holdForBooking(booking);

        // Available balance should be 20000 - 6000 = 14000
        BigDecimal available = walletService.availableBalance("PASSENGER:" + passenger.getId());
        assertThat(available).isEqualByComparingTo(new BigDecimal("14000"));

        // Posted balance should still be 20000 (DRAFT not yet applied)
        LedgerAccount passAfterHold = ledgerAccountRepository.findByAccountKey("PASSENGER:" + passenger.getId()).orElseThrow();
        assertThat(passAfterHold.getBalance()).isEqualByComparingTo(new BigDecimal("20000"));

        // 6. Confirm the settlement: DRAFT → POSTED, balances applied
        walletService.confirmBookingSettlement(booking.getId());

        // Passenger: 20000 - 6000 = 14000 posted
        LedgerAccount passAfterConfirm = ledgerAccountRepository.findByAccountKey("PASSENGER:" + passenger.getId()).orElseThrow();
        assertThat(passAfterConfirm.getBalance()).isEqualByComparingTo(new BigDecimal("14000"));

        // Driver: net = 6000 - 1000 = 5000 posted
        LedgerAccount driverAcc = ledgerAccountRepository.findByAccountKey("DRIVER:" + driver.getId()).orElseThrow();
        assertThat(driverAcc.getBalance()).isEqualByComparingTo(new BigDecimal("5000"));

        // Platform: commission = 1000 posted
        LedgerAccount platform = ledgerAccountRepository.findByAccountKey("PLATFORM").orElseThrow();
        assertThat(platform.getBalance()).isEqualByComparingTo(new BigDecimal("1000"));

        // Escrow: all movements cancel out → balance = 0
        LedgerAccount escrow = ledgerAccountRepository.findByAccountKey("ESCROW").orElseThrow();
        assertThat(escrow.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void voidBookingSettlementReleasesHold() {
        // 1. Persist driver & passenger
        People driver = peopleRepository.saveAndFlush(buildPerson("Leroy", "+237699000003", "CNI-DRV-02"));
        People passenger = peopleRepository.saveAndFlush(buildPerson("Bernard", "+237699000004", "CNI-PAS-02"));

        // 2. Persist vehicle and ride
        Vehicle vehicle = vehicleRepository.saveAndFlush(buildVehicle(driver));
        Ride ride = rideRepository.saveAndFlush(buildRide(vehicle));

        // 3. Seed passenger wallet with 10 000
        LedgerAccount passAccount = walletService.getOrCreateAccount(LedgerAccountType.PASSENGER, passenger.getId());
        passAccount.setBalance(new BigDecimal("10000"));
        ledgerAccountRepository.saveAndFlush(passAccount);

        // 4. Create and persist booking
        Booking booking = new Booking();
        booking.setNbPlacesReservees(1L);
        booking.setMontantTotal(6000f);
        booking.setCommission(1000f);
        booking.setDateReservation(LocalDate.now());
        booking.setStatut(BookingStatusEnum.EN_ATTENTE);
        booking.setPassager(passenger);
        booking.setTrajet(ride);
        booking = bookingRepository.saveAndFlush(booking);

        // 5. Hold then void
        walletService.holdForBooking(booking);
        walletService.voidBookingSettlement(booking.getId());

        // After void, available balance returns to full posted balance (10000)
        BigDecimal availableAfterVoid = walletService.availableBalance("PASSENGER:" + passenger.getId());
        assertThat(availableAfterVoid).isEqualByComparingTo(new BigDecimal("10000"));

        // Posted balance unchanged (hold was DRAFT only, never applied)
        LedgerAccount passAfterVoid = ledgerAccountRepository.findByAccountKey("PASSENGER:" + passenger.getId()).orElseThrow();
        assertThat(passAfterVoid.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
    }
}
