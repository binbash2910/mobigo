package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.SavedPaymentMethod;
import com.binbash.mobigo.domain.enumeration.PaymentMethodEnum;
import com.binbash.mobigo.repository.SavedPaymentMethodRepository;
import com.binbash.mobigo.repository.search.SavedPaymentMethodSearchRepository;
import com.binbash.mobigo.security.SecurityUtils;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import com.binbash.mobigo.web.rest.errors.ElasticsearchExceptionMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
 * REST controller for managing {@link com.binbash.mobigo.domain.SavedPaymentMethod}.
 */
@RestController
@RequestMapping("/api/saved-payment-methods")
@Transactional
public class SavedPaymentMethodResource {

    private static final Logger LOG = LoggerFactory.getLogger(SavedPaymentMethodResource.class);

    private static final String ENTITY_NAME = "savedPaymentMethod";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final SavedPaymentMethodRepository savedPaymentMethodRepository;

    private final SavedPaymentMethodSearchRepository savedPaymentMethodSearchRepository;

    public SavedPaymentMethodResource(
        SavedPaymentMethodRepository savedPaymentMethodRepository,
        SavedPaymentMethodSearchRepository savedPaymentMethodSearchRepository
    ) {
        this.savedPaymentMethodRepository = savedPaymentMethodRepository;
        this.savedPaymentMethodSearchRepository = savedPaymentMethodSearchRepository;
    }

    /**
     * {@code POST  /saved-payment-methods} : Create a new saved payment method.
     *
     * @param savedPaymentMethod the saved payment method to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new saved payment method.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SavedPaymentMethod> createSavedPaymentMethod(@Valid @RequestBody SavedPaymentMethod savedPaymentMethod)
        throws URISyntaxException {
        LOG.debug("REST request to save SavedPaymentMethod : {}", savedPaymentMethod);
        if (savedPaymentMethod.getId() != null) {
            throw new BadRequestAlertException("A new savedPaymentMethod cannot already have an ID", ENTITY_NAME, "idexists");
        }
        savedPaymentMethod = savedPaymentMethodRepository.save(savedPaymentMethod);
        savedPaymentMethodSearchRepository.index(savedPaymentMethod);
        return ResponseEntity.created(new URI("/api/saved-payment-methods/" + savedPaymentMethod.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, savedPaymentMethod.getId().toString()))
            .body(savedPaymentMethod);
    }

    /**
     * {@code PUT  /saved-payment-methods/:id} : Updates an existing saved payment method.
     *
     * @param id the id of the saved payment method to save.
     * @param savedPaymentMethod the saved payment method to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated saved payment method.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SavedPaymentMethod> updateSavedPaymentMethod(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody SavedPaymentMethod savedPaymentMethod
    ) throws URISyntaxException {
        LOG.debug("REST request to update SavedPaymentMethod : {}, {}", id, savedPaymentMethod);
        if (savedPaymentMethod.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, savedPaymentMethod.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!savedPaymentMethodRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        savedPaymentMethod = savedPaymentMethodRepository.save(savedPaymentMethod);
        savedPaymentMethodSearchRepository.index(savedPaymentMethod);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, savedPaymentMethod.getId().toString()))
            .body(savedPaymentMethod);
    }

    /**
     * {@code PATCH  /saved-payment-methods/:id} : Partial updates given fields of an existing saved payment method.
     *
     * @param id the id of the saved payment method to save.
     * @param savedPaymentMethod the saved payment method to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated saved payment method.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SavedPaymentMethod> partialUpdateSavedPaymentMethod(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody SavedPaymentMethod savedPaymentMethod
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update SavedPaymentMethod partially : {}, {}", id, savedPaymentMethod);
        if (savedPaymentMethod.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, savedPaymentMethod.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!savedPaymentMethodRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SavedPaymentMethod> result = savedPaymentMethodRepository
            .findById(savedPaymentMethod.getId())
            .map(existing -> {
                if (savedPaymentMethod.getType() != null) {
                    existing.setType(savedPaymentMethod.getType());
                }
                if (savedPaymentMethod.getLast4() != null) {
                    existing.setLast4(savedPaymentMethod.getLast4());
                }
                if (savedPaymentMethod.getHolderName() != null) {
                    existing.setHolderName(savedPaymentMethod.getHolderName());
                }
                if (savedPaymentMethod.getExpiryDate() != null) {
                    existing.setExpiryDate(savedPaymentMethod.getExpiryDate());
                }
                if (savedPaymentMethod.getPhone() != null) {
                    existing.setPhone(savedPaymentMethod.getPhone());
                }
                if (savedPaymentMethod.getLabel() != null) {
                    existing.setLabel(savedPaymentMethod.getLabel());
                }
                if (savedPaymentMethod.getIsDefault() != null) {
                    existing.setIsDefault(savedPaymentMethod.getIsDefault());
                }

                return existing;
            })
            .map(savedPaymentMethodRepository::save)
            .map(saved -> {
                savedPaymentMethodSearchRepository.index(saved);
                return saved;
            });

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, savedPaymentMethod.getId().toString())
        );
    }

    /**
     * {@code GET  /saved-payment-methods} : get all saved payment methods.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of saved payment methods in body.
     */
    @GetMapping("")
    public List<SavedPaymentMethod> getAllSavedPaymentMethods() {
        LOG.debug("REST request to get all SavedPaymentMethods");
        return savedPaymentMethodRepository.findAll();
    }

