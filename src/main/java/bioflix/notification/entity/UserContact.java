package bioflix.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * Local read-model of users, kept up to date from user.* events.
 * This way notification can find an e-mail address without calling user-service
 * (no synchronous dependency – the services stay loosely coupled).
 *
 * Two rules make the handlers safe when events arrive late or out of order:
 * - lastEventAt: an event older than the last one we applied is ignored.
 * - deleted (tombstone): after user.deleted the row stays, with the personal data removed,
 *   so a late user.updated cannot bring the user back.
 */
@Entity
@Table(name = "user_contacts")
public class UserContact {

    /** Same id as in user-service – NOT generated here. */
    @Id
    private Long userId;

    private String email;
    private String fullName;
    private String phone;

    @Column(nullable = false)
    private boolean deleted;

    /** occurredAt of the last user.* event applied to this row. */
    private Instant lastEventAt;

    private Instant updatedAt;

    /**
     * Optimistic lock: two user.* events for the same user handled at the same time (we run several
     * consumers) cannot overwrite each other – the loser fails, is retried and sees the new state.
     */
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    protected UserContact() {
        // for JPA
    }

    public UserContact(Long userId) {
        this.userId = userId;
    }

    /** True if an event that happened at {@code occurredAt} is older than what we already have. */
    public boolean isStale(Instant occurredAt) {
        return lastEventAt != null && occurredAt != null && occurredAt.isBefore(lastEventAt);
    }

    public void update(String email, String fullName, String phone, Instant occurredAt) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        touch(occurredAt);
    }

    /** Tombstone: forget the personal data but remember that the user is gone. */
    public void markDeleted(Instant occurredAt) {
        this.email = null;
        this.fullName = null;
        this.phone = null;
        this.deleted = true;
        touch(occurredAt);
    }

    private void touch(Instant occurredAt) {
        if (occurredAt != null) {
            this.lastEventAt = occurredAt;
        }
        this.updatedAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public boolean isDeleted() { return deleted; }
    public Instant getLastEventAt() { return lastEventAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
