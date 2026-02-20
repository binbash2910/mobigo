package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.Rating;
import com.binbash.mobigo.repository.RatingRepository;
import com.binbash.mobigo.repository.search.RatingSearchRepository;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import com.binbash.mobigo.web.rest.errors.ElasticsearchExceptionMapper;
import com.binbash.mobigo.web.websocket.WebSocketNotificationService;
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
 * REST controller for managing {@link com.binbash.mobigo.domain.Rating}.
 */
@RestController
@RequestMapping("/api/ratings")
@Transactional
public class RatingResource {

    private static final Logger LOG = LoggerFactory.getLogger(RatingResource.class);

    private static final String ENTITY_NAME = "rating";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RatingRepository ratingRepository;

    private final RatingSearchRepository ratingSearchRepository;

    private final WebSocketNotificationService webSocketNotificationService;

    public RatingResource(
        RatingRepository ratingRepository,
        RatingSearchRepository ratingSearchRepository,
        WebSocketNotificationService webSocketNotificationService
    ) {
        this.ratingRepository = ratingRepository;
        this.ratingSearchRepository = ratingSearchRepository;
        this.webSocketNotificationService = webSocketNotificationService;
    }

    /**
     * {@code POST  /ratings} : Create a new rating.
     *
     * @param rating the rating to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new rating, or with status {@code 400 (Bad Request)} if the rating has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Rating> createRating(@Valid @RequestBody Rating rating) throws URISyntaxException {
        LOG.debug("REST request to save Rating : {}", rating);
        if (rating.getId() != null) {
            throw new BadRequestAlertException("A new rating cannot already have an ID", ENTITY_NAME, "idexists");
        }
        rating = ratingRepository.save(rating);
        ratingSearchRepository.index(rating);
        webSocketNotificationService.notifyDataChanged("RATINGS_CHANGED");
        return ResponseEntity.created(new URI("/api/ratings/" + rating.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, rating.getId().toString()))
            .body(rating);
    }

    /**
     * {@code PUT  /ratings/:id} : Updates an existing rating.
     *
     * @param id the id of the rating to save.
     * @param rating the rating to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated rating,
     * or with status {@code 400 (Bad Request)} if the rating is not valid,
     * or with status {@code 500 (Internal Server Error)} if the rating couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Rating> updateRating(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Rating rating
    ) throws URISyntaxException {
        LOG.debug("REST request to update Rating : {}, {}", id, rating);
        if (rating.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, rating.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!ratingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        rating = ratingRepository.save(rating);
        ratingSearchRepository.index(rating);
        webSocketNotificationService.notifyDataChanged("RATINGS_CHANGED");
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, rating.getId().toString()))
            .body(rating);
    }

    /**
     * {@code PATCH  /ratings/:id} : Partial updates given fields of an existing rating, field will ignore if it is null
     *
     * @param id the id of the rating to save.
     * @param rating the rating to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated rating,
     * or with status {@code 400 (Bad Request)} if the rating is not valid,
     * or with status {@code 404 (Not Found)} if the rating is not found,
     * or with status {@code 500 (Internal Server Error)} if the rating couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Rating> partialUpdateRating(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Rating rating
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Rating partially : {}, {}", id, rating);
        if (rating.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, rating.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!ratingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Rating> result = ratingRepository
            .findById(rating.getId())
            .map(existingRating -> {
                if (rating.getNote() != null) {
                    existingRating.setNote(rating.getNote());
                }
                if (rating.getCommentaire() != null) {
                    existingRating.setCommentaire(rating.getCommentaire());
                }
                if (rating.getRatingDate() != null) {
                    existingRating.setRatingDate(rating.getRatingDate());
                }

                return existingRating;
            })
            .map(ratingRepository::save)
            .map(savedRating -> {
                ratingSearchRepository.index(savedRating);
                return savedRating;
            });

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, rating.getId().toString())
        );
    }

    /**
     * {@code GET  /ratings} : get all the ratings.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of ratings in body.
     */
    @GetMapping("")
    public List<Rating> getAllRatings() {
        LOG.debug("REST request to get all Ratings");
        return ratingRepository.findAll();
    }

    /**
     * {@code GET  /ratings/:id} : get the "id" rating.
     *
     * @param id the id of the rating to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the rating, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Rating> getRating(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Rating : {}", id);
        Optional<Rating> rating = ratingRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(rating);
    }

    /**
     * {@code DELETE  /ratings/:id} : delete the "id" rating.
     *
     * @param id the id of the rating to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRating(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Rating : {}", id);
        ratingRepository.deleteById(id);
        ratingSearchRepository.deleteFromIndexById(id);
        webSocketNotificationService.notifyDataChanged("RATINGS_CHANGED");
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /ratings/_search?query=:query} : search for the rating corresponding
     * to the query.
     *
     * @param query the query of the rating search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<Rating> searchRatings(@RequestParam("query") String query) {
        LOG.debug("REST request to search Ratings for query {}", query);
        try {
            return StreamSupport.stream(ratingSearchRepository.search(query).spliterator(), false).toList();
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
