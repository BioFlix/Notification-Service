package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/** data of ticket.issued – one event per booking, with all its tickets. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketIssuedData(
        String bookingId,
        Long userId,
        String userEmail,
        String movieTitle,
        String cinemaName,
        String hallName,
        Instant startsAt,
        List<TicketData> tickets
) implements UserScopedData {
}
