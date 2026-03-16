package com.binbash.mobigo.service;

import com.binbash.mobigo.config.ApplicationProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CampayService {

    private static final Logger LOG = LoggerFactory.getLogger(CampayService.class);

    private final ApplicationProperties.Campay config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private String cachedToken;
    private Instant tokenExpiry;

    public CampayService(ApplicationProperties applicationProperties, ObjectMapper objectMapper) {
        this.config = applicationProperties.getCampay();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    }

    /**
     * Get Campay API token.
     * Uses permanent token if configured, otherwise falls back to username/password authentication.
     */
    public String getToken() throws Exception {
        // Use permanent token if available
        if (config.getPermanentToken() != null && !config.getPermanentToken().isBlank()) {
            return config.getPermanentToken();
        }

        // Fallback: authenticate with username/password
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }

        String body = objectMapper.writeValueAsString(Map.of("username", config.getApiUsername(), "password", config.getApiPassword()));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getBaseUrl() + "/token/"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Campay token request failed: " + response.statusCode() + " " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        cachedToken = json.get("token").asText();
        tokenExpiry = Instant.now().plusSeconds(3000);
        LOG.debug("Campay token refreshed");
        return cachedToken;
    }

    /**
     * Initiate a collect (non-blocking — sends USSD prompt to user's phone).
     * Returns reference, status, and optional ussd_code.
     */
    public CollectResponse collect(String phoneNumber, int amount, String externalReference, String description) throws Exception {
        if (!config.isEnabled()) {
            LOG.warn("Campay disabled — simulating collect for ref {}", externalReference);
            return new CollectResponse(externalReference, "SUCCESSFUL", "*126#");
        }

        String token = getToken();
        String body = objectMapper.writeValueAsString(
            Map.of(
                "amount",
                String.valueOf(amount),
                "currency",
                "XAF",
                "from",
                phoneNumber,
                "description",
                description,
                "external_reference",
                externalReference
            )
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getBaseUrl() + "/collect/"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Token " + token)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.info("Campay collect response: {} {}", response.statusCode(), response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Campay collect failed: " + response.statusCode() + " " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        String reference = json.has("reference") ? json.get("reference").asText() : externalReference;
        String status = json.has("status") ? json.get("status").asText() : "PENDING";
        String ussdCode = json.has("ussd_code") ? json.get("ussd_code").asText() : null;

        return new CollectResponse(reference, status, ussdCode);
    }

    /**
     * Disburse funds (transfer to a mobile money number).
     * Used for: driver payment after trip completion, passenger refund.
     */
    public DisbursementResponse disburse(String phoneNumber, int amount, String externalReference, String description) throws Exception {
        if (!config.isEnabled()) {
            LOG.warn("Campay disabled — simulating disbursement for ref {}", externalReference);
            return new DisbursementResponse(externalReference, "SUCCESSFUL");
        }

        String token = getToken();
        String body = objectMapper.writeValueAsString(
            Map.of(
                "amount",
                String.valueOf(amount),
                "currency",
                "XAF",
                "to",
                phoneNumber,
                "description",
                description,
                "external_reference",
                externalReference
            )
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getBaseUrl() + "/withdraw/"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Token " + token)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.info("Campay disburse response: {} {}", response.statusCode(), response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Campay disburse failed: " + response.statusCode() + " " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        String reference = json.has("reference") ? json.get("reference").asText() : externalReference;
        String status = json.has("status") ? json.get("status").asText() : "PENDING";

        return new DisbursementResponse(reference, status);
    }

    /**
     * Check status of a transaction by its Campay reference.
     */
    public String getTransactionStatus(String reference) throws Exception {
        if (!config.isEnabled()) {
            return "SUCCESSFUL";
        }

        String token = getToken();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.getBaseUrl() + "/transaction/" + reference + "/"))
            .header("Authorization", "Token " + token)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());
        return json.has("status") ? json.get("status").asText() : "UNKNOWN";
    }

    // --- Response records ---

    public record CollectResponse(String reference, String status, String ussdCode) {}

    public record DisbursementResponse(String reference, String status) {}
}
