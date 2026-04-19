package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.config.ApplicationProperties;
import com.binbash.mobigo.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Campay -> Mobigo webhook endpoint.
 *
 * Configured in the Campay dashboard as: https://&lt;your-domain&gt;/api/webhooks/campay
 * When {@code application.campay.webhook-secret} is set, requests are verified using
 * HMAC-SHA256 of the raw body. Otherwise, signature verification is skipped (dev / sandbox).
 */
@RestController
@RequestMapping("/api/webhooks")
public class CampayWebhookResource {

    private static final Logger LOG = LoggerFactory.getLogger(CampayWebhookResource.class);

    private final PaymentService paymentService;
    private final ApplicationProperties.Campay campayConfig;
    private final ObjectMapper objectMapper;

    public CampayWebhookResource(PaymentService paymentService, ApplicationProperties applicationProperties, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.campayConfig = applicationProperties.getCampay();
        this.objectMapper = objectMapper;
    }

    @PostMapping("/campay")
    public ResponseEntity<Void> handleCampayWebhook(
        @RequestBody String rawBody,
        @RequestHeader(value = "x-campay-signature", required = false) String headerSignature
    ) {
        LOG.info("Campay webhook received");

        // Optional signature verification (active only if webhook-secret is configured)
        if (campayConfig.getWebhookSecret() != null && !campayConfig.getWebhookSecret().isBlank()) {
            if (!verifySignature(rawBody, headerSignature)) {
                LOG.warn("Campay webhook rejected: invalid HMAC signature");
                return ResponseEntity.status(401).build();
            }
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            String reference = payload.get("reference") != null ? payload.get("reference").toString() : null;
            String status = payload.get("status") != null ? payload.get("status").toString() : null;
            String externalReference = payload.get("external_reference") != null ? payload.get("external_reference").toString() : null;

            if (reference == null && externalReference == null) {
                LOG.warn("Campay webhook missing reference fields");
                return ResponseEntity.badRequest().build();
            }

            paymentService.handleWebhook(reference, status, externalReference);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LOG.error("Error processing Campay webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Verify the HMAC-SHA256 signature (hex-encoded) sent by Campay against our shared webhook secret.
     * Also tolerates payloads where the signature is embedded inside the JSON body.
     */
    private boolean verifySignature(String rawBody, String headerSignature) {
        try {
            String expected = hmacSha256Hex(rawBody, campayConfig.getWebhookSecret());
            if (headerSignature != null && expected.equalsIgnoreCase(headerSignature)) {
                return true;
            }
            // Fallback: Campay sometimes embeds the signature inside the body as "signature": "..."
            return rawBody.contains(expected);
        } catch (Exception e) {
            LOG.error("HMAC verification error", e);
            return false;
        }
    }

    private static String hmacSha256Hex(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
