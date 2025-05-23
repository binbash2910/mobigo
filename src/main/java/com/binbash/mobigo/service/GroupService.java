package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.Group;
import com.binbash.mobigo.repository.GroupRepository;
import com.binbash.mobigo.repository.search.GroupSearchRepository;
import com.binbash.mobigo.service.dto.GroupDTO;
import com.binbash.mobigo.service.mapper.GroupMapper;
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
 * Service Implementation for managing {@link com.binbash.mobigo.domain.Group}.
 */
@Service
@Transactional
public class GroupService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;

    private final GroupMapper groupMapper;

    private final GroupSearchRepository groupSearchRepository;

    public GroupService(GroupRepository groupRepository, GroupMapper groupMapper, GroupSearchRepository groupSearchRepository) {
        this.groupRepository = groupRepository;
        this.groupMapper = groupMapper;
        this.groupSearchRepository = groupSearchRepository;
    }

    /**
     * Save a group.
     *
     * @param groupDTO the entity to save.
     * @return the persisted entity.
     */
    public GroupDTO save(GroupDTO groupDTO) {
        LOG.debug("Request to save Group : {}", groupDTO);
        Group group = groupMapper.toEntity(groupDTO);
        group = groupRepository.save(group);
        groupSearchRepository.index(group);
        return groupMapper.toDto(group);
    }

    /**
     * Update a group.
     *
     * @param groupDTO the entity to save.
     * @return the persisted entity.
     */
    public GroupDTO update(GroupDTO groupDTO) {
        LOG.debug("Request to update Group : {}", groupDTO);
        Group group = groupMapper.toEntity(groupDTO);
        group = groupRepository.save(group);
        groupSearchRepository.index(group);
        return groupMapper.toDto(group);
    }

    /**
     * Partially update a group.
     *
     * @param groupDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<GroupDTO> partialUpdate(GroupDTO groupDTO) {
        LOG.debug("Request to partially update Group : {}", groupDTO);

        return groupRepository
            .findById(groupDTO.getId())
            .map(existingGroup -> {
                groupMapper.partialUpdate(existingGroup, groupDTO);

                return existingGroup;
            })
            .map(groupRepository::save)
            .map(savedGroup -> {
                groupSearchRepository.index(savedGroup);
                return savedGroup;
            })
            .map(groupMapper::toDto);
    }

    /**
     * Get all the groups.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<GroupDTO> findAll() {
        LOG.debug("Request to get all Groups");
        return groupRepository.findAll().stream().map(groupMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one group by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<GroupDTO> findOne(Long id) {
        LOG.debug("Request to get Group : {}", id);
        return groupRepository.findById(id).map(groupMapper::toDto);
    }

    /**
     * Delete the group by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete Group : {}", id);
        groupRepository.deleteById(id);
        groupSearchRepository.deleteFromIndexById(id);
    }

    /**
     * Search for the group corresponding to the query.
     *
     * @param query the query of the search.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<GroupDTO> search(String query) {
        LOG.debug("Request to search Groups for query {}", query);
        try {
            return StreamSupport.stream(groupSearchRepository.search(query).spliterator(), false).map(groupMapper::toDto).toList();
        } catch (RuntimeException e) {
            throw e;
        }
    }
}
