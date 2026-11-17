package bioflix.notification.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
public class EventParser {

    private final JsonMapper jsonMapper;

    public EventParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public EventEnvelope parse(Message message) {
        EventEnvelope event;
        try {
            event = jsonMapper.readValue(message.getBody(), EventEnvelope.class);
        } catch (JacksonException ex) {
            throw new InvalidEventException("Message body is not a valid event envelope: " + ex.getOriginalMessage(), ex);
        }
        if (event == null || event.eventId() == null) {
            throw new InvalidEventException("Event has no eventId");
        }
        if (event.type() == null || event.type().isBlank()) {
            // Fall back to the routing key – they are supposed to be the same anyway
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            if (routingKey == null || routingKey.isBlank()) {
                throw new InvalidEventException("Event " + event.eventId() + " has no type");
            }
            event = new EventEnvelope(event.eventId(), routingKey, event.occurredAt(), event.producer(),
                    event.correlationId(), event.data());
        }
        return event;
    }

    public <T> T data(EventEnvelope event, Class<T> type) {
        if (event.data() == null || event.data().isNull()) {
            throw new InvalidEventException("Event " + event.eventId() + " (" + event.type() + ") has no data");
        }
        try {
            return jsonMapper.treeToValue(event.data(), type);
        } catch (JacksonException ex) {
            throw new InvalidEventException("Event " + event.eventId() + " (" + event.type() + ") has invalid data: "
                    + ex.getOriginalMessage(), ex);
        }
    }
}
