package com.binbash.mobigo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Mobigo.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private String frontendUrl = "http://localhost:4200";
    private final Tesseract tesseract = new Tesseract();
    private final Anthropic anthropic = new Anthropic();
    private final Elasticsearch elasticsearch = new Elasticsearch();
    private final Storage storage = new Storage();
    private final Firebase firebase = new Firebase();
    private final Campay campay = new Campay();

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public Tesseract getTesseract() {
        return tesseract;
    }

    public Anthropic getAnthropic() {
        return anthropic;
    }

    public Elasticsearch getElasticsearch() {
        return elasticsearch;
    }

    public Storage getStorage() {
        return storage;
    }

    public Firebase getFirebase() {
        return firebase;
    }

    public Campay getCampay() {
        return campay;
    }

    public static class Tesseract {

        private String dataPath;
        private String language = "fra";

        public String getDataPath() {
            return dataPath;
        }

        public void setDataPath(String dataPath) {
            this.dataPath = dataPath;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }

    public static class Anthropic {

        private String apiKey;
        private String model = "claude-haiku-4-5-20251001";
        private boolean enabled = false;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Elasticsearch {

        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Storage {

        private String baseDir = "./uploads";
        private String peopleDir = "people";
        private String vehiclesDir = "vehicles";
        private String cniDir = "cni";

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }

        public String getPeopleDir() {
            return peopleDir;
        }

        public void setPeopleDir(String peopleDir) {
            this.peopleDir = peopleDir;
        }

        public String getVehiclesDir() {
            return vehiclesDir;
        }

        public void setVehiclesDir(String vehiclesDir) {
            this.vehiclesDir = vehiclesDir;
        }

        public String getCniDir() {
            return cniDir;
        }

        public void setCniDir(String cniDir) {
            this.cniDir = cniDir;
        }
    }

    public static class Firebase {

        private String credentialsPath;
        private boolean enabled = false;

        public String getCredentialsPath() {
            return credentialsPath;
        }

        public void setCredentialsPath(String credentialsPath) {
            this.credentialsPath = credentialsPath;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Campay {

        private String apiUsername;
        private String apiPassword;
        private String webhookSecret;
        private String baseUrl = "https://demo.campay.net/api";
        private boolean enabled = false;
        private int paymentTimeoutMinutes = 5;

        public String getApiUsername() {
            return apiUsername;
        }

        public void setApiUsername(String apiUsername) {
            this.apiUsername = apiUsername;
        }

        public String getApiPassword() {
            return apiPassword;
        }

        public void setApiPassword(String apiPassword) {
            this.apiPassword = apiPassword;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPaymentTimeoutMinutes() {
            return paymentTimeoutMinutes;
        }

        public void setPaymentTimeoutMinutes(int paymentTimeoutMinutes) {
            this.paymentTimeoutMinutes = paymentTimeoutMinutes;
        }
    }
}
