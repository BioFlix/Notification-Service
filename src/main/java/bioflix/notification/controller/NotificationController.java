package bioflix.notification.controller;

import bioflix.notification.dto.NotificationResponse;
import bioflix.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only view of the notification log. Notifications are only triggered by events. */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public List<NotificationResponse> mine(@RequestHeader("X-User-Id") Long userId) {
        return notificationService.getForUser(userId);
    }

    /** Latest 100 – TODO: admin only. */
    @GetMapping
    public List<NotificationResponse> latest() {
        return notificationService.getLatest();
    }
}
