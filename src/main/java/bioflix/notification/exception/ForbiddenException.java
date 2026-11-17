package bioflix.notification.exception;

/** Mapped to HTTP 403 by GlobalExceptionHandler. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
