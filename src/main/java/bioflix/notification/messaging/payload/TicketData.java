package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One ticket in ticket.issued. qrToken is the signed token the QR code must contain. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketData(
        String ticketNumber,
        String row,
        Integer number,
        String qrToken
) {
}
