package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.service.AppSettingService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ConfigResource {

    private final AppSettingService appSettingService;

    public ConfigResource(AppSettingService appSettingService) {
        this.appSettingService = appSettingService;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        return ResponseEntity.ok(Map.of("commissionRate", appSettingService.getCommissionRate()));
    }

    @PutMapping("/config/commission-rate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateCommissionRate(@RequestBody Map<String, Object> request) {
        Object rateObj = request.get("commissionRate");
        if (rateObj == null) {
            return ResponseEntity.badRequest().build();
        }

        double newRate;
        try {
            newRate = Double.parseDouble(rateObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }

        if (newRate < 0 || newRate > 1) {
            return ResponseEntity.badRequest().build();
        }

        appSettingService.setCommissionRate(newRate);
        return ResponseEntity.ok(Map.of("commissionRate", newRate));
    }
}
