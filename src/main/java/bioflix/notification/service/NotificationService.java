package bioflix.notification.service;

import bioflix.notification.dto.NotificationResponse;
import bioflix.notification.entity.Notification;
import bioflix.notification.entity.NotificationChannel;
import bioflix.notification.entity.NotificationStatus;
import bioflix.notification.entity.NotificationType;
import bioflix.notification.exception.DeliveryFailedException;
import bioflix.notification.exception.NotFoundException;
import bioflix.notification.messaging.EventEnvelope;
import bioflix.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

/**
 * Delivers one notification and keeps the notifications table up to date.
 *
 * Per (event, channel, template): find the row or insert a QUEUED one. Already SENT -> redelivery, skip.
 * Send -> SENT. Fails -> attempts++ / last_error saved and DeliveryFailedException thrown, so the listener
 * retries with backoff. After the last retry markFailed() sets FAILED and the message is dead-lettered.
 *
 * The rows are written in their own transactions (REQUIRES_NEW), not in the caller's inbox transaction:
 * an e-mail cannot be rolled back, so the log must say what happened even if the event is retried.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final TransactionTemplate ownTransaction;

    public NotificationService(NotificationRepository repository, EmailSender emailSender, SmsSender smsSender,
                               PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void deliver(EventEnvelope event, NotificationType template, NotificationChannel channel,
                        UUID userId, String recipient, RenderedMessage message) {
        Notification notification = findOrCreate(event, template, channel, userId, recipient, message);

        if (notification.getStatus() == NotificationStatus.SENT) {
            log.info("Duplicate event {} – {} {} was already sent, skipping", event.eventId(), template, channel);
            return;
        }
        if (notification.getStatus() == NotificationStatus.FAILED) {
            log.info("Event {} re-driven – retrying {} {}", event.eventId(), template, channel);
            notification.reopen();
        }

        String providerMessageId;
        try {
            providerMessageId = switch (channel) {
                case EMAIL -> emailSender.send(notification.getRecipient(), message.subject(), message.body(),
                        message.attachments());
                case SMS -> smsSender.send(notification.getRecipient(), message.smsText());
            };
        } catch (RuntimeException ex) {
            notification.markAttemptFailed(describe(ex));
            save(notification);
            log.warn("Attempt {} to send {} {} for event {} failed: {}", notification.getAttempts(), template, channel,
                    event.eventId(), describe(ex));
            throw new DeliveryFailedException("Could not send " + template + " " + channel + " for event " + event.eventId(), ex);
        }

        notification.markSent(providerMessageId);
        save(notification);
        log.info("Sent {} {} to user {} (event {}, correlationId {})", template, channel, userId, event.eventId(),
                event.correlationId());
    }

    @Transactional
    public void markFailed(UUID eventId, String error) {
        List<Notification> queued = repository.findByTriggerEventIdAndStatus(eventId, NotificationStatus.QUEUED);
        queued.forEach(n -> n.markFailed(error));
        repository.saveAll(queued);
    }

    private void save(Notification notification) {
        ownTransaction.executeWithoutResult(status -> repository.save(notification));
    }

    private Notification findOrCreate(EventEnvelope event, NotificationType template, NotificationChannel channel,
                                      UUID userId, String recipient, RenderedMessage message) {
        try {
            return ownTransaction.execute(status -> repository
                    .findByTriggerEventIdAndChannelAndTemplate(event.eventId(), channel, template)
                    .orElseGet(() -> {
                        String text = channel == NotificationChannel.SMS ? message.smsText() : message.body();
                        String subject = channel == NotificationChannel.SMS ? null : message.subject();
                        return repository.saveAndFlush(new Notification(event.eventId(), event.type(),
                                event.correlationId(), template, channel, userId, recipient, subject, text));
                    }));
        } catch (DataIntegrityViolationException ex) {
            // Another consumer inserted the same (event, channel, template) at the same moment
            return ownTransaction.execute(status -> repository
                    .findByTriggerEventIdAndChannelAndTemplate(event.eventId(), channel, template)
                    .orElseThrow(() -> ex));
        }
    }

    private static String describe(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    // ---- read side (REST) ----

    @Transactional(readOnly = true)
    public List<NotificationResponse> getForUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getLatest(NotificationStatus status) {
        List<Notification> rows = status == null
                ? repository.findTop100ByOrderByCreatedAtDesc()
                : repository.findTop100ByStatusOrderByCreatedAtDesc(status);
        return rows.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID id) {
        return repository.findById(id).map(NotificationResponse::from)
                .orElseThrow(() -> new NotFoundException("Notification " + id + " not found"));
    }
}
