package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** data of user.registered / user.updated / user.deleted (user.deleted only has userId). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserEventData(
        UUID userId,
        String email,
        String fullName,
        String phone,
        Boolean smsOptIn
) {
    public boolean wantsSms() {
        return Boolean.TRUE.equals(smsOptIn);
    }
}
