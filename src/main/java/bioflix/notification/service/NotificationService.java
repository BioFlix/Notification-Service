package bioflix.notification.service;

import bioflix.notification.dto.NotificationResponse;
import bioflix.notification.entity.NotificationChannel;
import bioflix.notification.entity.NotificationLog;
import bioflix.notification.entity.NotificationStatus;
import bioflix.notification.entity.NotificationType;
import bioflix.notification.exception.DeliveryFailedException;
import bioflix.notification.exception.NotFoundException;
import bioflix.notification.messaging.EventEnvelope;
import bioflix.notification.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Delivers one notification and keeps notification_log up to date.
 *
 * Flow per (event, type, channel):
 * <ol>
 *   <li>Find the existing row or insert a PENDING one. If it is already SENT, the event is a
 *       redelivery and nothing is sent again (idempotency).</li>
 *   <li>Send. On success the row becomes SENT.</li>
 *   <li>On failure attempts++ and last_error are saved, and a {@link DeliveryFailedException} is thrown.
 *       The RabbitMQ listener retries the message with backoff. When the retries are used up,
 *       {@link #markFailed} sets the row to FAILED and the message goes to the dead-letter queue.</li>
 * </ol>
 *
 * The method is deliberately not one big transaction: the e-mail cannot be rolled back anyway,
 * and the attempt counter must survive a failed attempt.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationLogRepository logRepository;
    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public NotificationService(NotificationLogRepository logRepository, EmailSender emailSender, SmsSender smsSender) {
        this.logRepository = logRepository;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    public void deliver(EventEnvelope event, NotificationType type, NotificationChannel channel,
                        Long userId, String recipient, RenderedMessage message) {
        NotificationLog notification = findOrCreate(event, type, channel, userId, recipient, message);

        if (notification.getStatus() == NotificationStatus.SENT) {
            log.info("Duplicate event {} – {} {} was already sent, skipping", event.eventId(), type, channel);
            return;
        }
        if (notification.getStatus() == NotificationStatus.FAILED) {
            log.info("Event {} re-driven – retrying {} {}", event.eventId(), type, channel);
            notification.reopen();
        }

        try {
            switch (channel) {
                case EMAIL -> emailSender.send(notification.getRecipient(), message.subject(), message.body(),
                        message.attachments());
                case SMS -> smsSender.send(notification.getRecipient(), message.smsText());
            }
        } catch (RuntimeException ex) {
            notification.markAttemptFailed(describe(ex));
            logRepository.save(notification);
            log.warn("Attempt {} to send {} {} for event {} failed: {}", notification.getAttempts(), type, channel,
                    event.eventId(), describe(ex));
            throw new DeliveryFailedException("Could not send " + type + " " + channel + " for event " + event.eventId(), ex);
        }

        notification.markSent();
        logRepository.save(notification);
        log.info("Sent {} {} to user {} (event {}, correlationId {})", type, channel, userId, event.eventId(),
                event.correlationId());
    }

    @Transactional
    public void markFailed(UUID eventId, String error) {
        List<NotificationLog> pending = logRepository.findByEventIdAndStatus(eventId, NotificationStatus.PENDING);
        pending.forEach(n -> n.markFailed(error));
        logRepository.saveAll(pending);
    }

    private NotificationLog findOrCreate(EventEnvelope event, NotificationType type, NotificationChannel channel,
                                         Long userId, String recipient, RenderedMessage message) {
        return logRepository.findByEventIdAndTypeAndChannel(event.eventId(), type, channel)
                .orElseGet(() -> {
                    String text = channel == NotificationChannel.SMS ? message.smsText() : message.body();
                    String subject = channel == NotificationChannel.SMS ? null : message.subject();
                    try {
                        return logRepository.saveAndFlush(new NotificationLog(event.eventId(), event.type(),
                                event.correlationId(), type, channel, userId, recipient, subject, text));
                    } catch (DataIntegrityViolationException ex) {
                        // Another consumer inserted the same (event, type, channel) at the same moment
                        return logRepository.findByEventIdAndTypeAndChannel(event.eventId(), type, channel)
                                .orElseThrow(() -> ex);
                    }
                });
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
    public List<NotificationResponse> getForUser(Long userId) {
        return logRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getLatest(NotificationStatus status) {
        List<NotificationLog> rows = status == null
                ? logRepository.findTop100ByOrderByCreatedAtDesc()
                : logRepository.findTop100ByStatusOrderByCreatedAtDesc(status);
        return rows.stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id) {
        return logRepository.findById(id).map(NotificationResponse::from)
                .orElseThrow(() -> new NotFoundException("Notification " + id + " not found"));
    }
}
