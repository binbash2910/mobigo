package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.GroupMember;
import com.binbash.mobigo.repository.GroupMemberRepository;
import com.binbash.mobigo.repository.search.GroupMemberSearchRepository;
import com.binbash.mobigo.service.dto.GroupMemberDTO;
import com.binbash.mobigo.service.mapper.GroupMemberMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.binbash.mobigo.domain.GroupMember}.
 */
@Service
@Transactional
public class GroupMemberService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupMemberService.class);

    private final GroupMemberRepository groupMemberRepository;

    private final GroupMemberMapper groupMemberMapper;

    private final GroupMemberSearchRepository groupMemberSearchRepository;

    public GroupMemberService(
        GroupMemberRepository groupMemberRepository,
        GroupMemberMapper groupMemberMapper,
        GroupMemberSearchRepository groupMemberSearchRepository
    ) {
        this.groupMemberRepository = groupMemberRepository;
        this.groupMemberMapper = groupMemberMapper;
        this.groupMemberSearchRepository = groupMemberSearchRepository;
    }

    /**
     * Save a groupMember.
     *
     * @param groupMemberDTO the entity to save.
     * @return the persisted entity.
     */
    public GroupMemberDTO save(GroupMemberDTO groupMemberDTO) {
        LOG.debug("Request to save GroupMember : {}", groupMemberDTO);
        GroupMember groupMember = groupMemberMapper.toEntity(groupMemberDTO);
        groupMember = groupMemberRepository.save(groupMember);
        groupMemberSearchRepository.index(groupMember);
        return groupMemberMapper.toDto(groupMember);
    }

    /**
     * Update a groupMember.
     *
     * @param groupMemberDTO the entity to save.
     * @return the persisted entity.
     */
    public GroupMemberDTO update(GroupMemberDTO groupMemberDTO) {
        LOG.debug("Request to update GroupMember : {}", groupMemberDTO);
        GroupMember groupMember = groupMemberMapper.toEntity(groupMemberDTO);
        groupMember = groupMemberRepository.save(groupMember);
        groupMemberSearchRepository.index(groupMember);
        return groupMemberMapper.toDto(groupMember);
    }

    /**
     * Partially update a groupMember.
     *
     * @param groupMemberDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<GroupMemberDTO> partialUpdate(GroupMemberDTO groupMemberDTO) {
        LOG.debug("Request to partially update GroupMember : {}", groupMemberDTO);

        return groupMemberRepository
            .findById(groupMemberDTO.getId())
            .map(existingGroupMember -> {
                groupMemberMapper.partialUpdate(existingGroupMember, groupMemberDTO);

                return existingGroupMember;
            })
            .map(groupMemberRepository::save)
            .map(savedGroupMember -> {
                groupMemberSearchRepository.index(savedGroupMember);
                return savedGroupMember;
            })
            .map(groupMemberMapper::toDto);
    }

    /**
     * Get all the groupMembers.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<GroupMemberDTO> findAll() {
        LOG.debug("Request to get all GroupMembers");
        return groupMemberRepository.findAll().stream().map(groupMemberMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the groupMembers with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<GroupMemberDTO> findAllWithEagerRelationships(Pageable pageable) {
        return groupMemberRepository.findAllWithEagerRelationships(pageable).map(groupMemberMapper::toDto);
    }

    /**
     * Get one groupMember by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<GroupMemberDTO> findOne(Long id) {
        LOG.debug("Request to get GroupMember : {}", id);
        return groupMemberRepository.findOneWithEagerRelationships(id).map(groupMemberMapper::toDto);
    }

    /**
     * Delete the groupMember by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        LOG.debug("Request to delete GroupMember : {}", id);
        groupMemberRepository.deleteById(id);
        groupMemberSearchRepository.deleteFromIndexById(id);
    }

    /**
     * Search for the groupMember corresponding to the query.
     *
     * @param query the query of the search.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<GroupMemberDTO> search(String query) {
        LOG.debug("Request to search GroupMembers for query {}", query);
        try {
            return StreamSupport.stream(groupMemberSearchRepository.search(query).spliterator(), false)
                .map(groupMemberMapper::toDto)
                .toList();
        } catch (RuntimeException e) {
            throw e;
        }
    }
}
