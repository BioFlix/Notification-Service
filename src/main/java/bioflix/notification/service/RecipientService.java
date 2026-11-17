package bioflix.notification.service;

import bioflix.notification.entity.Recipient;
import bioflix.notification.repository.RecipientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Keeps the recipients table in sync with user.* events. */
@Service
public class RecipientService {

    private static final Logger log = LoggerFactory.getLogger(RecipientService.class);

    private final RecipientRepository repository;

    public RecipientService(RecipientRepository repository) {
        this.repository = repository;
    }

    /** user.registered / user.updated. Ignored if older than what we have, or if the user is deleted. */
    @Transactional
    public void upsert(UUID userId, String email, String fullName, String phone, boolean smsOptIn, Instant occurredAt) {
        Recipient recipient = repository.findById(userId).orElseGet(() -> new Recipient(userId));
        if (recipient.isDeleted()) {
            log.info("Ignoring update for deleted user {}", userId);
            return;
        }
        if (recipient.isStale(occurredAt)) {
            log.info("Ignoring out-of-date update for user {} (event from {}, we have {})",
                    userId, occurredAt, recipient.getLastEventAt());
            return;
        }
        recipient.update(email, fullName, phone, smsOptIn, occurredAt);
        repository.save(recipient);
    }

    @Transactional
    public void markDeleted(UUID userId, Instant occurredAt) {
        Recipient recipient = repository.findById(userId).orElseGet(() -> new Recipient(userId));
        recipient.markDeleted(occurredAt);
        repository.save(recipient);
    }

    @Transactional(readOnly = true)
    public Optional<Recipient> find(UUID userId) {
        return repository.findById(userId);
    }
}
