package com.binbash.mobigo.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for serving uploaded images from the configurable storage directory.
 * The base directory is set via application.storage.base-dir (env var UPLOAD_DIR in production).
 */
@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {

    private final ApplicationProperties applicationProperties;

    public StaticResourceConfiguration(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String baseDir = applicationProperties.getStorage().getBaseDir();
        Path basePath = Path.of(baseDir).toAbsolutePath();

        String peopleDir = applicationProperties.getStorage().getPeopleDir();
        String vehiclesDir = applicationProperties.getStorage().getVehiclesDir();

        CacheControl noCache = CacheControl.noCache();

        // New URL pattern: /api/images/people/** -> file:{baseDir}/people/
        registry
            .addResourceHandler("/api/images/" + peopleDir + "/**")
            .addResourceLocations(basePath.resolve(peopleDir).toUri().toString())
            .setCacheControl(noCache);

        // New URL pattern: /api/images/vehicles/** -> file:{baseDir}/vehicles/
        registry
            .addResourceHandler("/api/images/" + vehiclesDir + "/**")
            .addResourceLocations(basePath.resolve(vehiclesDir).toUri().toString())
            .setCacheControl(noCache);

        // Backward compatibility: /content/images/people/** and /content/images/vehicles/**
        // These map to the same directories so old DB paths still resolve.
        registry
            .addResourceHandler("/content/images/" + peopleDir + "/**")
            .addResourceLocations(basePath.resolve(peopleDir).toUri().toString())
            .setCacheControl(noCache);

        registry
            .addResourceHandler("/content/images/" + vehiclesDir + "/**")
            .addResourceLocations(basePath.resolve(vehiclesDir).toUri().toString())
            .setCacheControl(noCache);
    }
}
