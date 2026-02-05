package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.domain.Payment;
import com.binbash.mobigo.domain.enumeration.PaymentMethodEnum;
import com.binbash.mobigo.domain.enumeration.PaymentStatusEnum;
import com.binbash.mobigo.repository.BookingRepository;
import com.binbash.mobigo.repository.PaymentRepository;
import com.binbash.mobigo.repository.search.PaymentSearchRepository;
import com.binbash.mobigo.security.SecurityUtils;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    public PaymentService(
        PaymentRepository paymentRepository,
        BookingRepository bookingRepository,
        PaymentSearchRepository paymentSearchRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.paymentSearchRepository = paymentSearchRepository;
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
}
