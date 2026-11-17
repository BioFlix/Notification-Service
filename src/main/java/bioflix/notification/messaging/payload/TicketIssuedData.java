package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * data of ticket.issued – one event per booking: bookingId, userId, tickets[].
 * movieTitle, cinemaName, hallName, startsAt and userEmail are optional extras.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketIssuedData(
        String bookingId,
        UUID userId,
        List<TicketData> tickets,
        String userEmail,
        String movieTitle,
        String cinemaName,
        String hallName,
        Instant startsAt
) implements UserScopedData {
}
