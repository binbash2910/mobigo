package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.domain.Vehicle;
import com.binbash.mobigo.repository.VehicleRepository;
import com.binbash.mobigo.repository.search.VehicleSearchRepository;
import com.binbash.mobigo.web.rest.errors.BadRequestAlertException;
import com.binbash.mobigo.web.rest.errors.ElasticsearchExceptionMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.binbash.mobigo.domain.Vehicle}.
 */
@RestController
@RequestMapping("/api/vehicles")
@Transactional
public class VehicleResource {

    private static final Logger LOG = LoggerFactory.getLogger(VehicleResource.class);

    private static final String ENTITY_NAME = "vehicle";
    private static final String VEHICLE_IMAGES_DIR = "content/images/vehicles";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final VehicleRepository vehicleRepository;

    private final VehicleSearchRepository vehicleSearchRepository;

    public VehicleResource(VehicleRepository vehicleRepository, VehicleSearchRepository vehicleSearchRepository) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleSearchRepository = vehicleSearchRepository;
    }

    /**
     * {@code POST  /vehicles/{id}/upload-photo} : Upload a photo for a vehicle.
     *
     * @param id the id of the vehicle.
     * @param file the image file to upload.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body containing the photo path.
     */
    @PostMapping(value = "/{id}/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadVehiclePhoto(@PathVariable("id") Long id, @RequestParam("file") MultipartFile file) {
        LOG.debug("REST request to upload photo for Vehicle : {}", id);

        if (!vehicleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        if (file.isEmpty()) {
            throw new BadRequestAlertException("File is empty", ENTITY_NAME, "fileempty");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestAlertException("File must be an image", ENTITY_NAME, "invalidfiletype");
        }

        try {
            // Get the file extension
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                // Default to .jpg if no extension
                extension = ".jpg";
            }

            // Create filename based on vehicle ID
            String filename = "vehicle_" + id + extension;

            // Get the webapp directory path (relative to the application)
            Path uploadDir = Paths.get("src/main/webapp/" + VEHICLE_IMAGES_DIR);

            // Create directory if it doesn't exist
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Save the file
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // The photo path that will be stored in the database (relative URL)
            String photoPath = "/" + VEHICLE_IMAGES_DIR + "/" + filename;

            // Update the vehicle with the photo path
            vehicleRepository
                .findById(id)
                .ifPresent(vehicle -> {
                    vehicle.setPhoto(photoPath);
                    vehicleRepository.save(vehicle);
                    vehicleSearchRepository.index(vehicle);
                });

            Map<String, String> response = new HashMap<>();
            response.put("photoPath", photoPath);
            response.put("message", "Photo uploaded successfully");

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            LOG.error("Error uploading vehicle photo", e);
            throw new BadRequestAlertException("Error uploading file: " + e.getMessage(), ENTITY_NAME, "uploaderror");
        }
    }

    /**
     * {@code PUT  /vehicles/{id}/set-default} : Set a vehicle as the default vehicle for its owner.
     *
     * @param id the id of the vehicle to set as default.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated vehicle.
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<Vehicle> setDefaultVehicle(@PathVariable("id") Long id) {
        LOG.debug("REST request to set Vehicle as default : {}", id);

        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(id);
        if (vehicleOpt.isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Vehicle vehicle = vehicleOpt.get();
        People owner = vehicle.getProprietaire();

        if (owner != null) {
            // Reset all other vehicles of the same owner to not default
            List<Vehicle> ownerVehicles = vehicleRepository
                .findAll()
                .stream()
                .filter(v -> v.getProprietaire() != null && v.getProprietaire().getId().equals(owner.getId()))
                .toList();

            for (Vehicle v : ownerVehicles) {
                if (Boolean.TRUE.equals(v.getParDefaut())) {
                    v.setParDefaut(false);
                    vehicleRepository.save(v);
                    vehicleSearchRepository.index(v);
                }
            }
        }

        // Set the selected vehicle as default
        vehicle.setParDefaut(true);
        vehicle = vehicleRepository.save(vehicle);
        vehicleSearchRepository.index(vehicle);

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, vehicle.getId().toString()))
            .body(vehicle);
    }

    /**
     * {@code POST  /vehicles} : Create a new vehicle.
     *
     * @param vehicle the vehicle to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new vehicle, or with status {@code 400 (Bad Request)} if the vehicle has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<Vehicle> createVehicle(@Valid @RequestBody Vehicle vehicle) throws URISyntaxException {
        LOG.debug("REST request to save Vehicle : {}", vehicle);
        if (vehicle.getId() != null) {
            throw new BadRequestAlertException("A new vehicle cannot already have an ID", ENTITY_NAME, "idexists");
        }
        vehicle = vehicleRepository.save(vehicle);
        vehicleSearchRepository.index(vehicle);
        return ResponseEntity.created(new URI("/api/vehicles/" + vehicle.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, vehicle.getId().toString()))
            .body(vehicle);
    }

    /**
     * {@code PUT  /vehicles/:id} : Updates an existing vehicle.
     *
     * @param id the id of the vehicle to save.
     * @param vehicle the vehicle to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated vehicle,
     * or with status {@code 400 (Bad Request)} if the vehicle is not valid,
     * or with status {@code 500 (Internal Server Error)} if the vehicle couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> updateVehicle(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Vehicle vehicle
    ) throws URISyntaxException {
        LOG.debug("REST request to update Vehicle : {}, {}", id, vehicle);
        if (vehicle.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, vehicle.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!vehicleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        vehicle = vehicleRepository.save(vehicle);
        vehicleSearchRepository.index(vehicle);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, vehicle.getId().toString()))
            .body(vehicle);
    }

    /**
     * {@code PATCH  /vehicles/:id} : Partial updates given fields of an existing vehicle, field will ignore if it is null
     *
     * @param id the id of the vehicle to save.
     * @param vehicle the vehicle to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated vehicle,
     * or with status {@code 400 (Bad Request)} if the vehicle is not valid,
     * or with status {@code 404 (Not Found)} if the vehicle is not found,
     * or with status {@code 500 (Internal Server Error)} if the vehicle couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<Vehicle> partialUpdateVehicle(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Vehicle vehicle
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Vehicle partially : {}, {}", id, vehicle);
        if (vehicle.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, vehicle.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!vehicleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Vehicle> result = vehicleRepository
            .findById(vehicle.getId())
            .map(existingVehicle -> {
                if (vehicle.getMarque() != null) {
                    existingVehicle.setMarque(vehicle.getMarque());
                }
                if (vehicle.getModele() != null) {
                    existingVehicle.setModele(vehicle.getModele());
                }
                if (vehicle.getAnnee() != null) {
                    existingVehicle.setAnnee(vehicle.getAnnee());
                }
                if (vehicle.getCarteGrise() != null) {
                    existingVehicle.setCarteGrise(vehicle.getCarteGrise());
                }
                if (vehicle.getImmatriculation() != null) {
                    existingVehicle.setImmatriculation(vehicle.getImmatriculation());
                }
                if (vehicle.getNbPlaces() != null) {
                    existingVehicle.setNbPlaces(vehicle.getNbPlaces());
                }
                if (vehicle.getCouleur() != null) {
                    existingVehicle.setCouleur(vehicle.getCouleur());
                }
                if (vehicle.getPhoto() != null) {
                    existingVehicle.setPhoto(vehicle.getPhoto());
                }
                if (vehicle.getActif() != null) {
                    existingVehicle.setActif(vehicle.getActif());
                }
                if (vehicle.getParDefaut() != null) {
                    existingVehicle.setParDefaut(vehicle.getParDefaut());
                }

                return existingVehicle;
            })
            .map(vehicleRepository::save)
            .map(savedVehicle -> {
                vehicleSearchRepository.index(savedVehicle);
                return savedVehicle;
            });

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, vehicle.getId().toString())
        );
    }

    /**
     * {@code GET  /vehicles} : get all the vehicles.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of vehicles in body.
     */
    @GetMapping("")
    public List<Vehicle> getAllVehicles() {
        LOG.debug("REST request to get all Vehicles");
        return vehicleRepository.findAll();
    }

    /**
     * {@code GET  /vehicles/:id} : get the "id" vehicle.
     *
     * @param id the id of the vehicle to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the vehicle, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicle(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Vehicle : {}", id);
        Optional<Vehicle> vehicle = vehicleRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(vehicle);
    }

    /**
     * {@code DELETE  /vehicles/:id} : delete the "id" vehicle.
     *
     * @param id the id of the vehicle to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Vehicle : {}", id);
        vehicleRepository.deleteById(id);
        vehicleSearchRepository.deleteFromIndexById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code SEARCH  /vehicles/_search?query=:query} : search for the vehicle corresponding
     * to the query.
     *
     * @param query the query of the vehicle search.
     * @return the result of the search.
     */
    @GetMapping("/_search")
    public List<Vehicle> searchVehicles(@RequestParam("query") String query) {
        LOG.debug("REST request to search Vehicles for query {}", query);
        try {
            return StreamSupport.stream(vehicleSearchRepository.search(query).spliterator(), false).toList();
        } catch (RuntimeException e) {
            throw ElasticsearchExceptionMapper.mapException(e);
        }
    }
}
