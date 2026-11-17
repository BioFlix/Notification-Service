package bioflix.notification.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * The common envelope on cinema.events:
 * { eventId, type, version, occurredAt, producer, correlationId, data }.
 * Unknown fields are ignored, so producers can add fields without breaking us.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(
        UUID eventId,
        String type,
        Integer version,
        Instant occurredAt,
        String producer,
        String correlationId,
        JsonNode data
) {
}
