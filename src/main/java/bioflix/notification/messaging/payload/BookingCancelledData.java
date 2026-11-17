package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/** data of booking.cancelled. reason: e.g. USER, PAYMENT_FAILED, EXPIRED. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingCancelledData(
        String bookingId,
        Long userId,
        String userEmail,
        String reason,
        String movieTitle,
        Instant startsAt
) implements UserScopedData {
}
