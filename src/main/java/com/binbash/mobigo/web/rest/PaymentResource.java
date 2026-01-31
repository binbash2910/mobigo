package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.Payment;
import com.binbash.mobigo.domain.enumeration.PaymentMethodEnum;
import com.binbash.mobigo.domain.enumeration.PaymentStatusEnum;
import com.binbash.mobigo.repository.PaymentRepository;
import com.binbash.mobigo.repository.search.PaymentSearchRepository;
import com.binbash.mobigo.service.PaymentService;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import com.binbash.mobigo.web.rest.errors.ElasticsearchExceptionMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.binbash.mobigo.domain.Payment}.
 */
@RestController
@RequestMapping("/api/payments")
@Transactional
public class PaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentResource.class);

    private static final String ENTITY_NAME = "payment";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PaymentRepository paymentRepository;

    private final PaymentSearchRepository paymentSearchRepository;

    private final PaymentService paymentService;

    public PaymentResource(
        PaymentRepository paymentRepository,
        PaymentSearchRepository paymentSearchRepository,
        PaymentService paymentService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentSearchRepository = paymentSearchRepository;
        this.paymentService = paymentService;
    }

    /**
     * {@code POST  /payments/process} : Process a payment for a booking.
     *
     * @param request the payment processing request.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new payment.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/process")
    public ResponseEntity<Payment> processPayment(@Valid @RequestBody ProcessPaymentRequest request) throws URISyntaxException {
        LOG.debug("REST request to process Payment for booking {} with method {}", request.bookingId(), request.methode());
        Payment payment = paymentService.processPayment(request.bookingId(), request.methode());
        return ResponseEntity.created(new URI("/api/payments/" + payment.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, payment.getId().toString()))
            .body(payment);
    }

    /**
     * {@code GET  /payments/my-payments} : get all payments for the current user.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of payments in body.
     */
    @GetMapping("/my-payments")
    public List<Payment> getMyPayments() {
        LOG.debug("REST request to get current user Payments");
        return paymentService.getPaymentsByCurrentUser();
    }

    /**
     * {@code GET  /payments/booking/:bookingId} : get the payment for a booking.
     *
     * @param bookingId the booking id.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the payment, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<Payment> getPaymentByBooking(@PathVariable("bookingId") Long bookingId) {
        LOG.debug("REST request to get Payment for booking {}", bookingId);
        Optional<Payment> payment = paymentService.getPaymentByBooking(bookingId);
        return ResponseUtil.wrapOrNotFound(payment);
    }

    /**
     * {@code PATCH  /payments/:id/status} : Update the status of a payment.
     *
     * @param id the payment id.
     * @param request the status update request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated payment.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Payment> updatePaymentStatus(@PathVariable("id") Long id, @Valid @RequestBody UpdateStatusRequest request) {
        LOG.debug("REST request to update Payment {} status to {}", id, request.statut());
        Payment payment = paymentService.updatePaymentStatus(id, request.statut());
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(payment);
    }

    /**
     * {@code POST  /payments/:id/confirm} : Confirm a payment.
     *
     * @param id the payment id.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the confirmed payment.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Payment> confirmPayment(@PathVariable("id") Long id) {
        LOG.debug("REST request to confirm Payment {}", id);
        Payment payment = paymentService.confirmPayment(id);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(payment);
    }

    /**
     * {@code POST  /payments} : Create a new payment.
     *
     * @param payment the payment to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new payment, or with status {@code 400 (Bad Request)} if the payment has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody Payment payment) throws URISyntaxException {
        LOG.debug("REST request to save Payment : {}", payment);
        if (payment.getId() != null) {
            throw new BadRequestAlertException("A new payment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        payment = paymentService.save(payment);
        return ResponseEntity.created(new URI("/api/payments/" + payment.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, payment.getId().toString()))
            .body(payment);
    }

    /**
     * {@code PUT  /payments/:id} : Updates an existing payment.
     *
     * @param id the id of the payment to save.
     * @param payment the payment to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated payment,
     * or with status {@code 400 (Bad Request)} if the payment is not valid,
     * or with status {@code 500 (Internal Server Error)} if the payment couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Payment> updatePayment(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Payment payment
    ) throws URISyntaxException {
        LOG.debug("REST request to update Payment : {}, {}", id, payment);
        if (payment.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, payment.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!paymentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        payment = paymentService.save(payment);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, payment.getId().toString()))
            .body(payment);
    }

    /**
     * {@code PATCH  /payments/:id} : Partial updates given fields of an existing payment, field will ignore if it is null
     *
     * @param id the id of the payment to save.
     * @param payment the payment to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated payment,
     * or with status {@code 400 (Bad Request)} if the payment is not valid,
     * or with status {@code 404 (Not Found)} if the payment is not found,
     * or with status {@code 500 (Internal Server Error)} if the payment couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Payment> partialUpdatePayment(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Payment payment
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Payment partially : {}, {}", id, payment);
        if (payment.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, payment.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!paymentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Payment> result = paymentRepository
            .findById(payment.getId())
            .map(existingPayment -> {
                if (payment.getMontant() != null) {
                    existingPayment.setMontant(payment.getMontant());
                }
                if (payment.getDatePaiement() != null) {
                    existingPayment.setDatePaiement(payment.getDatePaiement());
                }
                if (payment.getMethode() != null) {
                    existingPayment.setMethode(payment.getMethode());
                }
                if (payment.getStatut() != null) {
                    existingPayment.setStatut(payment.getStatut());
                }

                return existingPayment;
            })
            .map(paymentRepository::save)
            .map(savedPayment -> {
                paymentSearchRepository.index(savedPayment);
                return savedPayment;
            });

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, payment.getId().toString())
        );
    }

    /**
     * {@code GET  /payments} : get all the payments.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of payments in body.
     */
    @GetMapping("")
    public List<Payment> getAllPayments() {
        LOG.debug("REST request to get all Payments");
        return paymentService.findAll();
    }

    /**
     * {@code GET  /payments/:id} : get the "id" payment.
     *
     * @param id the id of the payment to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the payment, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Payment : {}", id);
        Optional<Payment> payment = paymentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(payment);
    }

    /**
     * {@code DELETE  /payments/:id} : delete the "id" payment.
     *
     * @param id the id of the payment to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Payment : {}", id);
        paymentService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /payments/_search?query=:query} : search for the payment corresponding
     * to the query.
     *
     * @param query the query of the payment search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<Payment> searchPayments(@RequestParam("query") String query) {
        LOG.debug("REST request to search Payments for query {}", query);
        try {
            return StreamSupport.stream(paymentSearchRepository.search(query).spliterator(), false).toList();
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }

    /**
     * Request body for processing a payment.
     */
    public record ProcessPaymentRequest(@NotNull Long bookingId, @NotNull PaymentMethodEnum methode) {}

    /**
     * Request body for updating payment status.
     */
    public record UpdateStatusRequest(@NotNull PaymentStatusEnum statut) {}
}
