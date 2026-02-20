package com.binbash.mobigo.web.websocket;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketNotificationService.class);

    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketNotificationService(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyDataChanged(String eventType) {
        LOG.debug("Broadcasting data change event: {}", eventType);
        messagingTemplate.convertAndSend("/topic/data-updates", Map.of("type", eventType));
    }
}
