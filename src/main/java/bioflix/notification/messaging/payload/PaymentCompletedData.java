package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** data of payment.completed. amount is in minor units (øre), e.g. 13500 = 135,00 DKK. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCompletedData(
        String paymentId,
        String bookingId,
        Long userId,
        String userEmail,
        Long amount,
        String currency,
        String receiptNumber
) implements UserScopedData {
}
