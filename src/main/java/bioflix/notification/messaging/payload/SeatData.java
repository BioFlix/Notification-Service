package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One seat, e.g. { "row": "F", "number": 7 }. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SeatData(
        String row,
        Integer number
) {
}
