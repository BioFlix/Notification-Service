package bioflix.notification.service;

import java.util.List;

/**
 * A notification ready to send.
 *
 * @param smsText short text for SMS, or null if this notification is e-mail only
 */
public record RenderedMessage(
        String subject,
        String body,
        String smsText,
        List<Attachment> attachments
) {
    public RenderedMessage {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    public static RenderedMessage email(String subject, String body) {
        return new RenderedMessage(subject, body, null, List.of());
    }

    public record Attachment(String filename, String contentType, byte[] content) {
    }
}
