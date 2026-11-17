package bioflix.notification.messaging.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One seat: { "row": "F", "number": 7, "type": "STANDARD", "price": 13500 }. price in øre. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SeatData(
        String row,
        Integer number,
        String type,
        Long price
) {
}
