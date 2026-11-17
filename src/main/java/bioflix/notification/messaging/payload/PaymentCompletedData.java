package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** data of payment.completed. amount in øre, e.g. 13500 = 135,00 DKK. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCompletedData(
        String paymentId,
        String bookingId,
        UUID userId,
        Long amount,
        String currency,
        String receiptNumber,
        String receiptUrl,
        String userEmail
) implements UserScopedData {
}
