package bioflix.notification.exception;

/** The mail/SMS provider failed. Thrown so the message is retried with backoff. */
public class DeliveryFailedException extends RuntimeException {

    public DeliveryFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
