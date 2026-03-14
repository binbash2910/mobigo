package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.Rating;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import com.binbash.mobigo.domain.enumeration.NotificationType;
import com.binbash.mobigo.repository.BookingRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for creating notifications in response to domain events
 * (bookings, ride changes, ratings).
 *
 * Each method is wrapped in try/catch so that notification failures
 * never break the calling domain logic.
 */
@Service
@Transactional
public class NotificationEventService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationEventService.class);

    private final NotificationService notificationService;
    private final BookingRepository bookingRepository;

    public NotificationEventService(NotificationService notificationService, BookingRepository bookingRepository) {
        this.notificationService = notificationService;
        this.bookingRepository = bookingRepository;
    }

    // -------------------------------------------------------------------------
    // Booking events
    // -------------------------------------------------------------------------

    /**
     * A new booking was created -- notify the driver.
     */
    public void onBookingCreated(Booking booking, Ride ride) {
        try {
            People driver = getDriver(ride);
            if (driver == null || driver.getUser() == null) {
                LOG.warn("Cannot notify driver for new booking {}: driver or user is null", booking.getId());
                return;
            }

            People passenger = booking.getPassager();
            String passengerName = formatName(passenger);
            String route = formatRoute(ride);

            String title = "Nouvelle r\u00e9servation";
            String message =
                passengerName + " souhaite r\u00e9server " + booking.getNbPlacesReservees() + " place(s) sur votre trajet " + route;

            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());
            data.put("rideId", ride.getId());

            notificationService.createAndSend(
                driver.getUser().getLogin(),
                driver.getId(),
                NotificationType.BOOKING_RECEIVED,
                title,
                message,
                data
            );
        } catch (Exception e) {
            LOG.warn("Failed to create notification for new booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    /**
     * A booking was accepted -- notify the passenger.
     */
    public void onBookingAccepted(Booking booking, Ride ride) {
        try {
            People passenger = booking.getPassager();
            if (passenger == null || passenger.getUser() == null) {
                LOG.warn("Cannot notify passenger for accepted booking {}: passenger or user is null", booking.getId());
                return;
            }

            String route = formatRoute(ride);

            String title = "R\u00e9servation accept\u00e9e";
            String message = "Votre r\u00e9servation pour " + route + " a \u00e9t\u00e9 accept\u00e9e !";

            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());
            data.put("rideId", ride.getId());

            notificationService.createAndSend(
                passenger.getUser().getLogin(),
                passenger.getId(),
                NotificationType.BOOKING_ACCEPTED,
                title,
                message,
                data
            );
        } catch (Exception e) {
            LOG.warn("Failed to create notification for accepted booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    /**
     * A booking was rejected -- notify the passenger.
     */
    public void onBookingRejected(Booking booking, Ride ride) {
        try {
            People passenger = booking.getPassager();
            if (passenger == null || passenger.getUser() == null) {
                LOG.warn("Cannot notify passenger for rejected booking {}: passenger or user is null", booking.getId());
                return;
            }

            String route = formatRoute(ride);

            String title = "R\u00e9servation refus\u00e9e";
            String message = "Votre r\u00e9servation pour " + route + " a \u00e9t\u00e9 refus\u00e9e.";

            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());
            data.put("rideId", ride.getId());

            notificationService.createAndSend(
                passenger.getUser().getLogin(),
                passenger.getId(),
                NotificationType.BOOKING_REJECTED,
                title,
                message,
                data
            );
        } catch (Exception e) {
            LOG.warn("Failed to create notification for rejected booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    /**
     * A booking was cancelled -- notify the other party.
     *
     * @param cancelledByDriver true if driver cancelled, false if passenger cancelled
     */
    public void onBookingCancelled(Booking booking, Ride ride, boolean cancelledByDriver) {
        try {
            String route = formatRoute(ride);
            String title = "R\u00e9servation annul\u00e9e";
            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());
            data.put("rideId", ride.getId());

            if (cancelledByDriver) {
                // Notify passenger that the driver cancelled their booking
                People passenger = booking.getPassager();
                if (passenger == null || passenger.getUser() == null) {
                    LOG.warn("Cannot notify passenger for cancelled booking {}: passenger or user is null", booking.getId());
                    return;
                }
                String message = "Votre r\u00e9servation pour " + route + " a \u00e9t\u00e9 annul\u00e9e par le conducteur.";

                notificationService.createAndSend(
                    passenger.getUser().getLogin(),
                    passenger.getId(),
                    NotificationType.BOOKING_CANCELLED,
                    title,
                    message,
                    data
                );
            } else {
                // Notify driver that the passenger cancelled
                People driver = getDriver(ride);
                if (driver == null || driver.getUser() == null) {
                    LOG.warn("Cannot notify driver for cancelled booking {}: driver or user is null", booking.getId());
                    return;
                }
                People passenger = booking.getPassager();
                String passengerName = formatName(passenger);
                String message = passengerName + " a annul\u00e9 sa r\u00e9servation pour " + route + ".";

                notificationService.createAndSend(
                    driver.getUser().getLogin(),
                    driver.getId(),
                    NotificationType.BOOKING_CANCELLED,
                    title,
                    message,
                    data
                );
            }
        } catch (Exception e) {
            LOG.warn("Failed to create notification for cancelled booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Ride events
    // -------------------------------------------------------------------------

    /**
     * A ride was cancelled -- notify all booked passengers.
     */
    public void onTripCancelled(Ride ride) {
        try {
            String route = formatRoute(ride);
            String title = "Trajet annul\u00e9";
            String message = "Le trajet " + route + " a \u00e9t\u00e9 annul\u00e9.";

            List<Booking> bookings = bookingRepository.findByTrajetIdWithRelations(ride.getId());
            for (Booking booking : bookings) {
                if (
                    booking.getStatut() == BookingStatusEnum.CONFIRME ||
                    booking.getStatut() == BookingStatusEnum.EN_ATTENTE ||
                    booking.getStatut() == BookingStatusEnum.ANNULE
                ) {
                    People passenger = booking.getPassager();
                    if (passenger != null && passenger.getUser() != null) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("bookingId", booking.getId());
                        data.put("rideId", ride.getId());

                        try {
                            notificationService.createAndSend(
                                passenger.getUser().getLogin(),
                                passenger.getId(),
                                NotificationType.TRIP_CANCELLED,
                                title,
                                message,
                                data
                            );
                        } catch (Exception e) {
                            LOG.warn(
                                "Failed to notify passenger {} for cancelled ride {}: {}",
                                passenger.getId(),
                                ride.getId(),
                                e.getMessage()
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to create notifications for cancelled ride {}: {}", ride.getId(), e.getMessage());
        }
    }

    /**
     * A ride was modified -- notify all booked passengers.
     */
    public void onTripModified(Ride ride) {
        try {
            String route = formatRoute(ride);
            String title = "Trajet modifi\u00e9";
            String message = "Le trajet " + route + " a \u00e9t\u00e9 modifi\u00e9. V\u00e9rifiez les d\u00e9tails.";

            List<Booking> bookings = bookingRepository.findByTrajetIdWithRelations(ride.getId());
            for (Booking booking : bookings) {
                if (booking.getStatut() == BookingStatusEnum.CONFIRME || booking.getStatut() == BookingStatusEnum.EN_ATTENTE) {
                    People passenger = booking.getPassager();
                    if (passenger != null && passenger.getUser() != null) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("bookingId", booking.getId());
                        data.put("rideId", ride.getId());

                        try {
                            notificationService.createAndSend(
                                passenger.getUser().getLogin(),
                                passenger.getId(),
                                NotificationType.TRIP_MODIFIED,
                                title,
                                message,
                                data
                            );
                        } catch (Exception e) {
                            LOG.warn(
                                "Failed to notify passenger {} for modified ride {}: {}",
                                passenger.getId(),
                                ride.getId(),
                                e.getMessage()
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to create notifications for modified ride {}: {}", ride.getId(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Rating events
    // -------------------------------------------------------------------------

    /**
     * A rating was received -- notify the rated driver.
     */
    public void onRatingReceived(Rating rating, People driver) {
        try {
            if (driver == null || driver.getUser() == null) {
                LOG.warn("Cannot notify driver for rating {}: driver or user is null", rating.getId());
                return;
            }

            String title = "Nouvelle note re\u00e7ue";
            String noteValue = rating.getNote() != null ? String.format("%.0f", rating.getNote()) : "?";
            String message = "Vous avez re\u00e7u une note de " + noteValue + "/5";

            Map<String, Object> data = new HashMap<>();
            data.put("ratingId", rating.getId());
            if (rating.getTrajet() != null) {
                data.put("rideId", rating.getTrajet().getId());
            }

            notificationService.createAndSend(
                driver.getUser().getLogin(),
                driver.getId(),
                NotificationType.RATING_RECEIVED,
                title,
                message,
                data
            );
        } catch (Exception e) {
            LOG.warn("Failed to create notification for rating {}: {}", rating.getId(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Payment events
    // -------------------------------------------------------------------------

    public void onPaymentRequested(Booking booking) {
        try {
            People passenger = booking.getPassager();
            if (passenger == null || passenger.getUser() == null) return;

            String route = formatRoute(booking.getTrajet());
            String title = "Confirmez votre paiement";
            String message =
                "Votre réservation pour " +
                route +
                " a été acceptée. Veuillez confirmer le paiement de " +
                Math.round(booking.getMontantTotal()) +
                " FCFA.";

            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());
            data.put("rideId", booking.getTrajet().getId());
            data.put("action", "PAYMENT_REQUESTED");

            notificationService.createAndSend(
                passenger.getUser().getLogin(),
                passenger.getId(),
                NotificationType.PAYMENT_REQUESTED,
                title,
                message,
                data
            );
        } catch (Exception e) {
            LOG.warn("Failed to notify payment requested for booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    public void onPaymentSuccess(Booking booking) {
        try {
            People passenger = booking.getPassager();
            if (passenger == null || passenger.getUser() == null) return;

            String route = formatRoute(booking.getTrajet());
            String title = "Paiement confirmé";
            String message = "Votre paiement de " + Math.round(booking.getMontantTotal()) + " FCFA pour " + route + " a été confirmé.";

            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());
            data.put("rideId", booking.getTrajet().getId());

            notificationService.createAndSend(
                passenger.getUser().getLogin(),
                passenger.getId(),
                NotificationType.PAYMENT_SUCCESS,
                title,
                message,
                data
            );
        } catch (Exception e) {
            LOG.warn("Failed to notify payment success for booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    public void onPaymentFailed(Booking booking) {
        try {
            People passenger = booking.getPassager();
            if (passenger == null || passenger.getUser() == null) return;

            String route = formatRoute(booking.getTrajet());
            String title = "Paiement échoué";
            String message = "Votre paiement pour " + route + " a échoué. Veuillez réessayer.";

            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());
            data.put("rideId", booking.getTrajet().getId());
            data.put("action", "PAYMENT_FAILED");

            notificationService.createAndSend(
                passenger.getUser().getLogin(),
                passenger.getId(),
                NotificationType.PAYMENT_FAILED,
                title,
                message,
                data
            );
        } catch (Exception e) {
            LOG.warn("Failed to notify payment failure for booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    public void onPaymentRefunded(Booking booking) {
        try {
            People passenger = booking.getPassager();
            if (passenger == null || passenger.getUser() == null) return;

            String route = formatRoute(booking.getTrajet());
            String title = "Remboursement effectué";
            String message = "Vous avez été remboursé de " + Math.round(booking.getMontantTotal()) + " FCFA pour " + route + ".";

            Map<String, Object> data = new HashMap<>();
            data.put("bookingId", booking.getId());

            notificationService.createAndSend(
                passenger.getUser().getLogin(),
                passenger.getId(),
                NotificationType.PAYMENT_REFUNDED,
                title,
                message,
                data
            );
        } catch (Exception e) {
            LOG.warn("Failed to notify payment refund for booking {}: {}", booking.getId(), e.getMessage());
        }
    }

    public void onPaymentDisbursed(Ride ride, People driver, float amount) {
        try {
            if (driver.getUser() == null) return;

            String route = formatRoute(ride);
            String title = "Versement reçu";
            String message = "Vous avez reçu " + Math.round(amount) + " FCFA pour votre trajet " + route + ".";

            Map<String, Object> data = new HashMap<>();
            data.put("rideId", ride.getId());

            notificationService.createAndSend(
                driver.getUser().getLogin(),
                driver.getId(),
                NotificationType.PAYMENT_DISBURSED,
                title,
                message,
                data
            );
        } catch (Exception e) {
            LOG.warn("Failed to notify payment disbursement for ride {}: {}", ride.getId(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Identity verification events
    // -------------------------------------------------------------------------

    public void onIdentityVerified(People people, String adminComment) {
        sendIdentityNotification(
            people,
            NotificationType.IDENTITY_VERIFIED,
            "Identit\u00e9 v\u00e9rifi\u00e9e",
            "Votre identit\u00e9 a \u00e9t\u00e9 v\u00e9rifi\u00e9e avec succ\u00e8s. Vous pouvez d\u00e9sormais profiter de toutes les fonctionnalit\u00e9s.",
            adminComment
        );
    }

    public void onIdentityRejected(People people, String adminComment) {
        sendIdentityNotification(
            people,
            NotificationType.IDENTITY_REJECTED,
            "V\u00e9rification refus\u00e9e",
            "Votre v\u00e9rification d'identit\u00e9 n'a pas pu \u00eatre valid\u00e9e. Veuillez soumettre \u00e0 nouveau votre document.",
            adminComment
        );
    }

    private void sendIdentityNotification(People people, NotificationType type, String title, String message, String adminComment) {
        try {
            if (people.getUser() == null) {
                LOG.warn("Cannot notify user for identity verification: user is null for people {}", people.getId());
                return;
            }

            if (adminComment != null && !adminComment.isBlank()) {
                message = message + " \u2014 " + adminComment;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("peopleId", people.getId());

            notificationService.createAndSend(people.getUser().getLogin(), people.getId(), type, title, message, data);
        } catch (Exception e) {
            LOG.warn("Failed to create notification for identity verification of people {}: {}", people.getId(), e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Get the driver (owner) from a ride via ride -> vehicule -> proprietaire.
     */
    private People getDriver(Ride ride) {
        if (ride.getVehicule() != null && ride.getVehicule().getProprietaire() != null) {
            return ride.getVehicule().getProprietaire();
        }
        return null;
    }

    /**
     * Format a person's name as "Pr\u00e9nom N." (e.g. "Jean D.").
     */
    private String formatName(People person) {
        if (person == null) {
            return "Un passager";
        }
        String prenom = person.getPrenom() != null ? person.getPrenom() : "";
        String nom = person.getNom() != null && !person.getNom().isEmpty() ? person.getNom().charAt(0) + "." : "";
        String fullName = (prenom + " " + nom).trim();
        return fullName.isEmpty() ? "Un passager" : fullName;
    }

    /**
     * Format route as "VilleDepart \u2192 VilleArrivee".
     */
    private String formatRoute(Ride ride) {
        String depart = ride.getVilleDepart() != null ? ride.getVilleDepart() : "?";
        String arrivee = ride.getVilleArrivee() != null ? ride.getVilleArrivee() : "?";
        return depart + " \u2192 " + arrivee;
    }
}
