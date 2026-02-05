package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import com.binbash.mobigo.domain.enumeration.RideStatusEnum;
import com.binbash.mobigo.repository.BookingRepository;
import com.binbash.mobigo.repository.RideRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing ride lifecycle (complete, cancel) with cascade logic on bookings.
 */
@Service
@Transactional
public class RideService {

    private static final Logger LOG = LoggerFactory.getLogger(RideService.class);

    private final RideRepository rideRepository;
    private final BookingRepository bookingRepository;

    public RideService(RideRepository rideRepository, BookingRepository bookingRepository) {
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Mark a ride as completed (EFFECTUE).
     * Only rides with status OUVERT or COMPLET can be completed.
     * All confirmed/pending bookings are marked as CONFIRME (trip happened as planned).
     *
     * @param rideId the ride ID
     * @return the updated ride
     * @throws IllegalStateException if the ride cannot be completed
     */
    public Ride completeRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new IllegalArgumentException("Ride not found with id: " + rideId));

        if (ride.getStatut() != RideStatusEnum.OUVERT && ride.getStatut() != RideStatusEnum.COMPLET) {
            throw new IllegalStateException(
                "Cannot complete a ride with status: " + ride.getStatut() + ". Only OUVERT or COMPLET rides can be completed."
            );
        }

        LOG.info("Completing ride {} (previous status: {})", rideId, ride.getStatut());

        // Update ride status
        ride.setStatut(RideStatusEnum.EFFECTUE);
        ride = rideRepository.save(ride);

        // Confirm all pending bookings (the trip happened)
        List<Booking> activeBookings = bookingRepository.findByTrajetId(rideId);
        for (Booking booking : activeBookings) {
            if (booking.getStatut() == BookingStatusEnum.EN_ATTENTE) {
                booking.setStatut(BookingStatusEnum.CONFIRME);
                bookingRepository.save(booking);
                LOG.debug("Confirmed pending booking {} for completed ride {}", booking.getId(), rideId);
            }
        }

        LOG.info("Ride {} completed successfully", rideId);
        return ride;
    }

    /**
     * Cancel a ride (ANNULE).
     * Only rides with status OUVERT or COMPLET can be cancelled.
     * All active bookings (CONFIRME, EN_ATTENTE) are cancelled and available seats are restored.
     *
     * @param rideId the ride ID
     * @return the updated ride
     * @throws IllegalStateException if the ride cannot be cancelled
     */
    public Ride cancelRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId).orElseThrow(() -> new IllegalArgumentException("Ride not found with id: " + rideId));

        if (ride.getStatut() != RideStatusEnum.OUVERT && ride.getStatut() != RideStatusEnum.COMPLET) {
            throw new IllegalStateException(
                "Cannot cancel a ride with status: " + ride.getStatut() + ". Only OUVERT or COMPLET rides can be cancelled."
            );
        }

        LOG.info("Cancelling ride {} (previous status: {})", rideId, ride.getStatut());

        // Cancel all active bookings and restore seats
        List<Booking> activeBookings = bookingRepository.findByTrajetId(rideId);
        int restoredSeats = 0;
        for (Booking booking : activeBookings) {
            if (booking.getStatut() == BookingStatusEnum.CONFIRME || booking.getStatut() == BookingStatusEnum.EN_ATTENTE) {
                booking.setStatut(BookingStatusEnum.ANNULE);
                bookingRepository.save(booking);
                restoredSeats += booking.getNbPlacesReservees() != null ? booking.getNbPlacesReservees().intValue() : 0;
                LOG.debug("Cancelled booking {} for cancelled ride {}", booking.getId(), rideId);
            }
        }

        // Update ride status
        ride.setStatut(RideStatusEnum.ANNULE);
        ride = rideRepository.save(ride);

        LOG.info("Ride {} cancelled successfully. {} bookings cancelled, {} seats restored.", rideId, activeBookings.size(), restoredSeats);
        return ride;
    }
}
