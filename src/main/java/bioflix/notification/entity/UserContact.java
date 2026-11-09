package bioflix.notification.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Local read-model of users, kept up to date from user.* events.
 * This way notification can find an e-mail address without calling user-service
 * (no synchronous dependency – the services stay loosely coupled).
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
    private Instant updatedAt;

    protected UserContact() {
        // for JPA
    }

    public UserContact(Long userId) {
        this.userId = userId;
    }

    public void update(String email, String fullName, String phone) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.updatedAt = Instant.now();
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public Instant getUpdatedAt() { return updatedAt; }
}
