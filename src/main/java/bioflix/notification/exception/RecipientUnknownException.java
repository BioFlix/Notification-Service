package bioflix.notification.exception;

/**
 * We have no e-mail address for the user yet – most likely user.registered has not been
 * processed yet (events can arrive out of order). Thrown so the message is retried later.
 */
public class RecipientUnknownException extends RuntimeException {

    public RecipientUnknownException(java.util.UUID userId) {
        super("No e-mail address known for user " + userId + " (yet)");
    }
}
