package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Authority;
import com.binbash.mobigo.repository.AuthorityRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.binbash.mobigo.domain.Authority}.
 */
@Service
@Transactional
public class AuthorityService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorityService.class);

    private final AuthorityRepository authorityRepository;

    public AuthorityService(AuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
    }

    /**
     * Save a authority.
     *
     * @param authority the entity to save.
     * @return the persisted entity.
     */
    public Authority save(Authority authority) {
        LOG.debug("Request to save Authority : {}", authority);
        return authorityRepository.save(authority);
    }

    /**
     * Update a authority.
     *
     * @param authority the entity to save.
     * @return the persisted entity.
     */
    public Authority update(Authority authority) {
        LOG.debug("Request to update Authority : {}", authority);
        authority.setIsPersisted();
        return authorityRepository.save(authority);
    }

    /**
     * Partially update a authority.
     *
     * @param authority the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<Authority> partialUpdate(Authority authority) {
        LOG.debug("Request to partially update Authority : {}", authority);

        return authorityRepository
            .findById(authority.getName())
            .map(existingAuthority -> {
                if (authority.getDescription() != null) {
                    existingAuthority.setDescription(authority.getDescription());
                }
                if (authority.getOrdre() != null) {
                    existingAuthority.setOrdre(authority.getOrdre());
                }

                return existingAuthority;
            })
            .map(authorityRepository::save);
    }

    /**
     * Get all the authorities.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<Authority> findAll() {
        LOG.debug("Request to get all Authorities");
        return authorityRepository.findAll();
    }

    /**
     * Get one authority by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Authority> findOne(String id) {
        LOG.debug("Request to get Authority : {}", id);
        return authorityRepository.findById(id);
    }

    /**
     * Delete the authority by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        LOG.debug("Request to delete Authority : {}", id);
        authorityRepository.deleteById(id);
    }
}
