package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * data of booking.confirmed: bookingId, userId, showtimeId, movieTitle, hallName, startsAt, seats[].
 * userEmail, cinemaName, totalAmount and currency are optional extras.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingConfirmedData(
        String bookingId,
        UUID userId,
        String showtimeId,
        String movieTitle,
        String hallName,
        Instant startsAt,
        List<SeatData> seats,
        String userEmail,
        String cinemaName,
        Long totalAmount,
        String currency
) implements UserScopedData {

    /** totalAmount if sent, otherwise the sum of the seat prices (null if no prices are known). */
    public Long total() {
        if (totalAmount != null) {
            return totalAmount;
        }
        if (seats == null || seats.isEmpty() || seats.stream().anyMatch(s -> s == null || s.price() == null)) {
            return null;
        }
        return seats.stream().filter(Objects::nonNull).mapToLong(SeatData::price).sum();
    }
}
