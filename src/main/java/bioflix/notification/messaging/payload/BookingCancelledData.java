package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * data of booking.cancelled: bookingId, userId, reason, wasPaid.
 * reason: USER | PAYMENT_FAILED | EXPIRED | SHOWTIME_CANCELLED. movieTitle / startsAt are optional extras.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingCancelledData(
        String bookingId,
        UUID userId,
        String reason,
        Boolean wasPaid,
        String userEmail,
        String movieTitle,
        Instant startsAt
) implements UserScopedData {
}
