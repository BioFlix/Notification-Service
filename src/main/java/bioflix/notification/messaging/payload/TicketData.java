package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One ticket inside ticket.issued. qrPng is the QR code as a Base64-encoded PNG (optional). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketData(
        String ticketCode,
        String row,
        Integer number,
        String qrPng
) {
}
