package bioflix.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Local copy of a user's contact details, built from user.* events, so we never call user-service.
 * last_event_at: older events are ignored. deleted: tombstone, a late user.updated cannot revive the user.
 * version: optimistic lock, two user.* events handled at the same time cannot overwrite each other.
 */
@Entity
@Table(name = "recipients")
public class Recipient {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 320)
    private String email;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(length = 32)
    private String phone;

    @Column(name = "sms_opt_in", nullable = false)
    private boolean smsOptIn;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "last_event_at")
    private Instant lastEventAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    protected Recipient() {
        // for JPA
    }

    public Recipient(UUID userId) {
        this.userId = userId;
    }

    public boolean isStale(Instant occurredAt) {
        return lastEventAt != null && occurredAt != null && occurredAt.isBefore(lastEventAt);
    }

    public void update(String email, String fullName, String phone, boolean smsOptIn, Instant occurredAt) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.smsOptIn = smsOptIn;
        touch(occurredAt);
    }

    public void markDeleted(Instant occurredAt) {
        this.email = null;
        this.fullName = null;
        this.phone = null;
        this.smsOptIn = false;
        this.deleted = true;
        touch(occurredAt);
    }

    private void touch(Instant occurredAt) {
        if (occurredAt != null) {
            this.lastEventAt = occurredAt;
        }
        this.updatedAt = Instant.now();
    }

    public boolean canReceiveSms() {
        return smsOptIn && !deleted && phone != null && !phone.isBlank();
    }

    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public boolean isSmsOptIn() { return smsOptIn; }
    public boolean isDeleted() { return deleted; }
    public Instant getLastEventAt() { return lastEventAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
