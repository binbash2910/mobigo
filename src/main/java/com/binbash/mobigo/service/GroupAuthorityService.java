package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.GroupAuthority;
import com.binbash.mobigo.repository.GroupAuthorityRepository;
import com.binbash.mobigo.repository.search.GroupAuthoritySearchRepository;
import com.binbash.mobigo.service.dto.GroupAuthorityDTO;
import com.binbash.mobigo.service.mapper.GroupAuthorityMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.binbash.mobigo.domain.GroupAuthority}.
 */
@Service
@Transactional
public class GroupAuthorityService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupAuthorityService.class);

    private final GroupAuthorityRepository groupAuthorityRepository;

    private final GroupAuthorityMapper groupAuthorityMapper;

    private final GroupAuthoritySearchRepository groupAuthoritySearchRepository;

    public GroupAuthorityService(
        GroupAuthorityRepository groupAuthorityRepository,
        GroupAuthorityMapper groupAuthorityMapper,
        GroupAuthoritySearchRepository groupAuthoritySearchRepository
    ) {
        this.groupAuthorityRepository = groupAuthorityRepository;
        this.groupAuthorityMapper = groupAuthorityMapper;
        this.groupAuthoritySearchRepository = groupAuthoritySearchRepository;
    }

    /**
     * Save a groupAuthority.
     *
     * @param groupAuthorityDTO the entity to save.
     * @return the persisted entity.
     */
    public GroupAuthorityDTO save(GroupAuthorityDTO groupAuthorityDTO) {
        LOG.debug("Request to save GroupAuthority : {}", groupAuthorityDTO);
        GroupAuthority groupAuthority = groupAuthorityMapper.toEntity(groupAuthorityDTO);
        groupAuthority = groupAuthorityRepository.save(groupAuthority);
        groupAuthoritySearchRepository.index(groupAuthority);
        return groupAuthorityMapper.toDto(groupAuthority);
    }

    /**
     * Update a groupAuthority.
     *
     * @param groupAuthorityDTO the entity to save.
     * @return the persisted entity.
     */
    public GroupAuthorityDTO update(GroupAuthorityDTO groupAuthorityDTO) {
        LOG.debug("Request to update GroupAuthority : {}", groupAuthorityDTO);
        GroupAuthority groupAuthority = groupAuthorityMapper.toEntity(groupAuthorityDTO);
        groupAuthority = groupAuthorityRepository.save(groupAuthority);
        groupAuthoritySearchRepository.index(groupAuthority);
        return groupAuthorityMapper.toDto(groupAuthority);
    }

    /**
     * Partially update a groupAuthority.
     *
     * @param groupAuthorityDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<GroupAuthorityDTO> partialUpdate(GroupAuthorityDTO groupAuthorityDTO) {
        LOG.debug("Request to partially update GroupAuthority : {}", groupAuthorityDTO);

        return groupAuthorityRepository
            .findById(groupAuthorityDTO.getId())
            .map(existingGroupAuthority -> {
                groupAuthorityMapper.partialUpdate(existingGroupAuthority, groupAuthorityDTO);

                return existingGroupAuthority;
            })
            .map(groupAuthorityRepository::save)
            .map(savedGroupAuthority -> {
                groupAuthoritySearchRepository.index(savedGroupAuthority);
                return savedGroupAuthority;
            })
            .map(groupAuthorityMapper::toDto);
    }

    /**
     * Get all the groupAuthorities.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<GroupAuthorityDTO> findAll() {
        LOG.debug("Request to get all GroupAuthorities");
        return groupAuthorityRepository
            .findAll()
            .stream()
            .map(groupAuthorityMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one groupAuthority by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<GroupAuthorityDTO> findOne(Long id) {
        LOG.debug("Request to get GroupAuthority : {}", id);
        return groupAuthorityRepository.findById(id).map(groupAuthorityMapper::toDto);
    }

    /**
     * Delete the groupAuthority by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete GroupAuthority : {}", id);
        groupAuthorityRepository.deleteById(id);
        groupAuthoritySearchRepository.deleteFromIndexById(id);
    }

    /**
     * Search for the groupAuthority corresponding to the query.
     *
     * @param query the query of the search.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<GroupAuthorityDTO> search(String query) {
        LOG.debug("Request to search GroupAuthorities for query {}", query);
        try {
            return StreamSupport.stream(groupAuthoritySearchRepository.search(query).spliterator(), false)
                .map(groupAuthorityMapper::toDto)
                .toList();
        } catch (RuntimeException e) {
            throw e;
        }
    }
}
