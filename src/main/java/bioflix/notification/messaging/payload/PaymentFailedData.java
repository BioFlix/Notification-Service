package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** data of payment.failed. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentFailedData(
        String paymentId,
        String bookingId,
        UUID userId,
        String reason,
        String userEmail
) implements UserScopedData {
}
