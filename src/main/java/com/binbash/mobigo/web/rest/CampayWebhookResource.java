package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.service.PaymentService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class CampayWebhookResource {

    private static final Logger LOG = LoggerFactory.getLogger(CampayWebhookResource.class);

    private final PaymentService paymentService;

    public CampayWebhookResource(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/campay")
    public ResponseEntity<Void> handleCampayWebhook(@RequestBody Map<String, Object> payload) {
        LOG.info("Campay webhook received: {}", payload);

        String reference = payload.get("reference") != null ? payload.get("reference").toString() : null;
        String status = payload.get("status") != null ? payload.get("status").toString() : null;
        String externalReference = payload.get("external_reference") != null ? payload.get("external_reference").toString() : null;

        if (reference == null && externalReference == null) {
            LOG.warn("Campay webhook missing reference fields");
            return ResponseEntity.badRequest().build();
        }

        try {
            paymentService.handleWebhook(reference, status, externalReference);
        } catch (Exception e) {
            LOG.error("Error processing Campay webhook: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
