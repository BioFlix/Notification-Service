package bioflix.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * The send log: one row per e-mail / SMS. Unique per (trigger event, channel, template),
 * so a redelivered event finds the existing row instead of sending the e-mail twice.
 *
 * No foreign key to recipients on purpose: an event may carry the e-mail itself and arrive
 * before user.registered has been processed.
 */
@Entity
@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "one_send_per_event",
                columnNames = {"trigger_event_id", "channel", "template"}),
        indexes = @Index(name = "notifications_by_user", columnList = "user_id, created_at"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private NotificationType template;

    /** E-mail address or phone number used. */
    @Column(nullable = false, length = 320)
    private String recipient;

    private String subject;

    @Column(columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationStatus status = NotificationStatus.QUEUED;

    @Column(nullable = false)
    private short attempts;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Column(name = "trigger_event_id", nullable = false)
    private UUID triggerEventId;

    @Column(name = "trigger_event_type", nullable = false, length = 64)
    private String triggerEventType;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    protected Notification() {
        // for JPA
    }

    public Notification(UUID triggerEventId, String triggerEventType, String correlationId, NotificationType template,
                        NotificationChannel channel, UUID userId, String recipient, String subject, String body) {
        this.triggerEventId = triggerEventId;
        this.triggerEventType = triggerEventType;
        this.correlationId = correlationId;
        this.template = template;
        this.channel = channel;
        this.userId = userId;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void markSent(String providerMessageId) {
        attempts++;
        status = NotificationStatus.SENT;
        sentAt = Instant.now();
        lastError = null;
        this.providerMessageId = providerMessageId;
    }

    public void markAttemptFailed(String error) {
        attempts++;
        lastError = error;
    }

    public void markFailed(String error) {
        status = NotificationStatus.FAILED;
        if (error != null) {
            lastError = error;
        }
    }

    /** A FAILED event was re-driven from the dead-letter queue. */
    public void reopen() {
        status = NotificationStatus.QUEUED;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public NotificationChannel getChannel() { return channel; }
    public NotificationType getTemplate() { return template; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getProviderMessageId() { return providerMessageId; }
    public String getLastError() { return lastError; }
    public UUID getTriggerEventId() { return triggerEventId; }
    public String getTriggerEventType() { return triggerEventType; }
    public String getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}
