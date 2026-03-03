package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.DeviceToken;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.repository.DeviceTokenRepository;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.security.SecurityUtils;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device-tokens")
@Transactional
public class DeviceTokenResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceTokenResource.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final PeopleRepository peopleRepository;

    public DeviceTokenResource(DeviceTokenRepository deviceTokenRepository, PeopleRepository peopleRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.peopleRepository = peopleRepository;
    }

    /**
     * POST /api/device-tokens : Register a device token for push notifications.
     * Body: { "token": "fcm-token-string", "platform": "android" }
     */
    @PostMapping
    public ResponseEntity<Void> registerToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String platform = body.get("platform");

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));
        People people = peopleRepository
            .findByUserLogin(login)
            .orElseThrow(() -> new RuntimeException("People not found for login: " + login));

        // Upsert: update if token exists, create otherwise
        DeviceToken deviceToken = deviceTokenRepository.findByToken(token).orElse(new DeviceToken());

        deviceToken.setUserId(people.getId());
        deviceToken.setToken(token);
        deviceToken.setPlatform(platform);
        deviceTokenRepository.save(deviceToken);

        LOG.info("Device token registered for user {} (platform: {})", login, platform);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE /api/device-tokens : Unregister a device token.
     * Body: { "token": "fcm-token-string" }
     */
    @DeleteMapping
    public ResponseEntity<Void> unregisterToken(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token != null) {
            deviceTokenRepository.deleteByToken(token);
            LOG.info("Device token unregistered: {}", token);
        }
        return ResponseEntity.noContent().build();
    }
}
