package bioflix.notification.service;

import bioflix.notification.entity.NotificationChannel;
import bioflix.notification.entity.NotificationType;
import bioflix.notification.entity.Recipient;
import bioflix.notification.exception.RecipientUnknownException;
import bioflix.notification.messaging.EventEnvelope;
import bioflix.notification.messaging.EventParser;
import bioflix.notification.messaging.InvalidEventException;
import bioflix.notification.messaging.payload.BookingCancelledData;
import bioflix.notification.messaging.payload.BookingConfirmedData;
import bioflix.notification.messaging.payload.PaymentCompletedData;
import bioflix.notification.messaging.payload.PaymentFailedData;
import bioflix.notification.messaging.payload.PaymentRefundedData;
import bioflix.notification.messaging.payload.TicketIssuedData;
import bioflix.notification.messaging.payload.UserEventData;
import bioflix.notification.messaging.payload.UserScopedData;
import bioflix.notification.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static bioflix.notification.messaging.EventRoutingKeys.BOOKING_CANCELLED;
import static bioflix.notification.messaging.EventRoutingKeys.BOOKING_CONFIRMED;
import static bioflix.notification.messaging.EventRoutingKeys.PAYMENT_COMPLETED;
import static bioflix.notification.messaging.EventRoutingKeys.PAYMENT_FAILED;
import static bioflix.notification.messaging.EventRoutingKeys.PAYMENT_REFUNDED;
import static bioflix.notification.messaging.EventRoutingKeys.TICKET_ISSUED;
import static bioflix.notification.messaging.EventRoutingKeys.USER_DELETED;
import static bioflix.notification.messaging.EventRoutingKeys.USER_REGISTERED;
import static bioflix.notification.messaging.EventRoutingKeys.USER_UPDATED;

@Service
public class NotificationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventHandler.class);

    private final EventParser parser;
    private final RecipientService recipientService;
    private final NotificationService notificationService;
    private final NotificationTemplates templates;
    private final ProcessedEventRepository processedEvents;
    private final boolean smsEnabled;

    public NotificationEventHandler(EventParser parser, RecipientService recipientService,
                                    NotificationService notificationService, NotificationTemplates templates,
                                    ProcessedEventRepository processedEvents,
                                    @Value("${app.sms.enabled:false}") boolean smsEnabled) {
        this.parser = parser;
        this.recipientService = recipientService;
        this.notificationService = notificationService;
        this.templates = templates;
        this.processedEvents = processedEvents;
        this.smsEnabled = smsEnabled;
    }

    /**
     * Inbox pattern: the eventId is inserted into processed_events first, in this transaction.
     * A redelivered (or concurrently delivered) copy finds it there and is skipped. If handling fails,
     * the transaction rolls back, the eventId is released and the retry handles the event again.
     */
    @Transactional
    public void handle(EventEnvelope event) {
        log.info("Received {} (eventId {}, correlationId {}, producer {})",
                event.type(), event.eventId(), event.correlationId(), event.producer());

        if (processedEvents.claim(event.eventId(), event.type()) == 0) {
            log.info("Duplicate event {} ({}) – already processed, skipping", event.eventId(), event.type());
            return;
        }

        switch (event.type()) {
            case USER_REGISTERED -> onUserRegistered(event);
            case USER_UPDATED -> onUserUpdated(event);
            case USER_DELETED -> onUserDeleted(event);
            case BOOKING_CONFIRMED -> notifyCustomer(event, NotificationType.BOOKING_CONFIRMED,
                    parser.data(event, BookingConfirmedData.class), templates::bookingConfirmed);
            case BOOKING_CANCELLED -> notifyCustomer(event, NotificationType.BOOKING_CANCELLED,
                    parser.data(event, BookingCancelledData.class), templates::bookingCancelled);
            case PAYMENT_COMPLETED -> notifyCustomer(event, NotificationType.PAYMENT_COMPLETED,
                    parser.data(event, PaymentCompletedData.class), templates::paymentCompleted);
            case PAYMENT_FAILED -> notifyCustomer(event, NotificationType.PAYMENT_FAILED,
                    parser.data(event, PaymentFailedData.class), templates::paymentFailed);
            case PAYMENT_REFUNDED -> notifyCustomer(event, NotificationType.PAYMENT_REFUNDED,
                    parser.data(event, PaymentRefundedData.class), templates::paymentRefunded);
            case TICKET_ISSUED -> notifyCustomer(event, NotificationType.TICKET_ISSUED,
                    parser.data(event, TicketIssuedData.class), templates::ticketsIssued);
            default -> log.info("Ignoring event type {} – nothing to notify", event.type());
        }
    }

    // ---- user.* : keep our own copy of the contact details ----

    private void onUserRegistered(EventEnvelope event) {
        UserEventData data = userData(event);
        recipientService.upsert(data.userId(), data.email(), data.fullName(), data.phone(), data.wantsSms(),
                event.occurredAt());

        Optional<Recipient> recipient = recipientService.find(data.userId());
        if (recipient.map(Recipient::isDeleted).orElse(false)) {
            log.info("User {} is already deleted – no welcome e-mail", data.userId());
            return;
        }
        String email = firstNonBlank(recipient.map(Recipient::getEmail).orElse(null), data.email());
        if (email == null) {
            log.warn("user.registered for user {} has no e-mail – no welcome e-mail", data.userId());
            return;
        }
        notificationService.deliver(event, NotificationType.WELCOME, NotificationChannel.EMAIL,
                data.userId(), email, templates.welcome(data));
    }

    private void onUserUpdated(EventEnvelope event) {
        UserEventData data = userData(event);
        recipientService.upsert(data.userId(), data.email(), data.fullName(), data.phone(), data.wantsSms(),
                event.occurredAt());
    }

    private void onUserDeleted(EventEnvelope event) {
        recipientService.markDeleted(userData(event).userId(), event.occurredAt());
    }

    private UserEventData userData(EventEnvelope event) {
        UserEventData data = parser.data(event, UserEventData.class);
        if (data.userId() == null) {
            throw new InvalidEventException("Event " + event.eventId() + " (" + event.type() + ") has no userId");
        }
        return data;
    }

    // ---- booking / payment / ticket: notify the customer ----

    private <T extends UserScopedData> void notifyCustomer(EventEnvelope event, NotificationType template, T data,
                                                           NotificationRenderer<T> renderer) {
        UUID userId = data.userId();
        if (userId == null) {
            throw new InvalidEventException("Event " + event.eventId() + " (" + event.type() + ") has no userId");
        }

        Optional<Recipient> recipient = recipientService.find(userId);
        if (recipient.map(Recipient::isDeleted).orElse(false)) {
            log.info("User {} is deleted – not sending {}", userId, template);
            return;
        }

        String email = firstNonBlank(data.userEmail(), recipient.map(Recipient::getEmail).orElse(null));
        if (email == null) {
            // Probably user.registered has not reached us yet – throw so the message is retried
            throw new RecipientUnknownException(userId);
        }

        String fullName = recipient.map(Recipient::getFullName).orElse(null);
        RenderedMessage message = renderer.render(data, fullName);
        notificationService.deliver(event, template, NotificationChannel.EMAIL, userId, email, message);

        if (smsEnabled && message.smsText() != null && recipient.map(Recipient::canReceiveSms).orElse(false)) {
            notificationService.deliver(event, template, NotificationChannel.SMS, userId, recipient.get().getPhone(), message);
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b != null && !b.isBlank() ? b : null;
    }

    @FunctionalInterface
    interface NotificationRenderer<T> {
        RenderedMessage render(T data, String fullName);
    }
}
