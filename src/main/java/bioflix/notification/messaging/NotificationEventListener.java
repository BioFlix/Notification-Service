package bioflix.notification.messaging;

import bioflix.notification.service.NotificationEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * The asynchronous entry point: one queue with all the events we care about.
 *
 * What happens when something goes wrong:
 * - Invalid message (bad JSON, missing ids)  -> rejected at once, goes to the dead-letter queue.
 * - Mail server down / user not known yet    -> exception -> retried with backoff (application.yml),
 *                                               then FAILED + dead-letter queue (NotificationFailureRecoverer).
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final EventParser parser;
    private final NotificationEventHandler handler;

    public NotificationEventListener(EventParser parser, NotificationEventHandler handler) {
        this.parser = parser;
        this.handler = handler;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void onMessage(Message message) {
        try {
            handler.handle(parser.parse(message));
        } catch (InvalidEventException ex) {
            log.error("Rejecting invalid message (routing key {}): {}",
                    message.getMessageProperties().getReceivedRoutingKey(), ex.getMessage());
            throw new AmqpRejectAndDontRequeueException(ex.getMessage(), ex);
        }
    }
}
