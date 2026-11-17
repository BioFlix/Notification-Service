package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** data of payment.failed. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentFailedData(
        String paymentId,
        String bookingId,
        Long userId,
        String userEmail,
        String reason
) implements UserScopedData {
}
