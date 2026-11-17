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
 * "notif. log" in the diagram – one row per notification we send (or tried to send).
 *
 * The unique constraint on (event_id, type, channel) is what makes the consumer idempotent:
 * RabbitMQ delivers at-least-once, and a redelivered event finds the existing row instead of
 * sending the customer the same e-mail twice.
 */
@Entity
@Table(name = "notification_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_event_type_channel",
                columnNames = {"event_id", "type", "channel"}),
        indexes = {
                @Index(name = "idx_notification_user", columnList = "user_id"),
                @Index(name = "idx_notification_status", columnList = "status")
        })
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** eventId of the event that triggered it. */
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    /** The event type / routing key, e.g. "booking.confirmed". */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** Follows the whole booking saga, so one booking can be traced across services. */
    @Column(name = "correlation_id")
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** E-mail address or phone number used. */
    @Column(nullable = false)
    private String recipient;

    private String subject;

    @Column(columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column(columnDefinition = "text")
    private String lastError;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant sentAt;

    protected NotificationLog() {
        // for JPA
    }

    public NotificationLog(UUID eventId, String eventType, String correlationId, NotificationType type,
                           NotificationChannel channel, Long userId, String recipient, String subject, String body) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.correlationId = correlationId;
        this.type = type;
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

    public void markSent() {
        attempts++;
        status = NotificationStatus.SENT;
        sentAt = Instant.now();
        lastError = null;
    }

    /** One delivery attempt failed – stays PENDING, the message will be retried. */
    public void markAttemptFailed(String error) {
        attempts++;
        lastError = error;
    }

    /** No more retries. */
    public void markFailed(String error) {
        status = NotificationStatus.FAILED;
        if (error != null) {
            lastError = error;
        }
    }

    /** A FAILED event was re-driven from the dead-letter queue – try again. */
    public void reopen() {
        status = NotificationStatus.PENDING;
    }

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getCorrelationId() { return correlationId; }
    public NotificationType getType() { return type; }
    public NotificationChannel getChannel() { return channel; }
    public Long getUserId() { return userId; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
}
