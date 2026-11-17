package bioflix.notification.dto;

import bioflix.notification.entity.NotificationChannel;
import bioflix.notification.entity.NotificationLog;
import bioflix.notification.entity.NotificationStatus;
import bioflix.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

/** Delivery status of one notification – answers "did the customer get their ticket?". */
public record NotificationResponse(
        Long id,
        Long userId,
        NotificationType type,
        NotificationChannel channel,
        String recipient,
        String subject,
        NotificationStatus status,
        int attempts,
        String lastError,
        UUID eventId,
        String eventType,
        String correlationId,
        Instant createdAt,
        Instant sentAt
) {
    public static NotificationResponse from(NotificationLog n) {
        return new NotificationResponse(n.getId(), n.getUserId(), n.getType(), n.getChannel(), n.getRecipient(),
                n.getSubject(), n.getStatus(), n.getAttempts(), n.getLastError(), n.getEventId(), n.getEventType(),
                n.getCorrelationId(), n.getCreatedAt(), n.getSentAt());
    }
}
