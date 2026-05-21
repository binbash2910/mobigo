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
     * Called automatically by BookingService.acceptBooking when the driver accepts a mobile-money booking,
     * or manually by the passenger via POST /api/payments/initiate-collect/{bookingId}.
     *
     * Idempotent: returns the existing payment if a collect is already in progress or succeeded.
     *
     * @deprecated Remplacé par le porte-monnaie interne (WalletService). Conservé
     * temporairement pour compatibilité jusqu'à la migration du frontend (Plan 2).
     */
    @Deprecated
    public Payment initiateCollect(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // Guard: only mobile money methods can be collected via Campay USSD
        PaymentMethodEnum method = booking.getMethodePayment();
        if (method != PaymentMethodEnum.ORANGE_MONEY && method != PaymentMethodEnum.MTN_MOBILE_MONEY) {
            throw new BadRequestAlertException(
                "Collect only supported for ORANGE_MONEY or MTN_MOBILE_MONEY (booking method: " + method + ")",
                ENTITY_NAME,
                "invalidmethod"
            );
        }

        // Idempotency: if a collect is already pending / successful, just return it
        Payment existingPayment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (existingPayment != null) {
            PaymentStatusEnum st = existingPayment.getStatut();
            if (st == PaymentStatusEnum.EN_COURS || st == PaymentStatusEnum.COLLECTE_REUSSIE || st == PaymentStatusEnum.REUSSI) {
                LOG.info("Campay collect already in state {} for booking {}, returning existing payment", st, bookingId);
                return existingPayment;
            }
            // For EN_ATTENTE / ECHOUE / REMBOURSE we retry with a fresh external reference
        }

        People passenger = booking.getPassager();
        String phoneNumber = booking.getTelephonePaiement() != null && !booking.getTelephonePaiement().isBlank()
            ? booking.getTelephonePaiement()
            : resolvePhoneNumber(passenger, method);

        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new BadRequestAlertException("No phone number available for collect", ENTITY_NAME, "nophonenumber");
        }
        phoneNumber = normalizePhone(phoneNumber);

        String operateur = method == PaymentMethodEnum.ORANGE_MONEY ? "OM" : "MOMO";

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
        // Track commission breakdown
        payment.setCommissionPlateforme(booking.getCommission());
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
     * Refresh a payment's status from Campay when it's still EN_COURS.
     * Avoids relying solely on the webhook — the frontend polls this endpoint.
     */
    public Payment refreshStatusFromCampay(Payment payment) {
        if (payment == null) return null;
        if (payment.getStatut() != PaymentStatusEnum.EN_COURS) return payment;
        String ref = payment.getCampayTransactionId() != null ? payment.getCampayTransactionId() : payment.getExternalReference();
        if (ref == null || ref.isBlank()) return payment;

        try {
            String remoteStatus = campayService.getTransactionStatus(ref);
            if ("SUCCESSFUL".equalsIgnoreCase(remoteStatus)) {
                payment.setStatut(PaymentStatusEnum.COLLECTE_REUSSIE);
                paymentRepository.save(payment);
                paymentSearchRepository.index(payment);
                if (payment.getBooking() != null) {
                    notificationEventService.onPaymentSuccess(payment.getBooking());
                }
            } else if ("FAILED".equalsIgnoreCase(remoteStatus)) {
                payment.setStatut(PaymentStatusEnum.ECHOUE);
                paymentRepository.save(payment);
                paymentSearchRepository.index(payment);
                if (payment.getBooking() != null) {
                    notificationEventService.onPaymentFailed(payment.getBooking());
                }
            }
        } catch (Exception e) {
            LOG.warn("Unable to refresh Campay status for payment {}: {}", payment.getId(), e.getMessage());
        }
        return payment;
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
     * On-demand payout to the current driver's mobile wallet.
     * Used by the frontend DriverPayout page (POST /api/payments/disburse).
     *
     * @param amount      XAF amount requested
     * @param phone       driver's mobile money number (will be normalized)
     * @param operator    "ORANGE" or "MTN" (informational)
     * @param description free-text description shown in Campay statement
     * @return the saved Payment (as DISBURSE type tracking row)
     */
    public Payment disburseOnDemand(int amount, String phone, String operator, String description) {
        if (amount <= 0) {
            throw new BadRequestAlertException("Invalid amount", ENTITY_NAME, "invalidamount");
        }
        if (phone == null || phone.isBlank()) {
            throw new BadRequestAlertException("Phone number required", ENTITY_NAME, "nophonenumber");
        }
        String normalizedPhone = normalizePhone(phone);
        String externalRef = "PAYOUT-" + System.currentTimeMillis();
        String desc = description != null && !description.isBlank() ? description : "Mobigo versement conducteur";
        PaymentMethodEnum methode = "MTN".equalsIgnoreCase(operator) ? PaymentMethodEnum.MTN_MOBILE_MONEY : PaymentMethodEnum.ORANGE_MONEY;

        Payment payout = new Payment();
        payout.setMontant((float) amount);
        payout.setDatePaiement(LocalDate.now());
        payout.setMethode(methode);
        payout.setStatut(PaymentStatusEnum.EN_COURS);
        payout.setOperateur("MTN".equalsIgnoreCase(operator) ? "MOMO" : "OM");
        payout.setPhoneNumber(normalizedPhone);
        payout.setExternalReference(externalRef);
        payout = paymentRepository.save(payout);

        try {
            CampayService.DisbursementResponse resp = campayService.disburse(normalizedPhone, amount, externalRef, desc);
            payout.setDisbursementReference(resp.reference());
            payout.setDisbursementStatus(resp.status());
            if ("SUCCESSFUL".equalsIgnoreCase(resp.status())) {
                payout.setStatut(PaymentStatusEnum.REUSSI);
            }
            payout = paymentRepository.save(payout);
            paymentSearchRepository.index(payout);
            LOG.info("On-demand payout {} FCFA initiated to {} (ref: {})", amount, normalizedPhone, externalRef);
            return payout;
        } catch (Exception e) {
            LOG.error("On-demand payout failed: {}", e.getMessage());
            payout.setStatut(PaymentStatusEnum.ECHOUE);
            payout.setDisbursementStatus("FAILED");
            paymentRepository.save(payout);
            throw new RuntimeException("Payout failed: " + e.getMessage());
        }
    }

    /**
     * Disburse funds to driver for all completed bookings of a ride.
     *
     * @deprecated Remplacé par le porte-monnaie interne (WalletService). Conservé
     * temporairement pour compatibilité jusqu'à la migration du frontend (Plan 2).
     */
    @Deprecated
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
            // Campay charges ~1.5% on collect + 1.5% on disburse (estimate)
            float fraisCampayCollect = booking.getMontantTotal() * 0.015f;
            float fraisCampayDisburse = driverAmount * 0.015f;
            float totalFraisCampay = fraisCampayCollect + fraisCampayDisburse;
            float revenuNet = booking.getCommission() - totalFraisCampay;
            totalToDisburse += driverAmount;

            Payment payment = paymentRepository.findByBookingId(booking.getId()).orElse(null);
            if (payment != null) {
                payment.setFraisCampay(totalFraisCampay);
                payment.setRevenuNetPlateforme(revenuNet);
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
        float totalCommission = paidBookings.stream().map(b -> b.getCommission()).reduce(0f, Float::sum);
        float totalFrais = paidBookings
            .stream()
            .map(b -> paymentRepository.findByBookingId(b.getId()).map(Payment::getFraisCampay).orElse(0f))
            .reduce(0f, Float::sum);
        LOG.info(
            "Ride {} financial summary: collected={} FCFA, driver={} FCFA, commission={} FCFA, campayFees={} FCFA, netRevenue={} FCFA",
            rideId,
            paidBookings.stream().map(b -> b.getMontantTotal()).reduce(0f, Float::sum).intValue(),
            Math.round(totalToDisburse),
            Math.round(totalCommission),
            Math.round(totalFrais),
            Math.round(totalCommission - totalFrais)
        );
    }

    /**
     * Refund passenger for a booking (no reason provided).
     * Called by BookingService.cancelBooking.
     *
     * @deprecated Remplacé par le porte-monnaie interne (WalletService). Conservé
     * temporairement pour compatibilité jusqu'à la migration du frontend (Plan 2).
     */
    @Deprecated
    public void refundPassenger(Long bookingId) {
        refundPassenger(bookingId, null);
    }

    /**
     * Refund passenger for a booking with an optional audit reason.
     * Only refunds if the payment was collected (COLLECTE_REUSSIE or REUSSI).
     *
     * @deprecated Remplacé par le porte-monnaie interne (WalletService). Conservé
     * temporairement pour compatibilité jusqu'à la migration du frontend (Plan 2).
     */
    @Deprecated
    public void refundPassenger(Long bookingId, String reason) {
        Payment payment = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (payment == null) {
            LOG.debug("No payment to refund for booking {}", bookingId);
            return;
        }
        if (payment.getStatut() != PaymentStatusEnum.COLLECTE_REUSSIE && payment.getStatut() != PaymentStatusEnum.REUSSI) {
            LOG.debug("Payment for booking {} is in state {}, skipping refund", bookingId, payment.getStatut());
            return;
        }

        Booking booking = payment.getBooking();
        String refundRef = "REF-" + bookingId + "-" + System.currentTimeMillis();
        int amount = Math.round(payment.getMontant());
        String description = reason != null && !reason.isBlank()
            ? "Mobigo remboursement (" + truncate(reason, 60) + ")"
            : "Mobigo remboursement";

        try {
            campayService.disburse(payment.getPhoneNumber(), amount, refundRef, description);
            payment.setStatut(PaymentStatusEnum.REMBOURSE);
            // Refund costs: platform loses the collect fees + refund disburse fees
            float fraisRefund = payment.getMontant() * 0.015f; // disburse fee for refund
            float totalFrais = (payment.getFraisCampay() != null ? payment.getFraisCampay() : 0) + fraisRefund;
            payment.setFraisCampay(totalFrais);
            payment.setRevenuNetPlateforme(-totalFrais); // Net loss on refunded payment
            payment.setDisbursementReference(refundRef);
            paymentRepository.save(payment);
            paymentSearchRepository.index(payment);
            notificationEventService.onPaymentRefunded(booking);
            LOG.info("Refunded {} FCFA for booking {} (reason: {})", amount, bookingId, reason);
        } catch (Exception e) {
            LOG.error("Refund failed for booking {}: {}", bookingId, e.getMessage());
        }
    }

    /**
     * Alias exposed as {@code refund(Long, String)} for clarity at the REST layer.
     *
     * @deprecated Remplacé par le porte-monnaie interne (WalletService). Conservé
     * temporairement pour compatibilité jusqu'à la migration du frontend (Plan 2).
     */
    @Deprecated
    public void refund(Long bookingId, String reason) {
        refundPassenger(bookingId, reason);
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

    /**
     * Normalize a Cameroonian phone number to Campay's expected international format: 237XXXXXXXXX (no '+').
     * Accepts local (6XXXXXXXX) or international ('00237...' / '+237...' / '237...') formats.
     */
    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return phone;
        if (digits.startsWith("00237")) digits = digits.substring(2);
        if (digits.length() == 9 && digits.startsWith("6")) {
            return "237" + digits;
        }
        if (digits.length() == 12 && digits.startsWith("237")) {
            return digits;
        }
        return digits;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
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
