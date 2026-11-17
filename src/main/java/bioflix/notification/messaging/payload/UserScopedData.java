package bioflix.notification.messaging.payload;

/** Payloads that are about one customer. userEmail is optional in all of them. */
public interface UserScopedData {

    Long userId();

    String userEmail();
}
