package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.Step;
import com.binbash.mobigo.repository.StepRepository;
import com.binbash.mobigo.repository.search.StepSearchRepository;
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
 * REST controller for managing {@link com.binbash.mobigo.domain.Step}.
 */
@RestController
@RequestMapping("/api/steps")
@Transactional
public class StepResource {

    private static final Logger LOG = LoggerFactory.getLogger(StepResource.class);

    private static final String ENTITY_NAME = "step";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final StepRepository stepRepository;

    private final StepSearchRepository stepSearchRepository;

    public StepResource(StepRepository stepRepository, StepSearchRepository stepSearchRepository) {
        this.stepRepository = stepRepository;
        this.stepSearchRepository = stepSearchRepository;
    }

    /**
     * {@code POST  /steps} : Create a new step.
     *
     * @param step the step to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new step, or with status {@code 400 (Bad Request)} if the step has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Step> createStep(@Valid @RequestBody Step step) throws URISyntaxException {
        LOG.debug("REST request to save Step : {}", step);
        if (step.getId() != null) {
            throw new BadRequestAlertException("A new step cannot already have an ID", ENTITY_NAME, "idexists");
        }
        step = stepRepository.save(step);
        stepSearchRepository.index(step);
        return ResponseEntity.created(new URI("/api/steps/" + step.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, step.getId().toString()))
            .body(step);
    }

    /**
     * {@code PUT  /steps/:id} : Updates an existing step.
     *
     * @param id the id of the step to save.
     * @param step the step to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated step,
     * or with status {@code 400 (Bad Request)} if the step is not valid,
     * or with status {@code 500 (Internal Server Error)} if the step couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Step> updateStep(@PathVariable(value = "id", required = false) final Long id, @Valid @RequestBody Step step)
        throws URISyntaxException {
        LOG.debug("REST request to update Step : {}, {}", id, step);
        if (step.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, step.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!stepRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        step = stepRepository.save(step);
        stepSearchRepository.index(step);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, step.getId().toString()))
            .body(step);
    }

    /**
     * {@code PATCH  /steps/:id} : Partial updates given fields of an existing step, field will ignore if it is null
     *
     * @param id the id of the step to save.
     * @param step the step to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated step,
     * or with status {@code 400 (Bad Request)} if the step is not valid,
     * or with status {@code 404 (Not Found)} if the step is not found,
     * or with status {@code 500 (Internal Server Error)} if the step couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Step> partialUpdateStep(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Step step
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Step partially : {}, {}", id, step);
        if (step.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, step.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!stepRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Step> result = stepRepository
            .findById(step.getId())
            .map(existingStep -> {
                if (step.getVille() != null) {
                    existingStep.setVille(step.getVille());
                }
                if (step.getHeureDepart() != null) {
                    existingStep.setHeureDepart(step.getHeureDepart());
                }

                return existingStep;
            })
            .map(stepRepository::save)
            .map(savedStep -> {
                stepSearchRepository.index(savedStep);
                return savedStep;
            });

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, step.getId().toString())
        );
    }

    /**
     * {@code GET  /steps} : get all the steps.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of steps in body.
     */
    @GetMapping("")
    public List<Step> getAllSteps() {
        LOG.debug("REST request to get all Steps");
        return stepRepository.findAll();
    }

    /**
     * {@code GET  /steps/:id} : get the "id" step.
     *
     * @param id the id of the step to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the step, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Step> getStep(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Step : {}", id);
        Optional<Step> step = stepRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(step);
    }

    /**
     * {@code DELETE  /steps/:id} : delete the "id" step.
     *
     * @param id the id of the step to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStep(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Step : {}", id);
        stepRepository.deleteById(id);
        stepSearchRepository.deleteFromIndexById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /steps/_search?query=:query} : search for the step corresponding
     * to the query.
     *
     * @param query the query of the step search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<Step> searchSteps(@RequestParam("query") String query) {
        LOG.debug("REST request to search Steps for query {}", query);
        try {
            return StreamSupport.stream(stepSearchRepository.search(query).spliterator(), false).toList();
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
