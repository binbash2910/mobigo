package com.binbash.mobigo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for serving static resources like uploaded vehicle images.
 */
@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve vehicle images from the webapp directory
        registry.addResourceHandler("/content/images/vehicles/**").addResourceLocations("file:src/main/webapp/content/images/vehicles/");

        // Serve people profile images from the webapp directory
        registry.addResourceHandler("/content/images/people/**").addResourceLocations("file:src/main/webapp/content/images/people/");
    }
}
