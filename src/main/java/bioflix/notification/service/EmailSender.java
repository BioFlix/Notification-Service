package bioflix.notification.service;

import java.util.List;

public interface EmailSender {

    void send(String to, String subject, String body, List<RenderedMessage.Attachment> attachments);
}
