package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** data of user.registered / user.updated / user.deleted (user.deleted only needs userId). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserEventData(
        Long userId,
        String email,
        String fullName,
        String phone
) {
}
