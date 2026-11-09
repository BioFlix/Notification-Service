package bioflix.notification.entity;

public enum NotificationStatus {
    SENT,
    FAILED,
    SKIPPED   // e.g. no e-mail address known for the user
}
