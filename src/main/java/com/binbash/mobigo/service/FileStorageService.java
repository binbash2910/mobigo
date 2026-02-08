package com.binbash.mobigo.service;

import com.binbash.mobigo.config.ApplicationProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for storing uploaded files on the filesystem.
 * Storage base directory is configured via application.storage.base-dir
 * (environment variable UPLOAD_DIR in production).
 */
@Service
public class FileStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(FileStorageService.class);

    private final ApplicationProperties applicationProperties;

    public FileStorageService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    /**
     * Store a people profile photo.
     *
     * @param peopleId the people entity ID
     * @param file the uploaded image file
     * @return the URL path to store in the database (e.g. "/api/images/people/people_42.jpg")
     */
    public String storePeoplePhoto(Long peopleId, MultipartFile file) throws IOException {
        String subDir = applicationProperties.getStorage().getPeopleDir();
        String filename = "people_" + peopleId + getExtension(file.getOriginalFilename());
        Path dir = resolveDir(subDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        LOG.debug("Stored people photo for id {} at {}", peopleId, target);
        return "/api/images/" + subDir + "/" + filename;
    }

    /**
     * Store a vehicle photo.
     *
     * @param vehicleId the vehicle entity ID
     * @param file the uploaded image file
     * @return the URL path to store in the database (e.g. "/api/images/vehicles/vehicle_7.jpg")
     */
    public String storeVehiclePhoto(Long vehicleId, MultipartFile file) throws IOException {
        String subDir = applicationProperties.getStorage().getVehiclesDir();
        String filename = "vehicle_" + vehicleId + getExtension(file.getOriginalFilename());
        Path dir = resolveDir(subDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        LOG.debug("Stored vehicle photo for id {} at {}", vehicleId, target);
        return "/api/images/" + subDir + "/" + filename;
    }

    /**
     * Store a CNI/identity document image.
     *
     * @param peopleId the people entity ID
     * @param file the uploaded document image
     * @param side "recto" or "verso"
     * @return the absolute filesystem path (used for OCR processing, not served via URL)
     */
    public String storeCniImage(Long peopleId, MultipartFile file, String side) throws IOException {
        String subDir = applicationProperties.getStorage().getCniDir();
        String filename = "cni_" + side + "_" + peopleId + getExtension(file.getOriginalFilename());
        Path dir = resolveDir(subDir);
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        LOG.debug("Stored CNI {} image for people {} at {}", side, peopleId, target);
        return target.toAbsolutePath().toString();
    }

    /**
     * Resolve the full filesystem path for a given subdirectory under the storage base dir.
     */
    public Path resolveDir(String subDir) {
        return Path.of(applicationProperties.getStorage().getBaseDir(), subDir);
    }

    private String getExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return ".jpg";
    }
}
