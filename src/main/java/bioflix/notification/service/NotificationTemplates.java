package bioflix.notification.service;

import bioflix.notification.messaging.payload.BookingCancelledData;
import bioflix.notification.messaging.payload.BookingConfirmedData;
import bioflix.notification.messaging.payload.PaymentCompletedData;
import bioflix.notification.messaging.payload.PaymentFailedData;
import bioflix.notification.messaging.payload.PaymentRefundedData;
import bioflix.notification.messaging.payload.TicketData;
import bioflix.notification.messaging.payload.TicketIssuedData;
import bioflix.notification.messaging.payload.UserEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static bioflix.notification.service.MessageFormatter.dateTime;
import static bioflix.notification.service.MessageFormatter.greeting;
import static bioflix.notification.service.MessageFormatter.money;
import static bioflix.notification.service.MessageFormatter.orDash;
import static bioflix.notification.service.MessageFormatter.seat;
import static bioflix.notification.service.MessageFormatter.seats;

@Component
public class NotificationTemplates {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplates.class);

    private static final String SIGNATURE = "\n\nVenlig hilsen\nBioFlix";

    private static final Map<String, String> CANCEL_REASONS = Map.of(
            "USER", "du har selv annulleret bestillingen",
            "PAYMENT_FAILED", "betalingen gik ikke igennem",
            "EXPIRED", "betalingen ikke blev gennemført i tide",
            "SHOWTIME_CANCELLED", "forestillingen er blevet aflyst");

    private final QrCodeGenerator qrCodeGenerator;

    public NotificationTemplates(QrCodeGenerator qrCodeGenerator) {
        this.qrCodeGenerator = qrCodeGenerator;
    }

    public RenderedMessage welcome(UserEventData data) {
        return RenderedMessage.email(
                "Velkommen til BioFlix",
                greeting(data.fullName()) + "\n\n"
                        + "Tak fordi du har oprettet en profil hos BioFlix. "
                        + "Nu kan du finde film, vælge sæder og købe billetter direkte i appen."
                        + SIGNATURE);
    }

    public RenderedMessage bookingConfirmed(BookingConfirmedData data, String fullName) {
        Long total = data.total();
        String body = greeting(fullName) + "\n\n"
                + "Din bestilling er bekræftet.\n\n"
                + "Film:       " + orDash(data.movieTitle()) + "\n"
                + optionalLine("Biograf:    ", data.cinemaName())
                + "Sal:        " + orDash(data.hallName()) + "\n"
                + "Tidspunkt:  " + dateTime(data.startsAt()) + "\n"
                + "Sæder:      " + seats(data.seats()) + "\n"
                + (total != null ? "Pris:       " + money(total, data.currency()) + "\n" : "")
                + "Bestilling: " + orDash(data.bookingId()) + "\n\n"
                + "Dine billetter med QR-kode kommer i en separat e-mail."
                + SIGNATURE;
        String sms = "BioFlix: Bestilling bekræftet – " + orDash(data.movieTitle()) + ", " + dateTime(data.startsAt())
                + ", " + seats(data.seats()) + ".";
        return new RenderedMessage("Din bestilling er bekræftet – " + orDash(data.movieTitle()), body, sms, List.of());
    }

    public RenderedMessage bookingCancelled(BookingCancelledData data, String fullName) {
        String reason = data.reason() == null ? null : CANCEL_REASONS.get(data.reason());
        String refund;
        if (Boolean.TRUE.equals(data.wasPaid())) {
            refund = "\nDu har betalt, så pengene bliver automatisk refunderet – du får en bekræftelse, når det er sket.";
        } else if (Boolean.FALSE.equals(data.wasPaid())) {
            refund = "\nDu er ikke blevet trukket for noget.";
        } else {
            refund = "\nHar du betalt, bliver pengene automatisk refunderet – du får en bekræftelse, når det er sket.";
        }
        String body = greeting(fullName) + "\n\n"
                + "Din bestilling " + orDash(data.bookingId()) + " er blevet annulleret"
                + (reason != null ? ", fordi " + reason : "") + ".\n"
                + (data.movieTitle() != null || data.startsAt() != null ? "\n" : "")
                + optionalLine("Film:      ", data.movieTitle())
                + optionalLine("Tidspunkt: ", data.startsAt())
                + refund
                + SIGNATURE;
        String sms = "BioFlix: Din bestilling " + orDash(data.bookingId()) + " er annulleret.";
        return new RenderedMessage("Din bestilling er annulleret", body, sms, List.of());
    }

    public RenderedMessage paymentCompleted(PaymentCompletedData data, String fullName) {
        return RenderedMessage.email(
                "Kvittering for din betaling" + (data.receiptNumber() != null ? " – " + data.receiptNumber() : ""),
                greeting(fullName) + "\n\n"
                        + "Vi har modtaget din betaling.\n\n"
                        + "Kvitteringsnr.: " + orDash(data.receiptNumber()) + "\n"
                        + "Beløb:          " + money(data.amount(), data.currency()) + "\n"
                        + "Bestilling:     " + orDash(data.bookingId()) + "\n"
                        + optionalLine("Kvittering:     ", data.receiptUrl())
                        + SIGNATURE);
    }

    public RenderedMessage paymentFailed(PaymentFailedData data, String fullName) {
        return RenderedMessage.email(
                "Din betaling gik ikke igennem",
                greeting(fullName) + "\n\n"
                        + "Vi kunne desværre ikke gennemføre betalingen for bestilling " + orDash(data.bookingId()) + ".\n"
                        + optionalLine("Årsag: ", data.reason())
                        + "\nDine sæder er frigivet. Du er velkommen til at prøve igen."
                        + SIGNATURE);
    }

    public RenderedMessage paymentRefunded(PaymentRefundedData data, String fullName) {
        return RenderedMessage.email(
                "Du har fået pengene tilbage",
                greeting(fullName) + "\n\n"
                        + "Vi har refunderet " + money(data.amount(), data.currency())
                        + " for bestilling " + orDash(data.bookingId()) + ".\n"
                        + "Det kan tage et par bankdage, før beløbet står på din konto."
                        + SIGNATURE);
    }

    public RenderedMessage ticketsIssued(TicketIssuedData data, String fullName) {
        List<TicketData> tickets = data.tickets() == null ? List.of() : data.tickets();

        StringBuilder body = new StringBuilder()
                .append(greeting(fullName)).append("\n\n")
                .append("Her er dine billetter. Vis QR-koden ved indgangen.\n\n")
                .append(optionalLine("Film:      ", data.movieTitle()))
                .append(optionalLine("Biograf:   ", data.cinemaName()))
                .append(optionalLine("Sal:       ", data.hallName()))
                .append(optionalLine("Tidspunkt: ", data.startsAt()))
                .append("Bestilling: ").append(orDash(data.bookingId())).append("\n\n");

        List<RenderedMessage.Attachment> attachments = new ArrayList<>();
        for (TicketData ticket : tickets) {
            body.append("Billet ").append(orDash(ticket.ticketNumber()))
                    .append(" – ").append(seat(ticket.row(), ticket.number())).append('\n');
            if (ticket.qrToken() == null || ticket.qrToken().isBlank()) {
                log.warn("Ticket {} has no qrToken – sending the e-mail without a QR code", ticket.ticketNumber());
                continue;
            }
            attachments.add(new RenderedMessage.Attachment(
                    "billet-" + orDash(ticket.ticketNumber()) + ".png", "image/png", qrCodeGenerator.png(ticket.qrToken())));
        }
        body.append(SIGNATURE);

        String subject = "Dine billetter" + (data.movieTitle() != null ? " – " + data.movieTitle() : "");
        return new RenderedMessage(subject, body.toString(), null, attachments);
    }

    private static String optionalLine(String label, Object value) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            return "";
        }
        String text = value instanceof Instant instant ? dateTime(instant) : value.toString();
        return label + text + "\n";
    }
}
