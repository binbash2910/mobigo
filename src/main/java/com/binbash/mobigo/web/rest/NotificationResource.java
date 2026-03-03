package com.binbash.mobigo.web.rest;

import com.binbash.mobigo.domain.Notification;
import com.binbash.mobigo.domain.People;
import com.binbash.mobigo.repository.PeopleRepository;
import com.binbash.mobigo.security.SecurityUtils;
import com.binbash.mobigo.service.NotificationService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api/notifications")
public class NotificationResource {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationResource.class);

    private final NotificationService notificationService;
    private final PeopleRepository peopleRepository;

    public NotificationResource(NotificationService notificationService, PeopleRepository peopleRepository) {
        this.notificationService = notificationService;
        this.peopleRepository = peopleRepository;
    }

    private Long getCurrentPeopleId() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new RuntimeException("User not authenticated"));
        People people = peopleRepository
            .findByUserLogin(login)
            .orElseThrow(() -> new RuntimeException("People not found for login: " + login));
        return people.getId();
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getNotifications(Pageable pageable) {
        LOG.debug("REST request to get notifications");
        Long userId = getCurrentPeopleId();
        Page<Notification> page = notificationService.getNotifications(userId, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        Long userId = getCurrentPeopleId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable("id") Long id) {
        LOG.debug("REST request to mark notification {} as read", id);
        Notification notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        Long userId = getCurrentPeopleId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete notification {}", id);
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
