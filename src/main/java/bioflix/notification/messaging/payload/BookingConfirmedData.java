package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/** data of booking.confirmed. userEmail is optional – if missing we use our own user_contacts copy. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingConfirmedData(
        String bookingId,
        Long userId,
        String userEmail,
        String movieTitle,
        String cinemaName,
        String hallName,
        Instant startsAt,
        List<SeatData> seats,
        Long totalAmount,
        String currency
) implements UserScopedData {
}
