package com.binbash.mobigo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * Images are now served via ImageResource REST controller instead of static resource handlers.
 */
@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {}
