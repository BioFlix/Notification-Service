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

import java.time.Instant;

/** "notif. log" in the diagram – every notification we tried to send. */
@Entity
@Table(name = "notification_log", indexes = @Index(name = "idx_notification_user", columnList = "user_id"))
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    /** The event that triggered it, e.g. "booking.confirmed". */
    @Column(nullable = false)
    private String eventType;

    private String recipient;
    private String subject;

    @Column(columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationLog() {
        // for JPA
    }

    public NotificationLog(Long userId, NotificationChannel channel, String eventType, String recipient,
                           String subject, String body, NotificationStatus status, String errorMessage) {
        this.userId = userId;
        this.channel = channel;
        this.eventType = eventType;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public NotificationChannel getChannel() { return channel; }
    public String getEventType() { return eventType; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
