package bioflix.notification.dto;

import bioflix.notification.entity.NotificationChannel;
import bioflix.notification.entity.NotificationLog;
import bioflix.notification.entity.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long userId,
        NotificationChannel channel,
        String eventType,
        String recipient,
        String subject,
        NotificationStatus status,
        String errorMessage,
        Instant createdAt
) {
    public static NotificationResponse from(NotificationLog n) {
        return new NotificationResponse(n.getId(), n.getUserId(), n.getChannel(), n.getEventType(),
                n.getRecipient(), n.getSubject(), n.getStatus(), n.getErrorMessage(), n.getCreatedAt());
    }
}
