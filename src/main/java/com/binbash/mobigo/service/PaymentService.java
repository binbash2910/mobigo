package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.Payment;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.domain.enumeration.BookingStatusEnum;
import com.binbash.mobigo.domain.enumeration.PaymentMethodEnum;
import com.binbash.mobigo.domain.enumeration.PaymentStatusEnum;
import com.binbash.mobigo.repository.BookingRepository;
import com.binbash.mobigo.repository.PaymentRepository;
import com.binbash.mobigo.repository.SavedPaymentMethodRepository;
import com.binbash.mobigo.repository.search.PaymentSearchRepository;
import com.binbash.mobigo.security.SecurityUtils;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.binbash.mobigo.domain.Payment}.
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    private static final String ENTITY_NAME = "payment";

    private final PaymentRepository paymentRepository;

    private final BookingRepository bookingRepository;

    private final PaymentSearchRepository paymentSearchRepository;

    private final CampayService campayService;

    private final NotificationEventService notificationEventService;

    private final SavedPaymentMethodRepository savedPaymentMethodRepository;

    public PaymentService(
        PaymentRepository paymentRepository,
        BookingRepository bookingRepository,
        PaymentSearchRepository paymentSearchRepository,
        CampayService campayService,
        NotificationEventService notificationEventService,
        SavedPaymentMethodRepository savedPaymentMethodRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.paymentSearchRepository = paymentSearchRepository;
        this.campayService = campayService;
        this.notificationEventService = notificationEventService;
        this.savedPaymentMethodRepository = savedPaymentMethodRepository;
    }

    /**
     * Process a payment for a booking.
     *
     * @param bookingId the booking to pay for.
     * @param methode the payment method.
     * @return the created payment.
     */
    public Payment processPayment(Long bookingId, PaymentMethodEnum methode) {
        LOG.debug("Request to process payment for booking {} with method {}", bookingId, methode);

        Booking booking = bookingRepository
            .findById(bookingId)
            .orElseThrow(() -> new BadRequestAlertException("Booking not found", "booking", "idnotfound"));

        // Check if booking already has a payment
        Optional<Payment> existingPayment = paymentRepository.findByBookingId(bookingId);
        if (existingPayment.isPresent()) {
            throw new BadRequestAlertException("Booking already has a payment", ENTITY_NAME, "paymentexists");
        }

        Payment payment = new Payment();
        payment.setMontant(booking.getMontantTotal());
        payment.setDatePaiement(LocalDate.now());
        payment.setMethode(methode);
        payment.setStatut(PaymentStatusEnum.EN_ATTENTE);
        payment.setBooking(booking);

        payment = paymentRepository.save(payment);
        paymentSearchRepository.index(payment);

        LOG.debug("Payment created with id {} for booking {}", payment.getId(), bookingId);
        return payment;
    }

    /**
     * Get all payments for the current authenticated user.
     *
     * @return the list of payments.
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "user", "notauthenticated"));
        LOG.debug("Request to get payments for user {}", login);
        return paymentRepository.findByBookingPassagerUserLogin(login);
    }

    /**
     * Get the payment for a specific booking.
     *
     * @param bookingId the booking id.
     * @return the payment if found.
     */
    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentByBooking(Long bookingId) {
        LOG.debug("Request to get payment for booking {}", bookingId);
        return paymentRepository.findByBookingId(bookingId);
    }

    /**
     * Update the status of a payment.
     *
     * @param paymentId the payment id.
     * @param statut the new status.
     * @return the updated payment.
     */
    public Payment updatePaymentStatus(Long paymentId, PaymentStatusEnum statut) {
        LOG.debug("Request to update payment {} status to {}", paymentId, statut);

        Payment payment = paymentRepository
            .findById(paymentId)
            .orElseThrow(() -> new BadRequestAlertException("Payment not found", ENTITY_NAME, "idnotfound"));

        payment.setStatut(statut);
        payment = paymentRepository.save(payment);
        paymentSearchRepository.index(payment);

        return payment;
    }

    /**
     * Confirm a payment (set status to REUSSI).
     *
     * @param paymentId the payment id.
     * @return the confirmed payment.
     */
    public Payment confirmPayment(Long paymentId) {
        return updatePaymentStatus(paymentId, PaymentStatusEnum.REUSSI);
    }

    /**
     * Fail a payment (set status to ECHOUE).
     *
     * @param paymentId the payment id.
     * @return the failed payment.
     */
    public Payment failPayment(Long paymentId) {
        return updatePaymentStatus(paymentId, PaymentStatusEnum.ECHOUE);
    }

    /**
     * Save a payment.
     *
     * @param payment the entity to save.
     * @return the persisted entity.
     */
    public Payment save(Payment payment) {
        LOG.debug("Request to save Payment : {}", payment);
        payment = paymentRepository.save(payment);
        paymentSearchRepository.index(payment);
        return payment;
    }

    /**
     * Get one payment by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Payment> findOne(Long id) {
        LOG.debug("Request to get Payment : {}", id);
        return paymentRepository.findById(id);
    }

    /**
     * Get all payments.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<Payment> findAll() {
        LOG.debug("Request to get all Payments");
        return paymentRepository.findAll();
    }

    /**
     * Delete the payment by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Payment : {}", id);
        paymentRepository.deleteById(id);
        paymentSearchRepository.deleteFromIndexById(id);
    }

    // -------------------------------------------------------------------------
    // Campay escrow methods
    // -------------------------------------------------------------------------

    /**
     * Initiate a Campay collect for a booking.
     * Called when the passenger confirms payment after driver acceptance.
     */
    public Payment initiateCollect(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // Check existing payment
        Payment existingPayment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (existingPayment != null && existingPayment.getStatut() != PaymentStatusEnum.ECHOUE) {
            throw new RuntimeException("Payment already exists for booking " + bookingId);
        }

        People passenger = booking.getPassager();
        String phoneNumber = resolvePhoneNumber(passenger, booking.getMethodePayment());
        String operateur = booking.getMethodePayment() == PaymentMethodEnum.ORANGE_MONEY ? "OM" : "MOMO";

        int amount = Math.round(booking.getMontantTotal());
        String externalRef = "PAY-" + bookingId + "-" + System.currentTimeMillis();
        String description = "Mobigo trajet #" + booking.getTrajet().getId();

        Payment payment = existingPayment != null ? existingPayment : new Payment();
        payment.setMontant(booking.getMontantTotal());
        payment.setDatePaiement(LocalDate.now());
        payment.setMethode(booking.getMethodePayment());
        payment.setStatut(PaymentStatusEnum.EN_COURS);
        payment.setBooking(booking);
        payment.setExternalReference(externalRef);
        payment.setOperateur(operateur);
        payment.setPhoneNumber(phoneNumber);

        try {
            CampayService.CollectResponse response = campayService.collect(phoneNumber, amount, externalRef, description);
            payment.setCampayTransactionId(response.reference());
            payment = paymentRepository.save(payment);

            LOG.info("Campay collect initiated for booking {} - ref: {}", bookingId, externalRef);
            return payment;
        } catch (Exception e) {
            LOG.error("Campay collect failed for booking {}: {}", bookingId, e.getMessage());
            payment.setStatut(PaymentStatusEnum.ECHOUE);
            paymentRepository.save(payment);
            notificationEventService.onPaymentFailed(booking);
            throw new RuntimeException("Payment initiation failed: " + e.getMessage());
        }
    }

    /**
     * Handle Campay webhook callback.
     */
    public void handleWebhook(String reference, String status, String externalReference) {
        Payment payment = paymentRepository
            .findByExternalReference(externalReference)
            .or(() -> paymentRepository.findByCampayTransactionId(reference))
            .orElse(null);

        if (payment == null) {
            LOG.warn("Webhook for unknown payment: ref={}, extRef={}", reference, externalReference);
            return;
        }

        Booking booking = payment.getBooking();

        if ("SUCCESSFUL".equalsIgnoreCase(status)) {
            payment.setStatut(PaymentStatusEnum.COLLECTE_REUSSIE);
            payment.setCampayTransactionId(reference);
            paymentRepository.save(payment);
            notificationEventService.onPaymentSuccess(booking);
            LOG.info("Payment collected for booking {}", booking.getId());
        } else if ("FAILED".equalsIgnoreCase(status)) {
            payment.setStatut(PaymentStatusEnum.ECHOUE);
            paymentRepository.save(payment);
            notificationEventService.onPaymentFailed(booking);
            LOG.info("Payment failed for booking {}", booking.getId());
        }
    }

    /**
     * Disburse funds to driver for all completed bookings of a ride.
     */
    public void disburseToDriver(Long rideId, Ride ride, People driver) {
        List<Booking> paidBookings = bookingRepository
            .findByTrajetId(rideId)
            .stream()
            .filter(b -> b.getStatut() == BookingStatusEnum.CONFIRME)
            .filter(b -> {
                Payment p = paymentRepository.findByBookingId(b.getId()).orElse(null);
                return p != null && p.getStatut() == PaymentStatusEnum.COLLECTE_REUSSIE;
            })
            .collect(Collectors.toList());

        if (paidBookings.isEmpty()) {
            LOG.debug("No payments to disburse for ride {}", rideId);
            return;
        }

        String driverPhone = resolveDriverPhone(driver);
        if (driverPhone == null) {
            LOG.error("Driver {} has no mobile money account for disbursement", driver.getId());
            return;
        }

        float totalToDisburse = 0;
        for (Booking booking : paidBookings) {
            float driverAmount = booking.getMontantTotal() - booking.getCommission();
            totalToDisburse += driverAmount;

            Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
            if (payment != null) {
                String disbRef = "DIS-" + booking.getId() + "-" + System.currentTimeMillis();
                try {
                    CampayService.DisbursementResponse resp = campayService.disburse(
                        driverPhone,
                        Math.round(driverAmount),
                        disbRef,
                        "Mobigo versement trajet #" + rideId
                    );
                    payment.setDisbursementReference(resp.reference());
                    payment.setDisbursementStatus(resp.status());
                    payment.setStatut(PaymentStatusEnum.REUSSI);
                    paymentRepository.save(payment);
                } catch (Exception e) {
                    LOG.error("Disbursement failed for payment {}: {}", payment.getId(), e.getMessage());
                    payment.setDisbursementStatus("FAILED");
                    paymentRepository.save(payment);
                }
            }
        }

        notificationEventService.onPaymentDisbursed(ride, driver, totalToDisburse);
        LOG.info("Disbursed {} FCFA to driver {} for ride {}", Math.round(totalToDisburse), driver.getId(), rideId);
    }

    /**
     * Refund passenger for a booking.
     */
    public void refundPassenger(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment == null || payment.getStatut() != PaymentStatusEnum.COLLECTE_REUSSIE) {
            return;
        }

        Booking booking = payment.getBooking();
        String refundRef = "REF-" + bookingId + "-" + System.currentTimeMillis();
        int amount = Math.round(payment.getMontant());

        try {
            campayService.disburse(payment.getPhoneNumber(), amount, refundRef, "Mobigo remboursement");
            payment.setStatut(PaymentStatusEnum.REMBOURSE);
            payment.setDisbursementReference(refundRef);
            paymentRepository.save(payment);
            notificationEventService.onPaymentRefunded(booking);
            LOG.info("Refunded {} FCFA for booking {}", amount, bookingId);
        } catch (Exception e) {
            LOG.error("Refund failed for booking {}: {}", bookingId, e.getMessage());
        }
    }

    private String resolvePhoneNumber(People passenger, PaymentMethodEnum method) {
        return savedPaymentMethodRepository
            .findByProprietaireId(passenger.getId())
            .stream()
            .filter(m -> m.getType() == method)
            .filter(m -> Boolean.TRUE.equals(m.getIsDefault()))
            .findFirst()
            .or(() ->
                savedPaymentMethodRepository.findByProprietaireId(passenger.getId()).stream().filter(m -> m.getType() == method).findFirst()
            )
            .map(m -> m.getPhone())
            .orElseThrow(() -> new RuntimeException("No phone number found for payment method " + method));
    }

    private String resolveDriverPhone(People driver) {
        return savedPaymentMethodRepository
            .findByProprietaireId(driver.getId())
            .stream()
            .filter(m -> m.getType() == PaymentMethodEnum.ORANGE_MONEY || m.getType() == PaymentMethodEnum.MTN_MOBILE_MONEY)
            .filter(m -> Boolean.TRUE.equals(m.getIsDefault()))
            .findFirst()
            .or(() ->
                savedPaymentMethodRepository
                    .findByProprietaireId(driver.getId())
                    .stream()
                    .filter(m -> m.getType() == PaymentMethodEnum.ORANGE_MONEY || m.getType() == PaymentMethodEnum.MTN_MOBILE_MONEY)
                    .findFirst()
            )
            .map(m -> m.getPhone())
            .orElse(null);
    }
}