    /**
     * {@code GET  /saved-payment-methods/my-methods} : get all saved payment methods for the current user.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of saved payment methods in body.
     */
    @GetMapping("/my-methods")
    public List<SavedPaymentMethod> getMySavedPaymentMethods() {
        String login = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new BadRequestAlertException("User not authenticated", "user", "notauthenticated"));
        LOG.debug("REST request to get SavedPaymentMethods for user {}", login);
        return savedPaymentMethodRepository.findByProprietaireUserLogin(login);
    }

    /**
     * {@code GET  /saved-payment-methods/:id} : get the "id" saved payment method.
     *
     * @param id the id of the saved payment method to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the saved payment method.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SavedPaymentMethod> getSavedPaymentMethod(@PathVariable("id") Long id) {
        LOG.debug("REST request to get SavedPaymentMethod : {}", id);
        Optional<SavedPaymentMethod> savedPaymentMethod = savedPaymentMethodRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(savedPaymentMethod);
    }

    /**
     * {@code DELETE  /saved-payment-methods/:id} : delete the "id" saved payment method.
     *
     * @param id the id of the saved payment method to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSavedPaymentMethod(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete SavedPaymentMethod : {}", id);
        savedPaymentMethodRepository.deleteById(id);
        savedPaymentMethodSearchRepository.deleteFromIndexById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code PUT  /saved-payment-methods/:id/set-default} : Set a saved payment method as default.
     *
     * @param id the id of the saved payment method to set as default.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated saved payment method.
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<SavedPaymentMethod> setDefaultSavedPaymentMethod(@PathVariable("id") Long id) {
        LOG.debug("REST request to set SavedPaymentMethod as default : {}", id);

        SavedPaymentMethod method = savedPaymentMethodRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
        People owner = method.getProprietaire();

        if (owner != null) {
            // Only reset defaults for methods of the same category (card vs mobile)
            Set<PaymentMethodEnum> mobileTypes = Set.of(PaymentMethodEnum.ORANGE_MONEY, PaymentMethodEnum.MTN_MOBILE_MONEY);
            boolean targetIsMobile = mobileTypes.contains(method.getType());

            List<SavedPaymentMethod> ownerMethods = savedPaymentMethodRepository.findByProprietaireId(owner.getId());
            for (SavedPaymentMethod m : ownerMethods) {
                if (Boolean.TRUE.equals(m.getIsDefault()) && mobileTypes.contains(m.getType()) == targetIsMobile) {
                    m.setIsDefault(false);
                    savedPaymentMethodRepository.save(m);
                    savedPaymentMethodSearchRepository.index(m);
                }
            }
        }

        // Set the selected method as default
        method.setIsDefault(true);
        method = savedPaymentMethodRepository.save(method);
        savedPaymentMethodSearchRepository.index(method);
        //
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, method.getId().toString()))
            .body(method);
    }

    /**
     * {@code SEARCH  /saved-payment-methods/_search?query=:query} : search for the saved payment method corresponding to the query.
     *
     * @param query the query of the saved payment method search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<SavedPaymentMethod> searchSavedPaymentMethods(@RequestParam("query") String query) {
        LOG.debug("REST request to search SavedPaymentMethods for query {}", query);
        try {
            return StreamSupport.stream(savedPaymentMethodSearchRepository.search(query).spliterator(), false).toList();
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
