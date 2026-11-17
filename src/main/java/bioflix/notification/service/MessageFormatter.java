package bioflix.notification.service;

import bioflix.notification.messaging.payload.SeatData;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class MessageFormatter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
            .ofPattern("EEEE 'd.' d. MMMM 'kl.' HH:mm", Locale.forLanguageTag("da-DK"))
            .withZone(ZoneId.of("Europe/Copenhagen"));

    private MessageFormatter() {
    }

    public static String money(Long amountMinor, String currency) {
        if (amountMinor == null) {
            return "-";
        }
        String sign = amountMinor < 0 ? "-" : "";
        long abs = Math.abs(amountMinor);
        return String.format("%s%d,%02d %s", sign, abs / 100, abs % 100, currency == null ? "DKK" : currency);
    }

    public static String dateTime(Instant instant) {
        return instant == null ? "-" : DATE_TIME.format(instant);
    }

    public static String seats(List<SeatData> seats) {
        if (seats == null || seats.isEmpty()) {
            return "-";
        }
        return String.join(", ", seats.stream().filter(Objects::nonNull)
                .map(s -> seat(s.row(), s.number())).toList());
    }

    public static String seat(String row, Integer number) {
        return "Række " + orDash(row) + " sæde " + (number == null ? "-" : number);
    }

    public static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    public static String greeting(String fullName) {
        return fullName == null || fullName.isBlank() ? "Hej," : "Hej " + fullName + ",";
    }
}
