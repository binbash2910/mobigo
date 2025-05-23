package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.repository.GroupMemberRepository;
import com.binbash.mobigo.service.GroupMemberService;
import com.binbash.mobigo.service.dto.GroupMemberDTO;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import com.binbash.mobigo.web.rest.errors.ElasticsearchExceptionMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.binbash.mobigo.domain.GroupMember}.
 */
@RestController
@RequestMapping("/api/group-members")
public class GroupMemberResource {

    private static final Logger LOG = LoggerFactory.getLogger(GroupMemberResource.class);

    private static final String ENTITY_NAME = "groupMember";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final GroupMemberService groupMemberService;

    private final GroupMemberRepository groupMemberRepository;

    public GroupMemberResource(GroupMemberService groupMemberService, GroupMemberRepository groupMemberRepository) {
        this.groupMemberService = groupMemberService;
        this.groupMemberRepository = groupMemberRepository;
    }

    /**
     * {@code POST  /group-members} : Create a new groupMember.
     *
     * @param groupMemberDTO the groupMemberDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new groupMemberDTO, or with status {@code 400 (Bad Request)} if the groupMember has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<GroupMemberDTO> createGroupMember(@RequestBody GroupMemberDTO groupMemberDTO) throws URISyntaxException {
        LOG.debug("REST request to save GroupMember : {}", groupMemberDTO);
        if (groupMemberDTO.getId() != null) {
            throw new BadRequestAlertException("A new groupMember cannot already have an ID", ENTITY_NAME, "idexists");
        }
        groupMemberDTO = groupMemberService.save(groupMemberDTO);
        return ResponseEntity.created(new URI("/api/group-members/" + groupMemberDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, groupMemberDTO.getId().toString()))
            .body(groupMemberDTO);
    }

    /**
     * {@code PUT  /group-members/:id} : Updates an existing groupMember.
     *
     * @param id the id of the groupMemberDTO to save.
     * @param groupMemberDTO the groupMemberDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated groupMemberDTO,
     * or with status {@code 400 (Bad Request)} if the groupMemberDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the groupMemberDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<GroupMemberDTO> updateGroupMember(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody GroupMemberDTO groupMemberDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update GroupMember : {}, {}", id, groupMemberDTO);
        if (groupMemberDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, groupMemberDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!groupMemberRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        groupMemberDTO = groupMemberService.update(groupMemberDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, groupMemberDTO.getId().toString()))
            .body(groupMemberDTO);
    }

    /**
     * {@code PATCH  /group-members/:id} : Partial updates given fields of an existing groupMember, field will ignore if it is null
     *
     * @param id the id of the groupMemberDTO to save.
     * @param groupMemberDTO the groupMemberDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated groupMemberDTO,
     * or with status {@code 400 (Bad Request)} if the groupMemberDTO is not valid,
     * or with status {@code 404 (Not Found)} if the groupMemberDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the groupMemberDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<GroupMemberDTO> partialUpdateGroupMember(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody GroupMemberDTO groupMemberDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update GroupMember partially : {}, {}", id, groupMemberDTO);
        if (groupMemberDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, groupMemberDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!groupMemberRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<GroupMemberDTO> result = groupMemberService.partialUpdate(groupMemberDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, groupMemberDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /group-members} : get all the groupMembers.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of groupMembers in body.
     */
    @GetMapping("")
    public List<GroupMemberDTO> getAllGroupMembers(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all GroupMembers");
        return groupMemberService.findAll();
    }

    /**
     * {@code GET  /group-members/:id} : get the "id" groupMember.
     *
     * @param id the id of the groupMemberDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the groupMemberDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupMemberDTO> getGroupMember(@PathVariable("id") Long id) {
        LOG.debug("REST request to get GroupMember : {}", id);
        Optional<GroupMemberDTO> groupMemberDTO = groupMemberService.findOne(id);
        return ResponseUtil.wrapOrNotFound(groupMemberDTO);
    }

    /**
     * {@code DELETE  /group-members/:id} : delete the "id" groupMember.
     *
     * @param id the id of the groupMemberDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroupMember(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete GroupMember : {}", id);
        groupMemberService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /group-members/_search?query=:query} : search for the groupMember corresponding
     * to the query.
     *
     * @param query the query of the groupMember search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<GroupMemberDTO> searchGroupMembers(@RequestParam("query") String query) {
        LOG.debug("REST request to search GroupMembers for query {}", query);
        try {
            return groupMemberService.search(query);
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
