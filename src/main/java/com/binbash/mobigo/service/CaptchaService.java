package com.binbash.mobigo.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Service for verifying Cloudflare Turnstile CAPTCHA tokens.
 */
@Service
public class CaptchaService {

    private static final Logger LOG = LoggerFactory.getLogger(CaptchaService.class);

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    @Value("${turnstile.secret-key:}")
    private String secretKey;

    private final RestTemplate restTemplate;

    public CaptchaService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Verify a Turnstile CAPTCHA token.
     *
     * @param token the captcha response token from the client
     * @return true if the token is valid
     */
    public boolean verifyCaptcha(String token) {
        if (secretKey == null || secretKey.isBlank()) {
            LOG.warn("Turnstile secret key not configured — skipping CAPTCHA verification");
            return true;
        }

        if (token == null || token.isBlank()) {
            LOG.warn("Empty CAPTCHA token received");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("secret", secretKey);
            body.add("response", token);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            TurnstileResponse response = restTemplate.postForObject(VERIFY_URL, request, TurnstileResponse.class);

            if (response != null && response.isSuccess()) {
                return true;
            }

            LOG.warn("CAPTCHA verification failed: {}", response != null ? response.getErrorCodes() : "null response");
            return false;
        } catch (Exception e) {
            LOG.error("Error verifying CAPTCHA token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Response DTO for Turnstile siteverify endpoint.
     */
    private static class TurnstileResponse {

        private boolean success;

        @JsonProperty("error-codes")
        private String[] errorCodes;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String[] getErrorCodes() {
            return errorCodes;
        }

        public void setErrorCodes(String[] errorCodes) {
            this.errorCodes = errorCodes;
        }
    }
}
