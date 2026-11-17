package bioflix.notification.messaging;

import java.util.List;

/** Routing keys (= event types) on the cinema.events topic exchange that notification-service listens to. */
public final class EventRoutingKeys {

    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_UPDATED = "user.updated";
    public static final String USER_DELETED = "user.deleted";
    public static final String BOOKING_CONFIRMED = "booking.confirmed";
    public static final String BOOKING_CANCELLED = "booking.cancelled";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String PAYMENT_REFUNDED = "payment.refunded";
    public static final String TICKET_ISSUED = "ticket.issued";

    /** Binding patterns for our queue. booking.created is deliberately not included – nothing to tell the customer yet. */
    public static final List<String> SUBSCRIBED = List.of(
            "user.*",
            BOOKING_CONFIRMED,
            BOOKING_CANCELLED,
            "payment.*",
            TICKET_ISSUED);

    private EventRoutingKeys() {
    }
}
