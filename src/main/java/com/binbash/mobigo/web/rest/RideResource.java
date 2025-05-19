package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.Ride;
import com.binbash.mobigo.repository.RideRepository;
import com.binbash.mobigo.repository.search.RideSearchRepository;
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

    public RideResource(RideRepository rideRepository, RideSearchRepository rideSearchRepository) {
        this.rideRepository = rideRepository;
        this.rideSearchRepository = rideSearchRepository;
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
        return rideRepository.findAll();
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
        Optional<Ride> ride = rideRepository.findById(id);
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
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
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
