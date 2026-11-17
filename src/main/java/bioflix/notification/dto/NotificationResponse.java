package bioflix.notification.dto;

import bioflix.notification.entity.NotificationChannel;
import bioflix.notification.entity.Notification;
import bioflix.notification.entity.NotificationStatus;
import bioflix.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

/** Delivery status of one notification – answers "did the customer get their ticket?". */
public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType template,
        NotificationChannel channel,
        String recipient,
        String subject,
        NotificationStatus status,
        int attempts,
        String providerMessageId,
        String lastError,
        UUID triggerEventId,
        String triggerEventType,
        String correlationId,
        Instant createdAt,
        Instant sentAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getUserId(), n.getTemplate(), n.getChannel(), n.getRecipient(),
                n.getSubject(), n.getStatus(), n.getAttempts(), n.getProviderMessageId(), n.getLastError(),
                n.getTriggerEventId(), n.getTriggerEventType(), n.getCorrelationId(), n.getCreatedAt(), n.getSentAt());
    }
}
