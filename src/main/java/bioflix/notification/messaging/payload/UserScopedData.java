package bioflix.notification.messaging.payload;

import java.util.UUID;

/** Payloads that are about one customer. userEmail is optional – if missing, the recipients copy is used. */
public interface UserScopedData {

    UUID userId();

    String userEmail();
}
