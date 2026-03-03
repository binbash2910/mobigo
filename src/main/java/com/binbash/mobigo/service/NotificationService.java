package com.binbash.mobigo.service;

import com.binbash.mobigo.domain.DeviceToken;
import com.binbash.mobigo.domain.Notification;
import com.binbash.mobigo.domain.enumeration.NotificationType;
import com.binbash.mobigo.repository.DeviceTokenRepository;
import com.binbash.mobigo.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ObjectMapper objectMapper;
    private final FirebaseService firebaseService;
    private final DeviceTokenRepository deviceTokenRepository;

    public NotificationService(
        NotificationRepository notificationRepository,
        SimpMessageSendingOperations messagingTemplate,
        ObjectMapper objectMapper,
        FirebaseService firebaseService,
        DeviceTokenRepository deviceTokenRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.firebaseService = firebaseService;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    public Notification createAndSend(
        String userLogin,
        Long userId,
        NotificationType type,
        String title,
        String message,
        Map<String, Object> data
    ) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);

        if (data != null) {
            try {
                notification.setData(objectMapper.writeValueAsString(data));
            } catch (Exception e) {
                LOG.warn("Failed to serialize notification data: {}", e.getMessage());
            }
        }

        notification = notificationRepository.save(notification);
        LOG.info("Notification created: type={}, userId={}, title={}", type, userId, title);

        try {
            messagingTemplate.convertAndSendToUser(userLogin, "/topic/notifications", notification);
            LOG.debug("WebSocket notification sent to user {}", userLogin);
        } catch (Exception e) {
            LOG.warn("Failed to send WebSocket notification to user {}: {}", userLogin, e.getMessage());
        }

        // Send push notifications to all registered devices for this user
        try {
            List<DeviceToken> devices = deviceTokenRepository.findByUserId(userId);
            for (DeviceToken device : devices) {
                firebaseService.sendPush(device.getToken(), title, message, notification.getData());
            }
        } catch (Exception e) {
            LOG.warn("Failed to send push notifications for userId {}: {}", userId, e.getMessage());
        }

        return notification;
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository
            .findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(userId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
        LOG.info("Marked {} notifications as read for userId={}", unread.size(), userId);
    }

    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
