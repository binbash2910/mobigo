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

    private final Tesseract tesseract = new Tesseract();
    private final Cni cni = new Cni();

    public Tesseract getTesseract() {
        return tesseract;
    }

    public Cni getCni() {
        return cni;
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

    public static class Cni {

        private String imagesDir = "content/images/cni";

        public String getImagesDir() {
            return imagesDir;
        }

        public void setImagesDir(String imagesDir) {
            this.imagesDir = imagesDir;
        }
    }
}
