package bioflix.notification.entity;

public enum NotificationStatus {
    /** Created, not delivered yet (also while it is being retried). */
    QUEUED,
    SENT,
    /** Gave up after all retries – the event is in the dead-letter queue. */
    FAILED
}
