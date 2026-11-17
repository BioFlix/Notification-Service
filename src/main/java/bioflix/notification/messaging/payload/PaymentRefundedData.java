package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** data of payment.refunded. amount in øre. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentRefundedData(
        String paymentId,
        String refundId,
        String bookingId,
        UUID userId,
        Long amount,
        String currency,
        String userEmail
) implements UserScopedData {
}
