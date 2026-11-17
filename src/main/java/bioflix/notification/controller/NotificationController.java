package bioflix.notification.controller;

import bioflix.notification.dto.NotificationResponse;
import bioflix.notification.entity.NotificationStatus;
import bioflix.notification.exception.ForbiddenException;
import bioflix.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of the notification log. Notifications are only triggered by events.
 *
 * The API gateway validates the token (via user-service) and forwards the caller as
 * X-User-Id and X-User-Role – this service trusts those headers and is not exposed directly.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final String ADMIN = "ADMIN";

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** The caller's own notifications, newest first. */
    @GetMapping("/me")
    public List<NotificationResponse> mine(@RequestHeader("X-User-Id") UUID userId) {
        return notificationService.getForUser(userId);
    }

    /** Admin: latest 100, optionally only one status, e.g. ?status=FAILED. */
    @GetMapping
    public List<NotificationResponse> latest(@RequestHeader(value = "X-User-Role", required = false) String role,
                                             @RequestParam(required = false) NotificationStatus status) {
        requireAdmin(role);
        return notificationService.getLatest(status);
    }

    /** Delivery status of one notification – the owner or an admin. */
    @GetMapping("/{id}")
    public NotificationResponse byId(@PathVariable UUID id,
                                     @RequestHeader("X-User-Id") UUID userId,
                                     @RequestHeader(value = "X-User-Role", required = false) String role) {
        NotificationResponse notification = notificationService.getById(id);
        if (!ADMIN.equalsIgnoreCase(role) && !notification.userId().equals(userId)) {
            throw new ForbiddenException("You can only see your own notifications");
        }
        return notification;
    }

    @GetMapping("/users/{userId}")
    public List<NotificationResponse> forUser(@PathVariable UUID userId,
                                              @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return notificationService.getForUser(userId);
    }

    private static void requireAdmin(String role) {
        if (!ADMIN.equalsIgnoreCase(role)) {
            throw new ForbiddenException("Admin only");
        }
    }
}
