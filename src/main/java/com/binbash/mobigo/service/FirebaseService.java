package com.binbash.mobigo.service;

import com.binbash.mobigo.config.ApplicationProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebaseService {

    private static final Logger LOG = LoggerFactory.getLogger(FirebaseService.class);

    private final ApplicationProperties applicationProperties;
    private boolean initialized = false;

    public FirebaseService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @PostConstruct
    public void init() {
        if (!applicationProperties.getFirebase().isEnabled()) {
            LOG.info("Firebase is disabled, push notifications will not be sent");
            return;
        }

        String credentialsPath = applicationProperties.getFirebase().getCredentialsPath();
        if (credentialsPath == null || credentialsPath.isBlank()) {
            LOG.warn("Firebase credentials path not configured, push notifications disabled");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = Files.newInputStream(Paths.get(credentialsPath));
                FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(serviceAccount)).build();
                FirebaseApp.initializeApp(options);
                initialized = true;
                LOG.info("Firebase initialized successfully");
            } else {
                initialized = true;
                LOG.info("Firebase already initialized");
            }
        } catch (IOException e) {
            LOG.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }

    /**
     * Send a push notification to a specific device.
     * @param deviceToken FCM device token
     * @param title notification title
     * @param body notification body
     * @param data optional data payload (JSON string)
     */
    public void sendPush(String deviceToken, String title, String body, String data) {
        if (!initialized) {
            LOG.debug("Firebase not initialized, skipping push for token {}", deviceToken);
            return;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build());

            if (data != null) {
                messageBuilder.putData("payload", data);
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            LOG.debug("Push notification sent: {}", response);
        } catch (Exception e) {
            LOG.warn("Failed to send push notification to {}: {}", deviceToken, e.getMessage());
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}
