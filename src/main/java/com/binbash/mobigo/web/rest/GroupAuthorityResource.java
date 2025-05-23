package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.repository.GroupAuthorityRepository;
import com.binbash.mobigo.service.GroupAuthorityService;
import com.binbash.mobigo.service.dto.GroupAuthorityDTO;
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
 * REST controller for managing {@link com.binbash.mobigo.domain.GroupAuthority}.
 */
@RestController
@RequestMapping("/api/group-authorities")
public class GroupAuthorityResource {

    private static final Logger LOG = LoggerFactory.getLogger(GroupAuthorityResource.class);

    private static final String ENTITY_NAME = "groupAuthority";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final GroupAuthorityService groupAuthorityService;

    private final GroupAuthorityRepository groupAuthorityRepository;

    public GroupAuthorityResource(GroupAuthorityService groupAuthorityService, GroupAuthorityRepository groupAuthorityRepository) {
        this.groupAuthorityService = groupAuthorityService;
        this.groupAuthorityRepository = groupAuthorityRepository;
    }

    /**
     * {@code POST  /group-authorities} : Create a new groupAuthority.
     *
     * @param groupAuthorityDTO the groupAuthorityDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new groupAuthorityDTO, or with status {@code 400 (Bad Request)} if the groupAuthority has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<GroupAuthorityDTO> createGroupAuthority(@RequestBody GroupAuthorityDTO groupAuthorityDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save GroupAuthority : {}", groupAuthorityDTO);
        if (groupAuthorityDTO.getId() != null) {
            throw new BadRequestAlertException("A new groupAuthority cannot already have an ID", ENTITY_NAME, "idexists");
        }
        groupAuthorityDTO = groupAuthorityService.save(groupAuthorityDTO);
        return ResponseEntity.created(new URI("/api/group-authorities/" + groupAuthorityDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, groupAuthorityDTO.getId().toString()))
            .body(groupAuthorityDTO);
    }

    /**
     * {@code PUT  /group-authorities/:id} : Updates an existing groupAuthority.
     *
     * @param id the id of the groupAuthorityDTO to save.
     * @param groupAuthorityDTO the groupAuthorityDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated groupAuthorityDTO,
     * or with status {@code 400 (Bad Request)} if the groupAuthorityDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the groupAuthorityDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<GroupAuthorityDTO> updateGroupAuthority(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody GroupAuthorityDTO groupAuthorityDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update GroupAuthority : {}, {}", id, groupAuthorityDTO);
        if (groupAuthorityDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, groupAuthorityDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!groupAuthorityRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        groupAuthorityDTO = groupAuthorityService.update(groupAuthorityDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, groupAuthorityDTO.getId().toString()))
            .body(groupAuthorityDTO);
    }

    /**
     * {@code PATCH  /group-authorities/:id} : Partial updates given fields of an existing groupAuthority, field will ignore if it is null
     *
     * @param id the id of the groupAuthorityDTO to save.
     * @param groupAuthorityDTO the groupAuthorityDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated groupAuthorityDTO,
     * or with status {@code 400 (Bad Request)} if the groupAuthorityDTO is not valid,
     * or with status {@code 404 (Not Found)} if the groupAuthorityDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the groupAuthorityDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<GroupAuthorityDTO> partialUpdateGroupAuthority(
        @PathVariable(value = "id", required = false) final Long id,
        @RequestBody GroupAuthorityDTO groupAuthorityDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update GroupAuthority partially : {}, {}", id, groupAuthorityDTO);
        if (groupAuthorityDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, groupAuthorityDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!groupAuthorityRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<GroupAuthorityDTO> result = groupAuthorityService.partialUpdate(groupAuthorityDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, groupAuthorityDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /group-authorities} : get all the groupAuthorities.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of groupAuthorities in body.
     */
    @GetMapping("")
    public List<GroupAuthorityDTO> getAllGroupAuthorities() {
        LOG.debug("REST request to get all GroupAuthorities");
        return groupAuthorityService.findAll();
    }

    /**
     * {@code GET  /group-authorities/:id} : get the "id" groupAuthority.
     *
     * @param id the id of the groupAuthorityDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the groupAuthorityDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupAuthorityDTO> getGroupAuthority(@PathVariable("id") Long id) {
        LOG.debug("REST request to get GroupAuthority : {}", id);
        Optional<GroupAuthorityDTO> groupAuthorityDTO = groupAuthorityService.findOne(id);
        return ResponseUtil.wrapOrNotFound(groupAuthorityDTO);
    }

    /**
     * {@code DELETE  /group-authorities/:id} : delete the "id" groupAuthority.
     *
     * @param id the id of the groupAuthorityDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroupAuthority(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete GroupAuthority : {}", id);
        groupAuthorityService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /group-authorities/_search?query=:query} : search for the groupAuthority corresponding
     * to the query.
     *
     * @param query the query of the groupAuthority search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<GroupAuthorityDTO> searchGroupAuthorities(@RequestParam("query") String query) {
        LOG.debug("REST request to search GroupAuthorities for query {}", query);
        try {
            return groupAuthorityService.search(query);
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
