package bioflix.notification.messaging;

/**
 * The message can never be processed (not JSON, missing eventId/type/required data).
 * Retrying will not help, so it goes straight to the dead-letter queue.
 */
public class InvalidEventException extends RuntimeException {

    public InvalidEventException(String message) {
        super(message);
    }

    public InvalidEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
