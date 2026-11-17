package bioflix.notification.messaging;

import bioflix.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.stereotype.Component;

/**
 * Runs when all listener retries for a message are used up (see spring.rabbitmq.listener.simple.retry).
 * Marks the PENDING notifications of the event as FAILED, then rejects the message without requeue,
 * so RabbitMQ moves it to the dead-letter queue. Spring Boot picks this bean up automatically.
 */
@Component
public class NotificationFailureRecoverer extends RejectAndDontRequeueRecoverer {

    private static final Logger log = LoggerFactory.getLogger(NotificationFailureRecoverer.class);

    private final EventParser parser;
    private final NotificationService notificationService;

    public NotificationFailureRecoverer(EventParser parser, NotificationService notificationService) {
        this.parser = parser;
        this.notificationService = notificationService;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        String reason = rootMessage(cause);
        try {
            EventEnvelope event = parser.parse(message);
            notificationService.markFailed(event.eventId(), reason);
            log.error("Giving up on {} (eventId {}) – moved to dead-letter queue: {}", event.type(), event.eventId(), reason);
        } catch (RuntimeException ex) {
            log.error("Giving up on unreadable message – moved to dead-letter queue: {}", reason);
        }
        super.recover(message, cause);
    }

    private static String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }
}
