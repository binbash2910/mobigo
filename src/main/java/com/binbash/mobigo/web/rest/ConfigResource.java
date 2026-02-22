package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.config.ApplicationProperties;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ConfigResource {

    private final ApplicationProperties applicationProperties;

    public ConfigResource(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of("commissionRate", applicationProperties.getPricing().getCommissionRate()));
    }
}
