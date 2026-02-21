package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.service.FileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for serving uploaded images from persistent storage.
 * Reads files from the disk based on the path stored in the database.
 *
 * URL patterns:
 *   GET /api/images/{category}/{filename}      (current)
 *   GET /content/images/{category}/{filename}   (legacy backward compat)
 */
@RestController
public class ImageResource {

    private static final Logger LOG = LoggerFactory.getLogger(ImageResource.class);

    private final FileStorageService fileStorageService;

    public ImageResource(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/api/images/{category}/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable("category") String category, @PathVariable("filename") String filename) {
        return serveImage(category, filename);
    }

    @GetMapping("/content/images/{category}/{filename:.+}")
    public ResponseEntity<Resource> getLegacyImage(@PathVariable("category") String category, @PathVariable("filename") String filename) {
        return serveImage(category, filename);
    }

    private ResponseEntity<Resource> serveImage(String category, String filename) {
        LOG.debug("REST request to get image: {}/{}", category, filename);

        String subPath = category + "/" + filename;
        Path filePath = fileStorageService.resolveFile(subPath);

        if (filePath == null) {
            LOG.warn("Image not found on disk: {}", subPath);
            return ResponseEntity.notFound().build();
        }

        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            Resource resource = new FileSystemResource(filePath);

            return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .contentLength(Files.size(filePath))
                .body(resource);
        } catch (IOException e) {
            LOG.error("Error reading image file: {}", filePath, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
