package bioflix.notification.entity;

/** Which template was used. Together with eventId and channel it identifies a notification (idempotency). */
public enum NotificationType {
    WELCOME,
    BOOKING_CONFIRMED,
    BOOKING_CANCELLED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED,
    TICKET_ISSUED
}
