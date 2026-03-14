package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.repository.RideRepository;
import com.binbash.mobigo.repository.search.RideSearchRepository;
import com.binbash.mobigo.service.NotificationEventService;
import com.binbash.mobigo.service.RideService;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import com.binbash.mobigo.web.rest.errors.ElasticsearchExceptionMapper;
import com.binbash.mobigo.web.websocket.WebSocketNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
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
 * REST controller for managing {@link com.binbash.mobigo.domain.Ride}.
 */
@RestController
@RequestMapping("/api/rides")
@Transactional
public class RideResource {

    private static final Logger LOG = LoggerFactory.getLogger(RideResource.class);

    private static final String ENTITY_NAME = "ride";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RideRepository rideRepository;

    private final RideSearchRepository rideSearchRepository;

    private final RideService rideService;

    private final WebSocketNotificationService webSocketNotificationService;

    private final NotificationEventService notificationEventService;

    public RideResource(
        RideRepository rideRepository,
        RideSearchRepository rideSearchRepository,
        RideService rideService,
        WebSocketNotificationService webSocketNotificationService,
        NotificationEventService notificationEventService
    ) {
        this.rideRepository = rideRepository;
        this.rideSearchRepository = rideSearchRepository;
        this.rideService = rideService;
        this.webSocketNotificationService = webSocketNotificationService;
        this.notificationEventService = notificationEventService;
    }

    /**
     * {@code POST  /rides} : Create a new ride.
     *
     * @param ride the ride to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new ride, or with status {@code 400 (Bad Request)} if the ride has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Ride> createRide(@Valid @RequestBody Ride ride) throws URISyntaxException {
        LOG.debug("REST request to save Ride : {}", ride);
        if (ride.getId() != null) {
            throw new BadRequestAlertException("A new ride cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ride = rideRepository.save(ride);
        rideSearchRepository.index(ride);
        webSocketNotificationService.notifyDataChanged("RIDES_CHANGED");
        return ResponseEntity.created(new URI("/api/rides/" + ride.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, ride.getId().toString()))
            .body(ride);
    }

    /**
     * {@code PUT  /rides/:id} : Updates an existing ride.
     *
     * @param id the id of the ride to save.
     * @param ride the ride to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated ride,
     * or with status {@code 400 (Bad Request)} if the ride is not valid,
     * or with status {@code 500 (Internal Server Error)} if the ride couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Ride> updateRide(@PathVariable(value = "id", required = false) final Long id, @Valid @RequestBody Ride ride)
        throws URISyntaxException {
        LOG.debug("REST request to update Ride : {}, {}", id, ride);
        if (ride.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, ride.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!rideRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ride = rideRepository.save(ride);
        rideSearchRepository.index(ride);
        webSocketNotificationService.notifyDataChanged("RIDES_CHANGED");

        // Send in-app notifications to booked passengers
        try {
            notificationEventService.onTripModified(ride);
        } catch (Exception e) {
            LOG.warn("Failed to create notifications for modified ride: {}", e.getMessage());
        }

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, ride.getId().toString()))
            .body(ride);
    }

    /**
     * {@code PATCH  /rides/:id} : Partial updates given fields of an existing ride, field will ignore if it is null
     *
     * @param id the id of the ride to save.
     * @param ride the ride to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated ride,
     * or with status {@code 400 (Bad Request)} if the ride is not valid,
     * or with status {@code 404 (Not Found)} if the ride is not found,
     * or with status {@code 500 (Internal Server Error)} if the ride couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Ride> partialUpdateRide(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Ride ride
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Ride partially : {}, {}", id, ride);
        if (ride.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, ride.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!rideRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Ride> result = rideRepository
            .findById(ride.getId())
            .map(existingRide -> {
                if (ride.getVilleDepart() != null) {
                    existingRide.setVilleDepart(ride.getVilleDepart());
                }
                if (ride.getVilleArrivee() != null) {
                    existingRide.setVilleArrivee(ride.getVilleArrivee());
                }
                if (ride.getDateDepart() != null) {
                    existingRide.setDateDepart(ride.getDateDepart());
                }
                if (ride.getDateArrivee() != null) {
                    existingRide.setDateArrivee(ride.getDateArrivee());
                }
                if (ride.getHeureDepart() != null) {
                    existingRide.setHeureDepart(ride.getHeureDepart());
                }
                if (ride.getHeureArrivee() != null) {
                    existingRide.setHeureArrivee(ride.getHeureArrivee());
                }
                if (ride.getMinuteDepart() != null) {
                    existingRide.setMinuteDepart(ride.getMinuteDepart());
                }
                if (ride.getMinuteArrivee() != null) {
                    existingRide.setMinuteArrivee(ride.getMinuteArrivee());
                }
                if (ride.getPrixParPlace() != null) {
                    existingRide.setPrixParPlace(ride.getPrixParPlace());
                }
                if (ride.getNbrePlaceDisponible() != null) {
                    existingRide.setNbrePlaceDisponible(ride.getNbrePlaceDisponible());
                }
                if (ride.getStatut() != null) {
                    existingRide.setStatut(ride.getStatut());
                }
                if (ride.getDescription() != null) {
                    existingRide.setDescription(ride.getDescription());
                }
                if (ride.getLieuDitDepart() != null) {
                    existingRide.setLieuDitDepart(ride.getLieuDitDepart());
                }
                if (ride.getLieuDitArrivee() != null) {
                    existingRide.setLieuDitArrivee(ride.getLieuDitArrivee());
                }
                if (ride.getDistanceKm() != null) {
                    existingRide.setDistanceKm(ride.getDistanceKm());
                }
                if (ride.getDurationMinutes() != null) {
                    existingRide.setDurationMinutes(ride.getDurationMinutes());
                }

                return existingRide;
            })
            .map(rideRepository::save)
            .map(savedRide -> {
                rideSearchRepository.index(savedRide);
                return savedRide;
            });

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, ride.getId().toString())
        );
    }

    /**
     * {@code GET  /rides} : get all the rides.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of rides in body.
     */
    @GetMapping("")
    public List<Ride> getAllRides() {
        LOG.debug("REST request to get all Rides");
        return rideRepository.findAllWithVehiculeAndProprietaire();
    }

