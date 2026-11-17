package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** data of payment.refunded. amount is in minor units (øre). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentRefundedData(
        String paymentId,
        String bookingId,
        Long userId,
        String userEmail,
        Long amount,
        String currency
) implements UserScopedData {
}
