package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.Booking;
import com.binbash.mobigo.repository.BookingRepository;
import com.binbash.mobigo.repository.search.BookingSearchRepository;
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
 * REST controller for managing {@link com.binbash.mobigo.domain.Booking}.
 */
@RestController
@RequestMapping("/api/bookings")
@Transactional
public class BookingResource {

    private static final Logger LOG = LoggerFactory.getLogger(BookingResource.class);

    private static final String ENTITY_NAME = "booking";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final BookingRepository bookingRepository;

    private final BookingSearchRepository bookingSearchRepository;

    public BookingResource(BookingRepository bookingRepository, BookingSearchRepository bookingSearchRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSearchRepository = bookingSearchRepository;
    }

    /**
     * {@code POST  /bookings} : Create a new booking.
     *
     * @param booking the booking to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new booking, or with status {@code 400 (Bad Request)} if the booking has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Booking> createBooking(@Valid @RequestBody Booking booking) throws URISyntaxException {
        LOG.debug("REST request to save Booking : {}", booking);
        if (booking.getId() != null) {
            throw new BadRequestAlertException("A new booking cannot already have an ID", ENTITY_NAME, "idexists");
        }
        booking = bookingRepository.save(booking);
        bookingSearchRepository.index(booking);
        return ResponseEntity.created(new URI("/api/bookings/" + booking.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, booking.getId().toString()))
            .body(booking);
    }

    /**
     * {@code PUT  /bookings/:id} : Updates an existing booking.
     *
     * @param id the id of the booking to save.
     * @param booking the booking to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated booking,
     * or with status {@code 400 (Bad Request)} if the booking is not valid,
     * or with status {@code 500 (Internal Server Error)} if the booking couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Booking> updateBooking(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Booking booking
    ) throws URISyntaxException {
        LOG.debug("REST request to update Booking : {}, {}", id, booking);
        if (booking.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, booking.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!bookingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        booking = bookingRepository.save(booking);
        bookingSearchRepository.index(booking);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, booking.getId().toString()))
            .body(booking);
    }

    /**
     * {@code PATCH  /bookings/:id} : Partial updates given fields of an existing booking, field will ignore if it is null
     *
     * @param id the id of the booking to save.
     * @param booking the booking to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated booking,
     * or with status {@code 400 (Bad Request)} if the booking is not valid,
     * or with status {@code 404 (Not Found)} if the booking is not found,
     * or with status {@code 500 (Internal Server Error)} if the booking couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Booking> partialUpdateBooking(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Booking booking
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Booking partially : {}, {}", id, booking);
        if (booking.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, booking.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!bookingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Booking> result = bookingRepository
            .findById(booking.getId())
            .map(existingBooking -> {
                if (booking.getNbPlacesReservees() != null) {
                    existingBooking.setNbPlacesReservees(booking.getNbPlacesReservees());
                }
                if (booking.getMontantTotal() != null) {
                    existingBooking.setMontantTotal(booking.getMontantTotal());
                }
                if (booking.getDateReservation() != null) {
                    existingBooking.setDateReservation(booking.getDateReservation());
                }
                if (booking.getStatut() != null) {
                    existingBooking.setStatut(booking.getStatut());
                }

                return existingBooking;
            })
            .map(bookingRepository::save)
            .map(savedBooking -> {
                bookingSearchRepository.index(savedBooking);
                return savedBooking;
            });

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, booking.getId().toString())
        );
    }

    /**
     * {@code GET  /bookings} : get all the bookings.
     *
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of bookings in body.
     */
    @GetMapping("")
    public List<Booking> getAllBookings(@RequestParam(name = "filter", required = false) String filter) {
        if ("payement-is-null".equals(filter)) {
            LOG.debug("REST request to get all Bookings where payement is null");
            return StreamSupport.stream(bookingRepository.findAll().spliterator(), false)
                .filter(booking -> booking.getPayement() == null)
                .toList();
        }
        LOG.debug("REST request to get all Bookings");
        return bookingRepository.findAll();
    }

    /**
     * {@code GET  /bookings/:id} : get the "id" booking.
     *
     * @param id the id of the booking to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the booking, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBooking(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Booking : {}", id);
        Optional<Booking> booking = bookingRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(booking);
    }

    /**
     * {@code DELETE  /bookings/:id} : delete the "id" booking.
     *
     * @param id the id of the booking to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Booking : {}", id);
        bookingRepository.deleteById(id);
        bookingSearchRepository.deleteFromIndexById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /bookings/_search?query=:query} : search for the booking corresponding
     * to the query.
     *
     * @param query the query of the booking search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<Booking> searchBookings(@RequestParam("query") String query) {
        LOG.debug("REST request to search Bookings for query {}", query);
        try {
            return StreamSupport.stream(bookingSearchRepository.search(query).spliterator(), false).toList();
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