    /**
     * {@code GET  /rides/search} : search rides with filters.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchRides(
        @RequestParam(required = false) String departure,
        @RequestParam(required = false) String arrival,
        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(
            iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE
        ) java.time.LocalDate date,
        org.springframework.data.domain.Pageable pageable
    ) {
        LOG.debug("REST request to search rides: departure={}, arrival={}, date={}", departure, arrival, date);

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate dateFrom = null;
        java.time.LocalDate dateTo = null;
        java.time.LocalDate exactDate = today;

        if (date != null) {
            exactDate = date;
            dateFrom = date.minusDays(2);
            dateTo = date.plusDays(2);
            if (dateFrom.isBefore(today)) {
                dateFrom = today;
            }
        }

        String dep = (departure != null && !departure.isBlank()) ? departure : null;
        String arr = (arrival != null && !arrival.isBlank()) ? arrival : null;

        org.springframework.data.domain.Page<Ride> page = rideRepository.searchRides(
            dep,
            arr,
            dateFrom,
            dateTo,
            exactDate,
            today,
            pageable
        );

        List<Map<String, Object>> result = page
            .getContent()
            .stream()
            .map(ride -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", ride.getId());
                map.put("villeDepart", ride.getVilleDepart());
                map.put("villeArrivee", ride.getVilleArrivee());
                map.put("lieuDitDepart", ride.getLieuDitDepart());
                map.put("lieuDitArrivee", ride.getLieuDitArrivee());
                map.put("dateDepart", ride.getDateDepart() != null ? ride.getDateDepart().toString() : null);
                map.put("dateArrivee", ride.getDateArrivee() != null ? ride.getDateArrivee().toString() : null);
                map.put("heureDepart", ride.getHeureDepart());
                map.put("minuteDepart", ride.getMinuteDepart());
                map.put("heureArrivee", ride.getHeureArrivee());
                map.put("minuteArrivee", ride.getMinuteArrivee());
                map.put("prixParPlace", ride.getPrixParPlace());
                map.put("nbrePlaceDisponible", ride.getNbrePlaceDisponible());
                map.put("statut", ride.getStatut() != null ? ride.getStatut().name() : null);
                map.put("description", ride.getDescription());
                map.put("exactDateMatch", date != null && ride.getDateDepart() != null && ride.getDateDepart().equals(date));

                if (ride.getVehicule() != null && ride.getVehicule().getProprietaire() != null) {
                    var owner = ride.getVehicule().getProprietaire();
                    Map<String, Object> driver = new java.util.HashMap<>();
                    driver.put("id", owner.getId());
                    String driverName =
                        (owner.getPrenom() != null ? owner.getPrenom() : "") +
                        (owner.getNom() != null ? " " + owner.getNom().charAt(0) + "." : "");
                    driver.put("name", driverName.trim());
                    driver.put("photo", owner.getPhoto());
                    map.put("driver", driver);

                    Map<String, Object> vehicule = new java.util.HashMap<>();
                    vehicule.put("id", ride.getVehicule().getId());
                    vehicule.put("marque", ride.getVehicule().getMarque());
                    vehicule.put("modele", ride.getVehicule().getModele());
                    vehicule.put("couleur", ride.getVehicule().getCouleur());
                    vehicule.put("photo", ride.getVehicule().getPhoto());
                    map.put("vehicule", vehicule);
                }

                return map;
            })
            .collect(java.util.stream.Collectors.toList());

        org.springframework.http.HttpHeaders headers = tech.jhipster.web.util.PaginationUtil.generatePaginationHttpHeaders(
            org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest(),
            page
        );

        return ResponseEntity.ok().headers(headers).body(result);
    }

    /**
     * {@code GET  /rides/:id} : get the "id" ride.
     *
     * @param id the id of the ride to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the ride, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Ride> getRide(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Ride : {}", id);
        Optional<Ride> ride = rideRepository.findByIdWithVehiculeAndProprietaire(id);
        return ResponseUtil.wrapOrNotFound(ride);
    }

    /**
     * {@code DELETE  /rides/:id} : delete the "id" ride.
     *
     * @param id the id of the ride to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRide(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Ride : {}", id);
        rideRepository.deleteById(id);
        rideSearchRepository.deleteFromIndexById(id);
        webSocketNotificationService.notifyDataChanged("RIDES_CHANGED");
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code PUT  /rides/:id/complete} : Mark a ride as completed (EFFECTUE).
     * Cascades to bookings: pending bookings are confirmed.
     *
     * @param id the id of the ride to complete.
     * @return the updated ride.
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<Ride> completeRide(@PathVariable("id") Long id) {
        LOG.debug("REST request to complete Ride : {}", id);
        try {
            Ride ride = rideService.completeRide(id);
            webSocketNotificationService.notifyDataChanged("RIDES_CHANGED");
            return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
                .body(ride);
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "idnotfound");
        } catch (IllegalStateException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "invalidstatus");
        }
    }

    /**
     * {@code PUT  /rides/:id/cancel} : Cancel a ride (ANNULE).
     * Cascades to bookings: all active bookings are cancelled.
     *
     * @param id the id of the ride to cancel.
     * @return the updated ride.
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Ride> cancelRide(@PathVariable("id") Long id) {
        LOG.debug("REST request to cancel Ride : {}", id);
        try {
            Ride ride = rideService.cancelRide(id);
            webSocketNotificationService.notifyDataChanged("RIDES_CHANGED");
            return ResponseEntity.ok()
                .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
                .body(ride);
        } catch (IllegalArgumentException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "idnotfound");
        } catch (IllegalStateException e) {
            throw new BadRequestAlertException(e.getMessage(), ENTITY_NAME, "invalidstatus");
        }
    }

    /**
     * {@code SEARCH  /rides/_search?query=:query} : search for the ride corresponding
     * to the query.
     *
     * @param query the query of the ride search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<Ride> searchRides(@RequestParam("query") String query) {
        LOG.debug("REST request to search Rides for query {}", query);
        try {
            return StreamSupport.stream(rideSearchRepository.search(query).spliterator(), false).toList();
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
