package bioflix.notification.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * The common envelope every service puts around its events on cinema.events:
 *
 * <pre>
 * {
 *   "eventId":       "6b1f0c1e-2d0a-4c55-9a43-5d1b7f0e9a21",  // unique per event – used for idempotency
 *   "type":          "booking.confirmed",                     // same as the routing key
 *   "occurredAt":    "2026-11-11T18:42:10Z",
 *   "producer":      "booking-service",
 *   "correlationId": "d3c0...",                               // e.g. the bookingId, follows the whole saga
 *   "data":          { ... event specific ... }
 * }
 * </pre>
 *
 * Unknown fields are ignored, so producers can add fields without breaking us (additive-only versioning).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(
        UUID eventId,
        String type,
        Instant occurredAt,
        String producer,
        String correlationId,
        JsonNode data
) {
}
