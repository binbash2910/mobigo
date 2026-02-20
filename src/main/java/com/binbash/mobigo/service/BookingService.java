package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.Payment;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import com.binbash.mobigo.domain.enumeration.RideStatusEnum;
import com.binbash.mobigo.repository.BookingRepository;
import com.binbash.mobigo.repository.RideRepository;
import com.binbash.mobigo.repository.search.BookingSearchRepository;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing booking lifecycle (create, accept, reject, cancel).
 */
@Service
@Transactional
public class BookingService {

    private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);
    private static final String ENTITY_NAME = "booking";

    private final BookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final BookingSearchRepository bookingSearchRepository;
    private final PaymentService paymentService;
    private final MailService mailService;
    private final EntityManager entityManager;

    public BookingService(
        BookingRepository bookingRepository,
        RideRepository rideRepository,
        BookingSearchRepository bookingSearchRepository,
        PaymentService paymentService,
        MailService mailService,
        EntityManager entityManager
    ) {
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
        this.bookingSearchRepository = bookingSearchRepository;
        this.paymentService = paymentService;
        this.mailService = mailService;
        this.entityManager = entityManager;
    }

    /**
     * Create a new booking with EN_ATTENTE status.
     * Validates ride is OUVERT and has enough seats.
     * Does NOT decrement seats (deferred to accept).
     */
    public Booking createBooking(Booking booking) {
        LOG.debug("Request to create Booking : {}", booking);

        if (booking.getId() != null) {
            throw new BadRequestAlertException("A new booking cannot already have an ID", ENTITY_NAME, "idexists");
        }

        Ride ride = rideRepository
            .findById(booking.getTrajet().getId())
            .orElseThrow(() -> new BadRequestAlertException("Ride not found", "ride", "idnotfound"));

        if (ride.getStatut() != RideStatusEnum.OUVERT) {
            throw new BadRequestAlertException("Cannot book a ride with status: " + ride.getStatut(), ENTITY_NAME, "rideclosed");
        }

        if (ride.getNbrePlaceDisponible() < booking.getNbPlacesReservees()) {
            throw new BadRequestAlertException("Not enough available seats", ENTITY_NAME, "noseats");
        }

        // Force status and date
        booking.setStatut(BookingStatusEnum.EN_ATTENTE);
        booking.setDateReservation(LocalDate.now());

        booking = bookingRepository.save(booking);
        bookingSearchRepository.index(booking);

        LOG.info(
            "Booking {} created for ride {} with {} seats (EN_ATTENTE)",
            booking.getId(),
            ride.getId(),
            booking.getNbPlacesReservees()
        );

        // Send email notifications
        sendBookingEmails(booking.getId(), "NEW_BOOKING");

        return booking;
    }

    /**
     * Accept a booking: EN_ATTENTE -> CONFIRME.
     * Decrements available seats. Processes payment if method specified.
     * Updates ride status to COMPLET if full.
     */
    public Booking acceptBooking(Long bookingId) {
        LOG.debug("Request to accept Booking : {}", bookingId);

        Booking booking = bookingRepository
            .findById(bookingId)
            .orElseThrow(() -> new BadRequestAlertException("Booking not found", ENTITY_NAME, "idnotfound"));

        if (booking.getStatut() != BookingStatusEnum.EN_ATTENTE) {
            throw new BadRequestAlertException("Cannot accept booking with status: " + booking.getStatut(), ENTITY_NAME, "invalidstatus");
        }

        Ride ride = rideRepository
            .findById(booking.getTrajet().getId())
            .orElseThrow(() -> new BadRequestAlertException("Ride not found", "ride", "idnotfound"));

        int requestedSeats = booking.getNbPlacesReservees().intValue();
        if (ride.getNbrePlaceDisponible() < requestedSeats) {
            throw new BadRequestAlertException("Not enough available seats to accept this booking", ENTITY_NAME, "noseats");
        }

        // Decrement available seats
        ride.setNbrePlaceDisponible(ride.getNbrePlaceDisponible() - requestedSeats);

        // Auto-set ride to COMPLET if no more seats
        if (ride.getNbrePlaceDisponible() <= 0) {
            ride.setStatut(RideStatusEnum.COMPLET);
            LOG.info("Ride {} is now COMPLET (all seats filled)", ride.getId());
        }
        rideRepository.save(ride);

        // Update booking status
        booking.setStatut(BookingStatusEnum.CONFIRME);
        booking = bookingRepository.save(booking);
        bookingSearchRepository.index(booking);

        // Process payment if payment method was specified
        if (booking.getMethodePayment() != null) {
            try {
                Payment payment = paymentService.processPayment(bookingId, booking.getMethodePayment());
                paymentService.confirmPayment(payment.getId());
                LOG.info("Payment processed and confirmed for booking {}", bookingId);
            } catch (Exception e) {
                LOG.warn("Payment processing failed for booking {}: {}", bookingId, e.getMessage());
            }
        }

        LOG.info("Booking {} accepted for ride {}", bookingId, ride.getId());

        // Send email notifications
        sendBookingEmails(bookingId, "ACCEPTED");

        return booking;
    }

    /**
     * Reject a booking: EN_ATTENTE -> REFUSE.
     * No seat changes needed.
     */
    public Booking rejectBooking(Long bookingId) {
        LOG.debug("Request to reject Booking : {}", bookingId);

        Booking booking = bookingRepository
            .findById(bookingId)
            .orElseThrow(() -> new BadRequestAlertException("Booking not found", ENTITY_NAME, "idnotfound"));

        if (booking.getStatut() != BookingStatusEnum.EN_ATTENTE) {
            throw new BadRequestAlertException("Cannot reject booking with status: " + booking.getStatut(), ENTITY_NAME, "invalidstatus");
        }

        booking.setStatut(BookingStatusEnum.REFUSE);
        booking = bookingRepository.save(booking);
        bookingSearchRepository.index(booking);

        LOG.info("Booking {} rejected for ride {}", bookingId, booking.getTrajet().getId());

        // Send email notifications
        sendBookingEmails(bookingId, "REJECTED");

        return booking;
    }

    /**
     * Cancel a booking: EN_ATTENTE or CONFIRME -> ANNULE.
     * If was CONFIRME, restores seats and potentially reopens ride.
     */
    public Booking cancelBooking(Long bookingId) {
        LOG.debug("Request to cancel Booking : {}", bookingId);

        Booking booking = bookingRepository
            .findById(bookingId)
            .orElseThrow(() -> new BadRequestAlertException("Booking not found", ENTITY_NAME, "idnotfound"));

        if (booking.getStatut() != BookingStatusEnum.EN_ATTENTE && booking.getStatut() != BookingStatusEnum.CONFIRME) {
            throw new BadRequestAlertException("Cannot cancel booking with status: " + booking.getStatut(), ENTITY_NAME, "invalidstatus");
        }

        boolean wasConfirmed = booking.getStatut() == BookingStatusEnum.CONFIRME;

        booking.setStatut(BookingStatusEnum.ANNULE);
        booking = bookingRepository.save(booking);
        bookingSearchRepository.index(booking);

        // If was confirmed, restore seats and potentially reopen ride
        if (wasConfirmed) {
            Ride ride = rideRepository
                .findById(booking.getTrajet().getId())
                .orElseThrow(() -> new BadRequestAlertException("Ride not found", "ride", "idnotfound"));

            int restoredSeats = booking.getNbPlacesReservees().intValue();
            ride.setNbrePlaceDisponible(ride.getNbrePlaceDisponible() + restoredSeats);

            if (ride.getStatut() == RideStatusEnum.COMPLET) {
                ride.setStatut(RideStatusEnum.OUVERT);
                LOG.info("Ride {} reopened (COMPLET -> OUVERT)", ride.getId());
            }
            rideRepository.save(ride);

            LOG.info("Booking {} cancelled (was CONFIRME). {} seats restored for ride {}", bookingId, restoredSeats, ride.getId());
        } else {
            LOG.info("Booking {} cancelled (was EN_ATTENTE)", bookingId);
        }

        // Send email notifications
        sendBookingEmails(bookingId, "CANCELLED");

        return booking;
    }

    /**
     * Load booking with all relations and send notification emails to both passenger and driver.
     */
    private void sendBookingEmails(Long bookingId, String action) {
        try {
            // Flush pending changes to DB and clear first-level cache
            // so that findByIdWithRelations loads fresh entities with all associations
            entityManager.flush();
            entityManager.clear();

            Booking fullBooking = bookingRepository.findByIdWithRelations(bookingId).orElse(null);
            if (fullBooking == null) {
                LOG.warn("Cannot send booking notification: booking {} not found with relations", bookingId);
                return;
            }

            Ride ride = fullBooking.getTrajet();
            People passenger = fullBooking.getPassager();
            People driver = ride.getVehicule() != null && ride.getVehicule().getProprietaire() != null
                ? ride.getVehicule().getProprietaire()
                : null;

            LOG.info(
                "Booking {} email: passenger={} (user={}), driver={} (user={})",
                bookingId,
                passenger != null ? passenger.getId() : "null",
                passenger != null && passenger.getUser() != null ? passenger.getUser().getEmail() : "null",
                driver != null ? driver.getId() : "null",
                driver != null && driver.getUser() != null ? driver.getUser().getEmail() : "null"
            );

            // Notify passenger
            if (passenger != null && passenger.getUser() != null) {
                mailService.sendBookingNotificationEmail(
                    passenger.getUser().getEmail(),
                    passenger.getPrenom() != null ? passenger.getPrenom() : passenger.getNom(),
                    fullBooking,
                    ride,
                    action,
                    false
                );
            } else {
                LOG.warn("Cannot notify passenger for booking {}: passenger or user is null", bookingId);
            }

            // Notify driver
            if (driver != null && driver.getUser() != null) {
                mailService.sendBookingNotificationEmail(
                    driver.getUser().getEmail(),
                    driver.getPrenom() != null ? driver.getPrenom() : driver.getNom(),
                    fullBooking,
                    ride,
                    action,
                    true
                );
            } else {
                LOG.warn("Cannot notify driver for booking {}: driver or user is null", bookingId);
            }
        } catch (Exception e) {
            LOG.warn("Failed to send booking notification emails for booking {}: {}", bookingId, e.getMessage(), e);
        }
    }
}
