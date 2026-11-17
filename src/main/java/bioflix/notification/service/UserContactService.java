package bioflix.notification.service;

import bioflix.notification.entity.UserContact;
import bioflix.notification.repository.UserContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Keeps the local user_contacts table in sync with user.* events. */
@Service
public class UserContactService {

    private static final Logger log = LoggerFactory.getLogger(UserContactService.class);

    private final UserContactRepository repository;

    public UserContactService(UserContactRepository repository) {
        this.repository = repository;
    }

    /**
     * user.registered / user.updated. Returns false if the event was ignored
     * because it is older than what we have, or because the user is already deleted.
     */
    @Transactional
    public boolean upsert(Long userId, String email, String fullName, String phone, Instant occurredAt) {
        UserContact contact = repository.findById(userId).orElseGet(() -> new UserContact(userId));
        if (contact.isDeleted()) {
            log.info("Ignoring update for deleted user {}", userId);
            return false;
        }
        if (contact.isStale(occurredAt)) {
            log.info("Ignoring out-of-date update for user {} (event from {}, we have {})",
                    userId, occurredAt, contact.getLastEventAt());
            return false;
        }
        contact.update(email, fullName, phone, occurredAt);
        repository.save(contact);
        return true;
    }

    /** user.deleted – keeps a tombstone row without personal data. */
    @Transactional
    public void markDeleted(Long userId, Instant occurredAt) {
        UserContact contact = repository.findById(userId).orElseGet(() -> new UserContact(userId));
        contact.markDeleted(occurredAt);
        repository.save(contact);
    }

    @Transactional(readOnly = true)
    public Optional<UserContact> find(Long userId) {
        return repository.findById(userId);
    }
}
