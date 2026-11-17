package bioflix.notification.service;

import java.util.List;

public interface EmailSender {

    /** @return the provider's message id (stored as provider_message_id), or null if unknown */
    String send(String to, String subject, String body, List<RenderedMessage.Attachment> attachments);
}
