package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.repository.search.PeopleSearchRepository;
import com.binbash.mobigo.service.CniVerificationService;
import com.binbash.mobigo.service.FileStorageService;
import com.binbash.mobigo.service.dto.CniVerificationDTO;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import com.binbash.mobigo.web.rest.errors.ElasticsearchExceptionMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.binbash.mobigo.domain.People}.
 */
@RestController
@RequestMapping("/api/people")
@Transactional
public class PeopleResource {

    private static final Logger LOG = LoggerFactory.getLogger(PeopleResource.class);

    private static final String ENTITY_NAME = "people";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PeopleRepository peopleRepository;

    private final PeopleSearchRepository peopleSearchRepository;

    private final CniVerificationService cniVerificationService;

    private final FileStorageService fileStorageService;

    public PeopleResource(
        PeopleRepository peopleRepository,
        PeopleSearchRepository peopleSearchRepository,
        CniVerificationService cniVerificationService,
        FileStorageService fileStorageService
    ) {
        this.peopleRepository = peopleRepository;
        this.peopleSearchRepository = peopleSearchRepository;
        this.cniVerificationService = cniVerificationService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * {@code POST  /people/{id}/upload-photo} : Upload a photo for a people profile.
     *
     * @param id the id of the people.
     * @param file the image file to upload.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body containing the photo path.
     */
    @PostMapping(value = "/{id}/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadPeoplePhoto(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
        LOG.debug("REST request to upload photo for People : {}", id);

        if (!peopleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        if (file.isEmpty()) {
            throw new BadRequestAlertException("File is empty", ENTITY_NAME, "fileempty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestAlertException("File must be an image", ENTITY_NAME, "invalidfiletype");
        }

        try {
            String photoPath = fileStorageService.storePeoplePhoto(id, file);

            peopleRepository
                .findById(id)
                .ifPresent(p -> {
                    p.setPhoto(photoPath);
                    peopleRepository.save(p);
                    peopleSearchRepository.index(p);
                });

            Map<String, String> response = new HashMap<>();
            response.put("photoPath", photoPath);
            response.put("message", "Photo uploaded successfully");

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            LOG.error("Error uploading people photo", e);
            throw new BadRequestAlertException("Error uploading file: " + e.getMessage(), ENTITY_NAME, "uploaderror");
        }
    }

    /**
     * {@code POST  /people/{id}/verify-cni} : Verify identity via document OCR (CNI, passport, carte de sejour).
     *
     * @param id the id of the people.
     * @param recto the front image of the document (required).
     * @param verso the back image of the document (optional for passports).
     * @param documentType the user-selected document type (e.g. "CNI_CMR", "PASSPORT_FRA").
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the verification result.
     */
    @PostMapping(value = "/{id}/verify-cni", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CniVerificationDTO> verifyCni(
        @PathVariable("id") Long id,
        @RequestParam("recto") MultipartFile recto,
        @RequestParam(value = "verso", required = false) MultipartFile verso,
        @RequestParam(value = "documentType", required = false) String documentType
    ) {
        LOG.debug("REST request to verify document for People : {}, type: {}", id, documentType);

        if (!peopleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        if (recto.isEmpty()) {
            throw new BadRequestAlertException("Recto image is required", ENTITY_NAME, "fileempty");
        }

        // For non-passport documents, verso is required
        boolean isPassport = documentType != null && documentType.startsWith("PASSPORT");
        if (!isPassport && (verso == null || verso.isEmpty())) {
            throw new BadRequestAlertException("Verso image is required for this document type", ENTITY_NAME, "fileempty");
        }

        String rectoType = recto.getContentType();
        if (rectoType == null || (!rectoType.startsWith("image/") && !"application/pdf".equals(rectoType))) {
            throw new BadRequestAlertException("Files must be images or PDF", ENTITY_NAME, "invalidfiletype");
        }
        if (verso != null && !verso.isEmpty()) {
            String versoType = verso.getContentType();
            if (versoType == null || (!versoType.startsWith("image/") && !"application/pdf".equals(versoType))) {
                throw new BadRequestAlertException("Files must be images or PDF", ENTITY_NAME, "invalidfiletype");
            }
        }

        CniVerificationDTO result = cniVerificationService.verifyCni(id, recto, verso, documentType);
        return ResponseEntity.ok(result);
    }

    /**
     * {@code GET  /people/{id}/cni-status} : Get the CNI verification status.
     *
     * @param id the id of the people.
     * @return the {@link ResponseEntity} with the verification status.
     */
    @GetMapping("/{id}/cni-status")
    public ResponseEntity<CniVerificationDTO> getCniStatus(@PathVariable("id") Long id) {
        LOG.debug("REST request to get CNI status for People : {}", id);

        People people = peopleRepository
            .findById(id)
            .orElseThrow(() -> new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
        CniVerificationDTO dto = new CniVerificationDTO();
        dto.setStatus(people.getCniStatut() != null ? people.getCniStatut() : "PENDING");
        dto.setVerified("VERIFIED".equals(people.getCniStatut()));
        dto.setDocumentNumber(people.getCniNumero());
        dto.setNom(people.getCniNomMrz());
        dto.setPrenom(people.getCniPrenomMrz());
        dto.setDateNaissance(people.getCniDateNaissanceMrz());
        dto.setDateExpiration(people.getCniDateExpiration());
        dto.setSexe(people.getCniSexe());
        dto.setDocumentExpired(people.getCniDateExpiration() != null && people.getCniDateExpiration().isBefore(java.time.LocalDate.now()));
        dto.setDocumentType(people.getDocumentType());

        return ResponseEntity.ok(dto);
    }

    /**
     * {@code POST  /people} : Create a new people.
     *
     * @param people the people to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new people, or with status {@code 400 (Bad Request)} if the people has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<People> createPeople(@Valid @RequestBody People people) throws URISyntaxException {
        LOG.debug("REST request to save People : {}", people);
        if (people.getId() != null) {
            throw new BadRequestAlertException("A new people cannot already have an ID", ENTITY_NAME, "idexists");
        }
        people = peopleRepository.save(people);
        peopleSearchRepository.index(people);
        return ResponseEntity.created(new URI("/api/people/" + people.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, people.getId().toString()))
            .body(people);
    }

    /**
     * {@code PUT  /people/:id} : Updates an existing people.
     *
     * @param id the id of the people to save.
     * @param people the people to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated people,
     * or with status {@code 400 (Bad Request)} if the people is not valid,
     * or with status {@code 500 (Internal Server Error)} if the people couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<People> updatePeople(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody People people
    ) throws URISyntaxException {
        LOG.debug("REST request to update People : {}, {}", id, people);
        if (people.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, people.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!peopleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        people = peopleRepository.save(people);
        peopleSearchRepository.index(people);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, people.getId().toString()))
            .body(people);
    }

    /**
     * {@code PATCH  /people/:id} : Partial updates given fields of an existing people, field will ignore if it is null
     *
     * @param id the id of the people to save.
     * @param people the people to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated people,
     * or with status {@code 400 (Bad Request)} if the people is not valid,
     * or with status {@code 404 (Not Found)} if the people is not found,
     * or with status {@code 500 (Internal Server Error)} if the people couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<People> partialUpdatePeople(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody People people
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update People partially : {}, {}", id, people);
        if (people.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, people.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!peopleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<People> result = peopleRepository
            .findById(people.getId())
            .map(existingPeople -> {
                if (people.getNom() != null) {
                    existingPeople.setNom(people.getNom());
                }
                if (people.getPrenom() != null) {
                    existingPeople.setPrenom(people.getPrenom());
                }
                if (people.getTelephone() != null) {
                    existingPeople.setTelephone(people.getTelephone());
                }
                if (people.getCni() != null) {
                    existingPeople.setCni(people.getCni());
                }
                if (people.getPhoto() != null) {
                    existingPeople.setPhoto(people.getPhoto());
                }
                if (people.getActif() != null) {
                    existingPeople.setActif(people.getActif());
                }
                if (people.getDateNaissance() != null) {
                    existingPeople.setDateNaissance(people.getDateNaissance());
                }
                if (people.getAlcool() != null) {
                    existingPeople.setAlcool(people.getAlcool());
                }
                if (people.getConducteur() != null) {
                    existingPeople.setConducteur(people.getConducteur());
                }
                if (people.getPassager() != null) {
                    existingPeople.setPassager(people.getPassager());
                }
                if (people.getCniStatut() != null) {
                    existingPeople.setCniStatut(people.getCniStatut());
                }

                return existingPeople;
            })
            .map(peopleRepository::save)
            .map(savedPeople -> {
                peopleSearchRepository.index(savedPeople);
                return savedPeople;
            });

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, people.getId().toString())
        );
    }

    /**
     * {@code GET  /people} : get all the people.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of people in body.
     */
    @GetMapping("")
    public List<People> getAllPeople() {
        LOG.debug("REST request to get all People");
        return peopleRepository.findAll();
    }

    /**
     * {@code GET  /people/:id} : get the "id" people.
     *
     * @param id the id of the people to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the people, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<People> getPeople(@PathVariable("id") Long id) {
        LOG.debug("REST request to get People : {}", id);
        Optional<People> people = peopleRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(people);
    }

    /**
     * {@code DELETE  /people/:id} : delete the "id" people.
     *
     * @param id the id of the people to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePeople(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete People : {}", id);
        peopleRepository.deleteById(id);
        peopleSearchRepository.deleteFromIndexById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /people/_search?query=:query} : search for the people corresponding
     * to the query.
     *
     * @param query the query of the people search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<People> searchPeople(@RequestParam("query") String query) {
        LOG.debug("REST request to search People for query {}", query);
        try {
            return StreamSupport.stream(peopleSearchRepository.search(query).spliterator(), false).toList();
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
