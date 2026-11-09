package bioflix.notification.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Small helpers for Danish-formatted e-mail text. */
public final class MessageFormatter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("EEEE 'd.' d. MMMM 'kl.' HH:mm", Locale.forLanguageTag("da-DK"))
            .withZone(ZoneId.of("Europe/Copenhagen"));

    private MessageFormatter() {
    }

    /** 12500, "DKK" -> "125,00 DKK" */
    public static String money(long amountMinor, String currency) {
        return String.format("%d,%02d %s", amountMinor / 100, Math.abs(amountMinor % 100), currency);
    }

    public static String dateTime(Instant instant) {
        return instant == null ? "-" : DATE_TIME.format(instant);
    }

    public static String seats(List<String> seats) {
        return seats == null ? "-" : String.join(", ", seats);
    }
}
